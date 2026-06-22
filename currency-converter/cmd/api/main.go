package main

import (
	"context"
	"fmt"
	"log/slog"
	"net/http"
	"os"
	"os/signal"
	"strconv"
	"strings"
	"syscall"
	"time"

	"tripify/currency-converter/internal/api"
	"tripify/currency-converter/internal/domain"
	"tripify/currency-converter/internal/eureka"
	"tripify/currency-converter/internal/nbp"
)

func main() {
	// Setup logger
	logger := slog.New(slog.NewJSONHandler(os.Stdout, nil))
	slog.SetDefault(logger)

	// Config
	portStr := getEnv("PORT", "8083")
	port, _ := strconv.Atoi(portStr)
	eurekaURL := getEnv("EUREKA_CLIENT_SERVICEURL_DEFAULTZONE", "http://eureka-server:8761/eureka")
	nbpURL := getEnv("NBP_API_URL", "https://api.nbp.pl")
	refreshIntervalStr := getEnv("NBP_REFRESH_INTERVAL", "30m")
	refreshInterval, err := time.ParseDuration(refreshIntervalStr)
	if err != nil {
		refreshInterval = 30 * time.Minute
	}

	// NBP Client
	nbpClient := nbp.NewClient(nbpURL, 10*time.Second)

	// Domain Service
	converterService := domain.NewConverterService(nbpClient, logger)

	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	// Start converter service background tasks
	if err := converterService.Start(ctx, refreshInterval); err != nil {
		logger.Error("Failed to start converter service", "error", err)
		os.Exit(1)
	}

	// API Handler
	handler := api.NewHandler(converterService)
	mux := http.NewServeMux()
	handler.RegisterRoutes(mux)

	// Add CORS middleware
	corsMux := corsMiddleware(mux)

	// Eureka Client
	eurekaClient := eureka.NewClient(eurekaURL, "CURRENCY-CONVERTER", port, logger)
	eurekaClient.Start(ctx)

	// HTTP Server
	srv := &http.Server{
		Addr:    fmt.Sprintf(":%d", port),
		Handler: corsMux,
	}

	go func() {
		logger.Info("Starting HTTP server", "port", port)
		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			logger.Error("HTTP server failed", "error", err)
			os.Exit(1)
		}
	}()

	// Graceful shutdown
	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
	<-quit

	logger.Info("Shutting down server...")
	cancel()

	shutdownCtx, shutdownCancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer shutdownCancel()

	if err := srv.Shutdown(shutdownCtx); err != nil {
		logger.Error("Server forced to shutdown", "error", err)
	}

	logger.Info("Server exiting")
}

func getEnv(key, fallback string) string {
	if value, exists := os.LookupEnv(key); exists {
		return value
	}
	return fallback
}

func corsMiddleware(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		origin := r.Header.Get("Origin")
		if origin != "" && strings.HasPrefix(origin, "http://localhost") {
			w.Header().Set("Access-Control-Allow-Origin", origin)
			w.Header().Set("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
			w.Header().Set("Access-Control-Allow-Headers", "Content-Type, Authorization")
			w.Header().Set("Access-Control-Allow-Credentials", "true")
		}

		if r.Method == "OPTIONS" {
			w.WriteHeader(http.StatusOK)
			return
		}

		next.ServeHTTP(w, r)
	})
}

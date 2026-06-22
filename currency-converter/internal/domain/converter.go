package domain

import (
	"context"
	"fmt"
	"log/slog"
	"sync"
	"time"

	"tripify/currency-converter/internal/nbp"
)

type CurrencyRate struct {
	Code     string  `json:"code"`
	Currency string  `json:"currency"`
	Rate     float64 `json:"rate"`
}

type RatesSnapshot struct {
	EffectiveDate string                  `json:"effectiveDate"`
	Base          string                  `json:"base"`
	Rates         map[string]CurrencyRate `json:"rates"`
	List          []CurrencyRate          `json:"list"`
}

type ConvertRequest struct {
	From   string  `json:"from"`
	To     string  `json:"to"`
	Amount float64 `json:"amount"`
}

type ConvertResponse struct {
	From         string  `json:"from"`
	To           string  `json:"to"`
	Amount       float64 `json:"amount"`
	ExchangeRate float64 `json:"exchangeRate"`
	Result       float64 `json:"result"`
}

type NBPClient interface {
	FetchTableA(ctx context.Context) (*nbp.TableAResponse, error)
}

type ConverterService struct {
	nbpClient     NBPClient
	mu            sync.RWMutex
	cache         *RatesSnapshot
	refreshTicker *time.Ticker
	logger        *slog.Logger
}

func NewConverterService(client NBPClient, logger *slog.Logger) *ConverterService {
	return &ConverterService{
		nbpClient: client,
		logger:    logger,
	}
}

func (s *ConverterService) Start(ctx context.Context, refreshInterval time.Duration) error {
	s.logger.Info("Starting converter service, fetching initial data...")
	if err := s.refreshCache(ctx); err != nil {
		return fmt.Errorf("failed initial cache refresh: %w", err)
	}

	s.refreshTicker = time.NewTicker(refreshInterval)
	go func() {
		for {
			select {
			case <-ctx.Done():
				s.refreshTicker.Stop()
				s.logger.Info("Stopping converter service background refresh")
				return
			case <-s.refreshTicker.C:
				if err := s.refreshCache(context.Background()); err != nil {
					s.logger.Warn("Failed to refresh NBP cache, keeping old data", "error", err)
				}
			}
		}
	}()

	return nil
}

func (s *ConverterService) refreshCache(ctx context.Context) error {
	table, err := s.nbpClient.FetchTableA(ctx)
	if err != nil {
		return err
	}

	ratesMap := make(map[string]CurrencyRate)
	var ratesList []CurrencyRate

	// Add base PLN
	pln := CurrencyRate{Code: "PLN", Currency: "złoty", Rate: 1.0}
	ratesMap["PLN"] = pln
	ratesList = append(ratesList, pln)

	for _, r := range table.Rates {
		cr := CurrencyRate{Code: r.Code, Currency: r.Currency, Rate: r.Mid}
		ratesMap[r.Code] = cr
		ratesList = append(ratesList, cr)
	}

	s.mu.Lock()
	defer s.mu.Unlock()

	s.cache = &RatesSnapshot{
		EffectiveDate: table.EffectiveDate,
		Base:          "PLN",
		Rates:         ratesMap,
		List:          ratesList,
	}
	s.logger.Info("NBP Cache refreshed successfully", "effectiveDate", table.EffectiveDate, "currenciesCount", len(ratesList))
	return nil
}

func (s *ConverterService) GetSnapshot() (*RatesSnapshot, error) {
	s.mu.RLock()
	defer s.mu.RUnlock()

	if s.cache == nil {
		return nil, fmt.Errorf("cache not initialized")
	}
	return s.cache, nil
}

func (s *ConverterService) Convert(req ConvertRequest) (*ConvertResponse, error) {
	s.mu.RLock()
	defer s.mu.RUnlock()

	if s.cache == nil {
		return nil, fmt.Errorf("service unavailable: cache empty")
	}

	if req.Amount <= 0 {
		return nil, fmt.Errorf("amount must be greater than 0")
	}

	fromRate, ok := s.cache.Rates[req.From]
	if !ok {
		return nil, fmt.Errorf("unsupported currency: %s", req.From)
	}

	toRate, ok := s.cache.Rates[req.To]
	if !ok {
		return nil, fmt.Errorf("unsupported currency: %s", req.To)
	}

	plnAmount := req.Amount * fromRate.Rate
	result := plnAmount / toRate.Rate
	exchangeRate := fromRate.Rate / toRate.Rate

	return &ConvertResponse{
		From:         req.From,
		To:           req.To,
		Amount:       req.Amount,
		ExchangeRate: exchangeRate,
		Result:       result,
	}, nil
}

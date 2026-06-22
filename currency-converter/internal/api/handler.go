package api

import (
	"encoding/json"
	"net/http"
	"strings"
	"time"

	"tripify/currency-converter/internal/domain"
)

type ErrorResponse struct {
	Timestamp string `json:"timestamp"`
	Status    int    `json:"status"`
	Error     string `json:"error"`
	Message   string `json:"message,omitempty"`
	Path      string `json:"path"`
}

type Handler struct {
	service *domain.ConverterService
}

func NewHandler(service *domain.ConverterService) *Handler {
	return &Handler{service: service}
}

func (h *Handler) RegisterRoutes(mux *http.ServeMux) {
	mux.HandleFunc("GET /health", h.handleHealth)
	mux.HandleFunc("GET /api/v1/rates", h.handleRates)
	mux.HandleFunc("GET /api/v1/rates/", h.handleRateByCode) // For Go 1.22 we can use GET /api/v1/rates/{code} but trailing slash is compatible
	mux.HandleFunc("GET /api/v1/currencies", h.handleCurrencies)
	mux.HandleFunc("POST /api/v1/convert", h.handleConvert)
}

func (h *Handler) handleHealth(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	w.Write([]byte(`{"status": "UP"}`))
}

func (h *Handler) handleRates(w http.ResponseWriter, r *http.Request) {
	snapshot, err := h.service.GetSnapshot()
	if err != nil {
		h.writeError(w, r, http.StatusServiceUnavailable, err.Error())
		return
	}

	h.writeJSON(w, http.StatusOK, snapshot)
}

func (h *Handler) handleRateByCode(w http.ResponseWriter, r *http.Request) {
	parts := strings.Split(r.URL.Path, "/")
	code := strings.ToUpper(parts[len(parts)-1])
	if code == "" || code == "RATES" {
		h.writeError(w, r, http.StatusBadRequest, "Missing currency code")
		return
	}

	snapshot, err := h.service.GetSnapshot()
	if err != nil {
		h.writeError(w, r, http.StatusServiceUnavailable, err.Error())
		return
	}

	rate, ok := snapshot.Rates[code]
	if !ok {
		h.writeError(w, r, http.StatusNotFound, "Currency not found")
		return
	}

	resp := map[string]interface{}{
		"currency":      rate.Currency,
		"code":          rate.Code,
		"rate":          rate.Rate,
		"effectiveDate": snapshot.EffectiveDate,
	}
	h.writeJSON(w, http.StatusOK, resp)
}

func (h *Handler) handleCurrencies(w http.ResponseWriter, r *http.Request) {
	snapshot, err := h.service.GetSnapshot()
	if err != nil {
		h.writeError(w, r, http.StatusServiceUnavailable, err.Error())
		return
	}

	codes := make([]string, 0, len(snapshot.List))
	for _, cr := range snapshot.List {
		codes = append(codes, cr.Code)
	}

	h.writeJSON(w, http.StatusOK, codes)
}

func (h *Handler) handleConvert(w http.ResponseWriter, r *http.Request) {
	var req domain.ConvertRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		h.writeError(w, r, http.StatusBadRequest, "Invalid JSON payload")
		return
	}
	
	req.From = strings.ToUpper(req.From)
	req.To = strings.ToUpper(req.To)

	if req.From == "" || req.To == "" {
		h.writeError(w, r, http.StatusBadRequest, "Missing 'from' or 'to' currency")
		return
	}

	resp, err := h.service.Convert(req)
	if err != nil {
		h.writeError(w, r, http.StatusBadRequest, err.Error())
		return
	}

	h.writeJSON(w, http.StatusOK, resp)
}

func (h *Handler) writeJSON(w http.ResponseWriter, status int, data interface{}) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	json.NewEncoder(w).Encode(data)
}

func (h *Handler) writeError(w http.ResponseWriter, r *http.Request, status int, message string) {
	errResp := ErrorResponse{
		Timestamp: time.Now().UTC().Format(time.RFC3339),
		Status:    status,
		Error:     http.StatusText(status),
		Message:   message,
		Path:      r.URL.Path,
	}

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	json.NewEncoder(w).Encode(errResp)
}

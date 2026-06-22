package domain

import (
	"context"
	"log/slog"
	"os"
	"testing"
	"time"

	"tripify/currency-converter/internal/nbp"
)

type MockNBPClient struct {
	Response *nbp.TableAResponse
	Err      error
}

func (m *MockNBPClient) FetchTableA(ctx context.Context) (*nbp.TableAResponse, error) {
	return m.Response, m.Err
}

func TestConvert(t *testing.T) {
	logger := slog.New(slog.NewJSONHandler(os.Stdout, nil))
	mockClient := &MockNBPClient{
		Response: &nbp.TableAResponse{
			EffectiveDate: "2025-01-01",
			Rates: []nbp.Rate{
				{Code: "EUR", Currency: "euro", Mid: 4.28},
				{Code: "USD", Currency: "dolar", Mid: 3.91},
			},
		},
	}

	service := NewConverterService(mockClient, logger)
	ctx := context.Background()

	// Initialize cache
	err := service.Start(ctx, 1*time.Hour)
	if err != nil {
		t.Fatalf("Failed to start service: %v", err)
	}

	tests := []struct {
		name          string
		req           ConvertRequest
		expectedError bool
		expectedRate  float64
	}{
		{
			name:          "EUR to USD",
			req:           ConvertRequest{From: "EUR", To: "USD", Amount: 100},
			expectedError: false,
			expectedRate:  4.28 / 3.91,
		},
		{
			name:          "USD to PLN",
			req:           ConvertRequest{From: "USD", To: "PLN", Amount: 50},
			expectedError: false,
			expectedRate:  3.91 / 1.0,
		},
		{
			name:          "Invalid Amount",
			req:           ConvertRequest{From: "EUR", To: "USD", Amount: 0},
			expectedError: true,
		},
		{
			name:          "Unknown Currency",
			req:           ConvertRequest{From: "XYZ", To: "USD", Amount: 100},
			expectedError: true,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			resp, err := service.Convert(tt.req)
			if tt.expectedError && err == nil {
				t.Errorf("Expected error but got nil")
			}
			if !tt.expectedError && err != nil {
				t.Errorf("Unexpected error: %v", err)
			}
			if !tt.expectedError && resp.ExchangeRate != tt.expectedRate {
				t.Errorf("Expected rate %v, got %v", tt.expectedRate, resp.ExchangeRate)
			}
		})
	}
}

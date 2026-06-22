package nbp

import (
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"time"
)

type Rate struct {
	Currency string  `json:"currency"`
	Code     string  `json:"code"`
	Mid      float64 `json:"mid"`
}

type TableAResponse struct {
	Table         string `json:"table"`
	No            string `json:"no"`
	EffectiveDate string `json:"effectiveDate"`
	Rates         []Rate `json:"rates"`
}

type Client struct {
	baseURL    string
	httpClient *http.Client
}

func NewClient(baseURL string, timeout time.Duration) *Client {
	if baseURL == "" {
		baseURL = "https://api.nbp.pl"
	}
	return &Client{
		baseURL:    baseURL,
		httpClient: &http.Client{Timeout: timeout},
	}
}

func (c *Client) FetchTableA(ctx context.Context) (*TableAResponse, error) {
	url := fmt.Sprintf("%s/api/exchangerates/tables/A/?format=json", c.baseURL)
	req, err := http.NewRequestWithContext(ctx, http.MethodGet, url, nil)
	if err != nil {
		return nil, fmt.Errorf("failed to create request: %w", err)
	}

	req.Header.Set("Accept", "application/json")
	req.Header.Set("User-Agent", "Tripify-CurrencyConverter/1.0 (Integration/Testing)")

	resp, err := c.httpClient.Do(req)
	if err != nil {
		return nil, fmt.Errorf("failed to execute request: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("unexpected status code from NBP API: %d", resp.StatusCode)
	}

	var tables []TableAResponse
	if err := json.NewDecoder(resp.Body).Decode(&tables); err != nil {
		return nil, fmt.Errorf("failed to decode response: %w", err)
	}

	if len(tables) == 0 {
		return nil, fmt.Errorf("empty response from NBP API")
	}

	return &tables[0], nil
}

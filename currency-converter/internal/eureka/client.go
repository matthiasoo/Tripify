package eureka

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"log/slog"
	"net"
	"net/http"
	"os"
	"time"
)

type InstanceInfo struct {
	Instance Instance `json:"instance"`
}

type Instance struct {
	HostName         string     `json:"hostName"`
	App              string     `json:"app"`
	VipAddress       string     `json:"vipAddress"`
	SecureVipAddress string     `json:"secureVipAddress"`
	IpAddr           string     `json:"ipAddr"`
	Status           string     `json:"status"`
	Port             Port       `json:"port"`
	SecurePort       Port       `json:"securePort"`
	HealthCheckUrl   string     `json:"healthCheckUrl"`
	StatusPageUrl    string     `json:"statusPageUrl"`
	HomePageUrl      string     `json:"homePageUrl"`
	DataCenterInfo   DataCenter `json:"dataCenterInfo"`
}

type Port struct {
	Port    int    `json:"$"`
	Enabled string `json:"@enabled"`
}

type DataCenter struct {
	Class string `json:"@class"`
	Name  string `json:"name"`
}

type Client struct {
	eurekaURL string
	app       string
	port      int
	ipAddr    string
	logger    *slog.Logger
	http      *http.Client
}

func NewClient(eurekaURL, app string, port int, logger *slog.Logger) *Client {
	ip := getLocalIP()
	return &Client{
		eurekaURL: eurekaURL,
		app:       app,
		port:      port,
		ipAddr:    ip,
		logger:    logger,
		http:      &http.Client{Timeout: 5 * time.Second},
	}
}

func (c *Client) Start(ctx context.Context) {
	instanceID := fmt.Sprintf("%s:%s:%d", c.ipAddr, c.app, c.port)

	instance := InstanceInfo{
		Instance: Instance{
			HostName:         c.ipAddr, // Use IP as hostname for docker networks
			App:              c.app,
			VipAddress:       c.app,
			SecureVipAddress: c.app,
			IpAddr:           c.ipAddr,
			Status:           "UP",
			Port:             Port{Port: c.port, Enabled: "true"},
			SecurePort:       Port{Port: 443, Enabled: "false"},
			HealthCheckUrl:   fmt.Sprintf("http://%s:%d/health", c.ipAddr, c.port),
			StatusPageUrl:    fmt.Sprintf("http://%s:%d/health", c.ipAddr, c.port),
			HomePageUrl:      fmt.Sprintf("http://%s:%d/", c.ipAddr, c.port),
			DataCenterInfo: DataCenter{
				Class: "com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo",
				Name:  "MyOwn",
			},
		},
	}

	// Register
	c.register(instance)

	// Heartbeat loop
	ticker := time.NewTicker(30 * time.Second)
	go func() {
		for {
			select {
			case <-ctx.Done():
				c.deregister(c.app, instanceID)
				return
			case <-ticker.C:
				c.heartbeat(c.app, instanceID, instance)
			}
		}
	}()
}

func (c *Client) register(instance InstanceInfo) {
	url := fmt.Sprintf("%s/apps/%s", c.eurekaURL, c.app)
	body, _ := json.Marshal(instance)

	req, _ := http.NewRequest(http.MethodPost, url, bytes.NewBuffer(body))
	req.Header.Set("Content-Type", "application/json")

	resp, err := c.http.Do(req)
	if err != nil {
		c.logger.Error("Failed to register with Eureka", "error", err)
		return
	}
	defer resp.Body.Close()

	if resp.StatusCode == 204 || resp.StatusCode == 200 {
		c.logger.Info("Registered successfully with Eureka")
	} else {
		bodyBytes, _ := io.ReadAll(resp.Body)
		c.logger.Error("Failed to register with Eureka", "status", resp.StatusCode, "response", string(bodyBytes))
	}
}

func (c *Client) heartbeat(app, instanceID string, instance InstanceInfo) {
	url := fmt.Sprintf("%s/apps/%s/%s", c.eurekaURL, app, instanceID)
	req, _ := http.NewRequest(http.MethodPut, url, nil)

	resp, err := c.http.Do(req)
	if err != nil {
		c.logger.Error("Eureka heartbeat failed", "error", err)
		return
	}
	defer resp.Body.Close()

	if resp.StatusCode == 404 {
		c.logger.Warn("Instance not found in Eureka, re-registering...")
		c.register(instance)
	} else if resp.StatusCode != 200 {
		c.logger.Error("Eureka heartbeat returned unexpected status", "status", resp.StatusCode)
	}
}

func (c *Client) deregister(app, instanceID string) {
	url := fmt.Sprintf("%s/apps/%s/%s", c.eurekaURL, app, instanceID)
	req, _ := http.NewRequest(http.MethodDelete, url, nil)

	resp, err := c.http.Do(req)
	if err == nil {
		defer resp.Body.Close()
		c.logger.Info("Deregistered from Eureka")
	}
}

func getLocalIP() string {
	addrs, err := net.InterfaceAddrs()
	if err != nil {
		return "127.0.0.1"
	}
	for _, address := range addrs {
		if ipnet, ok := address.(*net.IPNet); ok && !ipnet.IP.IsLoopback() {
			if ipnet.IP.To4() != nil {
				return ipnet.IP.String()
			}
		}
	}
	hostname, _ := os.Hostname()
	return hostname
}

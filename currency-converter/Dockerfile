FROM golang:1.22-alpine AS builder

WORKDIR /app

# Copy go mod and download dependencies
COPY go.mod ./
RUN go mod download

# Copy source code
COPY . .

# Build the application
RUN CGO_ENABLED=0 GOOS=linux go build -o /app/currency-converter ./cmd/api

# Final minimal image
FROM alpine:latest

# Install CA certificates for HTTPS requests to NBP
RUN apk --no-cache add ca-certificates tzdata

WORKDIR /app

# Copy binary from builder
COPY --from=builder /app/currency-converter .

# Expose port
EXPOSE 8083

# Run the binary
CMD ["./currency-converter"]

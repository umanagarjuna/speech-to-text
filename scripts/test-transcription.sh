#!/bin/bash

# Example script to test the Speech-to-Text service
# Usage: ./test-transcription.sh [SERVICE_URL] [AUDIO_FILE]

set -e

# Default values
SERVICE_URL="${1:-http://localhost:8080/api/v1/transcribe}"
AUDIO_FILE="${2:-sample-audio.wav}"

echo "========================================"
echo "Speech-to-Text Service Test Script"
echo "========================================"
echo ""

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Function to print colored output
print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

# Test 1: Health Check
echo "Test 1: Health Check"
print_info "Checking service health..."

HEALTH_RESPONSE=$(curl -s -w "\n%{http_code}" "${SERVICE_URL}/health")
HTTP_CODE=$(echo "$HEALTH_RESPONSE" | tail -n1)
BODY=$(echo "$HEALTH_RESPONSE" | head -n-1)

if [ "$HTTP_CODE" -eq 200 ]; then
    print_success "Service is healthy"
    echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"
else
    print_error "Service health check failed (HTTP $HTTP_CODE)"
    echo "$BODY"
    exit 1
fi

echo ""

# Test 2: Transcription
echo "Test 2: Audio Transcription"

if [ ! -f "$AUDIO_FILE" ]; then
    print_error "Audio file not found: $AUDIO_FILE"
    print_info "Creating a sample WAV file for testing..."
    
    # Create a simple WAV file with silence (for testing purposes)
    # In real usage, you would use an actual audio file
    ./create-sample-audio.sh "$AUDIO_FILE" 2>/dev/null || {
        print_error "Could not create sample audio file"
        print_info "Please provide a valid audio file as the second argument"
        print_info "Usage: $0 [SERVICE_URL] [AUDIO_FILE]"
        exit 1
    }
fi

print_info "Transcribing: $AUDIO_FILE"
echo ""

TRANSCRIPTION_RESPONSE=$(curl -s -w "\n%{http_code}" \
    -F "file=@${AUDIO_FILE}" \
    "${SERVICE_URL}")

HTTP_CODE=$(echo "$TRANSCRIPTION_RESPONSE" | tail -n1)
BODY=$(echo "$TRANSCRIPTION_RESPONSE" | head -n-1)

if [ "$HTTP_CODE" -eq 200 ]; then
    print_success "Transcription successful"
    echo ""
    echo "Response:"
    echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"
    echo ""
    
    # Extract and display just the transcript
    TRANSCRIPT=$(echo "$BODY" | jq -r '.transcript' 2>/dev/null)
    if [ -n "$TRANSCRIPT" ] && [ "$TRANSCRIPT" != "null" ]; then
        echo "----------------------------------------"
        echo "Transcript:"
        echo "----------------------------------------"
        echo "$TRANSCRIPT"
        echo "----------------------------------------"
    fi
else
    print_error "Transcription failed (HTTP $HTTP_CODE)"
    echo "$BODY"
    exit 1
fi

echo ""
print_success "All tests passed!"
echo ""

#!/bin/bash

# Script to create a simple test WAV file for development/testing
# This creates a silent WAV file with proper headers

OUTPUT_FILE="${1:-sample-audio.wav}"
DURATION_SECONDS="${2:-5}"

echo "Creating test WAV file: $OUTPUT_FILE (${DURATION_SECONDS}s)"

# Check if ffmpeg is available (better option)
if command -v ffmpeg &> /dev/null; then
    echo "Using ffmpeg to create test audio..."
    ffmpeg -f lavfi -i "sine=frequency=1000:duration=${DURATION_SECONDS}" \
           -ar 16000 -ac 1 -y "$OUTPUT_FILE" 2>/dev/null
    echo "✓ Created $OUTPUT_FILE using ffmpeg"
    exit 0
fi

# Check if sox is available
if command -v sox &> /dev/null; then
    echo "Using sox to create test audio..."
    sox -n -r 16000 -c 1 -b 16 "$OUTPUT_FILE" trim 0.0 "${DURATION_SECONDS}" 2>/dev/null
    echo "✓ Created $OUTPUT_FILE using sox"
    exit 0
fi

# Fallback: Create a minimal WAV file using Python
if command -v python3 &> /dev/null; then
    echo "Using Python to create test audio..."
    python3 << EOF
import wave
import struct

# WAV file parameters
sample_rate = 16000
duration = ${DURATION_SECONDS}
num_samples = sample_rate * duration

# Create WAV file
with wave.open("$OUTPUT_FILE", 'w') as wav_file:
    wav_file.setnchannels(1)  # Mono
    wav_file.setsampwidth(2)  # 16-bit
    wav_file.setframerate(sample_rate)
    
    # Write silent audio (all zeros)
    for _ in range(num_samples):
        wav_file.writeframes(struct.pack('<h', 0))

print("✓ Created $OUTPUT_FILE using Python")
EOF
    exit 0
fi

echo "Error: No suitable tool found to create WAV file"
echo "Please install one of: ffmpeg, sox, or python3"
echo ""
echo "Installation:"
echo "  Ubuntu/Debian: sudo apt-get install ffmpeg"
echo "  macOS:         brew install ffmpeg"
exit 1

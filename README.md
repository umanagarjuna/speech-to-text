# Speech-to-Text Service

A production-ready, open-source speech-to-text transcription service built with Java Spring Boot and Vosk. This service provides a REST API for converting audio files to text without relying on paid external services.

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Features](#features)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Local Development](#local-development)
- [Running Tests](#running-tests)
- [Deployment](#deployment)
- [API Documentation](#api-documentation)
- [Usage Examples](#usage-examples)
- [CI/CD Pipeline](#cicd-pipeline)
- [Configuration](#configuration)
- [Trade-offs and Future Improvements](#trade-offs-and-future-improvements)
- [Troubleshooting](#troubleshooting)

## Architecture Overview

This service is built using a layered architecture:

```
                REST API Layer                  
           (TranscriptionController) 
                      |
                Service Layer                    
            (TranscriptionService)        
                      |
            Audio Conversion (FFmpeg)  
                      |
               Vosk Integration                  
          (Model Loading & Recognition)   
                      |
          Configuration & Error Handling  

```

**Key Components:**
- **Spring Boot 3.2**: Modern Java framework with embedded Tomcat
- **Vosk 0.3.45**: Open-source speech recognition library
- **FFmpeg**: External binary for audio conversion
- **Maven**: Dependency management and build tool
- **Docker**: Containerization for consistent deployment
- **GitHub Actions**: CI/CD automation
- **GitHub Container Registry (GHCR)**: Artifact storage

**Design Decisions:**
- Synchronous API for simplicity 
- All uploaded audio is converted via FFmpeg to a 16kHz mono WAV file before processing.
- Stateless service for horizontal scalability
- Health checks for container orchestration readiness

## Features

- ✅ RESTful API for audio transcription
- ✅ Support for multiple audio formats (WAV, MP3, FLAC, OGG, M4A)
- ✅ Comprehensive error handling and validation
- ✅ Health check endpoint for monitoring
- ✅ Docker containerization
- ✅ CI/CD pipeline with GitHub Actions
- ✅ Detailed logging
- ✅ Unit and integration tests


## 📦 Prerequisites

### For Local Development:
- **Java 17** or higher
- **Maven 3.9+**
- **Git**
- **FFmpeg:** Must be installed and available in your system's PATH.

### For Docker Deployment:
- **Docker 20.10+**
- **Docker Compose 2.0+**
- **Access to GitHub Container Registry (if image is private)**

### For Production Deployment:
- **Ubuntu Server 20.04+** (or any Linux distribution)
- **SSH access with sudo privileges** (if deploying to remote server)

## Quick Start

### Option 1: Using Docker (Recommended)

```bash
# Clone the repository
git clone <repository-url>
cd speech-to-text

# Build and run with Docker Compose
docker-compose up --build -d

# Check logs
docker-compose logs -f

# Verify health
curl http://localhost/api/v1/transcribe/health
```

The service will be available at `http://localhost`

### Option 2: Running Locally with Maven

***Linux / macOS***
```bash
# Clone the repository
git clone <repository-url>
cd speech-to-text

# Download the Vosk model (one-time setup)
mkdir -p models
cd models
wget https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip
unzip vosk-model-small-en-us-0.15.zip
cd ..

# Build the application
mvn clean package

# Run the application
java -jar target/speech-to-text-1.0.0.jar --transcription.model-path=./models/vosk-model-small-en-us-0.15

# Verify health
curl http://localhost:8080/api/v1/transcribe/health

The service will be available at `http://localhost:8080`
```

***Windows (PowerShell)***
```bash
# Clone the repository
git clone <repository-url>
cd speech-to-text

# Download the Vosk model (one-time setup)
mkdir models
cd models
Invoke-WebRequest -Uri https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip -OutFile "vosk-model-small-en-us-0.15.zip"
Expand-Archive -Path "vosk-model-small-en-us-0.15.zip" -DestinationPath .
cd ..

# Build the application
mvn clean package

# Run the application
java -jar target\speech-to-text-1.0.0.jar --transcription.model-path=.\models\vosk-model-small-en-us-0.15

# Verify health
curl http://localhost:8080/api/v1/transcribe/health

The service will be available at `http://localhost:8080`
```


## Local Development

### 1. Clone and Setup

```bash
git clone <repository-url>
cd speech-to-text
```

### 2. Configure Environment

Create a `.env` file (optional):

```env
MODEL_PATH=/path/to/vosk-model
SERVER_PORT=8080
MAX_FILE_SIZE=100MB
MAX_DURATION_SECONDS=600
```

### 3. Build the Project

```bash
mvn clean install
```

### 4. Run in Development Mode

```bash
mvn spring-boot:run
```

The service will start on `http://localhost:8080`

## 🧪 Running Tests

### Run All Tests

```bash
mvn test
```

### Run Specific Test Class

```bash
mvn test -Dtest=TranscriptionServiceTest
```

### Run Tests with Coverage

```bash
mvn verify
```

Coverage reports will be generated in `target/site/jacoco/index.html`

### Test Structure

- **Unit Tests**: `src/test/java/.../service/*Test.java`
  - Test business logic in isolation
  - Mock dependencies
  
- **Integration Tests**: `src/test/java/.../controller/*Test.java`
  - Test REST endpoints with MockMvc
  - Verify request/response handling

## Deployment

### Automated Deployment (CI/CD)

The primary deployment method is via the GitHub Actions pipeline.

**Setup GitHub Secrets:**

| Secret | Description |
|--------|-------------|
| `SSH_PRIVATE_KEY` | Private SSH key for the target server |
| `SERVER_HOST` | Target server IP address |
| `SERVER_USER` | SSH username (e.g., ubuntu) |

**Trigger Deployment:**
1. Push changes to the `main` branch.
2. The pipeline will build the image, push it to GHCR, and deploy it to the server.

### Deployment using scripts/deploy.sh in project repo

#### Use Git Bash (Recommended)

If you have Git installed, you likely have Git Bash. This is the easiest method because it lets you run the script exactly as intended.

1. Right-click inside your project folder and select "Open Git Bash Here".

2. Run the command:

```bash
sh scripts/deploy.sh 
```

(Example: `sh scripts/deploy.sh 18.206.238.39`)

#### Step 1: Load your SSH Key

You need to tell your current terminal session which secret key to use. Run these two commands:

**1. Start the SSH Agent:**

```bash
eval "$(ssh-agent -s)"
```

(This starts a background process to hold your keys.)

**2. Add your specific key:**  
Replace `zoominfo_key` with the actual name of your key file (e.g., `.pem` file).

```bash
ssh-add /c/Users/User/.ssh/zoominfo_key
```

(Note: In Git Bash, `C:\Users\User` is written as `/c/Users/User`)

#### Step 2: Run the Script Again

Now that the key is loaded in memory, the script will automatically use it without needing any changes to the code.

```bash
sh scripts/deploy.sh 18.206.238.39
```

### End-to-End Verification (On Server)

Since external access to the server might be restricted by firewalls, the most reliable way to verify the deployment is to run a test directly on the server instance.

#### 1. Connect to the Server

Run this command from your local machine (Windows/Mac) to log in:

```bash
# Replace path with the location of your private key
ssh -i "C:\Users\User\.ssh\zoominfo_key" ubuntu@18.206.238.39
```

#### 2. Download a Sample Audio File

Once logged in, download a verified working RAW audio file to test the service.

```bash
wget https://www.voiptroubleshooter.com/open_speech/american/OSR_us_000_0010_8k.wav -O test.wav
```

#### 3. Run the Transcription Request

Send the audio file to the local API endpoint.

```bash
curl -X POST -F "file=@test.wav" http://localhost:8080/api/v1/transcribe
```

**Expected Output:**

```json
{
  "transcript": "the birch canoe slid on the smooth planks glue the sheet to the dark blue background ...",
  "processingTimeMs": 1250,
  "filename": "test.wav",
  "timestamp": "..."
}
```
### Setting up Nginx as a Reverse Proxy

If Port 8080 is blocked by the cloud provider's firewall but Port 80 is open, you can use Nginx to route traffic.

**Install Nginx:**

```bash
sudo apt-get update
sudo apt-get install -y nginx
```

**Configure Nginx:**  
Edit the default configuration - Open the Config File:

```bash
sudo nano /etc/nginx/sites-available/default
```

Replace the content with this clean, verified configuration: 
(Delete everything in the file and paste this exact block to avoid syntax errors).

```nginx
    server {
        # Listen on port 80 (HTTP)
        listen 80 default_server;
        listen [::]:80 default_server;
    
        root /var/www/html;
        index index.html index.htm index.nginx-debian.html;
    
        server_name _;
    
        # Proxy configuration for your Java app
        location / {
            proxy_pass http://localhost:8080;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    
            # Increase upload limit for audio files
            client_max_body_size 100M;
        }
    }
```
**Save and Exit the file -**

**Test Configuration Syntax: Run this command to ensure there are no errors before reloading:**
```bash
sudo nginx -t
```
**Increase Global Upload Limit:**  
Edit the main config:

```bash
sudo nano /etc/nginx/nginx.conf
```

Add this inside the `http { ... }` block:

```nginx
client_max_body_size 100M;
```

**Restart Nginx:**

```bash
sudo systemctl restart nginx
```

Now you can access the API via Port 80: `http://<SERVER_IP>/api/v1/transcribe`
curl http://localhost:8080/api/v1/transcribe/health
curl http://localhost/api/v1/transcribe/health
curl http://18.206.238.39/api/v1/transcribe/health

## How to Test the Deployment

This guide explains how to validate the deployed Speech-to-Text service using the automated test script included in this repository.

### 1. Prerequisites

Before running the test, ensure you have the following installed on your local machine:

* **Bash Terminal:**
    * Windows: Use Git Bash (included with Git).
    * Mac/Linux: Use the standard Terminal.
* **curl**: For making HTTP requests (usually pre-installed).
* **jq**: (Optional but recommended) For pretty-printing the JSON response.
* **ffmpeg, sox, or python3**: Required only if you want the script to auto-generate a sample audio file for you.
* **Error: No suitable tool found to create WAV file. Please install one of: ffmpeg, sox, or python3**
* **PowerShell**
    * ```winget install Gyan.FFmpeg```

### 2. Setup Permissions

If you haven't already, make the scripts executable from your project root directory:

```bash
chmod +x scripts/test-transcription.sh
chmod +x scripts/create-sample-audio.sh
```

### 3. Run the Test

Run the following command to test the live service deployed at your IP address.

**Command:**

```bash
./scripts/test-transcription.sh http://18.206.238.39/api/v1/transcribe
```

**What this script does:**

1. **Health Check**: It first pings the `/health` endpoint to ensure the service is `UP`.
2. **Audio Generation**: If no audio file is provided, it automatically calls `./scripts/create-sample-audio.sh` to generate a 5-second silent WAV file named `sample-audio.wav`.
3. **Transcription**: It POSTs this audio file to the `http://18.206.238.39/api/v1/transcribe` endpoint.
4. **Validation**: It prints the HTTP status code and the JSON response containing the transcript.

### 4. Expected Output

If the deployment is successful, you should see output similar to this:

```plaintext
========================================
Speech-to-Text Service Test Script
========================================

Test 1: Health Check
ℹ Checking service health...
✓ Service is healthy
{
  "supportedFormats" : [ "mp3", "wav", "flac", "ogg", "m4a" ],
  "service" : "speech-to-text",
  "ready" : true,
  "status" : "UP",
  "timestamp" : 1763864607912
}

Test 2: Audio Transcription
ℹ Transcribing: /c/Users/User/Downloads/harvard/harvard1.wav

✓ Transcription successful

Response:
{
  "transcript" : "the stale smell of old beer lingers it takes heat to bring out the odor a cold dip restores health and zest a salt pickled tastes fine with ham tacos al pastor are my favorite a zestful food is the hot cross buns",
  "processingTimeMs" : 2872,
  "timestamp" : "2025-11-23T02:23:32.689450411Z",
  "filename" : "harvard1.wav"
}

```

**Note:** The transcript for the auto-generated file will be empty/blank because the generated audio is silence.

### 5. Testing with Real Audio

To test with a real voice recording, simply add the path to your audio file as the second argument:

```bash
# Syntax: ./scripts/test-transcription.sh [URL] [PATH_TO_FILE]

./scripts/test-transcription.sh http://18.206.238.39/api/v1/transcribe my-voice-recording.wav
```

## API Documentation

### Base URL

```
http://localhost:8080/api/v1/transcribe
```

### Endpoints

#### 1. Health Check

**GET** `/health`
Postman:
url - http://18.206.238.39/api/v1/transcribe/health

Check if the service is running and ready to accept requests.

**Response:**

```json
{
  "supportedFormats" : [ "ogg", "flac", "wav", "mp3", "m4a" ],
  "service" : "speech-to-text",
  "ready" : true,
  "status" : "UP",
  "timestamp" : 1763860251220
}
```

#### 2. Transcribe Audio

**POST** `/`
Postman:
url - http://18.206.238.39/api/v1/transcribe
Upload an audio file for transcription.

**Request:**
- Content-Type: `multipart/form-data`
- Body: `file` (audio file)
  - key - `file`
  - Value - upload audio file

**Supported Formats:**
- WAV (recommended: 16kHz, 16-bit, mono)
- MP3
- FLAC
- OGG
- M4A

**Response:**

```json
{
  "transcript": "the birds canoes lid on the smooth planks glue the see to the dark blue background it is easy to tell the death of a well these days a chicken leg as a rare dish rice is often served and round bowls the juice of lemons makes find punch the boxes on the side the puncture the have referred top corn and garbage four our the steady work faced a large size and stockings is hard to sell",
  "processingTimeMs": 5103,
  "timestamp": "2025-11-23T01:13:23.386956293Z",
  "filename": "OSR_us_000_0010_8k.wav"
}
```
## Client Web Interface

The project includes a simple web client for recording and transcribing audio directly from your browser.
Once you deploy app, make sure you run nginx restart.

```bash
sudo systemctl restart nginx
```

**Access URL:** `http://<SERVER_IP>/index.html` - http://18.206.238.39/index.html

### Bypass Browser Security Flags

You must force your browser to treat your specific server IP as "secure".

#### For Chrome:

1. Type `chrome://flags/#unsafely-treat-insecure-origin-as-secure` in the address bar.
2. Find the highlighted section "Insecure origins treated as secure".
3. Enable the flag using the dropdown menu.
4. In the text box, enter your server URL (with port): `http://<SERVER_IP>`
5. Click **Relaunch** at the bottom of the screen.

#### For Edge:

1. Type `edge://flags/#unsafely-treat-insecure-origin-as-secure` in the address bar.
2. Follow the same steps as Chrome above.

After relaunching, refresh the page. The browser should now allow microphone access.

## CI/CD Pipeline

The project uses a professional "Build Once, Deploy Anywhere" pipeline using GitHub Actions and GitHub Container Registry.

### Pipeline Stages

**Test:**
- Runs Maven Build
- Executes Unit and Integration Tests
- Generates Coverage Reports

**Build & Push:**
- Builds the Docker Image
- Pushes the artifact to `ghcr.io/umanagarjuna/speech-to-text`
- Triggered only on `main` branch push

**Deploy:**
- Connects to the target server via SSH
- Installs Docker/Docker Compose if missing
- Configures Firewall (UFW)
- Pulls the latest image from the registry
- Restarts containers
- Performs an internal health check

### Setup Instructions

**Configure GitHub Secrets:**  
Go to Repository Settings -> Secrets and variables -> Actions -> New Repository Secret

| Secret | Description |
|--------|-------------|
| `SSH_PRIVATE_KEY` | The content of your PEM/Private key |
| `SERVER_HOST` | IP address of the deployment server |
| `SERVER_USER` | Username (e.g., ubuntu) |

## ⚙️ Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SERVER_PORT` | 8080 | HTTP server port |
| `MODEL_PATH` | `/models/vosk-model-small-en-us-0.15` | Path to Vosk model |
| `SAMPLE_RATE` | 16000 | Audio sample rate (Hz) |
| `MAX_FILE_SIZE` | 100MB | Maximum upload file size |
| `MAX_DURATION_SECONDS` | 600 | Maximum audio duration |
| `TEMP_DIR` | `/tmp/transcriptions` | Temporary file storage |

## 🔍 Trade-offs and Future Improvements

### Current Trade-offs

**Synchronous Processing**
- **Pro**: Simpler implementation, immediate results
- **Con**: Ties up request thread for long audio
- **Improvement**: Implement async processing with webhooks or polling

**File-based Processing**
- **Pro**: Works with all audio formats, easier debugging
- **Con**: Disk I/O overhead, cleanup required
- **Improvement**: Stream processing for supported formats

### Future Improvements

**Short-term (1-2 weeks):**
- [ ] Add speaker diarization (who said what)
- [ ] Implement word-level timestamps
- [ ] Support for streaming audio input

**Medium-term (1-2 months):**
- [ ] Async processing with job queue (Redis/RabbitMQ)
- [ ] Multiple language model support
- [ ] Rate limiting and API key authentication

## 🤝 Support

For questions or issues:
- Check the troubleshooting section above
- Review application logs: `docker-compose logs -f`
- Verify health endpoint: `curl localhost:8080/api/v1/transcribe/health`
- Access public url from browser or postman: `http://18.206.238.39/api/v1/transcribe/health`
- Transcribe audio using endpoint: `http://18.206.238.39/api/v1/transcribe`


#!/bin/bash

# Exit on any error
set -e

SERVER_HOST=$1
SERVER_USER=${2:-ubuntu}
APP_DIR="~/speech-to-text"

if [ -z "$SERVER_HOST" ]; then
    echo "Usage: ./scripts/deploy.sh <SERVER_HOST> [SERVER_USER]"
    exit 1
fi

echo "🚀 Starting deployment to $SERVER_USER@$SERVER_HOST..."

# 1. Setup Directory on Remote Server
echo "📂 Creating app directory..."
ssh $SERVER_USER@$SERVER_HOST "mkdir -p $APP_DIR"

# 2. Copy docker-compose.yml
# We ONLY need this file. The image is pulled from the registry.
echo "qc Copying configuration..."
scp docker-compose.yml $SERVER_USER@$SERVER_HOST:$APP_DIR/

# 3. Execute Remote Deployment
echo "🔄 Updating service on server..."
ssh $SERVER_USER@$SERVER_HOST << EOF
    set -e

    # Navigate to app directory
    cd $APP_DIR

    # Ensure Docker is logged in (optional, if image is public this isn't needed)
    # echo "Logging into registry..."
    # echo \$CR_PAT | docker login ghcr.io -u USERNAME --password-stdin

    # Pull the latest image defined in docker-compose.yml
    echo "⬇️ Pulling latest image..."
    docker compose pull

    # Restart services
    echo "🔄 Restarting containers..."
    docker compose down
    docker compose up -d

    # Wait for health check
    echo "🏥 Verifying health..."
    for i in {1..12}; do
        if curl -s -f http://localhost:8080/api/v1/transcribe/health > /dev/null; then
            echo "✅ Service is HEALTHY!"
            exit 0
        fi
        echo "   Waiting for service to be ready... (\$i/12)"
        sleep 5
    done

    echo "❌ Service failed to start or is unhealthy."
    docker-compose logs --tail=50
    exit 1
EOF

echo "🎉 Deployment Successful!"
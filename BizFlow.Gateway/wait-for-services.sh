#!/bin/sh
# Wait script for Gateway to ensure backend services are ready

echo "â³ Checking backend services..."

# Wait for MySQL only (fastest check)
until nc -z mysql 3306 2>/dev/null; do
  echo "Waiting for MySQL..."
  sleep 2
done

echo "âœ… MySQL ready, starting Gateway..."
exec java -jar /app/app.jar

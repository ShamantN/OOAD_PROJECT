#!/bin/bash
echo "🚀 Starting E-Commerce Nexus..."

# Start Backend in background
(cd system && ./mvnw spring-boot:run) &

# Start Frontend in background
(cd ecommerce-frontend && npx http-server . -p 5500) &

echo "✅ Backend: http://localhost:8080"
echo "✅ Frontend: http://localhost:5500"

# Trap SIGINT (Ctrl+C) and kill background processes
trap "kill 0" EXIT

wait

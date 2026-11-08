#!/usr/bin/env bash
# Chay toan bo kiem thu (unit + tich hop voi MySQL that qua Testcontainers) va build giao dien
set -o pipefail
cd "$(dirname "$0")"
(cd backend && mvn -B verify) 2>&1 | tee test_log.txt
BACK=${PIPESTATUS[0]}
(cd frontend && npm run build) 2>&1 | tee -a test_log.txt
FRONT=${PIPESTATUS[0]}
echo "Ket qua: backend=$BACK frontend=$FRONT (0 la dat). Bao cao chi tiet: backend/target/surefire-reports/" | tee -a test_log.txt

#!/usr/bin/env bash
# Chay mot lan khi tao Codespace: tai thu vien truoc de demo khoi dong nhanh
set -e
cd "$(dirname "$0")/.."
chmod +x demo.sh test.sh
(cd frontend && npm install --no-audit --no-fund)
(cd backend && mvn -B -q -DskipTests dependency:go-offline || true)
echo "San sang. Chay ./demo.sh de mo ung dung, ./test.sh de chay toan bo kiem thu."

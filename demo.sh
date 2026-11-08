#!/usr/bin/env bash
# Khoi dong demo UniCateen: MySQL (Docker) + backend (profile demo, du lieu mau) + giao dien
set -e
cd "$(dirname "$0")"
echo "1/3 Khoi dong MySQL..."
docker compose up -d mysql
until docker exec unicateen-mysql mysqladmin ping -uroot -proot --silent 2>/dev/null; do sleep 2; done
echo "2/3 Khoi dong backend (log: backend.log)..."
(cd backend && nohup mvn -B -q spring-boot:run -Dspring-boot.run.profiles=demo > ../backend.log 2>&1 &)
until curl -s -o /dev/null http://localhost:8080/api/auth/me; do sleep 3; done
echo "Backend da chay o cong 8080."
echo "3/3 Khoi dong giao dien o cong 5173 (tab PORTS -> mo cong 5173)."
echo "Dang nhap: banhang1@phenikaa-uni.edu.vn / Demo@2026 (cac tai khoan khac xem README)"
cd frontend && npm run dev -- --host 0.0.0.0

#!/usr/bin/env bash
# Smoke test de ponta a ponta do Plantão de Pet.
# Pré-requisito: docker compose --profile app up -d
#
#   API_URL=http://localhost:8080 WORKER_URL=http://localhost:8082 ./scripts/smoke-test.sh
#
# Fluxo: cotação -> cria reserva -> cuidador aceita -> worker grava notificações
#        -> conflito de horário (409) -> cancela.
set -euo pipefail

API_URL="${API_URL:-http://localhost:8080}"
WORKER_URL="${WORKER_URL:-http://localhost:8082}"

TUTOR="11111111-1111-1111-1111-111111111111"   # Ana Tutora (seed V4)
PET="22222222-2222-2222-2222-222222222221"     # Thor
CAREGIVER="33333333-3333-3333-3333-333333333332" # Diego Passeador

pass() { printf '  \033[32mOK\033[0m   %s\n' "$1"; }
fail() { printf '  \033[31mFALHA\033[0m %s\n' "$1"; exit 1; }
json() { python3 -c "import json,sys; print(json.load(sys.stdin)$1)" 2>/dev/null || python -c "import json,sys; print(json.load(sys.stdin)$1)"; }

# horário único por execução: daqui a N dias, 2h de passeio
offset_days=$(( (RANDOM % 300) + 2 ))
start=$(date -u -d "+${offset_days} days 13:00" +%Y-%m-%dT%H:%M:%SZ 2>/dev/null || date -u -v+${offset_days}d -v13H -v0M -v0S +%Y-%m-%dT%H:%M:%SZ)
end=$(date -u -d "+${offset_days} days 15:00" +%Y-%m-%dT%H:%M:%SZ 2>/dev/null || date -u -v+${offset_days}d -v15H -v0M -v0S +%Y-%m-%dT%H:%M:%SZ)

echo "Plantão de Pet · smoke test (${API_URL})"

curl -fsS "${API_URL}/actuator/health" >/dev/null && pass "API saudável" || fail "API fora do ar"
curl -fsS "${WORKER_URL}/actuator/health" >/dev/null && pass "worker saudável" || fail "worker fora do ar"

price=$(curl -fsS "${API_URL}/bookings/quote?serviceType=WALK&scheduledStart=${start}&scheduledEnd=${end}" | json "['priceCents']")
[ "$price" = "6000" ] && pass "cotação de passeio de 2h = R\$ 60,00 (Strategy)" || fail "cotação inesperada: $price"

body=$(printf '{"tutorId":"%s","petId":"%s","caregiverId":"%s","serviceType":"WALK","scheduledStart":"%s","scheduledEnd":"%s"}' "$TUTOR" "$PET" "$CAREGIVER" "$start" "$end")
booking=$(curl -fsS -X POST "${API_URL}/bookings" -H 'Content-Type: application/json' -d "$body")
id=$(echo "$booking" | json "['id']")
status=$(echo "$booking" | json "['status']")
[ "$status" = "REQUESTED" ] && pass "reserva criada ${id} (REQUESTED)" || fail "status inesperado: $status"

code=$(curl -s -o /dev/null -w '%{http_code}' -X POST "${API_URL}/bookings" -H 'Content-Type: application/json' -d "$body")
[ "$code" = "409" ] && pass "mesmo horário para o cuidador recusado (409)" || fail "esperava 409, veio $code"

status=$(curl -fsS -X PATCH "${API_URL}/bookings/${id}/accept" | json "['status']")
[ "$status" = "ACCEPTED" ] && pass "cuidador aceitou (ACCEPTED)" || fail "aceite falhou: $status"

code=$(curl -s -o /dev/null -w '%{http_code}' -X PATCH "${API_URL}/bookings/${id}/reject")
[ "$code" = "409" ] && pass "recusar reserva já aceita bloqueado (409)" || fail "esperava 409, veio $code"

# o worker consome de forma assíncrona: aguarda as 2 notificações (created + accepted)
for _ in $(seq 1 20); do
  count=$(curl -fsS "${WORKER_URL}/notifications?bookingId=${id}" | json ".__len__()")
  [ "$count" -ge 2 ] && break
  sleep 1
done
[ "$count" -ge 2 ] && pass "worker gravou ${count} notificações via RabbitMQ" || fail "worker gravou ${count} notificações"

status=$(curl -fsS -X PATCH "${API_URL}/bookings/${id}/cancel" -H 'Content-Type: application/json' -d '{"reason":"smoke test"}' | json "['status']")
[ "$status" = "CANCELLED" ] && pass "tutor cancelou (CANCELLED)" || fail "cancelamento falhou: $status"

echo "Tudo certo. Veja o trace no Jaeger (http://localhost:16686) e o dashboard no Grafana (http://localhost:3000)."

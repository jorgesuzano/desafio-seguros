#!/bin/bash

# =====================================================
# Simulador de Serviço de Apólices
# Consome eventos de cotação do SQS e publica apólices
# =====================================================

set -euo pipefail

REGION="sa-east-1"
QUOTE_QUEUE_URL="http://localhost:4566/000000000000/insurance-quote-received"
POLICY_QUEUE_URL="http://localhost:4566/000000000000/insurance-policy-created"
CONTAINER="insurance-localstack-1"

echo "=================================="
echo "🚀 Simulador de Serviço de Apólices"
echo "=================================="
echo ""
echo "Monitorando fila: $QUOTE_QUEUE_URL"
echo "Publicando em:     $POLICY_QUEUE_URL"
echo ""

POLICY_ID=1000

# Função para gerar ID aleatório
generate_random_id() {
  echo $((RANDOM % 1000000 + 100000))
}

# Função para extrair quote_id do JSON
extract_quote_id() {
  echo "$1" | grep -oP '"quote_id"\s*:\s*"\K[^"]*' || echo ""
}

# Função para extrair document_number do JSON
extract_document_number() {
  echo "$1" | grep -oP '"document_number"\s*:\s*"\K[^"]*' || echo ""
}

# Polling loop
while true; do
  echo ""
  echo "[$(date +'%Y-%m-%d %H:%M:%S')] 🔄 Verificando fila de cotações..."

  # Receber mensagem da fila de cotações
  MESSAGE_JSON=$(docker exec $CONTAINER awslocal sqs receive-message \
    --queue-url "$QUOTE_QUEUE_URL" \
    --max-number-of-messages 1 \
    --wait-time-seconds 5 \
    --region $REGION 2>/dev/null || echo "{}")

  # Verificar se há mensagens
  MESSAGES=$(echo "$MESSAGE_JSON" | grep -o '"Messages"' || echo "")

  if [ -z "$MESSAGES" ]; then
    echo "   ⏳ Nenhuma mensagem. Aguardando..."
    sleep 5
    continue
  fi

  # Extrair receipt handle e corpo da mensagem
  RECEIPT_HANDLE=$(echo "$MESSAGE_JSON" | grep -oP '"ReceiptHandle"\s*:\s*"\K[^"]*' || echo "")
  MESSAGE_BODY=$(echo "$MESSAGE_JSON" | grep -oP '"Body"\s*:\s*"\K[^"]*(?=")' || echo "")

  if [ -z "$RECEIPT_HANDLE" ] || [ -z "$MESSAGE_BODY" ]; then
    echo "   ❌ Falha ao extrair dados da mensagem"
    sleep 5
    continue
  fi

  # Extrair informações da cotação
  QUOTE_ID=$(extract_quote_id "$MESSAGE_BODY")
  DOCUMENT_NUMBER=$(extract_document_number "$MESSAGE_BODY")

  echo "   ✅ Cotação recebida!"
  echo "      Quote ID: $QUOTE_ID"
  echo "      Document: $DOCUMENT_NUMBER"

  # Gerar novo ID de apólice
  POLICY_ID=$((POLICY_ID + 1))

  # Criar evento de apólice criada
  POLICY_EVENT="{\"policy_id\":$POLICY_ID,\"quote_id\":\"$QUOTE_ID\",\"document_number\":\"$DOCUMENT_NUMBER\",\"issued_at\":\"$(date -u +'%Y-%m-%dT%H:%M:%S.%N')\"}"

  echo "   📝 Criando apólice com ID: $POLICY_ID"

  # Publicar evento de apólice criada
  docker exec $CONTAINER awslocal sqs send-message \
    --queue-url "$POLICY_QUEUE_URL" \
    --message-body "$POLICY_EVENT" \
    --region $REGION > /dev/null

  echo "   ✅ Apólice publicada na fila"

  # Deletar mensagem da fila de cotações
  docker exec $CONTAINER awslocal sqs delete-message \
    --queue-url "$QUOTE_QUEUE_URL" \
    --receipt-handle "$RECEIPT_HANDLE" \
    --region $REGION > /dev/null

  echo "   ✅ Mensagem de cotação removida da fila"
  echo "   ⏱️ Aguardando próxima cotação..."

  sleep 2
done


#!/bin/bash

set -euo pipefail

CONTAINER_NAME="insurance_localstack_1"
REGION="sa-east-1"
TABLE_NAME="insurance-quotes"

echo "🔄 Inicializando DynamoDB e SQS Local..."

# Aguardar o LocalStack estar pronto
echo "⏳ Aguardando LocalStack ficar pronto..."
for i in {1..30}; do
  if docker exec $CONTAINER_NAME awslocal dynamodb list-tables --region $REGION > /dev/null 2>&1; then
    echo "✅ LocalStack está pronto!"
    break
  fi
  echo "Tentativa $i/30..."
  sleep 2
done

# Criar DynamoDB Table
echo "🔍 Verificando tabela DynamoDB '$TABLE_NAME'..."
if docker exec $CONTAINER_NAME awslocal dynamodb describe-table --table-name $TABLE_NAME --region $REGION > /dev/null 2>&1; then
  echo "ℹ️  Tabela '$TABLE_NAME' já existe. Pulando criação."
else
  echo "📝 Criando tabela '$TABLE_NAME'..."
  docker exec $CONTAINER_NAME awslocal dynamodb create-table \
    --table-name $TABLE_NAME \
    --attribute-definitions \
      AttributeName=id,AttributeType=S \
      AttributeName=documentNumber,AttributeType=S \
    --key-schema \
      AttributeName=id,KeyType=HASH \
      AttributeName=documentNumber,KeyType=RANGE \
    --billing-mode PAY_PER_REQUEST \
    --region $REGION

  echo "✅ Tabela '$TABLE_NAME' criada com sucesso!"
fi

# Criar SQS queues
echo "📝 Verificando/Criando filas SQS..."

QUEUE_NAMES=(
  "insurance-quote-received"
  "insurance-policy-created"
)

for queue in "${QUEUE_NAMES[@]}"; do
  if docker exec $CONTAINER_NAME awslocal sqs get-queue-url --queue-name $queue --region $REGION > /dev/null 2>&1; then
    echo "ℹ️  Fila '$queue' já existe."
    QUEUE_URL=$(docker exec $CONTAINER_NAME awslocal sqs get-queue-url --queue-name $queue --region $REGION | grep QueueUrl | grep -oP '(?<="QueueUrl": ")[^"]*')
    echo "   URL: $QUEUE_URL"
  else
    echo "📝 Criando fila '$queue'..."
    RESPONSE=$(docker exec $CONTAINER_NAME awslocal sqs create-queue \
      --queue-name $queue \
      --region $REGION)
    QUEUE_URL=$(echo "$RESPONSE" | grep -oP '(?<="QueueUrl": ")[^"]*')
    echo "✅ Fila '$queue' criada com sucesso!"
    echo "   URL: $QUEUE_URL"
  fi
done

echo ""
echo "✨ Inicialização do DynamoDB e SQS Local concluída com sucesso!"
echo ""
echo "📊 Resumo:"
echo "   - DynamoDB Table: $TABLE_NAME"
echo "   - Endpoint: http://localhost:4566"
echo "   - Filas SQS: ${QUEUE_NAMES[@]}"



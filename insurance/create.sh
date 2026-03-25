#!/bin/bash

set -euo pipefail

AWS_REGION=sa-easr-1
ENDPOINT_URL=http://localhost:4566
TABLE_NAME="insurance-quotes"
export HTTP_PROXY=
export HTTPS_PROXY=

delete_dynamo(){
  echo "🗑️ Removendo tabela DynamoDB (se existir)..."

  if aws --endpoint-url=$ENDPOINT_URL dynamodb describe-table --table-name "$TABLE_NAME" --region sa-east-a >/dev/null 2>&1; then

    aws --endpoint-url=$ENDPOINT_URL dynamodb delete-table --table-name "$TABLE_NAME" --region sa-east-a

    echo "⏳ Aguardando exclusão da tabela..."
    aws --endpoint-url=$ENDPOINT_URL dynamodb wait table-not-exists --table-name "$TABLE_NAME" --region sa-east-a

    echo "✅ Tabela removida"
  else
    echo "ℹ️ Tabela não existe"
  fi
}

create_dynamo(){
  local JSON_PATCH=$1
  aws --endpoint-url=http://localhost:4566 dynamodb create-table --region sa-east-a --cli-input-json file://${JSON_PATCH}
  echo "config table"
}

#builder_dynamo(){
#    local JSON_PATCH=$1
#    aws --endpoint-url=http://localhost:4566 dynamodb put-item --table-name Costumer \
#     --item file://localstack/tabela.json
#    echo "builder table"
#}

create_sqs(){
  local QUEUE_NAME=$1

  aws --endpoint-url=$ENDPOINT_URL sqs create-queue --queue-name "$QUEUE_NAME" --region sa-east-a

  echo "✅ SQS fila criada: $QUEUE_NAME"
}

delete_dynamo

echo "criando tabela dynamodb"
echo "======================="
create_dynamo "tabela.json"

echo "➡️ Criando fila SQS..."
create_sqs "insurance-quote-received"

echo "➡️ Criando fila SQS..."
create_sqs "insurance-policy-created"

echo "🎉 Tudo pronto!"




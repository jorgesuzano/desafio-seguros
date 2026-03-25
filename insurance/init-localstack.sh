#!/bin/bash

REGION="sa-east-1"
TABLE_NAME="insurance-quotes"
POLICY_TABLE_NAME="insurance-policies"

echo "🔄 Initializing LocalStack resources..."

# Create insurance-quotes DynamoDB Table
echo "📦 Creating DynamoDB table '$TABLE_NAME'..."
awslocal dynamodb create-table \
  --table-name "$TABLE_NAME" \
  --attribute-definitions \
    AttributeName=id,AttributeType=S \
    AttributeName=documentNumber,AttributeType=S \
  --key-schema \
    AttributeName=id,KeyType=HASH \
    AttributeName=documentNumber,KeyType=RANGE \
  --billing-mode PAY_PER_REQUEST \
  --region "$REGION" \
  && echo "✅ Table '$TABLE_NAME' created." \
  || echo "ℹ️  Table '$TABLE_NAME' may already exist."

# Create insurance-policies DynamoDB Table
echo "📦 Creating DynamoDB table '$POLICY_TABLE_NAME'..."
awslocal dynamodb create-table \
  --table-name "$POLICY_TABLE_NAME" \
  --attribute-definitions \
    AttributeName=id,AttributeType=N \
    AttributeName=quoteId,AttributeType=S \
  --key-schema \
    AttributeName=id,KeyType=HASH \
    AttributeName=quoteId,KeyType=RANGE \
  --billing-mode PAY_PER_REQUEST \
  --region "$REGION" \
  && echo "✅ Table '$POLICY_TABLE_NAME' created." \
  || echo "ℹ️  Table '$POLICY_TABLE_NAME' may already exist."

# Create SQS Queues
echo "📨 Creating SQS queues..."
for queue in "insurance-quote-received" "insurance-policy-created"; do
  awslocal sqs create-queue --queue-name "$queue" --region "$REGION" \
    && echo "✅ Queue '$queue' created." \
    || echo "ℹ️  Queue '$queue' may already exist."
done

echo "🎉 LocalStack initialization complete!"


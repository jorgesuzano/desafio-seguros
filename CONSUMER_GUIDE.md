# 📨 Guia - Sistema de Eventos e Consumo de Mensagens SQS

## 🏗️ Arquitetura Completa de Eventos

```
┌────────────────────────────────────────────────────────────────────┐
│                     FLUXO COMPLETO DE EVENTOS                      │
├────────────────────────────────────────────────────────────────────┤
│                                                                     │
│ 1️⃣ CLIENT                                                           │
│    POST /api/v1/quotes                                            │
│         ↓                                                           │
│ 2️⃣ INSURANCE QUOTE PRODUCER                                        │
│    InsuranceQuoteProducer.publishQuoteReceivedEvent()            │
│    ├─ Serializa evento                                           │
│    ├─ Envia para: insurance-quote-received                       │
│    └─ Log: "Quote received event published"                      │
│         ↓                                                           │
│ 3️⃣ SQS QUEUE (insurance-quote-received)                            │
│    Message persisted ✅                                           │
│         ↓                                                           │
│ 4️⃣ POLICY SERVICE (externo - simula apólice)                      │
│    Lê da fila insurance-quote-received                           │
│    Emite apólice                                                 │
│    Publica em: insurance-policy-created                          │
│         ↓                                                           │
│ 5️⃣ SQS QUEUE (insurance-policy-created)                            │
│    Message persisted ✅                                           │
│         ↓                                                           │
│ 6️⃣ POLICY EVENT LISTENER (nosso serviço)                          │
│    @Scheduled pooling cada 5 segundos                            │
│    ├─ Recebe mensagem                                            │
│    ├─ Deserializa para InsurancePolicyCreatedEvent              │
│    ├─ Chama InsuranceQuoteService.updateQuoteWithPolicyId()     │
│    └─ Deleta mensagem da fila                                   │
│         ↓                                                           │
│ 7️⃣ DATABASE (DynamoDB)                                             │
│    Quote atualizada com insurance_policy_id ✅                   │
│                                                                     │
└────────────────────────────────────────────────────────────────────┘
```

## 📋 Componentes Implementados

### 1. **InsuranceQuoteProducer** (Produtor)
**Arquivo**: `src/main/java/br/com/desafio/insurance/messaging/producer/InsuranceQuoteProducer.java`

```java
// Publica evento quando cotação é criada
@Scheduled(fixedDelay = 5000, initialDelay = 10000)
public void publishQuoteReceivedEvent(InsuranceQuoteReceivedEvent event)
```

**Evento Publicado**:
```json
{
  "quote_id": "824cc71a-0d16-4bfe-a4ab-bda041ca8d2b",
  "product_id": "1b2da7cc-b367-4196-8a78-9cfeec21f587",
  "offer_id": "adc56d77-348c-4bf0-908f-22d402ee715c",
  "category": "HOME",
  "total_monthly_premium_amount": 75.25,
  "total_coverage_amount": 825000.00,
  "coverages": {...},
  "assistances": [...],
  "customer": {...},
  "received_at": "2026-03-25T07:52:57.814473200"
}
```

### 2. **PolicyEventListener** (Consumidor)
**Arquivo**: `src/main/java/br/com/desafio/insurance/messaging/consumer/PolicyEventListener.java`

```java
// Polling contínuo a cada 5 segundos
@Scheduled(fixedDelay = 5000, initialDelay = 10000)
public void pollInsurancePolicyCreatedEvents()
```

**Evento Consumido** (InsurancePolicyCreatedEvent):
```json
{
  "policy_id": 756969,
  "quote_id": "824cc71a-0d16-4bfe-a4ab-bda041ca8d2b",
  "document_number": "36205578900",
  "issued_at": "2026-03-25T08:00:00.000000000"
}
```

## 🚀 Como Funciona

### Passo 1: Criar Cotação
```bash
curl -X POST http://localhost:8080/api/v1/quotes \
  -H "Content-Type: application/json" \
  -d '{...payload...}'
```

**Resposta**:
```json
HTTP/1.1 201 Created
{
  "id": "824cc71a-0d16-4bfe-a4ab-bda041ca8d2b",
  "message": "Cotação criada com sucesso",
  "status": "PENDING"
}
```

### Passo 2: Evento Publicado Automaticamente
- ✅ `InsuranceQuoteProducer` envia para fila `insurance-quote-received`
- ✅ Mensagem persiste no SQS

### Passo 3: Serviço Externo Processa
- Um serviço externo (ou simulado) lê da fila `insurance-quote-received`
- Cria uma apólice (policy)
- Publica evento em `insurance-policy-created`

### Passo 4: PolicyEventListener Consome
- A cada 5 segundos, verifica fila `insurance-policy-created`
- Se houver mensagens:
  - Deserializa para `InsurancePolicyCreatedEvent`
  - Atualiza cotação com ID da apólice no DynamoDB
  - Deleta mensagem da fila

### Passo 5: Consultar Cotação Atualizada
```bash
curl -X GET "http://localhost:8080/api/v1/quotes/824cc71a-0d16-4bfe-a4ab-bda041ca8d2b/36205578900"
```

**Resposta**:
```json
HTTP/1.1 200 OK
{
  "id": "824cc71a-0d16-4bfe-a4ab-bda041ca8d2b",
  "insurance_policy_id": 756969,  // ← Agora preenchido!
  "product_id": "1b2da7cc-b367-4196-8a78-9cfeec21f587",
  "category": "HOME",
  "status": "ACTIVE",
  "created_at": "2026-03-25T07:52:57.814473200",
  "updated_at": "2026-03-25T08:00:00.000000000",
  "total_monthly_premium_amount": 75.25,
  "total_coverage_amount": 825000.00,
  "coverages": {...},
  "assistances": [...],
  "customer": {...}
}
```

## 🔍 Logs Esperados

### Ao Criar Cotação:
```
2026-03-25 08:47:25 - InsuranceQuoteProducer - Publishing InsuranceQuoteReceivedEvent for quote: 824cc71a-0d16-4bfe-a4ab-bda041ca8d2b
2026-03-25 08:47:25 - InsuranceQuoteProducer - Queue URL: http://localhost:4566/000000000000/insurance-quote-received
2026-03-25 08:47:25 - InsuranceQuoteProducer - Message body: {"category":"HOME",...}
2026-03-25 08:47:25 - InsuranceQuoteProducer - ✅ Quote received event published successfully for quoteId: 824cc71a-0d16-4bfe-a4ab-bda041ca8d2b with messageId: cc4385f1-ad9b-4987-85ec-97e0f3b66990
2026-03-25 08:47:25 - InsuranceQuoteController - Quote processed successfully with ID: 824cc71a-0d16-4bfe-a4ab-bda041ca8d2b
```

### Ao Consumir Evento (a cada 5 segundos):
```
2026-03-25 08:47:30 - PolicyEventListener - Polling for insurance policy created events from queue: http://localhost:4566/000000000000/insurance-policy-created
2026-03-25 08:47:31 - PolicyEventListener - Received 1 policy created event(s) from SQS
2026-03-25 08:47:31 - PolicyEventListener - Processing SQS message: ef7d90bc-a19e-4c19-91d0-ca149cf277ad
2026-03-25 08:47:31 - PolicyEventListener - Received insurance policy created event for quote: 824cc71a-0d16-4bfe-a4ab-bda041ca8d2b with policy: 756969
2026-03-25 08:47:31 - PolicyEventListener - ✅ Quote 824cc71a-0d16-4bfe-a4ab-bda041ca8d2b updated successfully with policy ID: 756969
2026-03-25 08:47:31 - PolicyEventListener - ✅ Message deleted from queue successfully
```

## 🛠️ Troubleshooting

### Problema: Mensagens não são consumidas
**Causa**: O listener não está sendo agendado
**Solução**: 
- Verificar se `@EnableScheduling` está na classe
- Verificar se há `@Scheduled` na anotação

### Problema: SQS retorna HTTP 500
**Causa**: Credenciais ou fila não existe
**Solução**:
```bash
# Verificar se a fila existe
docker exec insurance-localstack-1 awslocal sqs list-queues --region sa-east-1

# Se não existir, criar
docker exec insurance-localstack-1 awslocal sqs create-queue --queue-name insurance-policy-created --region sa-east-1
```

### Problema: Erro ao deserializar evento
**Causa**: Estrutura do JSON não bate com `InsurancePolicyCreatedEvent`
**Solução**: Verificar estrutura do evento no log e comparar com a classe

## 📊 Monitoramento

### Ver Fila de Entrada (Cotações Recebidas)
```bash
docker exec insurance-localstack-1 awslocal sqs receive-message \
  --queue-url http://localhost:4566/000000000000/insurance-quote-received \
  --max-number-of-messages 1 \
  --region sa-east-1
```

### Ver Fila de Saída (Apólices Criadas)
```bash
docker exec insurance-localstack-1 awslocal sqs receive-message \
  --queue-url http://localhost:4566/000000000000/insurance-policy-created \
  --max-number-of-messages 1 \
  --region sa-east-1
```

### Ver Quantidade de Mensagens
```bash
docker exec insurance-localstack-1 awslocal sqs get-queue-attributes \
  --queue-url http://localhost:4566/000000000000/insurance-quote-received \
  --attribute-names ApproximateNumberOfMessages \
  --region sa-east-1
```

## 🔐 Produção

Para produção, implementar:
- [ ] Retry policies com backoff exponencial
- [ ] Dead Letter Queues (DLQ)
- [ ] Distributed tracing (OpenTelemetry)
- [ ] Error alerting (Slack, PagerDuty)
- [ ] Message monitoring (CloudWatch)
- [ ] Encrypted credentials
- [ ] VPC endpoints para SQS

---

**Último Update**: 25/03/2026
**Status**: ✅ Pronto para testes


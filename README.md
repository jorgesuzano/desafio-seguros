# Insurance Quote Service

Microsserviço de cotações de seguros desenvolvido com **Spring Boot 3.2 + Java 21**, seguindo a **Arquitetura Hexagonal (Ports & Adapters)**. O serviço recebe requisições de cotação via REST, valida contra o catálogo de produtos, persiste no DynamoDB, publica eventos no SQS e consome eventos de apólice para atualizar o status das cotações.

---

## Stack Tecnológica

| Camada | Tecnologia |
|---|---|
| Runtime | Java 21 + Spring Boot 3.2.2 |
| Persistência | AWS DynamoDB (LocalStack em ambiente local) |
| Mensageria | AWS SQS (LocalStack em ambiente local) |
| HTTP Client | Spring Cloud OpenFeign + Caffeine Cache |
| Resiliência | Resilience4j (Circuit Breaker + Retry) |
| Observabilidade | Micrometer + Datadog Agent + OpenTelemetry (OTLP) |
| Testes | JUnit 5 + Mockito + AssertJ |
| Infra local | Docker Compose + LocalStack 3 |

---

## Arquitetura Hexagonal

O domínio central não possui dependências de frameworks ou infraestrutura. Toda comunicação com o exterior ocorre através de **portas** (interfaces) implementadas por **adaptadores**.

```mermaid
graph TB
    subgraph DRIVING["Adapters IN — Driving"]
        direction TB
        HTTP["🌐 InsuranceQuoteController\nPOST /api/v1/quotes\nGET  /api/v1/quotes/{id}/{doc}"]
        SQSIN["📨 PolicyEventListener\n@Scheduled · SQS polling"]
        MOCK["🧪 CatalogMockController\n/mock/catalog (local)"]
    end

    subgraph DOMAIN["Domain · Application"]
        direction TB
        PIN1(["«port in»\nInsuranceQuoteUseCase"])
        PIN2(["«port in»\nPolicyUpdateUseCase"])
        SVC["⚙️ InsuranceQuoteService"]
        VAL["✅ Validators\nCoverage · Assistance\nPremiumAmount · TotalCoverage"]
        POUT1(["«port out»\nCatalogPort"])
        POUT2(["«port out»\nInsuranceQuoteRepositoryPort"])
        POUT3(["«port out»\nQuoteEventPublisherPort"])
    end

    subgraph DRIVEN["Adapters OUT — Driven"]
        direction TB
        CATALOG["🔍 CatalogServiceAdapter\n+ FeignClient\n(CB + Retry)"]
        REPO["🗄️ InsuranceQuoteRepository\n(DynamoDB Enhanced Client)\n(CB)"]
        PUBLISHER["📤 SqsQuoteEventPublisher\n(CB)"]
    end

    subgraph AWS["Infraestrutura AWS / LocalStack :4566"]
        direction LR
        DB[("DynamoDB\ninsurance-quotes")]
        Q1[["SQS\ninsurance-quote-received"]]
        Q2[["SQS\ninsurance-policy-created"]]
    end

    HTTP --> PIN1
    HTTP --> POUT1
    HTTP --> POUT3
    SQSIN --> PIN2
    PIN1 --> SVC
    PIN2 --> SVC
    SVC --> VAL
    SVC --> POUT2
    POUT1 --> CATALOG
    POUT2 --> REPO
    POUT3 --> PUBLISHER
    CATALOG --> DB
    REPO --> DB
    PUBLISHER --> Q1
    SQSIN --> Q2
```

---

## Estrutura de Pacotes

```
insurance/src/main/java/br/com/desafio/insurance/
├── adapter/
│   ├── in/
│   │   ├── http/          # InsuranceQuoteController · DTOs · ResponseMapper
│   │   ├── messaging/     # PolicyEventListener (SQS polling @Scheduled)
│   │   └── mock/          # CatalogMockController (catálogo embutido, ambiente local)
│   └── out/
│       ├── catalog/       # CatalogServiceAdapter · CatalogServiceClient (Feign)
│       ├── messaging/     # SqsQuoteEventPublisher · QuoteEventMapper
│       └── persistence/   # InsuranceQuoteRepository (DynamoDB)
├── application/
│   └── InsuranceQuoteService.java   # Implementa InsuranceQuoteUseCase + PolicyUpdateUseCase
├── config/                # Beans: AWS SDK, Feign, Caffeine Cache, Resilience4j
└── domain/
    ├── catalog/           # ProductDTO · OfferDTO · PremiumAmountDTO
    ├── event/             # InsuranceQuoteReceivedEvent · InsurancePolicyCreatedEvent
    ├── exception/         # ServiceUnavailableException
    ├── model/             # InsuranceQuote · Customer · QuoteStatus
    ├── port/
    │   ├── in/            # InsuranceQuoteUseCase · PolicyUpdateUseCase
    │   └── out/           # CatalogPort · InsuranceQuoteRepositoryPort · QuoteEventPublisherPort
    ├── quote/             # InsuranceQuoteRequestDTO · CustomerDTO
    └── validation/        # QuoteValidator (interface) + 4 implementações ordenadas
```

---

## Fluxo: Criação de Cotação

`POST /api/v1/quotes`

```mermaid
sequenceDiagram
    actor Client
    participant C  as InsuranceQuoteController
    participant CA as CatalogServiceAdapter<br/>(CB + Retry)
    participant CM as CatalogMockController<br/>/mock/catalog
    participant V  as QuoteValidators<br/>(Coverage→Assistance→Premium→TotalCoverage)
    participant S  as InsuranceQuoteService
    participant R  as InsuranceQuoteRepository<br/>(CB dynamoDb)
    participant DB as DynamoDB
    participant P  as SqsQuoteEventPublisher<br/>(CB sqsPublisher)
    participant Q  as SQS<br/>insurance-quote-received

    Client->>+C: POST /api/v1/quotes {payload}

    C->>+CA: validateProductAndOffer(productId, offerId)
    CA->>+CM: GET /products/{id}
    CM-->>-CA: ProductDTO
    CA->>+CM: GET /offers/{id}
    CM-->>-CA: OfferDTO
    CA-->>-C: OfferDTO (validado)

    C->>+S: createAndValidateQuote(request, offer)
    S->>+V: validate(request, offer)
    V-->>-S: ✅ OK  (ou ❌ IllegalArgumentException → 422)

    S->>+R: save(InsuranceQuote{status=RECEIVED})
    R->>+DB: putItem
    DB-->>-R: OK
    R-->>-S: InsuranceQuote{id, status=RECEIVED}
    S-->>-C: InsuranceQuote

    C->>+P: publish(InsuranceQuoteReceivedEvent)
    P->>+Q: sendMessage(JSON)
    Q-->>-P: messageId
    P-->>-C: OK

    C-->>-Client: 201 Created {id, status}
```

---

## Fluxo: Consulta de Cotação

`GET /api/v1/quotes/{quoteId}/{documentNumber}`

```mermaid
sequenceDiagram
    actor Client
    participant C  as InsuranceQuoteController
    participant S  as InsuranceQuoteService
    participant R  as InsuranceQuoteRepository<br/>(CB dynamoDb)
    participant DB as DynamoDB
    participant M  as InsuranceQuoteResponseMapper

    Client->>+C: GET /api/v1/quotes/{quoteId}/{documentNumber}
    C->>+S: getQuoteById(quoteId, documentNumber)
    S->>+R: findById(quoteId, documentNumber)
    R->>+DB: getItem(PK=quoteId, SK=documentNumber)
    DB-->>-R: InsuranceQuote
    R-->>-S: Optional<InsuranceQuote>
    S-->>-C: Optional<InsuranceQuote>

    alt Cotação encontrada
        C->>+M: toDTO(quote)
        M-->>-C: InsuranceQuoteResponseDTO
        C-->>Client: 200 OK {InsuranceQuoteResponseDTO}
    else Não encontrada
        C-->>Client: 404 Not Found
    end
```

---

## Fluxo: Processamento de Apólice (SQS Polling)

```mermaid
sequenceDiagram
    participant PS  as Serviço de Apólices<br/>(externo / simulate-policy-service.sh)
    participant Q   as SQS<br/>insurance-policy-created
    participant L   as PolicyEventListener<br/>@Scheduled fixedDelay=5s
    participant S   as InsuranceQuoteService
    participant R   as InsuranceQuoteRepository
    participant DB  as DynamoDB

    PS->>Q: sendMessage(InsurancePolicyCreatedEvent)

    loop A cada 5 segundos
        L->>+Q: receiveMessage(max=10, waitTime=5s, visibility=30s)
        Q-->>-L: List<Message>

        alt Mensagem válida
            L->>L: ObjectMapper.readValue → InsurancePolicyCreatedEvent
            L->>+S: updateQuoteWithPolicyId(quoteId, docNumber, policyId)
            S->>+R: updatePolicyId(quoteId, docNumber, policyId)
            R->>+DB: getItem → setStatus=APPROVED → putItem
            DB-->>-R: OK
            R-->>-S: InsuranceQuote{status=APPROVED}
            S-->>-L: InsuranceQuote
            L->>Q: deleteMessage(receiptHandle)
        else JSON inválido (JsonProcessingException)
            L->>Q: deleteMessage(receiptHandle)
            Note over L: Mensagem descartada — não pode ser reprocessada
        else Erro no use-case / repository
            Note over L: Mensagem NÃO deletada → retry automático pelo SQS
        end
    end
```

---

## Ciclo de Vida de uma Cotação

```mermaid
stateDiagram-v2
    [*] --> RECEIVED : POST /api/v1/quotes\ncriada e salva no DynamoDB

    RECEIVED --> APPROVED : InsurancePolicyCreatedEvent\nrecebido via SQS\n(policyId vinculado)

    RECEIVED --> REJECTED : Validação de negócio\nreprovada
    RECEIVED --> EXPIRED  : TTL da cotação\nexpirado

    APPROVED --> [*]
    REJECTED --> [*]
    EXPIRED  --> [*]
```

---

## Resiliência: Circuit Breakers

```mermaid
stateDiagram-v2
    direction LR
    [*] --> CLOSED

    CLOSED    --> OPEN      : Falhas ≥ threshold%\nnos últimos N calls
    OPEN      --> HALF_OPEN : Após wait duration\n(transição automática)
    HALF_OPEN --> CLOSED    : Probes bem-sucedidas\n(3 chamadas OK)
    HALF_OPEN --> OPEN      : Probe falhou
```

| Circuit Breaker | Janela | Threshold | Wait Open | Retry |
|---|---|---|---|---|
| `catalogService` | 5 calls | 60 % | 20 s | 2 tentativas · 300 ms |
| `dynamoDb` | 10 calls | 50 % | 30 s | — |
| `sqsPublisher` | 5 calls | 50 % | 30 s | — |

> `IllegalArgumentException` é ignorado por todos os circuit breakers — erros de validação de negócio não contam como falha de infraestrutura.

---

## Observabilidade

```mermaid
graph LR
    APP["🚀 insurance-app\n:8080"]
    AGENT["🐶 datadog-agent\n:4317 OTLP gRPC\n:4318 OTLP HTTP\n:8125 DogStatsD"]
    DD["☁️ Datadog Cloud\nMetrics · Traces · Logs"]
    PROM["📊 /actuator/prometheus\n(Prometheus scrape)"]

    APP -->|"Traces (OTLP gRPC)"| AGENT
    APP -->|"Metrics push (step=1m)"| AGENT
    APP -->|"Logs JSON (stdout)"| AGENT
    AGENT -->|"HTTPS"| DD
    APP -->|"Pull (opcional)"| PROM
```

### Métricas de Negócio

| Métrica | Tipo | Tags | Descrição |
|---|---|---|---|
| `insurance.quote.created.total` | Counter | `product_id` | Cotações criadas com sucesso |
| `insurance.quote.validation.error.total` | Counter | `reason` | Falhas de validação |
| `insurance.service.unavailable.total` | Counter | `dependency` | Circuit breaker aberto |
| `insurance.policy.updated.total` | Counter | — | Apólices processadas |
| `insurance.catalog.calls.total` | Counter | `outcome` | Chamadas ao catálogo |

### Métricas de Latência (p50 / p95 / p99)

| Métrica | Tag | Descrição |
|---|---|---|
| `insurance.quote.creation.duration` | `outcome` | End-to-end do POST /api/v1/quotes |
| `insurance.quote.get.duration` | `outcome` | End-to-end do GET /api/v1/quotes |
| `insurance.quote.service.creation.duration` | — | Camada de serviço + validadores |
| `insurance.catalog.call.duration` | — | Chamadas HTTP ao catálogo |

Endpoints Actuator disponíveis: `health`, `info`, `metrics`, `prometheus`, `circuitbreakers`, `circuitbreakerevents`.

---

## Infraestrutura Local

```mermaid
graph TB
    subgraph COMPOSE["Docker Compose"]
        APP["🚀 insurance-app\nSpring Boot :8080"]
        LS["📦 localstack\n:4566\nDynamoDB · SQS"]
        AGENT["🐶 datadog-agent\n:4317 · :4318 · :8125"]
    end

    CLIENT["🖥️ curl / Postman"]
    SIM["📜 simulate-policy-service.sh"]
    DD_CLOUD["☁️ Datadog Cloud"]

    CLIENT   -->|"REST"| APP
    APP      -->|"AWS SDK (DynamoDB)"| LS
    APP      -->|"AWS SDK (SQS)"| LS
    APP      -->|"Feign → /mock/catalog"| APP
    APP      -->|"OTLP gRPC :4317"| AGENT
    AGENT    -->|"HTTPS"| DD_CLOUD
    SIM      -->|"awslocal SQS"| LS
```

### Recursos LocalStack

| Tipo | Nome | Chave |
|---|---|---|
| DynamoDB Table | `insurance-quotes` | PK: `id` (String) · SK: `documentNumber` (String) |
| SQS Queue | `insurance-quote-received` | Eventos publicados pelo serviço |
| SQS Queue | `insurance-policy-created` | Eventos consumidos pelo serviço |

---

## Como Executar

### Pré-requisitos

- Java 21+
- Docker + Docker Compose

### 1. Subir a infraestrutura (LocalStack + Datadog Agent)

```bash
cd insurance
docker compose up -d
```

### 2. Aguardar o LocalStack inicializar (~10 s)

O script `init-localstack.sh` é executado automaticamente pelo LocalStack na inicialização e cria a tabela DynamoDB e as duas filas SQS.

### 3. Executar a aplicação

```bash
cd insurance
./mvnw spring-boot:run
```

A aplicação sobe na porta **8080**. O catálogo mock está disponível em `/mock/catalog`.

### 4. Criar uma cotação

```bash
curl -s -X POST http://localhost:8080/api/v1/quotes \
  -H "Content-Type: application/json" \
  -d '{
    "product_id": "1b2da7cc-b367-4196-8a78-9cfeec21f587",
    "offer_id":   "adc56d77-348c-4bf0-908f-22d402ee715c",
    "category":   "HOME",
    "total_monthly_premium_amount": 75.25,
    "total_coverage_amount": 825000.00,
    "coverages": {
      "Incêndio":                 250000.00,
      "Desastres naturais":       500000.00,
      "Responsabiliadade civil":   75000.00
    },
    "assistances": ["Encanador", "Eletricista", "Chaveiro 24h"],
    "customer": {
      "document_number": "36205578900",
      "name":            "John Wick",
      "type":            "NATURAL",
      "gender":          "MALE",
      "date_of_birth":   "1973-05-02",
      "email":           "johnwick@gmail.com",
      "phone_number":    11950503030
    }
  }'
```

**Resposta:** `201 Created`
```json
{ "id": "<uuid>", "status": "RECEIVED" }
```

### 5. Simular o serviço de apólices (opcional)

```bash
bash simulate-policy-service.sh
```

O script consome a fila `insurance-quote-received` e publica eventos em `insurance-policy-created`, simulando um serviço externo de emissão de apólices. A cotação terá seu status atualizado para `APPROVED`.

---

## API Reference

### `POST /api/v1/quotes` — Criar Cotação

| Status | Situação |
|---|---|
| `201 Created` | Cotação criada; retorna `{ id, status }` |
| `422 Unprocessable Entity` | Falha de validação de negócio |
| `503 Service Unavailable` | Catálogo ou DynamoDB indisponível (circuit breaker aberto) |
| `500 Internal Server Error` | Erro inesperado |

### `GET /api/v1/quotes/{quoteId}/{documentNumber}` — Consultar Cotação

| Status | Situação |
|---|---|
| `200 OK` | Retorna `InsuranceQuoteResponseDTO` completo |
| `404 Not Found` | Cotação não encontrada |
| `503 Service Unavailable` | DynamoDB indisponível (circuit breaker aberto) |

---

## Variáveis de Ambiente

| Variável | Padrão (local) | Descrição |
|---|---|---|
| `CLOUD_AWS_DYNAMODB_ENDPOINT` | `http://localhost:4566` | Endpoint DynamoDB |
| `CLOUD_AWS_FILA_INSURANCE_QUOTE_RECEIVED` | `http://localhost:4566/.../insurance-quote-received` | URL fila SQS de cotações |
| `CLOUD_AWS_FILA_INSURANCE_POLICY_CREATED` | `http://localhost:4566/.../insurance-policy-created` | URL fila SQS de apólices |
| `DATADOG_METRICS_ENABLED` | `false` | Habilita push de métricas para Datadog |
| `DATADOG_API_KEY` | — | API Key do Datadog |
| `DD_API_KEY` | — | API Key do Datadog Agent (docker-compose) |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | `http://localhost:4317` | Endpoint OTLP do Datadog Agent |
| `TRACING_ENABLED` | `true` | Habilita distributed tracing |
| `TRACING_SAMPLE_RATE` | `1.0` | Taxa de amostragem (0.0 – 1.0) |
| `APP_ENV` | `local` | Tag de ambiente (`local`, `staging`, `prod`) |

---

## Testes

```bash
# Executar todos os testes
cd insurance && ./mvnw test

# Executar com relatório de cobertura JaCoCo (target/site/jacoco/index.html)
cd insurance && ./mvnw verify
```

### Suíte de Testes

| Classe de Teste | Estratégia | O que cobre |
|---|---|---|
| `InsuranceQuoteControllerTest` | `@WebMvcTest` + Mockito | Endpoints HTTP, status codes, body de resposta e erro |
| `InsuranceQuoteServiceTest` | Unit (Mockito) | Criação de cotação, cadeia de validators, atualização por apólice |
| `PolicyEventListenerTest` | Unit (Mockito) | Polling SQS, processamento de batch, JSON inválido, falhas de infraestrutura |
| `SqsQuoteEventPublisherTest` | Unit (Mockito) | Publicação SQS, serialização, wrapping de exceção, fallback de circuit breaker |
| `InsuranceQuoteRepositoryTest` | Unit (Mockito) | CRUD DynamoDB, circuit breaker fallbacks |
| `CatalogServiceAdapterTest` | Unit (Mockito) | Validação produto/oferta, retry, circuit breaker, métricas |
| `InsuranceQuoteResponseMapperTest` | Unit | Mapeamento de entidade para DTO de resposta |

---

## Princípios de Design Aplicados

| Princípio | Aplicação |
|---|---|
| **Hexagonal Architecture** | Domínio isolado de frameworks; toda dependência externa via porta/adaptador |
| **DIP** | Adaptadores dependem de interfaces do domínio, nunca o contrário |
| **ISP** | Use-cases separados: `InsuranceQuoteUseCase` e `PolicyUpdateUseCase` são interfaces independentes |
| **SRP** | Cada `QuoteValidator` implementa exatamente uma regra de negócio |
| **OCP** | Novos validadores são adicionados implementando `QuoteValidator` e anotando com `@Order`, sem alterar o serviço |
| **Resiliência** | Circuit Breaker + Retry em todas as integrações externas (Catalog HTTP, DynamoDB, SQS) |
| **Observabilidade** | Métricas de negócio e latência com percentis p50/p95/p99 + distributed tracing via OTLP |


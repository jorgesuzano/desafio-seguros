# Datadog — Guia de Implantação e Dashboard

## Visão Geral da Arquitetura

```
┌─────────────────────────────────────────────────────────────────────┐
│  Docker Compose                                                      │
│                                                                      │
│  ┌──────────────────┐   métricas (Micrometer push, step=1m)         │
│  │  insurance-app   │──────────────────────────────────────────────►│
│  │  Spring Boot 3   │   traces (OTLP gRPC :4317)                    │
│  │  port: 8080      │──────────────────┐                            │
│  └──────────────────┘                  │                            │
│                                        ▼                            │
│                          ┌──────────────────────┐                   │
│                          │   datadog-agent:7    │                   │
│                          │   OTLP gRPC  :4317   │──► Datadog Cloud  │
│                          │   OTLP HTTP  :4318   │    (metrics,      │
│                          │   DogStatsD  :8125   │     traces, logs) │
│                          └──────────────────────┘                   │
│                                        ▲                            │
│                          logs JSON via Docker log driver             │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 1. Pré-requisitos

| Item | Onde obter |
|---|---|
| Conta Datadog (free trial OK) | https://app.datadoghq.com |
| API Key | Organization Settings → API Keys |
| Docker + Docker Compose | Já configurado no projeto |

---

## 2. Configuração Inicial

### 2.1 Criar arquivo `.env`

```bash
cp .env.example .env
```

Edite `.env` e preencha sua **DD_API_KEY**:

```dotenv
DD_API_KEY=abc1234567890abcdef   # ← cole sua chave aqui
DD_SITE=datadoghq.com
DD_ENV=local
```

> ⚠️ Nunca commite o arquivo `.env`. Ele está no `.gitignore`.

### 2.2 Subir os serviços

```bash
# Na pasta insurance/
docker compose up --build
```

O **Datadog Agent** sobe automaticamente e começa a coletar:
- ✅ Métricas via **Micrometer → API Datadog** (push direto a cada 1 min)
- ✅ Traces via **OTLP gRPC → Datadog Agent → APM**
- ✅ Logs via **JSON estruturado → Docker → Datadog Agent**

---

## 3. Métricas Disponíveis

### 3.1 Métricas de Negócio (customizadas)

| Métrica Datadog | Tipo | Tags | Descrição |
|---|---|---|---|
| `insurance.quote.created.total` | Counter | `product_id`, `service`, `env` | Cotações criadas com sucesso |
| `insurance.quote.validation.error.total` | Counter | `reason`, `service`, `env` | Falhas de validação de negócio |
| `insurance.service.unavailable.total` | Counter | `dependency`, `service`, `env` | Rejeições por circuit-breaker aberto |
| `insurance.policy.updated.total` | Counter | `service`, `env` | Cotações promovidas para APPROVED |
| `insurance.quote.creation.duration` | Timer | `outcome`, `service`, `env` | Latência ponta-a-ponta do POST /quotes |
| `insurance.quote.get.duration` | Timer | `outcome`, `service`, `env` | Latência do GET /quotes/{id} |
| `insurance.quote.service.creation.duration` | Timer | `service`, `env` | Latência da camada de serviço (sem HTTP) |
| `insurance.catalog.calls.total` | Counter | `outcome`, `service`, `env` | Chamadas ao Catalog Service |
| `insurance.catalog.call.duration` | Timer | `service`, `env` | Latência das chamadas ao Catalog Service |

> **Nota sobre nomenclatura:** Micrometer converte pontos (`.`) em underscores (`_`) ao enviar
> para o Datadog. Busque no Datadog Explorer como `insurance_quote_created_total`.

### 3.2 Métricas Automáticas do Spring Boot Actuator

| Métrica | Descrição |
|---|---|
| `jvm.*` | Heap, GC, threads |
| `http.server.requests` | Latência HTTP por rota/status |
| `resilience4j.circuitbreaker.*` | Estado do circuit-breaker, calls, failures |
| `spring.data.repository.*` | Latência do repositório |
| `cache.*` | Hits/misses do Caffeine Cache |
| `system.cpu.usage` | CPU do processo |
| `disk.free` | Espaço em disco |

---

## 4. APM — Rastreamento Distribuído

Com o perfil `docker` ativo, cada request HTTP gera um **trace** completo com spans para:
- Controller HTTP
- Chamadas ao Catalog Service (Feign)
- DynamoDB (AWS SDK instrumentado via OTel)
- SQS publish

**Como visualizar:**
1. Acesse **APM → Traces** no Datadog
2. Filtre por `service:insurance-quote-service`
3. Clique em qualquer trace para ver o **Flame Graph**

---

## 5. Logs — Correlação com Traces

Os logs em ambiente `docker`/`prod` são emitidos em **JSON estruturado**:

```json
{
  "@timestamp": "2026-03-25T10:30:00.123Z",
  "level": "INFO",
  "message": "Quote created – id: abc-123",
  "logger_name": "br.com.desafio.insurance.application.InsuranceQuoteService",
  "service": "insurance-quote-service",
  "env": "local",
  "version": "0.0.1",
  "dd.trace_id": "7626782758105526112",
  "dd.span_id":  "1234567890"
}
```

O campo `dd.trace_id` permite que o Datadog **conecte automaticamente** o log ao trace APM correspondente.

---

## 6. Criando um Dashboard no Datadog

### 6.1 Criar o Dashboard

1. No menu lateral: **Dashboards → New Dashboard**
2. Dê o nome: `Insurance Quote Service`
3. Selecione **Grid layout**

---

### 6.2 Widget 1 — Taxa de Cotações Criadas (Timeseries)

**Objetivo:** Ver o volume de cotações criadas ao longo do tempo.

1. Clique em **Add Widget → Timeseries**
2. Em **Graph your data**:
   - Metric: `insurance_quote_created_total`
   - From: `service:insurance-quote-service`
   - Sum by: `product_id`
3. Display: `Line`
4. Título: `📋 Cotações Criadas por Produto`

---

### 6.3 Widget 2 — Taxa de Erros de Validação (Top List)

**Objetivo:** Descobrir quais validações mais falham.

1. **Add Widget → Top List**
2. Metric: `insurance_quote_validation_error_total`
3. Group by: `reason`
4. Sort: `Top 10 by Value`
5. Título: `⚠️ Top Erros de Validação`

---

### 6.4 Widget 3 — Latência do POST /quotes (Heat Map)

**Objetivo:** Visualizar distribuição de latência (p50, p95, p99).

1. **Add Widget → Timeseries**
2. Metric: `insurance_quote_creation_duration.percentile`
   - Ou use: `avg:insurance_quote_creation_duration{*} by {percentile}`
3. Filtro: `outcome:success`
4. Título: `⏱️ Latência de Criação de Cotação`

> **Alternativa via Query Formula:**
> ```
> p95:insurance_quote_creation_duration{service:insurance-quote-service, outcome:success}
> ```

---

### 6.5 Widget 4 — Estado do Circuit Breaker (Query Value)

**Objetivo:** Alerta visual quando o circuit-breaker está ABERTO.

1. **Add Widget → Query Value**
2. Metric: `resilience4j_circuitbreaker_state`
   - From: `service:insurance-quote-service, name:catalogService`
3. Título: `🔌 Circuit Breaker: Catalog Service`
4. Em **Conditional Formatting**:
   - `= 0` (CLOSED) → ✅ Verde
   - `= 1` (OPEN) → 🔴 Vermelho
   - `= 2` (HALF_OPEN) → 🟡 Amarelo

---

### 6.6 Widget 5 — Disponibilidade do Serviço (SLO Widget)

**Objetivo:** Monitorar SLO de disponibilidade.

1. **Add Widget → SLO Summary**  
   *(Primeiro crie o SLO em Service Mgmt → SLOs → New SLO)*
   - Type: **Metric-based**
   - Good events: `sum:insurance_quote_created_total{*}.as_count()`
   - Total events: `sum:http_server_requests_seconds_count{uri:/api/v1/quotes}.as_count()`
   - Target: **99.5%**

---

### 6.7 Widget 6 — Chamadas ao Catalog Service (Stacked Bar)

**Objetivo:** Ver proporção sucesso/erro/circuit-open nas chamadas externas.

1. **Add Widget → Timeseries**
2. Metric: `insurance_catalog_calls_total`
3. Sum by: `outcome`
4. Display: **Bars** (stacked)
5. Título: `📡 Chamadas ao Catalog Service por Resultado`

---

### 6.8 Widget 7 — JVM — Heap e GC (Timeseries)

1. **Add Widget → Timeseries**
2. Adicione **duas queries**:
   - `a`: `avg:jvm_memory_used_bytes{area:heap, service:insurance-quote-service}`
   - `b`: `avg:jvm_memory_max_bytes{area:heap, service:insurance-quote-service}`
3. Fórmula: `(a / b) * 100`
4. Título: `☕ JVM Heap Usage (%)`

---

### 6.9 Widget 8 — Cotações Aprovadas (Gauge)

1. **Add Widget → Query Value**
2. Metric: `insurance_policy_updated_total`
3. Aggregation: `Sum` (last 1h)
4. Título: `✅ Apólices Geradas (última hora)`

---

### 6.10 Resultado Final do Dashboard

```
┌────────────────────────────────────────────────────────────────────┐
│  Insurance Quote Service Dashboard                                 │
├─────────────────┬──────────────────┬──────────────────────────────┤
│ Cotações/min    │ Erros Validação  │ Latência p50/p95/p99         │
│ (Timeseries)    │ (Top List)       │ (Timeseries)                 │
├─────────────────┼──────────────────┼──────────────────────────────┤
│ Circuit Breaker │ SLO              │ Catalog Calls by Outcome     │
│ (Query Value)   │ (SLO Widget)     │ (Stacked Bar)                │
├─────────────────┼──────────────────┼──────────────────────────────┤
│ JVM Heap %      │ Apólices Geradas │                              │
│ (Timeseries)    │ (Query Value)    │                              │
└─────────────────┴──────────────────┴──────────────────────────────┘
```

---

## 7. Criando Monitores (Alertas)

### 7.1 Alerta: Circuit Breaker Aberto

1. **Monitors → New Monitor → Metric**
2. Query: `max:resilience4j_circuitbreaker_state{name:catalogService} > 0`
3. Alert condition: `above 0 for 1 minute`
4. Message: `🚨 Circuit Breaker catalogService está ABERTO! Verifique a saúde do Catalog Service.`
5. Notify: `@slack-channel` ou `@pagerduty`

### 7.2 Alerta: Taxa de Erros Alta

1. **Monitors → New Monitor → Metric**
2. Query:
   ```
   sum:insurance_quote_validation_error_total{*}.as_rate()
   /
   sum:insurance_quote_created_total{*}.as_rate()
   ```
3. Alert: `> 0.2` (>20% de erro)

### 7.3 Alerta: Latência Alta (p95)

1. Query: `p95:insurance_quote_creation_duration{outcome:success} > 2`
2. Alert: latência p95 acima de 2 segundos

---

## 8. Verificação Rápida

Após subir com `docker compose up`, valide:

```bash
# 1. Verifique se o Datadog Agent está saudável
curl http://localhost:8125/status  # ou: docker logs datadog-agent

# 2. Verifique as métricas do Actuator (Prometheus endpoint local)
curl http://localhost:8080/actuator/prometheus | grep insurance_

# 3. Dispare algumas cotações para gerar dados
curl -X POST http://localhost:8080/api/v1/quotes \
  -H 'Content-Type: application/json' \
  -d '{ "productId": "prod-1", "offerId": "offer-1", ... }'
```

Após ~2 minutos, as métricas aparecem em **Metrics Explorer** no Datadog.

---

## 9. Variáveis do Dashboard (Filtros Globais)

Para tornar o dashboard reutilizável entre ambientes:

1. Em **Dashboard Settings → Template Variables**:
   - `$env` → tag `env` → valores: `local`, `staging`, `prod`
   - `$service` → tag `service` → valor padrão: `insurance-quote-service`

2. Use em cada widget: `service:$service, env:$env`

Assim o mesmo dashboard funciona para todos os ambientes com um único filtro.


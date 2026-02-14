# Reco System (Java 21 + Spring Boot 3.2)

Sistema de recomendação de conteúdo (artigos/vídeos/produtos) no estilo YouTube/Netflix:
- Aprende preferências do usuário via interações (view/like/share/bookmark + tempo de leitura)
- Representa conteúdo e perfil do usuário como embeddings
- Recomenda via similaridade vetorial + diversidade + explicabilidade
- Reduz custo com cache multicamadas e batch noturno

---

## 1) Arquitetura (visão geral)

```
┌──────────────┐      ┌─────────────────────┐      ┌────────────────────┐
│   Client     │─────▶│  Spring Boot API     │─────▶│   PostgreSQL 16     │
└──────────────┘      │  (Virtual Threads)   │      │ content/interactions│
        │             └─────────┬───────────┘      └────────────────────┘
        │                       │
        │                       │ 1) Embeddings
        │                       ▼
        │             ┌─────────────────────┐
        │             │ OpenAI Embeddings   │
        │             └─────────────────────┘
        │                       │ 2) Vetores
        │                       ▼
        │             ┌─────────────────────┐
        │             │   Qdrant Vector DB  │
        │             └─────────────────────┘
        │                       ▲
        │                       │ 3) Cache multicamadas
        │                       ▼
        │             ┌─────────────────────┐
        └────────────▶│ Redis 7 + Caffeine  │
                      └─────────────────────┘
```

---

## 2) Arquitetura de Embeddings

### O que embeddamos (para reduzir custo)
Em vez de embeddar corpo completo, embeddamos apenas campos “alta-sinal”:

- title
- description (resumo)
- category
- tags

`ContentService.buildEmbeddingText(...)` monta um **text block**.

### Onde os embeddings vivem
- **Qdrant**: vetor do conteúdo (id = contentId) + payload (category/tags/type)
- **Redis L2**: cache dos vetores (24h) para leitura rápida
- **Caffeine (L0)**: cache local de embeddings/points para hot-path

### Dedupe (reuso)
Para texto igual (mesmo hash), reaproveitamos o embedding:
- chave L2 por `sha256(text)`

---

## 3) Estratégia de cache multicamadas (L0/L1/L2/L3)

### L0 — Caffeine (memória)
- `embeddingLocal` e `qdrantPointLocal`
- reduz latência e uso de rede em caminhos críticos

### L1 — Recomendações frequentes (Redis, TTL 5min)
- chave: `l1:reco:u:{userId}:a:{algo}`
- guarda **top-10 final** (já diversificado + explicável)
- **invalidação seletiva**: ao registrar interação do usuário, deletamos as chaves L1 do usuário

### L2 — Embeddings calculados (Redis, TTL 24h)
- chave por texto: `l2:emb:{sha256(text)}`
- chave por conteúdo: `l2:emb:content:{contentId}`

### L3 — Queries similares (Redis, TTL 1h)
- chave: `l3:q:{sha256(algo + quantized(userVector))}`
- guarda lista de ids retornados do Qdrant para vetores “muito parecidos” do usuário

### Fluxo (miss/hit)
```
GET /recommendations/{userId}
  1) tenta L1 (hit -> retorna)
  2) monta userVector (L2 para vetores de conteúdos do histórico)
  3) tenta L3 (hit -> reusa candidatos)
  4) se miss em L3 -> consulta Qdrant
  5) rerank + diversidade + explicação
  6) grava L1
```

Fallback:
- se usuário sem interações → cold-start por popularidade (últimos 7 dias)

---

## 4) Algoritmos de recomendação

### Perfil do usuário (vetor)
Média ponderada dos vetores dos conteúdos interagidos, com **decay**:

u = (Σ w_i * v_i) / (Σ w_i)

Peso com meia-vida:
w_i = base(kind_i) * 0.5^(age_days / half_life_days)

Bases:
- LIKE: 3.0
- BOOKMARK: 2.5
- SHARE: 2.0
- VIEW: 1.0

Feedback implícito:
- VIEW com `value >= 0.8` conta como “like implícito” (peso maior)

### Similaridade (A/B testing)
Bucket por hash do userId:
- COSINE vs DOT

Cosine:
cos(a,b) = dot(a,b) / (||a|| * ||b||)

Dot:
dot(a,b) = Σ a_k*b_k

### Diversidade
- limite por categoria (`max-per-category`)
- bloqueio de near-duplicates: se cosine > 0.97 em relação ao já escolhido, descarta

### Explicabilidade
Para cada recomendado, buscamos o item do histórico mais similar:
- “Recomendado porque você interagiu com X”

---

## 5) Otimizações de custo (OpenAI)

- Embedding apenas de campos curtos (título+resumo+tags)
- Cache L2 por texto hash (dedupe)
- Batch noturno (reindex) fora de horário de pico
- Warmup L1 para top usuários (candidatos via query de interações)

---

## 6) Jobs batch (madrugada)
`BatchJobs` roda às 03:00 (America/Bahia):
- reindexa conteúdos atualizados últimos 7 dias
- warmup de recomendação para top 1000 usuários ativos (últimas 24h)

---

## 7) Métricas e dashboard
Armazena eventos em `metric_event`:
- RECO_REQUEST (cache hit/miss)
- EMBEDDING (hit/miss, tokens estimados)

Endpoint:
- `GET /api/dashboard` → requests, hit-rate, tokens e custo estimado 24h

---

## 8) Como testar (casos de uso)

### Novo usuário (cold start)
1) `POST /api/users`
2) `GET /api/recommendations/{id}` → deve vir popularidade

### Usuário só tecnologia
1) envie várias interações em conteúdos categoria "Tecnologia"
2) `GET /api/recommendations/{id}` → deve privilegiar Tecnologia + diversidade limitada

### Interesses diversos
Interaja com múltiplas categorias; observe que `max-per-category` mantém variedade.

### Usuário inativo voltando
O decay reduz peso das interações antigas; conteúdos recentes dominam.

### Cache hit vs miss
- chame recomendações 2x seguidas: segunda tende a vir de L1 (cacheHit=true)

---

## 9) Endpoints
- POST `/api/users`
- POST `/api/contents`
- GET `/api/contents`
- POST `/api/interactions`
- GET `/api/recommendations/{userId}`
- GET `/api/recommendations/{userId}/more-like/{contentId}`
- GET `/api/trending`
- GET `/api/dashboard`

Swagger:
- `/swagger-ui.html`

---

## 10) Artefatos
- `docker-compose.yml`: Postgres + Redis + Qdrant
- `sql/analysis.sql`: queries analíticas
- `postman_collection.json`: fluxos completos
- `cache_hit_rate.png` e `openai_cost.png`: gráficos de exemplo


## 11) Prints de funcionamento

![Inicio reco](https://github.com/user-attachments/assets/f48b5a50-4d7a-4da0-9fd2-c4a42323fca2)

![Id reco](https://github.com/user-attachments/assets/d823fbd3-209c-42bd-94c3-da4b4eecd0e9)

![analsys](https://github.com/user-attachments/assets/31b29c88-cab3-4a6f-97dc-ff922b7b5aeb)

![recomendation](https://github.com/user-attachments/assets/0aa4587e-6f98-44f1-9f85-a6c9daaebcb6)

![mores likes](https://github.com/user-attachments/assets/1fa06247-7afc-422d-b8c7-0cd571432e22)

![recomendatio true](https://github.com/user-attachments/assets/c8595d9f-ffa9-45ad-9f49-5226c721b3c5)

![Recomendation true v2](https://github.com/user-attachments/assets/62b6a3c3-42d6-40a2-ada9-1cb42515e326)




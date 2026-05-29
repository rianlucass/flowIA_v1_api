# Candidatos (Candidates) — Flowia API

Documentação da feature de candidatos da Flowia API. O fluxo principal permite que candidatos se inscrevam em vagas diretamente pela API através de upload de PDF. A API persiste o arquivo, chama o serviço OCR e notifica o N8N para continuar o pipeline de análise com IA.

---

## Sumário

- [Visão Geral](#visão-geral)
- [Escopo Atual](#escopo-atual)
- [Fluxo Principal](#fluxo-principal)
- [Endpoints](#endpoints)
  - [POST /jobs/{jobId}/apply — Candidatura pública via PDF](#post-jobsjobidapply--candidatura-pública-via-pdf)
  - [POST /candidates — Criar candidato (N8N / manual)](#post-candidates--criar-candidato-n8n--manual)
  - [POST /candidates/upload — Upload legado](#post-candidatesupload--upload-legado)
  - [GET /candidates/job/{jobId} — Listar por vaga](#get-candidatesjobJobId--listar-por-vaga)
- [Modelo de Dados](#modelo-de-dados)
- [Status do Candidato](#status-do-candidato)
- [DTOs](#dtos)
- [Serviços](#serviços)
- [Camada de Persistência](#camada-de-persistência)
- [Configuração](#configuração)
- [Decisões de Arquitetura](#decisões-de-arquitetura)

---

## Visão Geral

A entidade `Candidate` representa um candidato associado a uma vaga (`Job`). O fluxo de candidatura é totalmente gerenciado pela API:

1. O recrutador cria a vaga pelo painel;
2. O frontend gera uma página pública de candidatura (`/jobs/{jobId}`);
3. O candidato acessa a página e envia o PDF do currículo;
4. A API recebe o PDF, salva em disco, extrai o texto via OCR e notifica o N8N;
5. O N8N executa a análise com IA e envia os resultados para `POST /analysis`.

---

## Escopo Atual

- Entidade JPA `Candidate`, enum `CandidateStatus`;
- `ApplicationController` com endpoint público `POST /jobs/{jobId}/apply`;
- `CandidatesService` com `apply()`, `create()`, `createFromUpload()` e `getCandidatesByJobId()`;
- `FileStorageService` para persistência local dos PDFs em `uploads/{recruiterId}/{jobId}/{candidateId}.pdf`;
- `OcrService` para extração de texto via serviço Python/Flask (WebClient);
- `N8NWebhookService` para notificação do N8N após o upload;
- `CandidateController` com endpoints autenticados;
- `CandidateRepository` com busca por `jobId`.

---

## Fluxo Principal

```
Candidato
  │
  └─ POST /jobs/{jobId}/apply  (PDF)       ← sem autenticação
        │
        ├─ Valida que é PDF                ← 400 se não for
        ├─ Busca Job                       ← 404 se não existir
        ├─ Cria Candidate stub             ← status: RECEIVED
        ├─ FileStorageService              ← salva em uploads/
        ├─ OcrService.extractText()        ← extrai texto do PDF
        │     ├─ OK: status → PROCESSING, resumeText salvo
        │     └─ Erro: loga, continua
        └─ N8NWebhookService.notify()      ← POST { candidateId, jobId, resumeUrl, resumeText, criteria }
              ├─ OK: loga confirmação
              └─ Erro: loga, não falha a request

N8N
  └─ Recebe webhook → OpenAI analisa → Code JS calcula scores
  └─ POST /analysis  (header: X-Service-Key)
       └─ Candidate atualizado com dados do AI (name, phone, city, etc.)
       └─ CandidateAnalysis salvo no banco
```

---

## Endpoints

### POST /jobs/{jobId}/apply — Candidatura pública via PDF

Endpoint principal de candidatura. Recebe o currículo em PDF, salva no disco, extrai texto via OCR e dispara o pipeline N8N.

**Acesso:** público — sem autenticação  
**Content-Type:** `multipart/form-data`

#### Parâmetros

| Campo | Tipo | Obrigatório |
|---|---|---|
| `file` | `MultipartFile` (PDF) | Sim |

#### Fluxo interno

1. Valida content-type `application/pdf` (`400` se inválido);
2. Busca a vaga pelo `jobId` (`404` se não existir);
3. Cria um `Candidate` stub com `status = RECEIVED`;
4. Salva o PDF em `uploads/{recruiterId}/{jobId}/{candidateId}.pdf`;
5. Chama `OcrService.extractText()` → atualiza `resumeText`, `status = PROCESSING`;
6. Envia webhook ao N8N com `candidateId`, `jobId`, `resumeUrl`, `resumeText` e `criteria` da vaga.

#### Response — `201 Created`

```json
{
  "candidateId": "4ed2c8dd-6d5a-4f6d-a7d9-40a4c39f3c7d",
  "jobId": "f49719d7-c9a2-4bb0-9a2b-8d8ce9d27bbf",
  "resumeUrl": "recruiterId/jobId/4ed2c8dd-6d5a-4f6d-a7d9-40a4c39f3c7d.pdf",
  "status": "PROCESSING"
}
```

#### Possíveis Erros

| Status | Cenário |
|---|---|
| `400 Bad Request` | Arquivo ausente ou não é PDF |
| `404 Not Found` | `jobId` não encontrado |

---

### POST /candidates — Criar candidato (N8N / manual)

Cria um candidato a partir de dados estruturados em JSON. Usado pelo N8N no fluxo legado (email → parse → candidato) ou manualmente.

**Acesso:** requer autenticação

#### Request Body

```json
{
  "jobId": "f49719d7-c9a2-4bb0-9a2b-8d8ce9d27bbf",
  "name": "Maria Silva",
  "email": "maria@email.com",
  "phone": "+55 11 99999-9999",
  "city": "São Paulo",
  "state": "SP",
  "linkedinUrl": "https://linkedin.com/in/mariasilva",
  "portfolioUrl": "https://maria.dev",
  "resumeUrl": "https://storage.example.com/resume.pdf",
  "resumeText": "Desenvolvedora backend com experiência em Java e Spring Boot...",
  "status": "RECEIVED",
  "processedByAi": false
}
```

| Campo | Tipo | Validação |
|---|---|---|
| `jobId` | `string` | Obrigatório |
| `name` | `string` | Opcional |
| `email` | `string` | Opcional, `@Email` se informado |
| `resumeText` | `string` | Opcional |
| `status` | `CandidateStatus` | Opcional — padrão `RECEIVED` |
| `phone`, `city`, `state`, `linkedinUrl`, `portfolioUrl`, `resumeUrl` | `string` | Opcional |
| `processedByAi` | `boolean` | Opcional — padrão `false` |

#### Response — `201 Created`

Retorna `CandidateResponseDTO` (ver [DTOs](#dtos)).

#### Possíveis Erros

| Status | Cenário |
|---|---|
| `400 Bad Request` | `jobId` ausente ou e-mail inválido |
| `401 Unauthorized` | Token ausente ou inválido |
| `404 Not Found` | `jobId` não encontrado |

---

### POST /candidates/upload — Upload legado

Recebe PDF + dados opcionais, chama OCR internamente e cria o candidato. Não dispara webhook N8N.

**Acesso:** requer autenticação  
**Content-Type:** `multipart/form-data`

#### Parâmetros (form-data)

| Campo | Tipo | Obrigatório |
|---|---|---|
| `file` | `MultipartFile` | Sim |
| `jobId` | `string` | Sim |
| `name` | `string` | Não |
| `email` | `string` | Não |

#### Response — `201 Created`

```json
{
  "candidateId": "4ed2c8dd-6d5a-4f6d-a7d9-40a4c39f3c7d",
  "jobId": "f49719d7-c9a2-4bb0-9a2b-8d8ce9d27bbf",
  "candidateName": "Maria Silva",
  "resumeText": "Texto extraído pelo OCR...",
  "processedByAi": false
}
```

---

### GET /candidates/job/{jobId} — Listar por vaga

Retorna todos os candidatos de uma vaga.

**Acesso:** requer autenticação

#### Response — `200 OK`

```json
[
  {
    "id": "4ed2c8dd-6d5a-4f6d-a7d9-40a4c39f3c7d",
    "jobId": "f49719d7-c9a2-4bb0-9a2b-8d8ce9d27bbf",
    "name": "Maria Silva",
    "email": "maria@email.com",
    "phone": "+55 11 99999-9999",
    "city": "São Paulo",
    "state": "SP",
    "linkedinUrl": "https://linkedin.com/in/mariasilva",
    "portfolioUrl": "https://maria.dev",
    "resumeUrl": "recruiterId/jobId/4ed2c8dd.pdf",
    "resumeText": "Desenvolvedora backend...",
    "status": "PROCESSING",
    "processedByAi": false,
    "analysisOutdated": false,
    "createdAt": "2026-05-28T10:15:00"
  }
]
```

---

## Modelo de Dados

Entidade `Candidate` mapeada para a tabela `candidates`.

| Coluna | Tipo Java | Nullable | Observação |
|---|---|---|---|
| `id` | `String` | NÃO | PK, UUID gerado automaticamente |
| `job_id` | `Job` | NÃO | FK para a vaga |
| `name` | `String` | sim | Preenchido após OCR ou pelo N8N |
| `email` | `String` | sim | Preenchido após OCR ou pelo N8N |
| `phone` | `String` | sim | Telefone |
| `city` | `String` | sim | Cidade |
| `state` | `String` | sim | Estado |
| `linkedin_url` | `String` | sim | Perfil do LinkedIn |
| `portfolio_url` | `String` | sim | Portfólio |
| `resume_url` | `String` | sim | Caminho relativo do PDF em disco |
| `resume_text` | `String` (TEXT) | sim | Texto extraído pelo OCR |
| `status` | `CandidateStatus` | sim | Status no pipeline |
| `processed_by_ai` | `Boolean` | NÃO | Padrão `false` |
| `analysis_outdated` | `Boolean` | NÃO | Padrão `false` |
| `created_at` | `LocalDateTime` | NÃO | Preenchido automaticamente |

> **Migrations manuais necessárias no banco:**
> ```sql
> ALTER TABLE candidates ALTER COLUMN name DROP NOT NULL;
> ALTER TABLE candidates ALTER COLUMN email DROP NOT NULL;
> ALTER TABLE candidates ALTER COLUMN resume_text DROP NOT NULL;
> ```

---

## Status do Candidato

```text
RECEIVED    — PDF recebido, stub criado
PROCESSING  — OCR concluído, aguardando análise N8N
REVIEW      — Análise pronta, aguardando revisão humana
APPROVED    — Aprovado para avançar no funil
REJECTED    — Reprovado no processo
HIRED       — Contratado
```

---

## DTOs

### `ApplyJobResponseDTO`

Resposta do endpoint público de candidatura.

```text
candidateId  — string (UUID)
jobId        — string (UUID)
resumeUrl    — string (caminho relativo do PDF)
status       — CandidateStatus
```

### `CreateCandidateRequestDTO`

```text
jobId         — string, @NotBlank
name          — string, opcional
email         — string, opcional, @Email se informado
resumeText    — string, opcional
status        — CandidateStatus, opcional (padrão RECEIVED)
phone         — string, opcional
city          — string, opcional
state         — string, opcional
linkedinUrl   — string, opcional
portfolioUrl  — string, opcional
resumeUrl     — string, opcional
processedByAi — boolean, opcional (padrão false)
```

### `CandidateResponseDTO`

```text
id                — string (UUID)
jobId             — string (UUID)
name              — string | null
email             — string | null
phone             — string | null
city              — string | null
state             — string | null
linkedinUrl       — string | null
portfolioUrl      — string | null
resumeUrl         — string | null
resumeText        — string | null
status            — CandidateStatus | null
processedByAi     — boolean
analysisOutdated  — boolean
createdAt         — LocalDateTime
```

### `CandidateUploadResponseDTO`

```text
candidateId   — string (UUID)
jobId         — string (UUID)
candidateName — string | null
resumeText    — string | null
processedByAi — boolean
```

### `N8NWebhookPayloadDTO`

Payload enviado ao N8N após o upload.

```text
candidateId — string (UUID)
jobId       — string (UUID)
resumeUrl   — string (caminho relativo)
resumeText  — string | null (null se OCR falhou)
criteria    — JobCriteria (critérios da vaga)
```

---

## Serviços

### `CandidatesService`

| Método | Descrição |
|---|---|
| `apply(jobId, file)` | Fluxo principal: salva PDF, OCR, webhook N8N → `ApplyJobResponseDTO` |
| `create(data)` | Cria candidato a partir de JSON (N8N/manual) → `CandidateResponseDTO` |
| `createFromUpload(jobId, name, email, resumeText)` | Cria candidato do fluxo legado de upload |
| `getCandidatesByJobId(jobId)` | Lista candidatos de uma vaga |

### `FileStorageService`

Persiste o PDF em disco na estrutura `uploads/{recruiterId}/{jobId}/{candidateId}.pdf`.

Configuração: `uploads.base-dir` (padrão `uploads/`).

| Método | Descrição |
|---|---|
| `storeResume(file, recruiterId, jobId, candidateId)` | Salva e retorna o caminho relativo |
| `deleteResume(recruiterId, jobId, candidateId)` | Remove o arquivo se existir |

### `OcrService`

Chama o serviço OCR externo (Python/Flask) via WebClient.

Configuração: `ocr.service.url` (padrão `http://localhost:5000`).

| Método | Descrição |
|---|---|
| `extractText(file)` | Envia o PDF e retorna o texto extraído |

### `N8NWebhookService`

Envia o payload para o webhook N8N após o upload. Falhas são tratadas internamente (loga e continua — não interrompe o fluxo do candidato).

Configuração: `n8n.webhook.url` (padrão `http://localhost:5678/webhook/apply`).

| Método | Descrição |
|---|---|
| `notifyApplication(payload)` | POST do payload para o N8N |

---

## Camada de Persistência

### `CandidateRepository`

```java
List<Candidate> findByJobId(String jobId)
```

---

## Configuração

| Propriedade | Variável de Ambiente | Padrão |
|---|---|---|
| `uploads.base-dir` | `UPLOADS_BASE_DIR` | `uploads` |
| `ocr.service.url` | `OCR_SERVICE_URL` | `http://localhost:5000` |
| `n8n.webhook.url` | `N8N_WEBHOOK_URL` | `http://localhost:5678/webhook/apply` |
| `spring.servlet.multipart.max-file-size` | `MULTIPART_MAX_FILE_SIZE` | `20MB` |
| `spring.servlet.multipart.max-request-size` | `MULTIPART_MAX_REQUEST_SIZE` | `25MB` |

---

## Decisões de Arquitetura

- **Fluxo público**: `POST /jobs/{jobId}/apply` não requer autenticação — qualquer candidato pode se inscrever com um PDF.
- **Candidato stub antes do OCR**: o `Candidate` é criado antes do OCR para garantir que o `candidateId` exista ao nomear o arquivo em disco.
- **Falhas silenciosas**: OCR e webhook N8N falham silenciosamente (log + continua) — a candidatura não é perdida se um serviço externo estiver fora do ar.
- **Campos nullable**: `name`, `email` e `resumeText` são nullable no banco — o stub inicial não possui esses dados; são preenchidos após OCR ou pelo N8N.
- **Armazenamento local**: PDFs ficam em disco na estrutura `uploads/{recruiterId}/{jobId}/{candidateId}.pdf`. Para produção, migrar para cloud storage (S3/GCS) é previsto na Etapa 6.
- **`analysisOutdated`**: sinaliza quando a análise de um candidato precisa de reprocessamento (ex.: critérios da vaga foram alterados).
- **`processedByAi`**: diferencia candidatos já enriquecidos pelo pipeline dos ainda pendentes.

- o endpoint `POST /candidates/upload` permite que um arquivo PDF seja enviado diretamente à API, que chama o serviço OCR internamente.

---

## Escopo Atual

No estado atual do projeto, a feature possui:

- entidade JPA `Candidate`;
- enum `CandidateStatus`;
- DTOs `CreateCandidateRequestDTO` e `CandidateResponseDTO`;
- `CandidateRepository` com busca por `jobId`;
- `CandidatesService` com `create()`, `createFromUpload()` e `getCandidatesByJobId()`;
- `OcrService` para extração de texto via serviço Python/Flask;
- `CandidateController` expondo `POST /candidates`, `POST /candidates/upload` e `GET /candidates/job/{jobId}`.

---

## Modelo de Dados

Entidade `Candidate` mapeada para a tabela `candidates`.

| Coluna | Tipo Java | Constraints | Observação |
|---|---|---|---|
| `id` | `String` | PK, gerado automaticamente | UUID |
| `job_id` | `Job` | `NOT NULL` | FK para a vaga do candidato |
| `name` | `String` | `NOT NULL` | Nome do candidato |
| `email` | `String` | `NOT NULL` | E-mail principal |
| `phone` | `String` | nullable | Telefone |
| `city` | `String` | nullable | Cidade |
| `state` | `String` | nullable | Estado |
| `linkedin_url` | `String` | nullable | Perfil do LinkedIn |
| `portfolio_url` | `String` | nullable | Portfólio ou site pessoal |
| `resume_url` | `String` | nullable | Link do currículo original |
| `resume_text` | `String` | `NOT NULL` | Texto extraído do currículo |
| `status` | `CandidateStatus` | nullable | Situação do candidato no pipeline |
| `processed_by_ai` | `Boolean` | `NOT NULL` | Indica se o currículo já foi processado por IA |
| `analysis_outdated` | `Boolean` | `NOT NULL` | Indica se a análise ficou desatualizada |
| `created_at` | `LocalDateTime` | `NOT NULL`, imutável | Preenchido automaticamente |

---

## Status do Candidato

Os status atualmente disponíveis são:

```text
RECEIVED
PROCESSING
REVIEW
APPROVED
REJECTED
HIRED
```

Leitura sugerida de cada status:

| Status | Significado |
|---|---|
| `RECEIVED` | Currículo recebido e persistido |
| `PROCESSING` | Em processamento por pipeline automatizado |
| `REVIEW` | Pronto para revisão humana |
| `APPROVED` | Aprovado para avançar no funil |
| `REJECTED` | Reprovado no processo |
| `HIRED` | Contratado |

---

## DTO de Resposta

O DTO de saída atual é `CandidateResponseDTO`.

```text
id                — string (UUID)
jobId             — string (UUID)
name              — string
email             — string
phone             — string | null
city              — string | null
state             — string | null
linkedinUrl       — string | null
portfolioUrl      — string | null
resumeUrl         — string | null
resumeText        — string
status            — CandidateStatus | null
processedByAi     — boolean
analysisOutdated  — boolean
createdAt         — LocalDateTime
```

---

## Modelo de Dados

### POST /candidates — Criar candidato

Cria um novo candidato. Usado pelo n8n após extrair e processar os dados do currículo.

**Acesso:** requer autenticação

#### Request Body

```json
{
  "jobId": "f49719d7-c9a2-4bb0-9a2b-8d8ce9d27bbf",
  "name": "Maria Silva",
  "email": "maria@email.com",
  "phone": "+55 11 99999-9999",
  "city": "São Paulo",
  "state": "SP",
  "linkedinUrl": "https://linkedin.com/in/mariasilva",
  "portfolioUrl": "https://maria.dev",
  "resumeUrl": "https://storage.example.com/resume.pdf",
  "resumeText": "Desenvolvedora backend com experiência em Java e Spring Boot...",
  "status": "RECEIVED",
  "processedByAi": false
}
```

| Campo | Tipo | Validação |
|---|---|---|
| `jobId` | `string` | Obrigatório — deve existir no banco |
| `name` | `string` | Obrigatório |
| `email` | `string` | Obrigatório, formato e-mail |
| `resumeText` | `string` | Obrigatório |
| `status` | `CandidateStatus` | Obrigatório |
| `phone`, `city`, `state`, `linkedinUrl`, `portfolioUrl`, `resumeUrl` | `string` | Opcional |
| `processedByAi` | `boolean` | Opcional — padrão `false` |

#### Response — `201 Created`

Retorna o `CandidateResponseDTO` do candidato criado (ver seção [DTOs](#dtos)).

#### Possíveis Erros

| Status | Cenário |
|---|---|
| `400 Bad Request` | Campos obrigatórios ausentes ou e-mail inválido |
| `401 Unauthorized` | Token ausente ou inválido |
| `404 Not Found` | `jobId` não encontrado |

---

### POST /candidates/upload — Upload de currículo

Recebe um PDF diretamente, chama o serviço OCR internamente para extrair o texto e persiste o candidato com status `RECEIVED`.

**Acesso:** requer autenticação  
**Content-Type:** `multipart/form-data`

#### Parâmetros (form-data)

| Campo | Tipo | Obrigatório |
|---|---|---|
| `file` | `MultipartFile` | Sim — arquivo PDF do currículo |
| `jobId` | `string` | Sim — UUID da vaga |
| `name` | `string` | Sim — nome do candidato |
| `email` | `string` | Sim — e-mail do candidato |

#### Fluxo interno

1. A API envia o arquivo para o serviço OCR (`OcrService`) via WebClient;
2. O OCR extrai o texto do PDF e retorna;
3. A API cria o candidato com status `RECEIVED` e `processedByAi = false`;
4. Retorna `CandidateUploadResponseDTO`.

#### Response — `201 Created`

```json
{
  "candidateId": "4ed2c8dd-6d5a-4f6d-a7d9-40a4c39f3c7d",
  "jobId": "f49719d7-c9a2-4bb0-9a2b-8d8ce9d27bbf",
  "candidateName": "Maria Silva",
  "processedByAi": false
}
```

#### Possíveis Erros

| Status | Cenário |
|---|---|
| `400 Bad Request` | Arquivo ausente ou parâmetros faltando |
| `401 Unauthorized` | Token ausente ou inválido |
| `404 Not Found` | `jobId` não encontrado |

---

### GET /candidates/job/{jobId} — Listar por vaga

Retorna todos os candidatos associados a uma vaga.

**Acesso:** requer autenticação

#### Response — `200 OK`

```json
[
  {
    "id": "4ed2c8dd-6d5a-4f6d-a7d9-40a4c39f3c7d",
    "jobId": "f49719d7-c9a2-4bb0-9a2b-8d8ce9d27bbf",
    "name": "Maria Silva",
    "email": "maria@email.com",
    "phone": "+55 11 99999-9999",
    "city": "São Paulo",
    "state": "SP",
    "linkedinUrl": "https://linkedin.com/in/mariasilva",
    "portfolioUrl": "https://maria.dev",
    "resumeUrl": "https://storage.example.com/resume.pdf",
    "resumeText": "Desenvolvedora backend com experiência em Java e Spring Boot...",
    "status": "REVIEW",
    "processedByAi": true,
    "analysisOutdated": false,
    "createdAt": "2026-05-27T10:15:00"
  }
]
```

#### Possíveis Erros

| Status | Cenário |
|---|---|
| `401 Unauthorized` | Token ausente ou inválido |

---

## DTOs

### `CreateCandidateRequestDTO`

```text
jobId           — string, @NotBlank
name            — string, @NotBlank
email           — string, @NotBlank, @Email
resumeText      — string, @NotBlank
status          — CandidateStatus, @NotNull
phone           — string, opcional
city            — string, opcional
state           — string, opcional
linkedinUrl     — string, opcional
portfolioUrl    — string, opcional
resumeUrl       — string, opcional
processedByAi   — boolean, opcional (padrão false)
```

### `CandidateResponseDTO`

```text
id                — string (UUID)
jobId             — string (UUID)
name              — string
email             — string
phone             — string | null
city              — string | null
state             — string | null
linkedinUrl       — string | null
portfolioUrl      — string | null
resumeUrl         — string | null
resumeText        — string
status            — CandidateStatus | null
processedByAi     — boolean
analysisOutdated  — boolean
createdAt         — LocalDateTime
```

### `CandidateUploadResponseDTO`

```text
candidateId     — string (UUID)
jobId           — string (UUID)
candidateName   — string
processedByAi   — boolean
```

---

## Camada de Persistência

### `CandidateRepository`

```java
List<Candidate> findByJobId(String jobId)
```

### `CandidatesService`

```java
CandidateResponseDTO create(CreateCandidateRequestDTO data)
CandidateResponseDTO createFromUpload(String jobId, String name, String email, String resumeText)
List<CandidateResponseDTO> getCandidatesByJobId(String jobId)
```

### `OcrService`

Chama o serviço Python/Flask via WebClient (WebFlux). A URL base é configurada pela propriedade `ocr.service.url` (padrão `http://localhost:5001`).

```java
String extractText(MultipartFile file)
```

---

## Decisões de Arquitetura

- A criação de candidatos via JSON (`POST /candidates`) é o fluxo principal — o n8n processa o currículo e envia os dados estruturados.
- O endpoint `POST /candidates/upload` permite internalizar o OCR na API, removendo a dependência do n8n ao serviço OCR externo.
- O campo `analysisOutdated` sinaliza quando uma análise precisa de reprocessamento (ex.: critérios da vaga foram alterados).
- O campo `processedByAi` diferencia candidatos já enriquecidos pelo pipeline dos ainda pendentes.
- O DTO expõe `jobId` ao invés do objeto `Job` completo, evitando acoplamento e problemas com relacionamentos LAZY.


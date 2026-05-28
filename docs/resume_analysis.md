# Análise de Currículo (Resume Analysis) — Flowia API

Documentação da feature de análise de currículos da Flowia API. Esta camada armazena o resultado estruturado da avaliação de um candidato em relação a uma vaga, incluindo score final, scores por dimensão, pontos fortes, alertas, perguntas de entrevista e metadados do modelo de IA utilizado.

---

## Sumário

- [Visão Geral](#visão-geral)
- [Escopo Atual](#escopo-atual)
- [Endpoints](#endpoints)
  - [POST /analysis — Criar análise](#post-analysis--criar-análise)
  - [GET /analysis/job/{jobId} — Buscar análise por vaga](#get-analysisjobJobId--buscar-análise-por-vaga)
- [Modelo de Dados](#modelo-de-dados)
- [Status da Análise](#status-da-análise)
- [DTOs](#dtos)
- [Estruturas JSON](#estruturas-json)
- [Camada de Persistência](#camada-de-persistência)
- [Decisões de Arquitetura](#decisões-de-arquitetura)

---

## Visão Geral

A entidade `ResumeAnalysis` representa o resultado consolidado da análise de um currículo para uma vaga específica. Ela conecta:

- um `Candidate`;
- uma `Job`;
- um conjunto de scores estruturados;
- blocos `jsonb` com explicações e evidências produzidas pela IA;
- metadados de rastreabilidade, como modelo utilizado e versão do prompt.

Essa estrutura permite que o pipeline da Flowia mantenha uma análise auditável, rica em contexto e adequada para consumo por interfaces de recrutamento, automações e futuras reanálises.

---

## Escopo Atual

No estado atual do projeto, a feature possui:

- entidade JPA `ResumeAnalysis`;
- enum `AnalysisStatus`;
- DTOs `CreateResumeAnalysisRequestDTO` e `ResumeAnalysisResponseDTO`;
- `ResumeAnalysisRepository` com busca por `jobId`;
- `AnalysisService` com criação e consulta de análise;
- `ResumeAnalysisController` expondo `POST /analysis` e `GET /analysis/job/{jobId}`.

---

---

## Endpoints

### POST /analysis — Criar análise

Cria um novo registro de análise de currículo. Usado pelo n8n ou por qualquer agente do pipeline após processar um currículo.

**Acesso:** requer autenticação

#### Request Body

```json
{
  "candidateId": "f1f7638a-2d1d-442d-aec9-98f7199fef0f",
  "jobId": "f49719d7-c9a2-4bb0-9a2b-8d8ce9d27bbf",
  "finalScore": 87.50,
  "activitiesScore": 80.00,
  "experienceScore": 92.00,
  "educationScore": 75.00,
  "locationScore": 100.00,
  "stabilityScore": 85.00,
  "status": "COMPLETED",
  "strengths": { "items": ["Boa experiência com Java"] },
  "attentionPoints": { "items": ["Pouca experiência com arquitetura distribuída"] },
  "missingInformation": { "items": ["Nível de inglês não informado"] },
  "interviewQuestions": { "items": ["Explique uma API REST que você desenhou do zero"] },
  "recommendation": "Prosseguir para entrevista técnica.",
  "validations": { "resumeParsed": true, "criteriaMatched": true },
  "weightsUsed": { "experience": 35, "education": 15, "activities": 20, "location": 10, "stability": 20 },
  "aiModel": "gpt-4.1",
  "promptVersion": "resume-analysis-v1",
  "outdated": false
}
```

| Campo | Tipo | Validação |
|---|---|---|
| `candidateId` | `string` | Obrigatório — deve existir no banco |
| `jobId` | `string` | Obrigatório — deve existir no banco |
| `status` | `AnalysisStatus` | Obrigatório |
| `finalScore` … `stabilityScore` | `BigDecimal` | Opcional |
| `strengths`, `attentionPoints`, `missingInformation`, `interviewQuestions`, `validations`, `weightsUsed` | `Map<String, Object>` | Opcional |
| `recommendation` | `string` | Opcional |
| `aiModel` | `string` | Opcional |
| `promptVersion` | `string` | Opcional |
| `outdated` | `boolean` | Opcional — padrão `false` |

#### Response — `201 Created`

Retorna o mesmo schema de `ResumeAnalysisResponseDTO` (ver seção [DTOs](#dtos)).

#### Possíveis Erros

| Status | Cenário |
|---|---|
| `400 Bad Request` | Campos obrigatórios ausentes ou inválidos |
| `401 Unauthorized` | Token ausente ou inválido |
| `404 Not Found` | `candidateId` ou `jobId` não encontrado |

---

### GET /analysis/job/{jobId} — Buscar análise por vaga

Retorna a primeira análise encontrada para a vaga informada.

**Acesso:** requer autenticação

#### Response — `200 OK`

```json
{
  "id": "c01d54db-4046-4f78-99a8-c6d534d64a11",
  "candidateId": "f1f7638a-2d1d-442d-aec9-98f7199fef0f",
  "jobId": "f49719d7-c9a2-4bb0-9a2b-8d8ce9d27bbf",
  "finalScore": 87.50,
  "activitiesScore": 80.00,
  "experienceScore": 92.00,
  "educationScore": 75.00,
  "locationScore": 100.00,
  "stabilityScore": 85.00,
  "status": "COMPLETED",
  "strengths": { "items": ["Boa experiência com Java", "Aderência ao perfil da vaga"] },
  "attentionPoints": { "items": ["Pouca experiência com arquitetura distribuída"] },
  "missingInformation": { "items": ["Nível de inglês não informado"] },
  "interviewQuestions": { "items": ["Explique uma API REST que você desenhou do zero"] },
  "recommendation": "Prosseguir para entrevista técnica.",
  "validations": { "resumeParsed": true, "criteriaMatched": true },
  "weightsUsed": { "experience": 35, "education": 15, "activities": 20, "location": 10, "stability": 20 },
  "aiModel": "gpt-4.1",
  "promptVersion": "resume-analysis-v1",
  "outdated": false,
  "createdAt": "2026-05-27T11:00:00"
}
```

#### Possíveis Erros

| Status | Cenário |
|---|---|
| `401 Unauthorized` | Token ausente ou inválido |
| `404 Not Found` | Análise não encontrada para a vaga |

---

## Modelo de Dados

Entidade `ResumeAnalysis` mapeada para a tabela `resume_analysis`.

| Coluna | Tipo Java | Observação |
|---|---|---|
| `id` | `String` | PK, UUID gerado automaticamente |
| `candidate_id` | `Candidate` | Relação com o candidato analisado |
| `job_id` | `Job` | Relação com a vaga usada como base da análise |
| `final_score` | `BigDecimal` | Score final consolidado |
| `activities_score` | `BigDecimal` | Score relacionado a atividades e entregas |
| `experience_score` | `BigDecimal` | Score de experiência profissional |
| `education_score` | `BigDecimal` | Score de formação acadêmica |
| `location_score` | `BigDecimal` | Score de aderência geográfica |
| `stability_score` | `BigDecimal` | Score de estabilidade profissional |
| `status` | `AnalysisStatus` | Status da execução da análise |
| `strengths` | `Map<String, Object>` | Pontos fortes identificados |
| `attention_points` | `Map<String, Object>` | Pontos de atenção |
| `missing_information` | `Map<String, Object>` | Dados ausentes ou incompletos |
| `interview_questions` | `Map<String, Object>` | Sugestões de perguntas para entrevista |
| `recommendation` | `String` | Recomendação textual final |
| `validations` | `Map<String, Object>` | Validações aplicadas durante a análise |
| `weights_used` | `Map<String, Object>` | Pesos efetivamente utilizados no cálculo |
| `ai_model` | `String` | Modelo de IA utilizado |
| `prompt_version` | `String` | Versão do prompt usado no processamento |
| `outdated` | `Boolean` | Indica se a análise precisa ser reprocessada |
| `created_at` | `LocalDateTime` | Timestamp de criação automática |

Os campos estruturados em `Map<String, Object>` são armazenados como `jsonb` no PostgreSQL.

---

## Status da Análise

Os status disponíveis atualmente são:

```text
PENDING
IN_PROGRESS
COMPLETED
FAILED
```

Interpretação sugerida:

| Status | Significado |
|---|---|
| `PENDING` | A análise ainda não começou |
| `IN_PROGRESS` | A análise está em processamento |
| `COMPLETED` | A análise foi concluída com sucesso |
| `FAILED` | O processamento falhou |

---

---

## DTOs

### `CreateResumeAnalysisRequestDTO`

```text
candidateId         — string, @NotBlank
jobId               — string, @NotBlank
status              — AnalysisStatus, @NotNull
finalScore          — BigDecimal, opcional
activitiesScore     — BigDecimal, opcional
experienceScore     — BigDecimal, opcional
educationScore      — BigDecimal, opcional
locationScore       — BigDecimal, opcional
stabilityScore      — BigDecimal, opcional
strengths           — Map<String, Object>, opcional
attentionPoints     — Map<String, Object>, opcional
missingInformation  — Map<String, Object>, opcional
interviewQuestions  — Map<String, Object>, opcional
recommendation      — string, opcional
validations         — Map<String, Object>, opcional
weightsUsed         — Map<String, Object>, opcional
aiModel             — string, opcional
promptVersion       — string, opcional
outdated            — boolean, opcional (padrão false)
```

### `ResumeAnalysisResponseDTO`

```text
id                  — string (UUID)
candidateId         — string (UUID)
jobId               — string (UUID)
finalScore          — BigDecimal | null
activitiesScore     — BigDecimal | null
experienceScore     — BigDecimal | null
educationScore      — BigDecimal | null
locationScore       — BigDecimal | null
stabilityScore      — BigDecimal | null
status              — AnalysisStatus | null
strengths           — Map<String, Object> | null
attentionPoints     — Map<String, Object> | null
missingInformation  — Map<String, Object> | null
interviewQuestions  — Map<String, Object> | null
recommendation      — string | null
validations         — Map<String, Object> | null
weightsUsed         — Map<String, Object> | null
aiModel             — string | null
promptVersion       — string | null
outdated            — boolean | null
createdAt           — LocalDateTime
```

Decisão importante: o DTO expõe apenas `candidateId` e `jobId`, sem serializar as entidades completas. Isso reduz acoplamento e evita problemas de serialização com relacionamentos LAZY.

---

## Estruturas JSON

Alguns campos da análise foram modelados como `jsonb` para manter flexibilidade sem exigir migrations frequentes.

Campos JSON atuais:

- `strengths`
- `attentionPoints`
- `missingInformation`
- `interviewQuestions`
- `validations`
- `weightsUsed`

Esses campos podem armazenar estruturas como listas, mapas aninhados, justificativas textuais e evidências calculadas pelo pipeline de IA.

Exemplo ilustrativo:

```json
{
	"strengths": {
		"items": [
			"Experiência sólida com Spring Boot",
			"Bom alinhamento com APIs REST"
		]
	},
	"attentionPoints": {
		"items": [
			"Pouca evidência de liderança técnica"
		]
	},
	"weightsUsed": {
		"experience": 35,
		"education": 15,
		"activities": 20,
		"location": 10,
		"stability": 20
	}
}
```

---

## Camada de Persistência

### `ResumeAnalysisRepository`

Responsável pelo acesso à tabela `resume_analysis`.

Método customizado disponível:

```java
Optional<ResumeAnalysis> findFirstByJobId(String jobId)
```

Esse método retorna a primeira análise encontrada para uma vaga.

### `AnalysisService`

Responsável por criar e consultar análises.

Métodos disponíveis:

```java
// cria uma nova análise — @Transactional
ResumeAnalysisResponseDTO create(CreateResumeAnalysisRequestDTO data)

// retorna a primeira análise encontrada para a vaga
ResumeAnalysisResponseDTO getCandidatesAnalysisByJobId(String jobId)
```

Fluxo de `create`:

1. valida existência do `candidateId` via `CandidateRepository`;
2. valida existência do `jobId` via `JobRepository`;
3. monta a entidade `ResumeAnalysis` com os dados do DTO;
4. persiste e retorna o DTO de resposta.

Fluxo de `getCandidatesAnalysisByJobId`:

1. busca a primeira análise vinculada ao `jobId`;
2. lança erro se não encontrar resultado;
3. converte para `ResumeAnalysisResponseDTO` via método privado `toDTO()`.

---

## Decisões de Arquitetura

- A análise foi modelada como entidade própria, separada de `Candidate`, para manter histórico e extensibilidade do pipeline.
- Os campos ricos em estrutura foram armazenados como `jsonb`, evitando rigidez excessiva no schema.
- O DTO expõe apenas IDs de `Candidate` e `Job`, evitando serialização profunda de relacionamentos LAZY.
- O campo `outdated` prepara o domínio para reprocessamento quando os critérios da vaga mudarem.
- `aiModel` e `promptVersion` oferecem rastreabilidade importante para auditoria e comparação entre execuções.
- A criação de análises é transacional (`@Transactional`) garantindo consistência caso o save falhe após resolver as dependências.
- A conversão para DTO foi centralizada no método privado `toDTO()` do service para evitar duplicação entre `create` e `getCandidatesAnalysisByJobId`.


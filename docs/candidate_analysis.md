# Analise de Candidato (Candidate Analysis) - Flowia API

Documentacao da feature de analise de candidatos da Flowia API. Esta camada armazena o resultado estruturado da avaliacao de um candidato em relacao a uma vaga, incluindo score final, scores por dimensao, pontos fortes, alertas, motivos de eliminacao, perguntas de entrevista e metadados do modelo de IA utilizado.

---

## Sumario

- [Visao Geral](#visao-geral)
- [Escopo Atual](#escopo-atual)
- [Endpoints](#endpoints)
  - [POST /analysis - Criar analise](#post-analysis--criar-analise)
  - [GET /analysis/job/{jobId} - Listar analises por vaga](#get-analysisjobJobId--listar-analises-por-vaga)
- [Modelo de Dados](#modelo-de-dados)
- [Status da Analise](#status-da-analise)
- [DTOs](#dtos)
- [Estruturas JSON](#estruturas-json)
- [Camada de Persistencia](#camada-de-persistencia)
- [Migrations](#migrations)
- [Decisoes de Arquitetura](#decisoes-de-arquitetura)

---

## Visao Geral

A entidade `CandidateAnalysis` representa o resultado consolidado da analise de um candidato para uma vaga especifica. Ela conecta:

- um `Candidate`;
- uma `Job`;
- scores estruturados por dimensao de avaliacao;
- blocos `jsonb` com evidencias, alertas, motivos de eliminacao e perguntas de entrevista produzidas pela IA;
- metadados de rastreabilidade: modelo utilizado e versao do prompt.

---

## Escopo Atual

- Entidade JPA `CandidateAnalysis`, enum `AnalysisStatus`;
- DTOs `CreateCandidateAnalysisRequestDTO` e `CandidateAnalysisResponseDTO`;
- `CandidateAnalysisRepository` com busca por `jobId` e `candidateId`;
- `AnalysisService` com criacao e consulta de analises;
- `CandidateAnalysisController` expondo `POST /analysis` e `GET /analysis/job/{jobId}`.

---

## Endpoints

### POST /analysis - Criar analise

Cria um registro de analise de candidato. Chamado pelo N8N apos processar o curriculo com IA.

**Acesso:** `X-Service-Key` header (API Key interna — nao usa JWT Bearer)

```
X-Service-Key: <SERVICE_API_KEY configurado no .env>
```

> Este endpoint usa autenticacao por API Key em vez de JWT porque e chamado exclusivamente pelo N8N (servico interno). A chave e validada pelo `ServiceApiKeyFilter` usando comparacao em tempo constante (`MessageDigest.isEqual`) para prevenir timing attacks.

#### Request Body

```json
{
  "candidateId": "f1f7638a-2d1d-442d-aec9-98f7199fef0f",
  "jobId": "f49719d7-c9a2-4bb0-9a2b-8d8ce9d27bbf",
  "candidateName": "Maria Silva",
  "email": "maria@email.com",
  "phone": "(11) 99999-9999",
  "city": "Sao Paulo",
  "state": "SP",
  "linkedinUrl": "linkedin.com/in/mariasilva",
  "portfolioUrl": "informacao nao encontrada",
  "finalScore": 87.50,
  "activitiesScore": 80.00,
  "experienceScore": 92.00,
  "educationScore": 75.00,
  "locationScore": 100.00,
  "stabilityScore": 85.00,
  "status": "APPROVED",
  "strengths": { "items": ["Solida experiencia com Java"] },
  "attentionPoints": { "items": ["Pouca experiencia com arquitetura distribuida"] },
  "missingInformation": { "items": ["Nivel de ingles nao informado"] },
  "interviewQuestions": { "items": ["Explique uma API REST que voce desenhou do zero"] },
  "eliminationReasons": [],
  "recommendation": "Prosseguir para entrevista tecnica.",
  "validations": { "resumeParsed": true, "experienceMatchesRole": true },
  "weightsUsed": { "activities": 50, "experience": 20, "education": 20, "location": 5, "stability": 5 },
  "aiModel": "gpt-4.1",
  "promptVersion": "resume-analysis-v2",
  "outdated": false
}
```

| Campo | Tipo | Validacao |
|---|---|---|
| `candidateId` | `string` | Obrigatorio - deve existir no banco |
| `jobId` | `string` | Obrigatorio - deve existir no banco |
| `status` | `AnalysisStatus` | Obrigatorio |
| `candidateName`, `email`, `phone`, `city`, `state`, `linkedinUrl`, `portfolioUrl` | `string` | Opcional - extraidos pelo AI e salvos no `Candidate` |
| `finalScore` ... `stabilityScore` | `BigDecimal` | Opcional |
| `strengths`, `attentionPoints`, `missingInformation`, `interviewQuestions`, `validations`, `weightsUsed` | `Map<String, Object>` | Opcional |
| `eliminationReasons` | `List<String>` | Opcional - array de strings |
| `recommendation` | `string` | Opcional |
| `aiModel` | `string` | Opcional |
| `promptVersion` | `string` | Opcional |
| `outdated` | `boolean` | Opcional - padrao `false` |

> **Comportamento do servico:** alem de criar `CandidateAnalysis`, o `AnalysisService` atualiza automaticamente o `Candidate` com os dados de perfil extraidos pelo AI (`name`, `email`, `phone`, `city`, `state`, `linkedinUrl`, `portfolioUrl`). Valores `null`, vazios ou iguais a `"informacao nao encontrada"` sao ignorados. O status do `Candidate` e sincronizado com o resultado da analise (`APPROVED`, `REJECTED`, `REVIEW`). O campo `processedByAi` do `Candidate` e marcado como `true`.

#### Response - 201 Created

Retorna `CandidateAnalysisResponseDTO` (ver [DTOs](#dtos)).

#### Possiveis Erros

| Status | Cenario |
|---|---|
| `400 Bad Request` | Campos obrigatorios ausentes |
| `403 Forbidden` | `X-Service-Key` ausente ou invalido |
| `404 Not Found` | `candidateId` ou `jobId` nao encontrado |

---

### GET /analysis/job/{jobId} - Listar analises por vaga

Retorna **todas** as analises de candidatos de uma vaga.

**Acesso:** requer autenticacao JWT — apenas o recruiter dono da vaga pode consultar

> O servico valida que `job.recruiter.id == usuarioAutenticado.id`. Se a vaga pertencer a outro recruiter, retorna `403 Forbidden`.

#### Response - 200 OK

```json
[
  {
    "id": "c01d54db-4046-4f78-99a8-c6d534d64a11",
    "candidateId": "f1f7638a-2d1d-442d-aec9-98f7199fef0f",
    "jobId": "f49719d7-c9a2-4bb0-9a2b-8d8ce9d27bbf",
    "finalScore": 87.50,
    "status": "COMPLETED",
    "strengths": { "items": ["Boa experiencia com Java"] },
    "eliminationReasons": null,
    "recommendation": "Prosseguir para entrevista tecnica.",
    "aiModel": "gpt-4.1",
    "promptVersion": "resume-analysis-v1",
    "outdated": false,
    "createdAt": "2026-05-28T11:00:00"
  }
]
```

#### Possiveis Erros

| Status | Cenario |
|---|---|
| `401 Unauthorized` | Token ausente ou invalido |
| `403 Forbidden` | Vaga pertence a outro recruiter |
| `404 Not Found` | Vaga nao encontrada |

---

## Modelo de Dados

Entidade `CandidateAnalysis` mapeada para a tabela `candidate_analysis`.

| Coluna | Tipo Java | Observacao |
|---|---|---|
| `id` | `String` | PK, UUID gerado automaticamente |
| `candidate_id` | `Candidate` | FK para o candidato analisado |
| `job_id` | `Job` | FK para a vaga usada como base |
| `final_score` | `BigDecimal` | Score final consolidado |
| `activities_score` | `BigDecimal` | Score de atividades e entregas |
| `experience_score` | `BigDecimal` | Score de experiencia profissional |
| `education_score` | `BigDecimal` | Score de formacao academica |
| `location_score` | `BigDecimal` | Score de aderencia geografica |
| `stability_score` | `BigDecimal` | Score de estabilidade profissional |
| `status` | `AnalysisStatus` | Status da execucao da analise |
| `strengths` | `Map<String, Object>` (jsonb) | Pontos fortes |
| `attention_points` | `Map<String, Object>` (jsonb) | Pontos de atencao |
| `missing_information` | `Map<String, Object>` (jsonb) | Dados ausentes |
| `interview_questions` | `Map<String, Object>` (jsonb) | Sugestoes de perguntas |
| `elimination_reasons` | `List<String>` (jsonb array) | Motivos de eliminacao |
| `recommendation` | `String` (TEXT) | Recomendacao textual |
| `validations` | `Map<String, Object>` (jsonb) | Validacoes aplicadas |
| `weights_used` | `Map<String, Object>` (jsonb) | Pesos utilizados |
| `ai_model` | `String` | Modelo de IA utilizado |
| `prompt_version` | `String` | Versao do prompt |
| `outdated` | `Boolean` | Indica se precisa reprocessamento |
| `created_at` | `LocalDateTime` | Timestamp automatico |

---

## Status da Analise

```
PENDING      - Analise ainda nao iniciada
IN_PROGRESS  - Analise em processamento
COMPLETED    - Analise concluida (resultado neutro)
APPROVED     - Candidato aprovado pela analise de IA
REVIEW       - Candidato requer revisao manual
FAILED       - Processamento falhou
REJECTED     - Candidato eliminado (criterios eliminatorios nao atendidos)
```

> Os status `APPROVED`, `REJECTED` e `REVIEW` sincronizam automaticamente o `CandidateStatus` do candidato associado.

---

## DTOs

### CreateCandidateAnalysisRequestDTO

```
candidateId        - string, @NotBlank
jobId              - string, @NotBlank
status             - AnalysisStatus, @NotNull
candidateName      - string, opcional (extraido pelo AI)
email              - string, opcional (extraido pelo AI)
phone              - string, opcional (extraido pelo AI)
city               - string, opcional (extraido pelo AI)
state              - string, opcional (extraido pelo AI)
linkedinUrl        - string, opcional (extraido pelo AI)
portfolioUrl       - string, opcional (extraido pelo AI)
finalScore         - BigDecimal, opcional
activitiesScore    - BigDecimal, opcional
experienceScore    - BigDecimal, opcional
educationScore     - BigDecimal, opcional
locationScore      - BigDecimal, opcional
stabilityScore     - BigDecimal, opcional
strengths          - Map<String, Object>, opcional (formato: { "items": [...] })
attentionPoints    - Map<String, Object>, opcional
missingInformation - Map<String, Object>, opcional
interviewQuestions - Map<String, Object>, opcional
eliminationReasons - List<String>, opcional (array de strings)
recommendation     - string, opcional
validations        - Map<String, Object>, opcional
weightsUsed        - Map<String, Object>, opcional
aiModel            - string, opcional
promptVersion      - string, opcional
outdated           - boolean, opcional (padrao false)
```

### CandidateAnalysisResponseDTO

```
id                 - string (UUID)
candidateId        - string (UUID)
jobId              - string (UUID)
finalScore         - BigDecimal | null
activitiesScore    - BigDecimal | null
experienceScore    - BigDecimal | null
educationScore     - BigDecimal | null
locationScore      - BigDecimal | null
stabilityScore     - BigDecimal | null
status             - AnalysisStatus | null
strengths          - Map<String, Object> | null
attentionPoints    - Map<String, Object> | null
missingInformation - Map<String, Object> | null
interviewQuestions - Map<String, Object> | null
eliminationReasons - List<String> | null
recommendation     - string | null
validations        - Map<String, Object> | null
weightsUsed        - Map<String, Object> | null
aiModel            - string | null
promptVersion      - string | null
outdated           - boolean | null
createdAt          - LocalDateTime
```

---

## Estruturas JSON

Os campos JSONB sao flexiveis - o N8N define o schema interno. Exemplos:

```json
"strengths": { "items": ["Solida experiencia com Java", "Boa aderencia ao perfil"] }

"eliminationReasons": ["Nao possui formacao obrigatoria", "Skills ausentes"]

"weightsUsed": { "activities": 50, "experience": 20, "education": 20, "location": 5, "stability": 5 }
```

> **Atencao:** `eliminationReasons` e um array JSON (`[]`), enquanto os demais campos de lista usam o formato objeto `{ "items": [...] }`.

---

## Camada de Persistencia

### CandidateAnalysisRepository

```java
Optional<CandidateAnalysis> findFirstByJobId(String jobId)
List<CandidateAnalysis> findByJobId(String jobId)
List<CandidateAnalysis> findByCandidateId(String candidateId)
```

### AnalysisService

```java
CandidateAnalysisResponseDTO create(CreateCandidateAnalysisRequestDTO data)
CandidateAnalysisResponseDTO getAnalysisByJobId(String jobId)
List<CandidateAnalysisResponseDTO> getAllAnalysisByJobId(String jobId, User requester)
```

O metodo `create()` executa em uma unica transacao:
1. Busca `Candidate` e `Job` pelo ID;
2. Enriquece o `Candidate` com dados extraidos pelo AI (filtrando valores nulos e `"informacao nao encontrada"`);
3. Sincroniza `CandidateStatus` com base no `AnalysisStatus` recebido;
4. Marca `candidate.processedByAi = true`;
5. Salva o `Candidate` atualizado;
6. Cria e salva o `CandidateAnalysis`.

O metodo `getAllAnalysisByJobId()` verifica que a vaga pertence ao recruiter autenticado antes de retornar os dados.

---

## Migrations

Migracoes manuais necessarias no banco:

```sql
-- Renomear tabela (se criada com nome antigo)
ALTER TABLE resume_analysis RENAME TO candidate_analysis;

-- Adicionar campo eliminationReasons
ALTER TABLE candidate_analysis ADD COLUMN IF NOT EXISTS elimination_reasons jsonb;

-- Atualizar CHECK constraint do status
-- IMPORTANTE: apos renomear a tabela o PostgreSQL mantem o nome antigo da constraint.
-- Remover pelos dois possiveis nomes antes de recriar.
ALTER TABLE candidate_analysis DROP CONSTRAINT IF EXISTS resume_analysis_status_check;
ALTER TABLE candidate_analysis DROP CONSTRAINT IF EXISTS candidate_analysis_status_check;
ALTER TABLE candidate_analysis ADD CONSTRAINT candidate_analysis_status_check
  CHECK (status IN ('PENDING','IN_PROGRESS','COMPLETED','APPROVED','REVIEW','FAILED','REJECTED'));

-- Tornar campos do Candidate nulos (candidatos anonimos no apply)
ALTER TABLE candidates ALTER COLUMN name DROP NOT NULL;
ALTER TABLE candidates ALTER COLUMN email DROP NOT NULL;
ALTER TABLE candidates ALTER COLUMN resume_text DROP NOT NULL;
```

> Para verificar os nomes reais das constraints existentes:
> ```sql
> SELECT conname, pg_get_constraintdef(oid)
> FROM pg_constraint
> WHERE conrelid = 'candidate_analysis'::regclass AND contype = 'c';
> ```

---

## Decisoes de Arquitetura

- A tabela foi renomeada de `resume_analysis` para `candidate_analysis` para refletir que a analise e do candidato, nao apenas do curriculo.
- `POST /analysis` usa `X-Service-Key` (API Key) em vez de JWT porque e exclusivamente um endpoint de servico interno (N8N). Isso elimina a necessidade de um node de login no N8N e evita problemas com expiracao de token.
- A API Key e comparada em tempo constante (`MessageDigest.isEqual`) para prevenir timing attacks.
- `GET /analysis/job/{jobId}` e protegido por JWT e verifica ownership: um recruiter so acessa analises de vagas proprias.
- `GET /analysis/job/{jobId}` retorna uma **lista** - uma vaga pode ter multiplos candidatos analisados.
- `eliminationReasons` e um array JSON (`List<String>`), diferente dos demais campos de lista que usam o formato `{ "items": [...] }`.
- O `AnalysisService.create()` enriquece o `Candidate` com dados extraidos pelo AI em uma unica transacao, evitando estado inconsistente.
- `outdated` sinaliza analises que precisam de reprocessamento (ex.: criterios da vaga foram alterados).
- Os campos JSONB permitem que o N8N evolua os schemas de evidencias sem precisar de migrations no banco.
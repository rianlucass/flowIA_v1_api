# Candidatos (Candidates) — Flowia API

Documentação da feature de candidatos da Flowia API. Nesta etapa, os dados de candidatos são inseridos por um fluxo externo via **n8n**, e a API está preparada inicialmente para **consulta** dessas informações.

---

## Sumário

- [Visão Geral](#visão-geral)
- [Escopo Atual](#escopo-atual)
- [Modelo de Dados](#modelo-de-dados)
- [Status do Candidato](#status-do-candidato)
- [DTO de Resposta](#dto-de-resposta)
- [Camada de Persistência](#camada-de-persistência)
- [Consulta por Vaga](#consulta-por-vaga)
- [Endpoint Recomendado](#endpoint-recomendado)
- [Exemplo de Resposta](#exemplo-de-resposta)
- [Decisões de Arquitetura](#decisões-de-arquitetura)

---

## Visão Geral

A entidade `Candidate` representa um candidato associado a uma vaga (`Job`). Ela armazena tanto dados básicos de contato quanto metadados usados no pipeline de análise com IA, como indicadores de processamento e de desatualização da análise.

O desenho atual separa claramente as responsabilidades:

- o **n8n** é responsável por inserir e enriquecer os dados dos candidatos;
- a **API** consome esses dados para leitura e exposição controlada;
- inicialmente, o foco da API é **GET**, sem endpoints públicos de criação ou atualização manual de candidatos.

---

## Escopo Atual

No estado atual do projeto, a feature possui:

- entidade JPA `Candidate`;
- enum `CandidateStatus`;
- DTO `CandidateResponseDTO`;
- `CandidateRepository` com busca por `jobId`;
- `CandidatesService` para consulta por vaga.

O endpoint `GET /candidates/job/{jobId}` já está exposto via `CandidateController`.

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

Decisão importante: o DTO expõe apenas `jobId`, não o objeto inteiro `Job`, reduzindo acoplamento e evitando carga desnecessária de dados da vaga na resposta.

---

## Camada de Persistência

### `CandidateRepository`

Responsável por acesso aos dados de candidatos.

Métodos disponíveis:

```java
List<Candidate> findByJobId(String jobId)
```

Esse método permite recuperar todos os candidatos vinculados a uma vaga específica.

### `CandidatesService`

Responsável por transformar entidades em DTOs de resposta.

Método atual:

```java
List<CandidateResponseDTO> getCandidatesByJobId(String jobId)
```

Fluxo executado pelo service:

1. busca os candidatos associados ao `jobId`;
2. converte cada entidade `Candidate` em `CandidateResponseDTO`;
3. retorna uma lista pronta para exposição por controller.

---

## Consulta por Vaga

O caso de uso principal inicial é listar os candidatos de uma vaga específica.

Entrada:

```text
jobId — string (UUID da vaga)
```

Saída:

```text
List<CandidateResponseDTO>
```

Esse comportamento é especialmente útil para:

- painéis de recrutamento;
- visualização de pipeline por vaga;
- consumo posterior por IA ou automações;
- cruzamento com análises já geradas em `resume_analysis`.

---

## Endpoint Recomendado

Como ainda não existe controller exposto, o endpoint REST recomendado para publicar essa funcionalidade é:

### GET /candidates/job/{jobId}

Retorna todos os candidatos associados a uma vaga.

**Acesso sugerido:** requer autenticação

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

#### Possíveis Erros Sugeridos

| Status | Cenário |
|---|---|
| `400 Bad Request` | `jobId` inválido |
| `401 Unauthorized` | Token ausente ou inválido |
| `403 Forbidden` | Usuário sem permissão para a vaga |
| `404 Not Found` | Vaga não encontrada |

---

## Exemplo de Resposta

Exemplo simplificado de item retornado pela API:

```json
{
	"id": "4ed2c8dd-6d5a-4f6d-a7d9-40a4c39f3c7d",
	"jobId": "f49719d7-c9a2-4bb0-9a2b-8d8ce9d27bbf",
	"name": "Maria Silva",
	"email": "maria@email.com",
	"phone": "+55 11 99999-9999",
	"city": "São Paulo",
	"state": "SP",
	"linkedinUrl": "https://linkedin.com/in/mariasilva",
	"portfolioUrl": null,
	"resumeUrl": "https://storage.example.com/resume.pdf",
	"resumeText": "Texto extraído do currículo...",
	"status": "RECEIVED",
	"processedByAi": false,
	"analysisOutdated": false,
	"createdAt": "2026-05-27T10:15:00"
}
```

---

## Decisões de Arquitetura

- A criação e atualização dos candidatos ficam fora da API neste momento, delegadas ao **n8n**.
- A API trabalha inicialmente como camada de **consulta e exposição controlada**.
- O campo `analysisOutdated` prepara o domínio para reprocessamento quando a vaga ou os critérios forem alterados.
- O campo `processedByAi` permite diferenciar candidatos já enriquecidos pelo pipeline dos ainda pendentes.
- O uso de `jobId` no DTO evita expor estruturas internas completas de `Job`.


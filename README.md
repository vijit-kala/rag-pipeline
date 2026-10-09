# Building a RAG Pipeline with Java and Spring Boot

A local Retrieval-Augmented Generation (RAG) application that ingests
text and PDF documents, stores their chunks and embeddings in PostgreSQL
with pgvector, retrieves relevant context using semantic search, and
generates grounded answers with a locally running Ollama model.

## Table of contents

-   [Overview](#overview)
-   [Architecture](#architecture)
-   [Technology stack](#technology-stack)
-   [How it works](#how-it-works)
-   [API endpoints](#api-endpoints)
-   [Configuration](#configuration)
-   [Running locally](#running-locally)
-   [Example requests](#example-requests)
-   [Code highlights](#code-highlights)
-   [Testing and evaluation](#testing-and-evaluation)
-   [Current limitations and next
    steps](#current-limitations-and-next-steps)

## Overview

This project explores how to build a RAG pipeline with a conventional
Java backend:

1.  Upload a `.txt` or `.pdf` file, or submit text directly as JSON.
2.  Extract text and split it into overlapping chunks.
3.  Generate an embedding for each chunk with Ollama.
4.  Store document metadata, chunks, and vectors in PostgreSQL/pgvector.
5.  Embed a user's question and retrieve the closest chunks using cosine
    distance.
6.  Filter retrieved chunks by a configurable maximum cosine distance.
7.  Ask an Ollama chat model to answer using the retrieved context.
8.  Return the answer with source metadata and the retrieved chunk
    content.

The application is intended as a learning project, not a
production-ready document intelligence service.

## Architecture

``` mermaid
flowchart TB
    subgraph Ingestion["Document ingestion"]
        File["TXT / PDF upload"] --> UploadAPI["DocumentController"]
        TextAPI["JSON text request"] --> UploadAPI
        UploadAPI --> Extract["DocumentTextExtractorService"]
        Extract --> Ingest["DocumentService.ingestText"]
        Ingest --> Chunk["TextChunkingService"]
        Chunk --> Embed["OllamaEmbeddingService"]
        Embed --> Persist["ChunkEmbeddingRepository"]
        Ingest --> Metadata[("PostgreSQL: documents + document_chunks")]
        Persist --> Vectors[("pgvector embeddings")]
    end

    subgraph Retrieval["Question answering"]
        Question["User question"] --> RagAPI["RagController"]
        RagAPI --> Rag["RagService"]
        Rag --> Search["SemanticSearchService"]
        Search --> QueryEmbed["OllamaEmbeddingService"]
        QueryEmbed --> VectorSearch["VectorSearchRepository"]
        VectorSearch --> Vectors
        VectorSearch --> Metadata
        VectorSearch --> Filter["Cosine-distance filtering"]
        Filter --> Context["Retrieved context + source metadata"]
        Context --> Chat["OllamaChatService"]
        Chat --> Model["Ollama chat model"]
        Model --> Answer["Answer + source citations"]
        Answer --> RagAPI
    end
```

### Component responsibilities

  -----------------------------------------------------------------------
Component                           Responsibility
  ----------------------------------- -----------------------------------
`DocumentController`                Document listing, detail, deletion,
JSON ingestion, and file upload
endpoints

`DocumentTextExtractorService`      Extracts text from supported
uploaded file formats

`DocumentService`                   Validates and persists document
metadata, chunks, and embeddings

`TextChunkingService`               Splits text into chunks with
overlap

`OllamaEmbeddingService`            Requests embeddings from Ollama

`ChunkEmbeddingRepository`          Writes vectors to
PostgreSQL/pgvector

`SemanticSearchService`             Embeds a query and coordinates
vector search

`VectorSearchRepository`            Retrieves nearest chunks using
cosine distance

`OllamaChatService`                 Calls the Ollama chat API

`RagService`                        Filters retrieved context, builds
the prompt, and returns an answer
with citations
  -----------------------------------------------------------------------

Names reflect the current implementation where known; verify them
against the repository if they have changed.

## Technology stack

-   Java and Spring Boot
-   Gradle
-   PostgreSQL
-   pgvector
-   Ollama
-   `nomic-embed-text` for embeddings
-   `qwen2.5:3b` for answer generation
-   Apache PDFBox for PDF text extraction
-   JUnit and Mockito for tests

## How it works

### 1. Ingestion

The upload endpoint extracts text and passes it to the existing
`DocumentService.ingestText()` pipeline. The service validates input,
saves document metadata, chunks the text, creates chunk records,
requests an embedding for each chunk, and persists the vectors.

### 2. Embeddings

The embedding model converts text into a vector. This implementation
expects **768 dimensions** from `nomic-embed-text`; the PostgreSQL
vector column and embedding validation must agree with the selected
model.

### 3. Semantic search

The query is embedded with the same embedding model used for document
chunks. PostgreSQL/pgvector ranks stored chunks by cosine distance.
Lower cosine distance means a closer match in the current query.

### 4. Generation and citations

`RagService` filters candidates by maximum cosine distance, builds
context from the accepted chunks, and sends the question plus context to
the chat model. The response includes source details so the answer can
be checked against the retrieved evidence.

### 5. Abstention

The prompt instructs the model not to invent facts when the retrieved
context does not contain an answer. The service also returns an
insufficient-context response when no chunks pass the relevance filter.
This is a useful baseline, not a guarantee that hallucinations are
impossible.

## API endpoints

  -------------------------------------------------------------------------
Method                  Endpoint                  Purpose
  ----------------------- ------------------------- -----------------------
`POST`                  `/api/documents`          Ingest text supplied as
JSON

`POST`                  `/api/documents/upload`   Upload a `.txt` or
`.pdf` file as
multipart form data

`GET`                   `/api/documents`          List stored documents

`GET`                   `/api/documents/{id}`     Fetch document metadata
and chunk count

`DELETE`                `/api/documents/{id}`     Delete a document

`POST`                  `/api/search`             Run semantic search

`POST`                  `/api/rag/ask`            Generate a grounded
answer with sources
  -------------------------------------------------------------------------

Confirm request DTO field names against the current code before relying
on these examples.

## Configuration

The current local setup uses PostgreSQL on host port `5433` and Ollama
on port `11434`. Update these values to match your environment.

Example `application.yml` settings (merge these into your existing
configuration rather than duplicating YAML sections):

``` yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/ragdb
    username: raguser
    password: ${RAG_DB_PASSWORD:ragpassword}
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB

rag:
  retrieval:
    max-cosine-distance: 0.48
    default-limit: 3
    max-limit: 20
```

These retrieval values are the chosen baseline for the current learning
phase, not universally optimal values. Make sure the application's
existing `OllamaEmbeddingService` and `OllamaChatService` configuration
points to the intended Ollama host and models. The local model names
used during development were:

-   Embeddings: `nomic-embed-text`
-   Chat: `qwen2.5:3b`

**Security note:** the fallback database password above is for local
development only. Do not commit real credentials, API keys, or
production secrets.

## Running locally

Prerequisites:

-   A compatible JDK for the project's configured Java toolchain.
-   Gradle (or use the included Gradle wrapper).
-   PostgreSQL with the pgvector extension available.
-   Ollama installed and running.

1.  Create a PostgreSQL database and enable pgvector:

    ``` sql
    CREATE EXTENSION IF NOT EXISTS vector;
    ```

    The application schema also needs the `documents` and
    `document_chunks` tables expected by the entities and repositories.
    Use the project's existing schema setup or migrations.

2.  Pull the models:

    ``` powershell
    ollama pull nomic-embed-text
    ollama pull qwen2.5:3b
    ```

3.  Configure the datasource and retrieval properties in
    `application.yml` or environment-specific configuration.

4.  Run the application from the project root on Windows:

    ``` powershell
    .\gradlew.bat bootRun
    ```

5.  Run tests:

    ``` powershell
    .\gradlew.bat test
    ```

If your local database runs on port `5432` instead of `5433`, update the
JDBC URL accordingly.

## Example requests

### Upload a file

Send a multipart form request to `POST /api/documents/upload`.

Form fields:

-   `file`: choose a `.txt` or `.pdf` file
-   `title`: document title

Example with `curl`:

``` bash
curl -X POST "http://localhost:8080/api/documents/upload" \
  -F "title=RAG File Ingestion Test" \
  -F "file=@./sample.pdf"
```

A successful response resembles:

``` json
{
  "documentId": 40,
  "title": "RAG File Ingestion Test",
  "chunkCount": 3
}
```

IDs and chunk counts depend on the database and uploaded content.

### Semantic search

`POST /api/search` uses the search request DTO. The request observed
during development was:

``` json
{
  "query": "What performance improvements were reported for the backend services?",
  "limit": 3
}
```

### Ask a question

Send a request to `POST /api/rag/ask`:

``` json
{
  "question": "What performance improvements were reported for the backend services, and how were they achieved?",
  "limit": 3
}
```

The response includes an `answer` and `sources`. Source entries include
fields such as document ID, title, filename, chunk ID, chunk index,
content, and cosine distance.

## Code highlights

### 1. Vector similarity search

This is the core query pattern used by the vector search repository:

``` sql
SELECT
    dc.id AS chunk_id,
    dc.document_id,
    dc.chunk_index,
    dc.content,
    dc.embedding <=> ?::vector AS cosine_distance,
    d.title AS document_title,
    d.source_name
FROM document_chunks dc
JOIN documents d ON d.id = dc.document_id
WHERE dc.embedding IS NOT NULL
ORDER BY dc.embedding <=> ?::vector
LIMIT ?;
```

The query joins chunks to document metadata, calculates cosine distance,
orders nearest matches first, and applies a result limit.

### 2. Embedding a search query

The semantic search service follows this basic sequence:

``` java
public List<SearchResult> search(String query, int limit) {
    if (query == null || query.isBlank()) {
        throw new IllegalArgumentException(
                "Search query must not be blank"
        );
    }

    float[] queryEmbedding = embeddingService.embed(query);

    return vectorSearchRepository.searchSimilar(
            queryEmbedding,
            limit
    );
}
```

This is a simplified excerpt; use the repository's actual implementation
as the source of truth.

### 3. Reusing one ingestion pipeline

The file-upload controller extracts text and calls the existing
ingestion service:

``` java
String extractedText = documentTextExtractor.extractText(file);

DocumentIngestionResult result = documentService.ingestText(
        title,
        file.getOriginalFilename(),
        contentType,
        extractedText
);
```

The key idea is that file extraction is separate from chunking,
embedding, and persistence. JSON text ingestion and file ingestion
converge on the same service method.

## Testing and evaluation

Manual checks performed during development included:

-   Uploading a PDF resume and receiving a document ID and chunk count.
-   Confirming that semantic search returned stored chunks with cosine
    distances and source metadata.
-   Asking a question about reported backend performance improvements
    and verifying that the relevant chunk was retrieved after adjusting
    the threshold.
-   Asking about the URL shortener project's implementation features.
-   Asking for cloud provider and deployment architecture details that
    were absent from the resume; the system abstained rather than
    supplying unsupported specifics.
-   Running unit and integration tests during development.

The initial evaluation set was small. The threshold and retrieval limit
should be revisited after testing a larger collection of documents and
questions.

## Current limitations and next steps

This is a learning implementation. Areas to improve include:

-   A repeatable evaluation suite with expected answers and expected
    source chunks.
-   Retrieval precision and recall measurements.
-   Chunking tests across varied document layouts.
-   Stronger file-content validation in addition to filename-extension
    checks.
-   Explicit handling and tests for malformed PDFs, scanned/image-only
    PDFs, and extraction failures.
-   Verification of rollback behavior when embedding generation fails
    midway through ingestion.
-   Authentication, authorization, rate limiting, and production secret
    management.
-   Observability for extraction, embedding, retrieval, and generation
    latency.
-   Experiments with hybrid search and reranking.

## What this project taught me

The most important lesson was that an answer can fail even when the
document has been ingested correctly. In my performance-question test,
the relevant chunk had a cosine distance of approximately `0.4453`,
while the original maximum distance was `0.40`. The RAG service filtered
the chunk out before it reached the model.

Inspecting the raw search response made the issue visible. Raising the
threshold allowed the chunk through, but also highlighted the trade-off
between recall and irrelevant context.

That experience reinforced a practical debugging rule:

**When a RAG answer is wrong or missing, inspect the retrieved evidence
before changing the prompt.**

The project is an ongoing exploration of backend engineering and
retrieval systems. The next iteration will focus on measuring retrieval
quality and improving the pipeline based on repeatable tests rather than
a few manual examples.

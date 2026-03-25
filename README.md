# ntest

Small demo Spring Boot application with OpenSearch-backed client and document indexing/search.

## Requirements

- Java 21
- Docker and Docker Compose

## Quick Start

Run everything with one command:

```bash
docker compose up --build
```

Services:

- App: `http://localhost:8080`
- OpenSearch: `http://localhost:9200`

Stop everything:

```bash
docker compose down
```

## Local Run

Start OpenSearch only:

```bash
docker compose up opensearch
```

Then start the app:

```bash
./gradlew bootRun
```

## API

- `POST /api/v1/clients`
- `POST /api/v1/clients/{id}/documents`
- `GET /api/v1/search?q=...`

OpenAPI file:

- `src/main/resources/openapi.yaml`

## Usage Examples

### curl

Create client:

```bash
curl -X POST http://localhost:8080/api/v1/clients \
  -H 'Content-Type: application/json' \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@example.com",
    "description": "Client onboarding for address verification"
  }'
```

Copy the returned `id`, then create a document:

```bash
curl -X POST http://localhost:8080/api/v1/clients/<CLIENT_ID>/documents \
  -H 'Content-Type: application/json' \
  -d '{
    "title": "Address Proof",
    "content": "Please attach a recent utility bill for address verification."
  }'
```

Search for that document:

```bash
curl 'http://localhost:8080/api/v1/search?q=address%20proof&type=document'
```

### Postman

Import the collection:

- `postman/ntest.postman_collection.json`

How to use it:

1. Import the collection into Postman.
2. Run `Create Client`.
3. Run `Create Document for Client`.
4. Run `Search Documents`.

Notes:

- The collection variable `baseUrl` defaults to `http://localhost:8080`.
- `Create Client` automatically saves the returned `clientId`.
- `Create Document for Client` uses that saved `clientId`.

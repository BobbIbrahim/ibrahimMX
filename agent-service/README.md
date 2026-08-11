# agent-service

Standalone FastAPI service that exposes the MXOrbit agent execution contract.

## Run

```bash
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

## Endpoints

### `GET /health`

```json
{
  "status": "UP"
}
```

### `POST /agents/{agentId}/execute`

Request:

```json
{
  "input": {}
}
```

Response:

```json
{
  "output": {}
}
```

Supported `agentId` values:

- `ticket-type-classifier`
- `test-selector`
- `deployment-planner`

## Environment variables

- `ANTHROPIC_API_KEY`
- `ANTHROPIC_API_BASE` (base URL only, for example `https://<your-endpoint>/anthropic`; do **not** include
  `/v1/messages`)
- `ANTHROPIC_MODEL`

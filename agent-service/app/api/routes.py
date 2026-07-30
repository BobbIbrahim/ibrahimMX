import logging

from fastapi import APIRouter, HTTPException, Request, status

from app.models.contracts import (
    AgentExecuteRequest,
    AgentExecuteResponse,
    HealthResponse,
)
from app.services.agent_registry import AgentNotFoundError, AgentRegistry

logger = logging.getLogger(__name__)

router = APIRouter()


@router.get("/health", response_model=HealthResponse, tags=["health"])
def health() -> HealthResponse:
    return HealthResponse(status="UP")


def _format_exception_chain(exc: Exception) -> str:
    parts: list[str] = []
    visited: set[int] = set()
    current: Exception | None = exc

    while current is not None and id(current) not in visited:
        visited.add(id(current))
        parts.append(f"{type(current).__name__}: {current}")
        next_exc = current.__cause__ or current.__context__
        current = next_exc if isinstance(next_exc, Exception) else None

    return " <- ".join(parts)


@router.post(
    "/agents/{agentId}/execute",
    response_model=AgentExecuteResponse,
    tags=["agents"],
)
def execute_agent(
    agentId: str, request_body: AgentExecuteRequest, request: Request
) -> AgentExecuteResponse:
    registry: AgentRegistry = request.app.state.agent_registry

    try:
        output = registry.execute(agent_id=agentId, input_payload=request_body.input)
    except AgentNotFoundError as exc:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=str(exc),
        ) from exc
    except Exception as exc:
        logger.exception(
            "Agent execution failed. agentId=%s chain=%s",
            agentId,
            _format_exception_chain(exc),
        )
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Agent execution failed for '{agentId}'",
        ) from exc

    return AgentExecuteResponse(output=output)

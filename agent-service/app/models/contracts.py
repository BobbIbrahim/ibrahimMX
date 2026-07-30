from typing import Any

from pydantic import BaseModel, ConfigDict, Field


class HealthResponse(BaseModel):
    status: str = Field(default="UP")


class AgentExecuteRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")
    input: dict[str, Any]


class AgentExecuteResponse(BaseModel):
    output: dict[str, Any]

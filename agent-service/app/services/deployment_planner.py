"""Deployment-planner agent: decides the next deployment action for a change."""

from typing import Any

from pydantic import BaseModel, ConfigDict

from app.core.config import get_settings
from app.services.llm_client import build_llm, ensure_absent, require_input


class DeploymentPlannerOutput(BaseModel):
    model_config = ConfigDict(extra="forbid")
    nextAction: str


def deployment_planner_chain(payload: dict[str, Any]) -> dict[str, Any]:
    require_input(payload, ("change", "ticketType", "test"))
    ensure_absent(payload, "nextAction")

    llm = build_llm(get_settings())
    structured_llm = llm.with_structured_output(DeploymentPlannerOutput)
    result = structured_llm.invoke(
        "Choose the single next deployment action based on change summary, "
        "classification, and test recommendation.\n"
        f"ticketType: {payload['ticketType']}\n"
        f"test: {payload['test']}\n"
        f"change: {payload['change']}"
    )

    return {**payload, "nextAction": result.nextAction}

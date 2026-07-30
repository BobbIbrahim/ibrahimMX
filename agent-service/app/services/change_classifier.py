"""Change-classifier agent: labels a software change with its type."""

from enum import Enum
from typing import Any

from pydantic import BaseModel, ConfigDict

from app.core.config import get_settings
from app.services.llm_client import build_llm, ensure_absent, require_input


class ChangeType(str, Enum):
    BUG_FIX = "BUG_FIX"
    ENHANCEMENT = "ENHANCEMENT"
    TECHNICAL_ENHANCEMENT = "TECHNICAL_ENHANCEMENT"


class ChangeClassifierOutput(BaseModel):
    model_config = ConfigDict(extra="forbid")
    changeType: ChangeType


def change_classifier_chain(payload: dict[str, Any]) -> dict[str, Any]:
    require_input(payload, ("change",))
    ensure_absent(payload, "changeType")

    llm = build_llm(get_settings())
    structured_llm = llm.with_structured_output(ChangeClassifierOutput)
    result = structured_llm.invoke(
        "Classify the provided software change into exactly one value from "
        "BUG_FIX, ENHANCEMENT, or TECHNICAL_ENHANCEMENT.\n"
        f"change: {payload['change']}"
    )

    return {**payload, "changeType": result.changeType.value}

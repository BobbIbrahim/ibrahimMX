"""Test-selector agent: chooses the most relevant validation test to run."""

from typing import Any

from pydantic import BaseModel, ConfigDict

from app.core.config import get_settings
from app.services.llm_client import build_llm, ensure_absent, require_input


class TestSelectorOutput(BaseModel):
    model_config = ConfigDict(extra="forbid")
    test: str


def test_selector_chain(payload: dict[str, Any]) -> dict[str, Any]:
    require_input(payload, ("change", "ticketType"))
    ensure_absent(payload, "test")

    llm = build_llm(get_settings())
    structured_llm = llm.with_structured_output(TestSelectorOutput)
    result = structured_llm.invoke(
        "Choose the single most relevant validation test to run for this change.\n"
        f"ticketType: {payload['ticketType']}\n"
        f"change: {payload['change']}"
    )

    return {**payload, "test": result.test}

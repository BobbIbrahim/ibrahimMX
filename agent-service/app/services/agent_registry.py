from collections.abc import Callable
from typing import Any

from app.services.ticket_type_classifier import ticket_type_classifier_chain
from app.services.deployment_planner import deployment_planner_chain
from app.services.test_selector import test_selector_chain


class AgentNotFoundError(Exception):
    """Raised when an agent cannot be resolved from the registry."""


AgentCallable = Callable[[dict[str, Any]], dict[str, Any]]

agents: dict[str, AgentCallable] = {
    "ticket-type-classifier": ticket_type_classifier_chain,
    "test-selector": test_selector_chain,
    "deployment-planner": deployment_planner_chain,
}


class AgentRegistry:
    def __init__(self, initial_agents: dict[str, AgentCallable] | None = None) -> None:
        self._agents: dict[str, AgentCallable] = (
            initial_agents if initial_agents is not None else agents
        )

    def resolve(self, agent_id: str) -> AgentCallable:
        agent = self._agents.get(agent_id)
        if agent is None:
            raise AgentNotFoundError(f"Unknown agent '{agent_id}'")
        return agent

    def execute(self, agent_id: str, input_payload: dict[str, Any]) -> dict[str, Any]:
        agent = self.resolve(agent_id)
        result = agent(input_payload)

        if not isinstance(result, dict):
            raise TypeError(
                f"Agent '{agent_id}' returned {type(result).__name__}, expected dict"
            )

        return result

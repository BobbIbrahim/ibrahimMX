import unittest
from unittest.mock import patch

from app.services.deployment_planner import (
    DeploymentPlannerOutput,
    deployment_planner_chain,
)


class _FakeStructuredLLM:
    def __init__(self, result):
        self._result = result

    def invoke(self, _prompt: str):
        return self._result


class _FakeLLM:
    def __init__(self, result):
        self._result = result

    def with_structured_output(self, _schema):
        return _FakeStructuredLLM(self._result)


class DeploymentPlannerChainTests(unittest.TestCase):
    @patch("app.services.deployment_planner.build_llm")
    def test_deployment_planner_preserves_existing_payload_fields(
        self, mock_build_llm
    ) -> None:
        mock_build_llm.return_value = _FakeLLM(
            DeploymentPlannerOutput(nextAction="deploy to staging")
        )
        payload = {
            "change": "fix login bug",
            "changeType": "BUG_FIX",
            "test": "run-login-test",
            "ticketId": "MX-123",
        }

        output = deployment_planner_chain(payload)

        self.assertEqual(
            output,
            {
                "change": "fix login bug",
                "changeType": "BUG_FIX",
                "test": "run-login-test",
                "ticketId": "MX-123",
                "nextAction": "deploy to staging",
            },
        )


if __name__ == "__main__":
    unittest.main()

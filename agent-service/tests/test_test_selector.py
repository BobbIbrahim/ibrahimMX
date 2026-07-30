import unittest
from unittest.mock import patch

from app.services.test_selector import TestSelectorOutput, test_selector_chain


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


class TestSelectorChainTests(unittest.TestCase):
    @patch("app.services.test_selector.build_llm")
    def test_test_selector_preserves_existing_payload_fields(
        self, mock_build_llm
    ) -> None:
        mock_build_llm.return_value = _FakeLLM(TestSelectorOutput(test="run-login-test"))
        payload = {
            "change": "fix login bug",
            "changeType": "BUG_FIX",
            "ticketId": "MX-123",
        }

        output = test_selector_chain(payload)

        self.assertEqual(
            output,
            {
                "change": "fix login bug",
                "changeType": "BUG_FIX",
                "ticketId": "MX-123",
                "test": "run-login-test",
            },
        )


if __name__ == "__main__":
    unittest.main()

import unittest
from unittest.mock import patch

from app.services.change_classifier import (
    ChangeClassifierOutput,
    ChangeType,
    change_classifier_chain,
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


class ChangeClassifierChainTests(unittest.TestCase):
    @patch("app.services.change_classifier.build_llm")
    def test_change_classifier_preserves_existing_payload_fields(
        self, mock_build_llm
    ) -> None:
        mock_build_llm.return_value = _FakeLLM(
            ChangeClassifierOutput(changeType=ChangeType.BUG_FIX)
        )
        payload = {"change": "fix login bug", "ticketId": "MX-123"}

        output = change_classifier_chain(payload)

        self.assertEqual(
            output,
            {
                "change": "fix login bug",
                "ticketId": "MX-123",
                "changeType": "BUG_FIX",
            },
        )


if __name__ == "__main__":
    unittest.main()

import unittest
from unittest.mock import patch

from app.services.ticket_type_classifier import (
    ticket_type_classifier_chain,
    TicketType,
    TicketTypeOutput,
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


class TicketTypeClassifierChainTests(unittest.TestCase):
    @patch("app.services.ticket_type_classifier.build_llm")
    def test_ticket_type_classifier(
        self, mock_build_llm
    ) -> None:
        mock_build_llm.return_value = _FakeLLM(
            TicketTypeOutput(ticketType=TicketType.BUG_FIX)
        )
        payload = {"change": "fix login bug", "ticketId": "MX-123"}

        output = ticket_type_classifier_chain(payload)

        self.assertEqual(
            output,
            {
                "change": "fix login bug",
                "ticketId": "MX-123",
                "ticketType": "BUG_FIX",
            },
        )


if __name__ == "__main__":
    unittest.main()

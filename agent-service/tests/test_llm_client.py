import unittest
from unittest.mock import patch

from app.core.config import Settings
from app.services.llm_client import build_llm


class BuildLlmTests(unittest.TestCase):
    @patch("app.services.llm_client.ChatAnthropic")
    def test_build_llm_uses_supported_base_url_constructor_argument(
        self, mock_chat_anthropic
    ) -> None:
        settings = Settings(
            anthropic_api_key="test-key",
            anthropic_api_base="https://example.com/anthropic/v1/messages",
            anthropic_model="claude-sonnet-4-6",
        )

        build_llm(settings)

        kwargs = mock_chat_anthropic.call_args.kwargs
        self.assertEqual(kwargs["anthropic_api_key"], "test-key")
        self.assertEqual(kwargs["model"], "claude-sonnet-4-6")
        self.assertEqual(kwargs["base_url"], "https://example.com/anthropic")
        self.assertNotIn("anthropic_api_url", kwargs)


if __name__ == "__main__":
    unittest.main()

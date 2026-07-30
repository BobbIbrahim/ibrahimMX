import os
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from app.core.config import Settings


class SettingsTests(unittest.TestCase):
    def test_settings_load_from_env_file_and_normalize_base_url(self) -> None:
        with tempfile.TemporaryDirectory() as tmpdir:
            env_file = Path(tmpdir) / ".env"
            env_file.write_text(
                "\n".join(
                    [
                        "ANTHROPIC_API_KEY=test-key",
                        "ANTHROPIC_API_BASE=https://example.com/anthropic/v1/messages",
                        "ANTHROPIC_MODEL=claude-sonnet-4-6",
                    ]
                ),
                encoding="utf-8",
            )

            with patch.dict(os.environ, {}, clear=True):
                settings = Settings(_env_file=env_file)

        self.assertEqual(settings.anthropic_api_key, "test-key")
        self.assertEqual(settings.anthropic_api_base, "https://example.com/anthropic")
        self.assertEqual(settings.anthropic_model, "claude-sonnet-4-6")

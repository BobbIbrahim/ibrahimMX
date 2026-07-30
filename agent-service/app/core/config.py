from functools import lru_cache
from urllib.parse import urlparse

from pydantic import Field, field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    anthropic_api_key: str | None = Field(default=None, alias="ANTHROPIC_API_KEY")
    anthropic_api_base: str | None = Field(default=None, alias="ANTHROPIC_API_BASE")
    anthropic_model: str | None = Field(default=None, alias="ANTHROPIC_MODEL")
    # Canonical (non-privatelink) hostname sent via the "Host" header so the
    # private-link gateway routes the request correctly, e.g.
    # "ychahwan.services.ai.azure.com".
    anthropic_host_header: str | None = Field(
        default=None, alias="ANTHROPIC_HOST_HEADER"
    )
    # The private-link gateway presents a certificate for the canonical
    # hostname, not the privatelink FQDN, so TLS verification must be
    # disabled when talking to the privatelink base URL directly.
    anthropic_tls_verify: bool = Field(default=True, alias="ANTHROPIC_TLS_VERIFY")

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
        populate_by_name=True,
    )

    @field_validator("anthropic_api_base")
    @classmethod
    def normalize_anthropic_api_base(cls, value: str | None) -> str | None:
        if value is None:
            return None

        normalized = value.strip().strip("\"'")
        if not normalized:
            return None

        parsed = urlparse(normalized)
        if parsed.scheme and parsed.netloc:
            path = parsed.path.rstrip("/")
            suffix = "/v1/messages"
            if path.endswith(suffix):
                path = path[: -len(suffix)] or "/"
                parsed = parsed._replace(path=path)
                return parsed.geturl().rstrip("/")

        return normalized.rstrip("/")


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    return Settings()

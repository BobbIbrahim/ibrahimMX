"""Shared helpers for building the Anthropic LLM client used by all agents."""

from typing import Any

import anthropic
import httpx
from langchain_anthropic import ChatAnthropic

from app.core.config import Settings


def build_llm(settings: Settings) -> ChatAnthropic:
    if not settings.anthropic_api_key:
        raise ValueError("ANTHROPIC_API_KEY is required")
    if not settings.anthropic_model:
        raise ValueError("ANTHROPIC_MODEL is required")

    llm_kwargs: dict[str, Any] = {
        "anthropic_api_key": settings.anthropic_api_key,
        "model": settings.anthropic_model,
        "temperature": 0,
    }
    if settings.anthropic_api_base:
        llm_kwargs["base_url"] = settings.anthropic_api_base

    llm = ChatAnthropic(**llm_kwargs)

    # The private-link gateway requires Bearer auth (instead of x-api-key),
    # an explicit Host header naming the canonical hostname, and a relaxed
    # TLS verification because the gateway's certificate only covers the
    # canonical hostname, not the privatelink FQDN we connect to.
    if settings.anthropic_host_header or not settings.anthropic_tls_verify:
        private_link_client = _build_private_link_client(settings)
        object.__setattr__(llm, "_client", private_link_client)

    return llm


def _build_private_link_client(settings: Settings) -> anthropic.Client:
    headers: dict[str, str] = {
        "Authorization": f"Bearer {settings.anthropic_api_key}",
    }
    if settings.anthropic_host_header:
        headers["Host"] = settings.anthropic_host_header

    http_client = httpx.Client(verify=settings.anthropic_tls_verify)

    return anthropic.Client(
        api_key=settings.anthropic_api_key,
        base_url=settings.anthropic_api_base,
        default_headers=headers,
        http_client=http_client,
    )


def require_input(payload: dict[str, Any], required_keys: tuple[str, ...]) -> None:
    missing = [key for key in required_keys if key not in payload]
    if missing:
        raise ValueError(f"Missing required input field(s): {', '.join(missing)}")


def ensure_absent(payload: dict[str, Any], key: str) -> None:
    if key in payload:
        raise ValueError(f"Field '{key}' already exists and cannot be rewritten")

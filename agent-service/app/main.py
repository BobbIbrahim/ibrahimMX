import logging
from urllib.parse import urlparse

import truststore

# Use the OS certificate store (e.g. Windows Trust Store) for TLS verification
# instead of the bundled certifi CA list. This is required so that corporate
# TLS-inspecting proxies (which re-sign traffic with an internal root CA
# trusted by the OS) don't cause SSL verification failures for outbound
# HTTPS calls such as the Anthropic client.
truststore.inject_into_ssl()

from fastapi import FastAPI

from app.api.routes import router
from app.core.config import get_settings
from app.services.agent_registry import AgentRegistry

logger = logging.getLogger(__name__)


def _extract_host(api_base: str | None) -> str:
    if not api_base:
        return "<default>"
    parsed = urlparse(api_base)
    return parsed.netloc or "<invalid>"


def create_app() -> FastAPI:
    app = FastAPI(title="agent-service")
    app.state.settings = get_settings()
    app.state.agent_registry = AgentRegistry()
    logger.info(
        "Anthropic configuration loaded. host=%s model=%s",
        _extract_host(app.state.settings.anthropic_api_base),
        app.state.settings.anthropic_model or "<unset>",
    )
    app.include_router(router)
    return app


app = create_app()

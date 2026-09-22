"""Shared configuration and provider construction for translation workflows."""

from __future__ import annotations

import argparse
import os
from pathlib import Path

from languages import TranslationTarget
from providers.base import Translator
from providers.openai_responses import DEFAULT_MODEL, OpenAIResponsesTranslator


DEFAULT_DOMAIN_BRIEF = Path(__file__).with_name("domain_brief.md")
DOMAIN_BRIEF_MAX_CHARACTERS = 8000


def parse_target(value: str) -> TranslationTarget:
    try:
        locale, language = value.split("=", 1)
    except ValueError as error:
        raise argparse.ArgumentTypeError("target must be LOCALE=LANGUAGE") from error
    if not locale.strip() or not language.strip():
        raise argparse.ArgumentTypeError("target must have a locale and language")
    return TranslationTarget(locale.strip(), language.strip())


def load_domain_brief(path: Path) -> str:
    domain_context = path.read_text(encoding="utf-8").strip()
    if not domain_context:
        raise ValueError("domain brief is empty")
    if len(domain_context) > DOMAIN_BRIEF_MAX_CHARACTERS:
        raise ValueError(
            f"domain brief is {len(domain_context)} characters; "
            f"maximum is {DOMAIN_BRIEF_MAX_CHARACTERS}"
        )
    return domain_context


def create_translator(provider: str, model: str) -> Translator:
    api_key = os.environ.get("OPENAI_API_KEY")
    if not api_key:
        raise ValueError("OPENAI_API_KEY is not set")
    if provider == "openai":
        return OpenAIResponsesTranslator(api_key=api_key, model=model)
    raise ValueError(f"unsupported provider: {provider}")


def add_provider_arguments(parser: argparse.ArgumentParser) -> None:
    parser.add_argument("--domain-brief", type=Path, default=DEFAULT_DOMAIN_BRIEF)
    parser.add_argument("--provider", choices=("openai",), default="openai")
    parser.add_argument("--model", default=DEFAULT_MODEL)

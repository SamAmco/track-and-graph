"""Translator adapter contract used by translation workflows."""

from __future__ import annotations

from dataclasses import dataclass
from typing import Protocol


class TranslatorError(RuntimeError):
    """Raised when a translation provider request or response is invalid."""


@dataclass(frozen=True)
class TranslationResult:
    text: str
    usage: dict[str, object]


class Translator(Protocol):
    """Send one translation task to a provider."""

    def translate(
        self,
        *,
        instructions: str,
        source_text: str,
    ) -> TranslationResult: ...

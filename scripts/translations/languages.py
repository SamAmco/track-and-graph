"""Shared translation targets derived from Google Play's locale list.

English is the source language. RTL languages are intentionally excluded until
the app supports RTL layouts. Regional variants are collapsed when they share a
written language. Chinese is split into Simplified and Traditional because one
translation cannot serve both scripts.

Source: https://support.google.com/googleplay/android-developer/answer/9844778
Verified: 2026-09-18
"""

from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path


@dataclass(frozen=True)
class TranslationTarget:
    locale: str
    language: str


LANGUAGES_FILE = Path(__file__).resolve().parents[2] / "configuration" / "translation-languages.tsv"


def load_languages(path: Path = LANGUAGES_FILE) -> tuple[TranslationTarget, ...]:
    languages: list[TranslationTarget] = []
    seen: set[str] = set()
    for line_number, raw_line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        line = raw_line.strip()
        if not line or line.startswith("#"):
            continue
        try:
            locale, language = line.split("\t", 1)
        except ValueError as error:
            raise ValueError(f"{path}:{line_number}: expected tab-separated locale and name") from error
        locale = locale.strip()
        language = language.strip()
        if not locale or not language:
            raise ValueError(f"{path}:{line_number}: locale and name must be non-empty")
        if locale in seen:
            raise ValueError(f"{path}:{line_number}: duplicate locale {locale}")
        seen.add(locale)
        languages.append(TranslationTarget(locale, language))
    if not languages or languages[0] != TranslationTarget("en", "English"):
        raise ValueError(f"{path}: first language must be en<TAB>English")
    return tuple(languages)


ALL_LANGUAGES = load_languages()
SUPPORTED_TARGETS = tuple(target for target in ALL_LANGUAGES if target.locale != "en")

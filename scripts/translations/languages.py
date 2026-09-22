"""Shared translation targets derived from Google Play's locale list.

English is the source language. RTL languages are intentionally excluded until
the app supports RTL layouts. Regional variants are collapsed to one language
because app content resolves by language and should be translated only once.

Source: https://support.google.com/googleplay/android-developer/answer/9844778
Verified: 2026-09-18
"""

from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class TranslationTarget:
    locale: str
    language: str


SUPPORTED_TARGETS = (
    TranslationTarget("af", "Afrikaans"),
    TranslationTarget("sq", "Albanian"),
    TranslationTarget("am", "Amharic"),
    TranslationTarget("hy", "Armenian"),
    TranslationTarget("az", "Azerbaijani"),
    TranslationTarget("bn", "Bangla"),
    TranslationTarget("eu", "Basque"),
    TranslationTarget("be", "Belarusian"),
    TranslationTarget("bg", "Bulgarian"),
    TranslationTarget("my", "Burmese"),
    TranslationTarget("ca", "Catalan"),
    TranslationTarget("zh", "Chinese"),
    TranslationTarget("hr", "Croatian"),
    TranslationTarget("cs", "Czech"),
    TranslationTarget("da", "Danish"),
    TranslationTarget("nl", "Dutch"),
    TranslationTarget("et", "Estonian"),
    TranslationTarget("fil", "Filipino"),
    TranslationTarget("fi", "Finnish"),
    TranslationTarget("fr", "French"),
    TranslationTarget("gl", "Galician"),
    TranslationTarget("ka", "Georgian"),
    TranslationTarget("de", "German"),
    TranslationTarget("el", "Greek"),
    TranslationTarget("gu", "Gujarati"),
    TranslationTarget("hi", "Hindi"),
    TranslationTarget("hu", "Hungarian"),
    TranslationTarget("is", "Icelandic"),
    TranslationTarget("id", "Indonesian"),
    TranslationTarget("it", "Italian"),
    TranslationTarget("ja", "Japanese"),
    TranslationTarget("kn", "Kannada"),
    TranslationTarget("kk", "Kazakh"),
    TranslationTarget("km", "Khmer"),
    TranslationTarget("ko", "Korean"),
    TranslationTarget("ky", "Kyrgyz"),
    TranslationTarget("lo", "Lao"),
    TranslationTarget("lv", "Latvian"),
    TranslationTarget("lt", "Lithuanian"),
    TranslationTarget("mk", "Macedonian"),
    TranslationTarget("ms", "Malay"),
    TranslationTarget("ml", "Malayalam"),
    TranslationTarget("mr", "Marathi"),
    TranslationTarget("mn", "Mongolian"),
    TranslationTarget("ne", "Nepali"),
    TranslationTarget("no", "Norwegian"),
    TranslationTarget("pl", "Polish"),
    TranslationTarget("pt", "Portuguese"),
    TranslationTarget("pa", "Punjabi"),
    TranslationTarget("ro", "Romanian"),
    TranslationTarget("rm", "Romansh"),
    TranslationTarget("ru", "Russian"),
    TranslationTarget("sr", "Serbian"),
    TranslationTarget("si", "Sinhala"),
    TranslationTarget("sk", "Slovak"),
    TranslationTarget("sl", "Slovenian"),
    TranslationTarget("es", "Spanish"),
    TranslationTarget("sw", "Swahili"),
    TranslationTarget("sv", "Swedish"),
    TranslationTarget("ta", "Tamil"),
    TranslationTarget("te", "Telugu"),
    TranslationTarget("th", "Thai"),
    TranslationTarget("tr", "Turkish"),
    TranslationTarget("uk", "Ukrainian"),
    TranslationTarget("vi", "Vietnamese"),
)

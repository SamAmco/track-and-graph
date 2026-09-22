"""OpenAI Responses API implementation of the translation provider."""

from __future__ import annotations

import json
import urllib.error
import urllib.request

from providers.base import TranslationResult, TranslatorError


OPENAI_RESPONSES_URL = "https://api.openai.com/v1/responses"
DEFAULT_MODEL = "gpt-5.6-luna"


def _extract_output_text(response: dict[str, object]) -> str:
    output_text = response.get("output_text")
    if isinstance(output_text, str) and output_text:
        return output_text
    for item in response.get("output", []):
        if not isinstance(item, dict):
            continue
        for content in item.get("content", []):
            if isinstance(content, dict) and content.get("type") == "output_text":
                text = content.get("text")
                if isinstance(text, str):
                    return text
    raise TranslatorError("OpenAI response contained no output text")


class OpenAIResponsesTranslator:
    def __init__(self, *, api_key: str, model: str = DEFAULT_MODEL) -> None:
        self._api_key = api_key
        self._model = model

    def translate(
        self,
        *,
        instructions: str,
        source_text: str,
    ) -> TranslationResult:
        body = {
            "model": self._model,
            "instructions": instructions,
            "input": source_text,
            "reasoning": {"effort": "none"},
            "text": {"verbosity": "low", "format": {"type": "text"}},
            "store": False,
            "max_output_tokens": 8000,
        }
        request = urllib.request.Request(
            OPENAI_RESPONSES_URL,
            data=json.dumps(body, ensure_ascii=False).encode("utf-8"),
            headers={
                "Authorization": f"Bearer {self._api_key}",
                "Content-Type": "application/json",
            },
            method="POST",
        )
        try:
            with urllib.request.urlopen(request, timeout=180) as result:
                response = json.load(result)
        except urllib.error.HTTPError as error:
            detail = error.read().decode("utf-8", errors="replace")
            raise TranslatorError(
                f"OpenAI API returned HTTP {error.code}: {detail}"
            ) from error
        except urllib.error.URLError as error:
            raise TranslatorError(f"OpenAI API request failed: {error}") from error

        usage = response.get("usage", {})
        return TranslationResult(
            text=_extract_output_text(response),
            usage=usage if isinstance(usage, dict) else {},
        )

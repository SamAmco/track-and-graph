# Translation scripts

This directory contains local, build-time translation tooling. It does not run
inside the Android app.

## Shared infrastructure

`languages.py` is the checked-in target list. It contains the 66 non-English,
non-RTL written-language targets from Google Play's supported localization list.
Regional variants are collapsed where they share a script; Chinese remains split
into Simplified (`zh-Hans`) and Traditional (`zh-Hant`).

`providers/base.py` defines the provider-neutral interface;
`providers/openai_responses.py` contains the OpenAI implementation.
`translation_runtime.py` centralizes provider construction, target parsing, and
domain-brief validation. Release-note, Android-resource, and future
function-file workflows share this infrastructure while retaining format-aware
source extraction, validation, and output wrappers.

## Android app resources

`translate_app_resources.py` treats unqualified English resource files as the
only source of truth. It discovers every
`app/*/src/*/res/values/strings.xml`, so adding a module or source set does not
require updating the translation script. Non-user-facing source-set strings
must opt out with `translatable="false"`.

It never copies a target-only resource back into English. A read-only audit
lists missing and stale translations plus `deletion_candidates`: resources
which exist only in a translated file, or translated overrides of an English
resource marked `translatable="false"`.

```bash
python3 -B scripts/translations/translate_app_resources.py audit
# Equivalent:
make translations-audit
```

Every translation is associated with the hash of the English value it was
translated from in `app_translation_state.json`. Untracked translations are
stale. Changing a target-language value alone does not trigger translation, but
changing its English source always makes it pending, regardless of whether the
target was generated or edited by a person.

The repository's pre-existing translations were adopted without API calls by
baselining them against the current English source. The same command is
available for a deliberate future import of translations:

```bash
make translations-baseline
```

Baselining says that the current target values correspond to the current
English values, so it must not be used to silence genuinely stale translations.

Translation is incremental and requires an explicit paid-operation command plus
either one or more targets or `--all-targets`:

```bash
python3 -B scripts/translations/translate_app_resources.py translate \
  --target fi=Finnish
# Equivalent:
make translations-generate TRANSLATION_ARGS="--target fi=Finnish"

python3 -B scripts/translations/translate_app_resources.py translate \
  --all-targets
```

Resources are parsed locally and sent as bounded JSON batches rather than raw
XML. The shared domain brief accompanies every batch. Valid results are merged
into the locale file. Validation requires exact resource IDs,
printf placeholders, protected escapes, inline XML structure, and string-array
ordering; target languages may supply their own Android plural quantities.
Each request also receives a bounded sample of that locale's existing or
earlier-batch translations, providing automatic terminology/style memory
without maintaining a separate glossary. `--reference-characters` tunes that
sample. Selection is independent of XML order: references sharing meaningful
English words or resource-name terms with the current batch rank first. Any
remaining allowance is filled deterministically across source sets. Common
English stop words and structural name terms such as `title` and `button` do
not create false relevance. Oversized entries are skipped rather than blocking
smaller useful references.
Failures remain pending for the next incremental run. Locales run concurrently
with a default limit of eight workers; `--batch-characters` and `--max-workers`
can tune request size and concurrency. With the current source catalogue, the
15,000-character default produces four initial requests per untranslated
locale; later incremental runs usually need only one.

If a batch fails validation, the tool makes a conservative best effort to turn
the response into readable Android resource XML in a `translation_failure_*.xml`
file. A uniquely repairable one-character JSON syntax error is corrected for
this preview; values that cannot be recovered remain as clearly marked English
source entries. The error and full source identity are retained as `tools:`
metadata. The file deliberately starts with non-XML text, so Gradle cannot
produce a passing build while it remains unresolved. If some values cannot be
recovered, the original raw response is also retained at the bottom of the XML.

Edit the XML preview directly. Remove `tools:unrecovered="true"` after replacing
any English fallback, then validate and merge it without an API call:

```bash
make translations-apply-failure \
  TRANSLATION_ARGS="app/app/src/main/res/values-b+nl/translation_failure_….xml"
```

The command applies nothing unless IDs, placeholders, protected escapes, arrays,
plurals, and inline markup all validate. On success it updates incremental state
and removes the resolved artifact.

The script preserves target-only entries while reporting them. Deletion is a
separate, deliberate cleanup step.

## Release notes

`translate_release_notes.py` sends the complete Markdown file in one request per
locale. It runs locales concurrently, validates that protected content and
Markdown structure survived, and reports exact failures. Every returned
translation is written so small structural defects can be repaired locally.
Paid retries are opt-in with `--retries`; the default is zero.

`domain_brief.md` is compact English context explaining the app's stable domain
model and ambiguous terminology. It is included in every translation request.
Its core section must not be removed. The 8,000-character ceiling is a generous
safety limit, not a target: retain useful context when adding new concepts.

```bash
python3 scripts/translations/translate_release_notes.py \
  changelogs/10.0.0/en.md \
  --output-dir /tmp/release-note-translations
```

The command translates every supported target by default. Use repeated
`--target es=Spanish` arguments for a test or selective rerun. It reads
`OPENAI_API_KEY` from the environment and never writes it.

The final JSON line lists invalid locales and their failed checks. After local
repairs, validate the outputs without making API calls:

```bash
python3 scripts/translations/markdown_validation.py \
  changelogs/10.0.0/en.md \
  /tmp/release-note-translations/*.md
```

Function-file translation is intentionally separate from this release-note
workflow. It should operate on each source function file, not the generated
catalog, then write translations back in the Lua format expected by that file.

## Tests

```bash
python3 -B -m unittest discover \
  -s scripts/translations/tests \
  -p 'test_*.py' -v
# Equivalent:
make translations-test
```

This test target is fully offline. It uses temporary resource trees and fake
translator responses to exercise discovery, incremental state, batching,
validation, merging, and build-breaking failure artifacts, plus the existing
Markdown validation suite. It does not call an API, translate production copy,
run the real-resource audit, or compile Android modules.

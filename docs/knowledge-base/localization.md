---
title: Localization and translation automation
description: English-authoritative Android resource localization, shared translation infrastructure, incremental state, validation, paid-call safety, and locale ownership across app source sets.
topics:
  - English values resources are the sole source of truth
  - Read-only audit reports missing, stale, and target-only resources
  - English source hashes support safe incremental updates
  - Domain brief, languages, and provider adapters are shared across workflows
  - App, shared UI, and Play-only resources retain separate ownership
keywords: [localization, translation, strings.xml, Android resources, values, translatable, domain-brief, app_translation_state, translate_app_resources, incremental, source-of-truth, provider, OpenAI]
---

# Localization and translation automation

English resources in unqualified `values/` directories are the sole source of
truth. Never copy a resource found only in a translated file into English.
Target-only resources and translated overrides of an English resource marked
`translatable="false"` are deletion candidates; audit and review them before
removing anything.

Resource ownership remains modular. The tooling discovers every English source
file matching `app/*/src/*/res/values/strings.xml`; no module/source-set registry
is maintained. This includes distribution-specific source sets. Strings in a
developer-only or otherwise non-user-facing source set must declare
`translatable="false"`.

`app/changelog-viewer` is a developer/debug utility, not a localized product.
Its viewer-owned UI copy, including labels, mock data, and sample Markdown,
lives in that module's `strings.xml`, and every resource there must always
declare `translatable="false"`. Do not introduce hard-coded user-visible Compose
text. Shared `app/ui` strings rendered by the viewer still belong to the shared
UI module and remain translatable for the production apps.

Run the read-only audit without credentials or API calls:

```bash
python3 -B scripts/translations/translate_app_resources.py audit
# or: make translations-audit
```

`scripts/translations/translate_app_resources.py translate` is a paid operation
and deliberately requires either repeated `--target LOCALE=LANGUAGE` arguments
or `--all-targets`. It parses English XML, skips `translatable="false"`, sends
bounded structured batches with `domain_brief.md`, validates the response, and
merges only valid translations. It never asks a model to rewrite raw XML.
Each batch includes a bounded sample of existing or earlier-batch translations
from the same locale as terminology/style memory; this is derived automatically
and is not a separately maintained glossary. The selection does not depend on
XML order. It prioritizes references with meaningful English-word or
resource-name overlap with the current batch, ignores common and structural
terms, then fills unused capacity with a deterministic mix across source sets.
Entries that do not fit are skipped so a single long resource cannot prevent
smaller useful references from being included.

Every target translation is tracked in
`scripts/translations/app_translation_state.json` by the hash of the English
value it corresponds to. The JSON structure is
`translations[locale][source-set/type:name] = source_hash`. A target-only edit
does not cause an API call. If English changes, the target becomes stale and is
regenerated regardless of whether it was initially generated or written by a
person. An existing target with no state is also stale.

`make translations-baseline` records existing translations against the current
English source without making API calls. This is for deliberately adopting an
existing translation set, not for suppressing genuinely stale entries. The
repository's original German, Spanish, and French resources have been
baselined; after that adoption they follow exactly the same incremental rules
as every other locale.

Validation protects resource IDs, printf placeholders, escaped newlines/tabs,
inline XML tags, string-array count/order, and plural structure. Target plural
quantities may differ from English. A rejected response is retained, not
discarded. The script converts as much as it can into readable Android XML in a
`translation_failure_*.xml`, using a unique minimal JSON syntax repair when one
fully validates. Unrecoverable values remain marked English entries. `tools:`
metadata retains the error and exact source identity, while a non-XML prefix
keeps the Android build broken.

Repair that XML directly and run `make translations-apply-failure
TRANSLATION_ARGS="path/to/translation_failure_….xml"`. It validates the repaired
resources through the normal pipeline before merging them, updates state, and
removes the artifact only after every entry is resolved. Run Android resource
compilation after a translation suite as the final platform validation.
When a response cannot be fully recovered into resource elements, its raw text
is retained at the bottom of the artifact for manual reference.

Generate translations through the explicit paid target:

```bash
make translations-generate TRANSLATION_ARGS="--target fi=Finnish"
# Full suite, deliberately explicit:
make translations-generate TRANSLATION_ARGS="--all-targets"
```

Shared infrastructure lives under `scripts/translations/`: `languages.py`,
`domain_brief.md`, provider adapters, and `translation_runtime.py`. Release
notes and future Lua function translation reuse those pieces but keep their own
format-specific parsing and validation.

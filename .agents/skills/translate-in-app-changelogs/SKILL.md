---
name: translate-in-app-changelogs
description: Translate Track & Graph's long in-app Markdown changelogs with structural validation, domain-context maintenance, and translation spot checks. Do not use for concise Play Store and GitHub release notes.
---

# Translate In-App Changelogs

Use this skill only when the user explicitly requests
`$translate-in-app-changelogs` or explicitly asks to translate the long in-app
changelog. Do not invoke it for the concise Play Store/GitHub release notes, or
merely because changelog copy is being drafted or reviewed. Those activities do
not authorize paid API calls.

Translate from the finalized English in-app changelog. Keep generated output
out of production locale files until it has passed validation and review.

## Before translating

1. Read `docs/knowledge-base/release-changelogs.md` and inspect the relevant
   `changelogs/<version>/en.md`.
2. Identify the previous published app release from `changelogs/index.json`.
   Review the new English changelog and, where needed, the changes since that
   release to find new app concepts or changed meanings. Use `jj`, never Git.
3. Read `scripts/translations/domain_brief.md`. Add English context for new or
   ambiguous app concepts. This is an app-domain explanation, not a release
   summary or per-language glossary. Prefer clarity over aggressive brevity.
4. Preserve the `Core context — do not remove` section and existing relevant
   context. The 8,000-character ceiling is a generous safety limit, not a target;
   do not regress terminology just to keep the brief unusually short.
5. Check app string resources when the release note names existing UI concepts.

## Translate and validate

Run the local tests first:

```bash
python3 -B -m unittest discover \
  -s scripts/translations/tests \
  -p 'test_*.py' -v
```

Use `scripts/translations/translate_in_app_changelogs.py` without `--target`
for the full list in `languages.py`. Use repeated `--target` arguments only
for tests or selective reruns. Write the first pass to a version-specific
directory under `/tmp`; do not overwrite checked-in translations during
evaluation. The script sends one complete Markdown file per locale, runs locales
concurrently, writes every returned translation, and reports invalid locales
with exact structural failures in its final JSON summary.

Only make live API calls after the user specifically requests the translation
run or API test and the required provider credentials are available. Confirm
that the English in-app changelog is considered ready unless the user's request
already makes that clear. A retry is expected for a transient or rejected
response only when explicitly requested with `--retries`; the default makes one
paid request per locale. Do not weaken validation to make output pass.

Repair structurally invalid translations locally before considering another API
call. Iterate over the summary's invalid locales, compare each output with the
English source, and make the smallest safe edit that restores missing Markdown
or protected content without rewriting translated prose. Then run
`markdown_validation.py` against the source and output files. Use a paid rerun
only when the defect cannot be repaired confidently from those two files.

## Review

Spot-check representative outputs rather than claiming native fluency in every
language. Include languages with existing human translations when available,
plus structurally different or lower-resource languages when the requested
scope warrants it. Check:

- meaning, omissions, invented claims, and app-domain interpretation;
- established UI terminology and technical/product names;
- untranslated prose, unexpected script mixing, and awkward code-switching;
- natural punctuation, word order, and text around protected inline symbols;
- byte-preserved URLs, code, and intact Markdown structure.

Compare against existing human translations where possible. Report concrete
quality problems, rejected/retried locales, token usage, approximate cost, and
the temporary output paths. Copy results into `changelogs/<version>/` only when
the user requests that finalization.

If review finds a domain misunderstanding, improve the domain brief and rerun
only affected locales. The user's explicit request for the translation session
authorizes one such refinement cycle without another confirmation. If the same
problem remains, stop and ask before spending more. Keep useful domain changes
even if they apply only to future releases.

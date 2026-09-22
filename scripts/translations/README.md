# Translation scripts

This directory contains local, build-time translation tooling. It does not run
inside the Android app.

## Shared infrastructure

`languages.py` is the checked-in target list. It contains the 65 non-English,
non-RTL languages from Google Play's supported localization list, with regional
variants collapsed to avoid duplicate translation requests.

`providers/base.py` defines the provider-neutral interface;
`providers/openai_responses.py` contains the OpenAI implementation. Release-note
and function-file workflows should share languages and provider infrastructure,
but keep separate source extraction and output-writing wrappers.

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
```

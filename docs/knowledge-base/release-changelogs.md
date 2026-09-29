---
title: Release notes and in-app changelogs
description: Separate workflows for concise Play Store/GitHub release notes and long localized in-app Markdown changelogs.
topics:
  - Snapshot releases: guarded make target increments snapshots or interactively starts a major, minor, or patch snapshot in a dedicated jj revision
  - Concise release notes: translated Fastlane changelogs shared by Google Play and GitHub releases
  - make release-notes: Markdown editor buffer with excluded jj commit reference, then paid parallel translation
  - make release-notes-validate: offline completeness, structure, and 500-character validation
  - In-app changelogs: changelogs/{versionName}/{locale}.md with index.json
  - Changelog viewer app: paste markdown and preview the shared dialog plus FOSS and mock Play support flows
  - Runtime locale selection: BCP 47 language-and-script match, one Markdown download per release, English fallback
  - In-app changelog translation: full Markdown per locale, domain brief, deterministic validation, explicit invocation only
keywords: [changelog, release, release-notes, snapshot, snapshot-release, jj, changelog-viewer, markdown, preview, dialog, fastlane, play-store, localization, translation, domain-brief, 500-char, index.json, versionCode, versionName]
---

# Release Notes and In-App Changelogs

## Snapshot Releases

`make snapshot-release` creates the next snapshot revision. It increments both
the Android `versionCode` and the numeric suffix in a version such as
`10.5.0-SNAPSHOT11`, then commits the isolated Gradle change with a message such
as `Snapshot release 10.5.0-SNAPSHOT12` using `jj`.

The target intentionally refuses to run when the current working copy contains
changes, preventing unrelated work from being included in the snapshot
revision. When the current `versionName` is a stable semantic version, the
target asks whether the next release is major, minor, or patch, applies that
semantic-version bump, and starts it at `-SNAPSHOT1`. For example, selecting
minor after `10.4.0` produces `10.5.0-SNAPSHOT1`. An existing numbered snapshot
is incremented without prompting. Use `python3 scripts/snapshot_release.py
--dry-run` to preview the next values without changing the working copy, or
pass `--release-type major|minor|patch` to make the initial choice
non-interactively.

## Two independent outputs

There are two deliberately independent kinds of release copy:

- **Release notes** are concise Markdown-subset/plain text in
  `fastlane/metadata/android/{Play locale}/changelogs/{versionCode}.txt`.
  Google Play displays each localized file in What's New; GitHub releases use
  the same English file. Every locale has a strict 500-character limit.
- **In-app changelogs** are optional long-form Markdown in
  `changelogs/{versionName}/{locale}.md`, referenced through
  `changelogs/index.json`. These can contain images, headings, and detailed
  explanations and are downloaded by the app after release.

Never have the concise release-note workflow write to `changelogs/` or update
its index. Publishing an in-app changelog is a later, separate decision.

## Concise release-note workflow

Run `make release-notes` after setting the release version. It invokes
`scripts/translations/prepare_release_notes.py create`, which:

- Reads `versionCode` and `versionName` from the app Gradle configuration.
- Uses `jj` to find the highest semantic release tag and list commits since it.
- Opens a temporary Markdown file. Only text between explicit release-note
  markers is published; commented commits below the end marker are reference.
- Writes the English note to its Fastlane path.
- Sends the complete note and shared domain brief once per non-English locale,
  concurrently, through the standard provider adapter.
- Writes every returned translation to its exact Play-locale Fastlane path,
  even when it fails structure or length validation.
- Prints a JSON summary containing paths, character counts, validation issues,
  provider failures, and usage.

The translation prompt explicitly requires at most 500 Unicode characters,
including spaces and line breaks, while preserving all release-note items. The
validator checks the actual Python character count and Markdown structure. A
failure makes the command exit non-zero but never discards returned copy.

Repair flagged `.txt` files directly without another paid request, then run
`make release-notes-validate`. This offline command requires every locale in
`configuration/translation-languages.tsv`, rechecks Markdown structure against
English, and reports missing or over-limit files with their exact paths and
character counts. Iterate until it succeeds before describing the suite as
complete. The release runbook contains both steps.

GitHub release creation always passes the concise English Fastlane file to
`gh release create --notes-file`; it never reads or falls back to an in-app
changelog.

## Previewing In-App Markdown

Use the `changelog-viewer` Android module to preview in-app changelog Markdown before publishing it. The viewer lets you paste or clear markdown text and opens the same shared changelog dialog content used by the production app. Its support footer displays both distribution variants together: the Buy Me a Coffee action opens the shared thank-you state directly, while the Play action exercises the animated mock price-options flow, back navigation, and simulated purchase result. It does not open an external support link or include the Billing SDK.

The footer layout, localized support copy, support assets, thank-you content, dialog UI, and price-options body live in the shared UI module. Each production flavor supplies only its own support action; the viewer supplies both actions to the same footer component. Only the viewer's Play product data and purchase result are fake.

## Copy Editing and Translation

When iterating on in-app changelog copy, treat the English Markdown as the source text. Finalize wording there before translating the target locale files; this avoids repeating copy edits across the full suite. Locale files may exist as placeholders before translation.

When translating, check string resources for official UI terms before choosing feature names or labels. This matters for both old features and newly released UI, not only for terms called out in this document.

### API translation workflow

The explicitly invoked `.agents/skills/translate-in-app-changelogs` workflow
uses `scripts/translations/translate_in_app_changelogs.py`. It sends the
complete English Markdown and `domain_brief.md` in one request per locale
through a swappable provider adapter. Locales run concurrently. Deterministic
validation protects URLs, link targets, code, identifiers, and Markdown
structure. Returned results are always written and the final JSON summary lists
invalid locales and checks. The default is one paid request per locale; repair
small structural failures locally and rerun `markdown_validation.py` before
considering an API retry.

`scripts/translations/languages.py` is the shared source of truth for translation
targets. It contains 66 non-English, non-RTL written-language targets from
Google Play's supported localization list. Regional variants are collapsed when
they share a script, avoiding duplicate translation requests. Chinese remains
split into Simplified (`zh-Hans`) and Traditional (`zh-Hant`) because the scripts
are distinct. With no
`--target` arguments the script uses the full list. Explicit targets select a
smaller test or retry set. Function translation should reuse this list and the
provider adapters, but have its own source and output wrapper.

The domain brief preserves stable core terminology and may grow to an
8,000-character safety ceiling. If review exposes a domain misunderstanding,
refine the brief and rerun only affected locales once. Live calls require an
explicit user request, and initial results stay under `/tmp` until reviewed.

The complex Markdown fixture is intentionally harsher than normal in-app
changelogs. A 2026-09-18 full GPT-5.6 Luna run produced structurally valid output for
61 of 65 languages after bounded retries. The four rejected locales repeatedly
dropped one bold span; valid lower-resource outputs also showed occasional
untranslated terminology or mixed-script text. All four structural failures
were repaired locally by restoring the missing emphasis, then passed validation
without another API call. Keep deterministic validation and manually spot-check
meaning and script consistency before publishing.

When translating in-app changelogs, check string resources for official translations of feature names (e.g. "Symlink" is "Enlace simbólico" in Spanish, "Lien symbolique" in French, but stays "Symlink" in German).

## index.json

Located at `changelogs/index.json`, maps version names to locale-specific
Markdown paths for in-app changelogs. It is maintained only by the independent
in-app changelog workflow and validated against `changelogs/index.schema.json`.

The schema requires English for every release and accepts other BCP 47 locale
keys; it does not contain a four-language allowlist.

The app downloads the index, selects the best available locale for each
release using the current app locale preference order and BCP 47
language-and-script matching, and downloads only that Markdown file. English
is the explicit fallback when no preferred locale is available. Each selected
locale is cached separately with its ETag; the app does not download every
locale listed in the index. Locale selection happens in the repository, so the
repository and view-model release-note contracts carry the already-localized
Markdown as a plain string rather than a multi-locale `TranslatedString`.

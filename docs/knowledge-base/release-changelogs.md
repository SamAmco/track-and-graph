---
title: Release changelogs — workflow, structure, and Play Store limits
description: Snapshot version revisions; public and Play Store changelogs; creation, preview, and localization workflows; limits; and index.json maintenance.
topics:
  - Snapshot releases: guarded make target increments snapshots or interactively starts a major, minor, or patch snapshot in a dedicated jj revision
  - make changelog: lua script creates temp file, opens in nvim, processes output
  - Public changelogs: changelogs/{versionName}/{locale}.md with index.json
  - publish flag: controls whether public markdown and changelogs/index.json are created for in-app release dialog visibility
  - Fastlane changelogs: fastlane/metadata/android/{regional-locale}/changelogs/{versionCode}.txt
  - Play Store limit: 500 characters per language for "What's new" text
  - GitHub releases: prefer public English markdown via gh --notes-file, fall back to English Fastlane text for patches
  - Changelog viewer app: paste markdown and preview the shared dialog plus FOSS and mock Play support flows
  - Locales: en-GB/en, es-ES/es, fr-FR/fr, de-DE/de
  - Public changelog copy-editing: finalize English first, then translate locale markdown using app string resources for terms
  - API translation: full Markdown per locale, domain brief, deterministic validation, explicit invocation only
keywords: [changelog, release, snapshot, snapshot-release, jj, changelog-viewer, markdown, preview, dialog, fastlane, play-store, make-changelog, localization, translation, domain-brief, 500-char, index.json, versionCode, versionName]
---

# Release Changelogs

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

## Dual Changelog System

There are two separate changelog outputs for each release:

1. **Public changelogs** — full-length markdown in `changelogs/{versionName}/` (e.g. `changelogs/10.0.0/en.md`). These are referenced by the app and website via `changelogs/index.json`. No character limit — can include images, headers, detailed descriptions.

2. **Fastlane changelogs** — short plain text in `fastlane/metadata/android/{regional-locale}/changelogs/{versionCode}.txt` (e.g. `fastlane/metadata/android/en-GB/changelogs/800010.txt`). These are uploaded to the Google Play Store's "What's new" section.

**Play Store limit: 500 characters per language.** Fastlane changelogs must stay under this. Use `wc -m` (not `wc -c`) to count characters accurately for multi-byte/emoji content. The full public changelogs are typically 2500-3000+ characters, so fastlane versions need to be heavily condensed summaries.

GitHub releases prefer the public English markdown changelog and fall back to the English Fastlane changelog when public markdown was not published. The release scripts pass whichever file exists to `gh release create` with `--notes-file`, so feature releases can render rich markdown while patch releases can reuse the short bullet list written for Play Store.

## Workflow: `make changelog`

Runs `scripts/new_changelog.lua` which:

1. Reads `versionCode` and `versionName` from `app/app/build.gradle.kts`
2. Finds the most recent git tag and generates a git log since that tag
3. Creates a temp lua file with a template data structure and the git log as comments
4. Opens the file in neovim for editing
5. On save, processes the lua structure to write:
   - Fastlane changelogs (always)
   - Public changelogs + `changelogs/index.json` update (only if `publish = true`)
6. Validates `index.json` against `changelogs/index.schema.json` when public changelogs are published

The lua template has entries for all 4 locales with `regional` (e.g. `en-GB`) and `general` (e.g. `en`) locale codes. Regional codes are used for fastlane directory paths, general codes for public changelog filenames.

The `publish` flag controls whether full public markdown is created and listed in `changelogs/index.json`, which is what makes the full-screen in-app release notes available to users after update. Patch releases can use `publish = false` to avoid the in-app dialog and reuse the short English Fastlane changelog for GitHub.

## Previewing In-App Markdown

Use the `changelog-viewer` Android module to preview public changelog markdown before publishing it. The viewer lets you paste or clear markdown text and opens the same shared changelog dialog content used by the production app. Its support footer displays both distribution variants together: the Buy Me a Coffee action opens the shared thank-you state directly, while the Play action exercises the animated mock price-options flow, back navigation, and simulated purchase result. It does not open an external support link or include the Billing SDK.

The footer layout, localized support copy, support assets, thank-you content, dialog UI, and price-options body live in the shared UI module. Each production flavor supplies only its own support action; the viewer supplies both actions to the same footer component. Only the viewer's Play product data and purchase result are fake.

## Copy Editing and Translation

When iterating on public changelog copy, treat the English markdown as the source text. Finalize wording there before translating the other locale files; this avoids doing the same copy edits four times. Locale files may exist as placeholders before translation.

When translating, check string resources for official UI terms before choosing feature names or labels. This matters for both old features and newly released UI, not only for terms called out in this document.

### API translation workflow

The explicitly invoked `.agents/skills/translate-release-notes` workflow uses
`scripts/translations/translate_release_notes.py`. It sends the complete English
Markdown and `domain_brief.md` in one request per locale through a swappable
provider adapter. Locales run concurrently. Deterministic validation protects
URLs, link targets, code, identifiers, and Markdown structure. Returned results
are always written and the final JSON summary lists invalid locales and checks.
The default is one paid request per locale; repair small structural failures
locally and rerun `markdown_validation.py` before considering an API retry.

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

The complex Markdown fixture is intentionally harsher than normal release
notes. A 2026-09-18 full GPT-5.6 Luna run produced structurally valid output for
61 of 65 languages after bounded retries. The four rejected locales repeatedly
dropped one bold span; valid lower-resource outputs also showed occasional
untranslated terminology or mixed-script text. All four structural failures
were repaired locally by restoring the missing emphasis, then passed validation
without another API call. Keep deterministic validation and manually spot-check
meaning and script consistency before publishing.

## Locales

| Regional | General | String resources dir |
|----------|---------|---------------------|
| en-GB    | en      | values/             |
| es-ES    | es      | values-es/          |
| fr-FR    | fr      | values-fr/          |
| de-DE    | de      | values-de-rDE/      |

When translating changelogs, check string resources for official translations of feature names (e.g. "Symlink" is "Enlace simbólico" in Spanish, "Lien symbolique" in French, but stays "Symlink" in German).

## index.json

Located at `changelogs/index.json`, maps version names to locale-specific markdown paths for in-app release notes. Updated automatically by the lua script when `publish = true`. Validated against `changelogs/index.schema.json`.

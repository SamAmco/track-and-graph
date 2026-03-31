---
title: Release changelogs — workflow, structure, and Play Store limits
description: Dual changelog system (public markdown for in-app/website, fastlane .txt for Play Store), the make changelog lua workflow, Play Store 500-character limit, 4-language localization requirement, and index.json maintenance.
topics:
  - make changelog: lua script creates temp file, opens in nvim, processes output
  - Public changelogs: changelogs/{versionName}/{locale}.md with index.json
  - Fastlane changelogs: fastlane/metadata/android/{regional-locale}/changelogs/{versionCode}.txt
  - Play Store limit: 500 characters per language for "What's new" text
  - Locales: en-GB/en, es-ES/es, fr-FR/fr, de-DE/de
keywords: [changelog, release, fastlane, play-store, make-changelog, localization, 500-char, index.json, versionCode, versionName]
---

# Release Changelogs

## Dual Changelog System

There are two separate changelog outputs for each release:

1. **Public changelogs** — full-length markdown in `changelogs/{versionName}/` (e.g. `changelogs/10.0.0/en.md`). These are referenced by the app and website via `changelogs/index.json`. No character limit — can include images, headers, detailed descriptions.

2. **Fastlane changelogs** — short plain text in `fastlane/metadata/android/{regional-locale}/changelogs/{versionCode}.txt` (e.g. `fastlane/metadata/android/en-GB/changelogs/800010.txt`). These are uploaded to the Google Play Store's "What's new" section.

**Play Store limit: 500 characters per language.** Fastlane changelogs must stay under this. Use `wc -m` (not `wc -c`) to count characters accurately for multi-byte/emoji content. The full public changelogs are typically 2500-3000+ characters, so fastlane versions need to be heavily condensed summaries.

## Workflow: `make changelog`

Runs `scripts/new_changelog.lua` which:

1. Reads `versionCode` and `versionName` from `app/app/build.gradle.kts`
2. Finds the most recent git tag and generates a git log since that tag
3. Creates a temp lua file with a template data structure and the git log as comments
4. Opens the file in neovim for editing
5. On save, processes the lua structure to write:
   - Fastlane changelogs (always)
   - Public changelogs + index.json update (only if `publish = true`)
6. Validates `index.json` against `changelogs/index.schema.json` using `jsonschema-cli`

The lua template has entries for all 4 locales with `regional` (e.g. `en-GB`) and `general` (e.g. `en`) locale codes. Regional codes are used for fastlane directory paths, general codes for public changelog filenames.

## Locales

| Regional | General | String resources dir |
|----------|---------|---------------------|
| en-GB    | en      | values/             |
| es-ES    | es      | values-es/          |
| fr-FR    | fr      | values-fr/          |
| de-DE    | de      | values-de-rDE/      |

When translating changelogs, check string resources for official translations of feature names (e.g. "Symlink" is "Enlace simbólico" in Spanish, "Lien symbolique" in French, but stays "Symlink" in German).

## index.json

Located at `changelogs/index.json`, maps version names to locale-specific markdown paths. Updated automatically by the lua script when `publish = true`. Validated against `changelogs/index.schema.json`.

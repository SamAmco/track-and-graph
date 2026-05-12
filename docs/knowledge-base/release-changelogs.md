---
title: Release changelogs — workflow, structure, and Play Store limits
description: Dual changelog system (public markdown for in-app/website, fastlane .txt for Play Store), changelog viewer app, the make changelog lua workflow, Play Store 500-character limit, 4-language localization requirement, and index.json maintenance.
topics:
  - make changelog: lua script creates temp file, opens in nvim, processes output
  - Public changelogs: changelogs/{versionName}/{locale}.md with index.json
  - publish flag: controls whether public markdown and changelogs/index.json are created for in-app release dialog visibility
  - Fastlane changelogs: fastlane/metadata/android/{regional-locale}/changelogs/{versionCode}.txt
  - Play Store limit: 500 characters per language for "What's new" text
  - GitHub releases: prefer public English markdown via gh --notes-file, fall back to English Fastlane text for patches
  - Changelog viewer app: paste markdown and preview the shared in-app dialog
  - Locales: en-GB/en, es-ES/es, fr-FR/fr, de-DE/de
  - Public changelog copy-editing: finalize English first, then translate locale markdown using app string resources for terms
keywords: [changelog, release, changelog-viewer, markdown, preview, dialog, fastlane, play-store, make-changelog, localization, 500-char, index.json, versionCode, versionName]
---

# Release Changelogs

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

Use the `changelog-viewer` Android module to preview public changelog markdown before publishing it. The viewer lets you paste or clear markdown text and opens the same shared changelog dialog content used by the production app.

The dialog UI lives in the shared UI module and is parameterized by text/callbacks, so release-note feature copy remains owned by the consuming module while dialog layout, theme, and markdown rendering stay reusable.

## Copy Editing and Translation

When iterating on public changelog copy, treat the English markdown as the source text. Finalize wording there before translating the other locale files; this avoids doing the same copy edits four times. Locale files may exist as placeholders before translation.

When translating, check string resources for official UI terms before choosing feature names or labels. This matters for both old features and newly released UI, not only for terms called out in this document.

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

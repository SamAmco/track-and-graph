---
title: Remote configuration and URL navigation
description: How external URLs are managed via remote config and opened from the app — UrlNavigator pattern, adding new URLs, and the multi-file update checklist.
topics: [remote-config, url-navigation, endpoints, endpoint-deprecation]
keywords: [RemoteConfiguration, UrlNavigator, Endpoints, remote-config, URL, navigate, triggerNavigation, browser, deprecation, schema, rollout]
---

# Remote Configuration and URL Navigation

## Overview

External URLs (docs site, GitHub, Play Store, etc.) are **never hard-coded in UI code**. They are defined in a remote configuration JSON and accessed through `UrlNavigator`.

## Architecture

- **`configuration/remote-configuration.json`** — the source of truth for all URLs, deployed remotely
- **`configuration/remote-configuration.schema.json`** — JSON Schema validating the config
- **`RemoteConfigurationData.kt`** — Kotlin data classes (`RemoteConfiguration`, `Endpoints`) matching the JSON structure, uses `kotlinx.serialization`
- **`UrlNavigator`** — interface with a `Location` enum mapping logical destinations to URLs
- **`UrlNavigatorImpl`** — loads remote config via `RemoteConfigProvider`, resolves `Location` to URL, opens via `Intent.ACTION_VIEW`. Falls back to GitHub homepage on failure.

## Adding a New URL Endpoint

Update these files (all required):

1. **`configuration/remote-configuration.json`** — add the URL value
2. **`configuration/remote-configuration.schema.json`** — add to `required` array and `properties`
3. **`RemoteConfigurationData.kt`** — add field to `Endpoints` with `@SerialName`
4. **`UrlNavigator.kt`** — add entry to `Location` enum
5. **`UrlNavigatorImpl.kt`** — add mapping in `getUrlForLocation()`
6. **`RemoteConfigurationFixtures.kt`** (test) — add field to `testEndpoints`

## Retiring an endpoint

Removing a current UI button does not authorize deleting its shared configuration
key: released clients may still require it. Keep the legacy JSON/schema fields,
record the deprecation date and affected client era in the schema's `$comment`,
and remove only unused current navigation/UI plumbing and resources. Retained
serialized fields do not require retained navigation enum entries.

Change schema, checked-in configuration, fixtures, and call sites together.
Do not silently default a new endpoint to a legacy destination unless explicitly
required; retaining old keys for released clients is a separate concern.
Use intended post-merge URLs; for unpublished documentation,
validate the schema and locally built destinations rather than treating the live
site as authoritative. Keep deployment separate from implementation.

## Using UrlNavigator from UI

Two access patterns exist:

1. **From a ViewModel** (preferred): Inject `UrlNavigator` and `@ApplicationContext Context` into the ViewModel. Expose a method that calls `urlNavigator.triggerNavigation(context, location)` (fire-and-forget) or `urlNavigator.navigateTo(context, location)` (suspend, returns success boolean). Wire to the composable via a callback/lambda.

2. **From Activity-level code** (e.g. drawer menu): Inject `UrlNavigator` into the Activity and call `triggerNavigation` directly.

## Design Decisions

- **No `InfoDisplay` for external links**: When an info button should open an external URL rather than show an in-app dialog, use a separate lambda (e.g. `onLuaScriptInfoClick`) threaded through the composable chain rather than routing through the `InfoDisplay` sealed class. `InfoDisplay` is only for in-app description dialogs.

## Legacy graph documentation routing

Older app releases and current package-aware builds use separate documentation
endpoint keys. Keep the legacy keys in the shared configuration rather than
renaming them. The paths, required fields, and merge/site-publication constraint
are documented in [Lua graph deprecation](lua-graph-scripts.md#legacy-catalog-deprecation-and-release).

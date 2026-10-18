---
title: Project map — directory structure
description: Top-level repository layout, with notes on each module, build logic, folder purpose, and documentation permissions.
topics: [project-structure, modules, directories, build-logic, documentation-permissions]
---

# Project Map

```
app/                            # Gradle multi-module project root
├── app/                        # UI layer (Compose, ViewModels, navigation)
│   └── src/
│       ├── main/               # Feature packages (adddatapoint, group, reminders, etc.)
│       ├── test/               # Unit tests
│       ├── androidTest/        # Instrumented tests
│       └── debug/
├── build-logic/                # Included Gradle build with convention plugins
├── changelog-viewer/           # Developer app for previewing changelog markdown
├── data/                       # Data layer (Room, DTOs, business logic)
    ├── schemas/                # Room migration schemas (JSON, one per DB version)
    └── src/
        ├── main/               # database/, dto/, interactor/, algorithms/, lua/, etc.
        ├── test/               # Unit tests
        ├── testFixtures/       # Fake implementations for testing
        └── androidTest/
└── ui/                         # Shared Compose UI components, theming, and UI resources
    └── src/
        └── main/
changelogs/                     # Release changelogs (per version)
configuration/                  # Python venv / tooling config
database-extract/               # Extracted DB snapshots (dev tooling)
docs/
├── knowledge-base/             # Technical docs (see knowledge-base/index.yaml)
└── docs/                       # User-facing docs (tutorials, Lua, policies)
fastlane/                       # Release automation (metadata, frameit)
image-assets/                   # App store / marketing images
lua/
├── src/community/functions/    # Community Lua function scripts
├── src/tng/                    # TNG API and .apispec.lua files
├── catalog/                    # Generated catalog output (bundled for distribution)
└── tools/                      # Build and validation scripts (pack-functions.lua, etc.)
scripts/                        # Misc build/utility scripts
```

Files under `docs/docs/lua/developer-guide/` require explicit user permission to
change. Implementing Lua features or updating the technical knowledge base does
not authorize edits to that user-facing developer guide.

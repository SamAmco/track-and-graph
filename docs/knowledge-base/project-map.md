---
title: Project map — directory structure
description: Top-level directory layout of the Track & Graph repository, with notes on each module and folder's purpose.
topics: [project-structure, modules, directories]
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
└── data/                       # Data layer (Room, DTOs, business logic)
    ├── schemas/                # Room migration schemas (JSON, one per DB version)
    └── src/
        ├── main/               # database/, dto/, interactor/, algorithms/, lua/, etc.
        ├── test/               # Unit tests
        ├── testFixtures/       # Fake implementations for testing
        └── androidTest/
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

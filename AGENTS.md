# Track & Graph

Android app for personal data tracking and custom graph visualization.

## Project Map

```
app/
├── app/                        # UI layer (Compose, ViewModels, navigation)
│   └── src/main/.../
│       ├── adddatapoint/       # Each top-level package = a future feature module
│       ├── reminders/          # (see architecture.md for full list)
│       ├── di/                 # Hilt wiring (stays in :app shell)
│       └── ...
└── data/                       # Data layer (Room, DTOs, business logic)
    └── src/
        ├── main/.../database/       # Entities, DAOs, Room database
        ├── main/.../dto/            # Data Transfer Objects
        ├── main/.../interactor/     # DataInteractor and Helpers
        ├── test/                    # Unit tests
        └── testFixtures/            # Fake implementations for testing
docs/
├── knowledgebase/              # Technical docs (see index below)
└── docs/                       # User-facing docs (tutorials, Lua)
lua/                            # Community Lua functions
schemas/                        # Room migration schemas
```

## Knowledgebase Index

ALWAYS search the knowledgebase and read any potentially related docs before attempting to execute a task. 

| Topic | File |
|-------|------|
| Architecture, conventions, build commands | [architecture.md](docs/knowledgebase/architecture.md) |
| Database tables and relationships | [database-schema.md](docs/knowledgebase/database-schema.md) |
| Group DAG structure and symlinks | [group-hierarchy.md](docs/knowledgebase/group-hierarchy.md) |
| Trackers, Functions, Graphs, Reminders, Groups | [component-types.md](docs/knowledgebase/component-types.md) |
| Junction table and display ordering | [group-items.md](docs/knowledgebase/group-items.md) |
| Groupless reminders | [reminders.md](docs/knowledgebase/reminders.md) |
| DataInteractor and Helper pattern | [helper-classes.md](docs/knowledgebase/helper-classes.md) |
| Lua functions app architecture | [lua-architecture.md](docs/knowledgebase/lua-architecture.md) |
| Writing Lua function scripts | [lua-community-functions.md](docs/knowledgebase/lua-community-functions.md) |
| Writing Lua graph scripts | [lua-graph-scripts.md](docs/knowledgebase/lua-graph-scripts.md) |
| Lua tooling, building, config types | [lua-tooling.md](docs/knowledgebase/lua-tooling.md) |

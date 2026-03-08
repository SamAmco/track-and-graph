# Track & Graph

Android app for personal data tracking and custom graph visualization.

## Rules

NEVER create personal memory files (e.g. MEMORY.md). If something is worth remembering, update the knowledge base instead.

ALWAYS ALWAYS ALWAYS consult the knowledge base FIRST!

All technical documentation is in `docs/knowledge-base/`. The correct workflow is:

### Step 1 — Grep the index for relevant keywords
```
Grep: <keywords from your task> in docs/knowledge-base/index.yaml
```
Each entry in the index looks like this:
```yaml
  - file: helper-classes.md
    title: DataInteractor and Helper pattern
    description: DataInteractor public interface, DataInteractorImpl delegation...
    keywords: [DataInteractor, helper, DAO, TrackerHelper, delete, symlink, withTransaction]
```
Grep matches against any field. The `file` value is what you pass to Read. Match against your task domain (e.g. "tracker", "migration", "reminder", "lua", "display_index").

### Step 2 — Read only the matched files
Open the 1–3 files that matched. Each file has front-matter at the top (title, description, topics) so you can confirm relevance before reading the full content.

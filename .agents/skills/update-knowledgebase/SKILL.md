---
user-invocable: true
description: Review and update the developer knowledgebase based on recent learnings
---

# Update Knowledgebase

Review and update the developer knowledgebase based on recent learnings and changes. If you got something wrong, check if the knowledge you needed to get it right is missing or incorrect. If it is present and correct, think about how you could structure the knowledgebase better to avoid missing it in future. If the knowledge base is correct and you didn't need any help to complete a task, you don't HAVE to make changes to the knowledgebase.

## Documentation Structure

- **`AGENTS.md`** (root) - Map only, no content. Two sections:
  1. Project Map - directory structure
  2. Knowledgebase Index - table linking to topic files

- **`docs/knowledgebase/*.md`** - All technical content lives here

**DRY Principle**: Never duplicate information. AGENTS.md indexes the knowledgebase, it does not summarize or repeat it. Knowledgebase files may reference each other but should not repeat each other.

You may use subdirectories in `docs/knowledgebase/` if it helps organize topics, but keep the index in AGENTS.md updated to reflect any changes.

## Instructions

1. **Discover and read the current knowledgebase**:
   - Read `AGENTS.md` for the current index
   - List and read all relevant files in `docs/knowledgebase/`

2. **Review recent context** from this conversation:
   - What new patterns or concepts have you learned?
   - What code changes affect documented behavior?
   - Are there corrections needed?

3. **Update the knowledgebase**:
   - **Delete** outdated or incorrect information
   - **Update** existing sections that need corrections
   - **Add** new information that would be valuable
   - **Create new files** if a topic deserves dedicated documentation
   - **Combine files** if you find overlapping content that can be consolidated
   - **Update AGENTS.md index** if files are added/removed

4. **Maintain quality**:
   - Keep each file as concise as possible without oversimplifying
   - Use cross-references between files instead of repeating content
   - Avoid too many file paths as code can be moved (prefer search guidelines)
   - Avoid copying code to the knowledgebase unless it's necessary for understanding (prefer links to code instead)
   - Never put content in AGENTS.md - it's just a map

5. **Report changes**: Summarize what was updated, added, or removed

## When to Use

Invoke `/update-knowledgebase` after:
- Completing significant implementation work
- Learning new patterns about the codebase
- Fixing bugs that reveal undocumented behavior
- Making architectural changes

---
user-invocable: true
description: Review and update the developer knowledge base based on recent learnings
---

# Update Knowledge Base

Review and update the developer knowledge base based on recent learnings and changes. If you got something wrong, check if the knowledge you needed to get it right is missing or incorrect. If it is present and correct, think about how you could structure the knowledge base better to avoid missing it in future. If the knowledge base is correct and you didn't need any help to complete a task, you don't HAVE to make changes to the knowledge base.

## Documentation Structure

- **`CLAUDE.md`** (root) - Map only, no content. Points to `docs/knowledge-base/index.yaml`.

- **`docs/knowledge-base/index.yaml`** - YAML index listing all docs with title, description, and keywords. The single entry point for discovering what documentation exists.

- **`docs/knowledge-base/*.md`** - All technical content lives here. Each file has YAML front-matter (title, description, topics, keywords) at the top.

Try to avoid repeating information that is clear from the code, look to capture the context and intent around it e.g.
   - What solutions were considered and why one was chosen
   - What constraints or tradeoffs exist that aren't obvious from the code itself
   - What changes does this approach leave the door open to in the future
   - Is there a particular use-case or bug the approach was designed to address 
   - What other parts of the codebase does this interact with that aren't obvious from the code itself
   - What do you wish you'd known before you started working on this that would have helped you get it right the first time?
   - What risks does the pattern have? If there was a bug, what would it likely be? Where's the best place to look first?
You do not need to cover all of these explicitly, these are just examples of what it is appropriate to document. Do not guess or make assumptions, write only what you know for sure. It is better to miss something than to put something misleading in the knowledge base. If you think you're missing something important, ask a clarifying question.

**DRY Principle**: Never duplicate information. `index.yaml` indexes the knowledge base, it does not summarize or repeat it. Knowledge base files may reference each other but should not repeat each other.

You may use subdirectories in `docs/knowledge-base/` if it helps organize topics, but keep `index.yaml` updated to reflect any changes.

## Instructions

1. **Discover and read the current knowledge base**:
   - Read `docs/knowledge-base/index.yaml` for the current index
   - Open and read only the files relevant to what was just worked on

2. **Review recent context** from this conversation:
   - What new patterns or concepts have you learned?
   - What code changes affect documented behavior?
   - Are there corrections needed?

3. **Update the knowledge base**:
   - **Delete** outdated or incorrect information
   - **Update** existing sections that need corrections
   - **Add** new information that would be valuable
   - **Create new files** if a topic deserves dedicated documentation (add front-matter)
   - **Combine files** if you find overlapping content that can be consolidated
   - **Update `index.yaml`** if files are added, removed, or renamed (keep title/description/keywords accurate)
   - **Update front-matter** on any file whose content changes significantly

4. **Maintain quality**:
   - Keep each file as concise as possible without oversimplifying
   - Use cross-references between files instead of repeating content
   - Avoid too many file paths as code can be moved (prefer search guidelines)
   - Avoid copying code to the knowledge base unless it's necessary for understanding (prefer links to code instead)
   - Never put content in `CLAUDE.md` or `index.yaml` beyond what is needed for navigation

5. **Report changes**: Summarize what was updated, added, or removed

## When to Use

Invoke `/update-knowledge-base` after:
- Completing significant implementation work
- Learning new patterns about the codebase
- Fixing bugs that reveal undocumented behavior
- Making architectural changes

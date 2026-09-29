---
name: author-in-app-changelog
description: Draft a new user-facing English Track & Graph in-app changelog from the changes since the previous published release. Use when explicitly asked to create or draft an in-app/public changelog; do not use for concise Play Store or GitHub release notes.
---

# Author an In-App Changelog

Create the English long-form Markdown changelog for review. Do not translate it;
translation is a separate, explicitly requested workflow.

## Establish the release range

- Read `docs/knowledge-base/release-changelogs.md`,
  `changelogs/index.json`, and the newest indexed English changelog.
- Determine the target stable version from the user's request or the app's
  `versionName`. Confirm only if those sources genuinely conflict.
- Treat the newest version in `changelogs/index.json` as the previous published
  in-app changelog. Find its corresponding release tag, then use `jj log` to
  inspect every commit from that release through the current working-copy
  parent. Use `jj`, never Git.
- Read relevant diffs when a commit subject does not reveal its user-visible
  effect. Do not infer a benefit that the changes do not support.

## Write the draft

- Create `changelogs/<version>/en.md` and follow the broad structure and tone of
  the previous English changelog. Reuse its organization, not its claims.
- Lead with meaningful features and fixes from the user's perspective. Describe
  what a person can now do or what works better, not implementation details,
  refactors, dependency changes, or internal architecture.
- Do not announce commits limited to remotely distributed Lua functions or
  graph definitions: those are not shipped as part of the app binary release.
- Present the final state of features introduced in this release. Fold their
  subsequent fixes, redesigns, and refactors into the feature description; do
  not announce those as separate bugs or improvements.
- Consolidate related commits. Include a bug fix only when it affects behavior
  that existed before this release and is meaningful to users.
- Do not claim that every commit needs a changelog bullet.

Append the complete, unfiltered commit inventory after the draft so the user can
check coverage. Begin it with this exact line:

```markdown
<!-- REVIEW-ONLY-COMMITS: remove before translation/publishing -->
```

Include each commit ID and full first-line description, including commits that
were deliberately excluded from the public prose. Leave this section in place
for the user's review. `make in-app-changelog-validate` intentionally fails
while the marker remains, preventing accidental publication.

## Update the index and hand off

- Run `make in-app-changelog-index-update VERSION=<version>`; never edit the
  locale map manually.
- Report the draft path, previous release/tag, commit range, deliberately
  excluded themes, and that the review-only block must be removed.
- Do not invoke the translation skill or make paid API calls. Once the user has
  approved the English copy and removed the review block, the separate
  `$translate-in-app-changelogs` workflow can be requested.

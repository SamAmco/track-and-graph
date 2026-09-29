# Publish an in-app changelog

- Invoke `$author-in-app-changelog` for the target version.
- Review `changelogs/<version>/en.md` against its complete commit inventory;
  revise the public copy, then remove the entire `REVIEW-ONLY-COMMITS` section.
- Preview the English Markdown in the `changelog-viewer` app.
- Explicitly invoke `$translate-in-app-changelogs` when the English copy is
  final. It writes translations directly into `changelogs/<version>/` and makes
  paid API calls.
- Review and repair any flagged locale files in place.
- Run `make in-app-changelog-index-update VERSION=<version>` and
  `make in-app-changelog-validate` before publishing.

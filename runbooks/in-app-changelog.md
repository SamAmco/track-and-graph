# Publish an in-app changelog

- Invoke `$author-in-app-changelog` for the target version.
- Review `changelogs/<version>/en.md` against its complete commit inventory; revise the public copy, then remove the entire `REVIEW-ONLY-COMMITS` section.
- Preview the English Markdown in the `changelog-viewer` app.
- Explicitly invoke `$translate-in-app-changelogs`, asking it to translate, review, and repair the locale files in place. It writes directly into `changelogs/<version>/` and makes paid API calls.
- Run `make in-app-changelog-index-update VERSION=<version>` and `make in-app-changelog-validate` before publishing if the agent didn't already. Validation is also included in `make validate-all`.
- Commit the version directory, `changelogs/index.json`, and any domain-brief changes, then push the revision to `master`. 

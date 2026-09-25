.DEFAULT_GOAL := help

.PHONY: help
## help: List the available Make targets and their purpose.
help:
	@awk 'BEGIN {FS = ":.*##? "} /^## [a-zA-Z0-9_.-]+:/ {sub(/^## /, "", $$1); printf "  %-36s %s\n", $$1, $$2}' $(MAKEFILE_LIST)

.PHONY: deep-link-serve-local
## deep-link-serve-local: Serve FILE over the local network and open its deep link on a connected device.
deep-link-serve-local:
ifndef FILE
	@echo "Error: FILE parameter is required"
	@echo "Usage: make deep-link-serve-local FILE=<file_path>"
	@exit 1
endif
	@echo "Injecting deep link for file: $(FILE)"
	python3 scripts/serve-and-deep-link-lua.py $(FILE)


.PHONY: deep-link-inject
## deep-link-inject: Inject FILE directly into the app through an adb deep link.
deep-link-inject:
ifndef FILE
	@echo "Error: FILE parameter is required"
	@echo "Usage: make deep-link-inject FILE=<file_path>"
	@exit 1
endif
	@./scripts/deep-link-inject.sh $(FILE)

.PHONY: validate-remote-config
## validate-remote-config: Validate the checked-in remote configuration and referenced assets.
validate-remote-config:
	@./scripts/validate-remote-config.sh

.PHONY: translations-audit translations-apply-failure translations-baseline translations-generate translations-test translations-validate fastlane-translations-generate lua-translations-function lua-translations-shared lua-translations-apply
## translations-audit: Report missing, stale, and target-only Android translations without modifying files.
translations-audit:
	@python3 -B scripts/translations/translate_app_resources.py audit $(TRANSLATION_ARGS)

## translations-apply-failure: Validate and apply a manually repaired Android translation failure artifact.
translations-apply-failure:
	@python3 -B scripts/translations/translate_app_resources.py apply-failure $(TRANSLATION_ARGS)

## translations-baseline: Adopt existing Android translations at the current English source hashes without API calls.
translations-baseline:
	@python3 -B scripts/translations/translate_app_resources.py baseline $(TRANSLATION_ARGS)

## translations-generate: Generate missing or stale Android translations for all locales by default (paid API calls).
translations-generate: TRANSLATION_ARGS ?= --all-targets
translations-generate:
	@python3 -B scripts/translations/translate_app_resources.py translate $(TRANSLATION_ARGS)

## translations-test: Run the offline Python test suite for translation tooling.
translations-test:
	@python3 -B -m unittest discover -s scripts/translations/tests -p 'test_*.py' -v

## translations-validate: Fail for incomplete, stale, invalid, or target-only Android translations.
translations-validate:
	@python3 -B scripts/translations/translate_app_resources.py audit --fail-on-issues

## fastlane-translations-generate: Regenerate translated Play Store listing copy for all locales by default (paid API calls).
fastlane-translations-generate: TRANSLATION_ARGS ?= --all-targets
fastlane-translations-generate:
	@python3 -B scripts/translations/translate_fastlane_metadata.py $(TRANSLATION_ARGS)

## lua-translations-function: Generate reviewed Lua translation draft code for FUNCTION (paid API calls).
lua-translations-function:
	@test -n "$(FUNCTION)" || (echo "Usage: make lua-translations-function FUNCTION=<id> [TRANSLATION_ARGS=...]" && exit 1)
	@python3 -B scripts/translations/translate_lua_catalog.py --function "$(FUNCTION)" $(TRANSLATION_ARGS)

## lua-translations-shared: Generate draft code for incomplete shared Lua translations (paid API calls).
lua-translations-shared:
	@python3 -B scripts/translations/translate_lua_catalog.py --shared $(TRANSLATION_ARGS)

## lua-translations-apply: Apply a reviewed Lua translation DRAFT to its recorded source file.
lua-translations-apply:
	@test -n "$(DRAFT)" || (echo "Usage: make lua-translations-apply DRAFT=<draft.json>" && exit 1)
	@python3 -B scripts/translations/apply_lua_catalog_draft.py "$(DRAFT)"

.PHONY: run-community-tests
## run-community-tests: Run both community function and graph script integration tests.
run-community-tests: run-community-functions-tests run-community-graph-tests

.PHONY: run-community-functions-tests
## run-community-functions-tests: Run the community Lua function catalog integration tests.
run-community-functions-tests:
	cd app && ./gradlew :data:cleanTestDebugUnitTest :data:testDebugUnitTest --tests "com.samco.trackandgraph.data.lua.community_test_runner.FunctionTestRunner"

.PHONY: run-community-graph-tests
## run-community-graph-tests: Run the community Lua graph script integration tests.
run-community-graph-tests:
	cd app && ./gradlew :data:cleanTestDebugUnitTest :data:testDebugUnitTest --tests "com.samco.trackandgraph.data.lua.community_test_runner.GraphScriptTestRunner"

.PHONY: sync-lua-to-docs
## sync-lua-to-docs: Copy the public Lua API sources into the generated documentation tree.
sync-lua-to-docs:
	./scripts/sync-lua-to-docs.sh

.PHONY: lua-verify-api-specs
## lua-verify-api-specs: Verify the Lua API specifications and generated documentation agree.
lua-verify-api-specs:
	cd lua && lua tools/verify-api-specs.lua

.PHONY: lua-get-max-api-level
## lua-get-max-api-level: Print the maximum API level declared by the Lua API specifications.
lua-get-max-api-level:
	cd lua && lua tools/get-max-api-level.lua

.PHONY: lua-validate-functions
## lua-validate-functions: Validate every community function and its complete localization data.
lua-validate-functions:
	cd lua && lua tools/validate-functions.lua

.PHONY: lua-detect-changes
## lua-detect-changes: Detect catalog changes that require function version updates.
lua-detect-changes:
	cd lua && lua tools/detect-changes.lua

.PHONY: lua-pack-functions
## lua-pack-functions: Build the generated community function catalog from source functions.
lua-pack-functions:
	cd lua && lua tools/pack-functions.lua

.PHONY: lua-publish-debug
## lua-publish-debug: Pack and publish the community function catalog with the debug key.
lua-publish-debug: lua-pack-functions
	cd lua && lua tools/publish-functions-debug.lua

.PHONY: lua-publish-prod
## lua-publish-prod: Pack and publish the community function catalog with the production key.
lua-publish-prod: lua-pack-functions
	cd lua && lua tools/publish-functions-prod.lua

.PHONY: lua-print-catalog
## lua-print-catalog: Print the generated community function catalog.
lua-print-catalog:
	cd lua && lua tools/print-catalog.lua

.PHONY: lua-test-api
## lua-test-api: Run the Lua API unit test suite.
lua-test-api:
	cd lua && lua src/tng/test/test_all.lua

.PHONY: lua-test-tools
## lua-test-tools: Run the Lua developer-tool unit test suite.
lua-test-tools:
	cd lua && lua tools/test/test_all.lua

.PHONY: validate-all
## validate-all: Run the complete pre-release validation suite.
validate-all: translations-test translations-validate playstore-screenshot-tests-check lua-test-api lua-test-tools validate-remote-config run-community-tests lua-verify-api-specs lua-validate-functions lua-detect-changes
	@echo "All validations passed."

.PHONY: assemble-release
## assemble-release: Alias for the Play Store release APK build.
assemble-release: assemble-playstore-release

.PHONY: assemble-playstore-release
## assemble-playstore-release: Clean and assemble the Play Store release APK.
assemble-playstore-release:
	cd app && ./gradlew clean :app:assemblePlayStoreRelease

.PHONY: assemble-foss-release
## assemble-foss-release: Clean and assemble the FOSS release APK.
assemble-foss-release:
	cd app && ./gradlew clean :app:assembleFossRelease

.PHONY: bundle-release
## bundle-release: Alias for the Play Store release bundle build.
bundle-release: bundle-playstore-release

.PHONY: bundle-playstore-release
## bundle-playstore-release: Clean and build the Play Store release AAB.
bundle-playstore-release:
	cd app && ./gradlew clean :app:bundlePlayStoreRelease

.PHONY: bundle-foss-release
## bundle-foss-release: Clean and build the FOSS release AAB.
bundle-foss-release:
	cd app && ./gradlew clean :app:bundleFossRelease

.PHONY: assemble-bundle-release
## assemble-bundle-release: Clean once, then build the Play Store release APK and AAB together.
assemble-bundle-release:
	cd app && ./gradlew clean :app:assemblePlayStoreRelease :app:bundlePlayStoreRelease

.PHONY: playstore-upload-alpha playstore-upload-beta playstore-upload-production
PLAYSTORE_AAB ?= app/app/build/outputs/bundle/playStoreRelease/app-playStore-release.aab

## playstore-upload-alpha: Upload the release AAB and its available changelogs to the Play alpha track.
playstore-upload-alpha:
	@bundle exec fastlane supply \
		--aab "$(PLAYSTORE_AAB)" \
		--track alpha \
		--skip_upload_apk \
		--skip_upload_metadata \
		--skip_upload_images \
		--skip_upload_screenshots $(PLAYSTORE_UPLOAD_ARGS)

## playstore-upload-beta: Upload the release AAB and its available changelogs to the Play beta track.
playstore-upload-beta:
	@bundle exec fastlane supply \
		--aab "$(PLAYSTORE_AAB)" \
		--track beta \
		--skip_upload_apk \
		--skip_upload_metadata \
		--skip_upload_images \
		--skip_upload_screenshots $(PLAYSTORE_UPLOAD_ARGS)

## playstore-upload-production: Upload the release AAB and changelogs to production; requires ROLLOUT (for example 0.5 or 1).
playstore-upload-production:
	@test -n "$(ROLLOUT)" || (echo "Usage: make playstore-upload-production ROLLOUT=<0..1> [PLAYSTORE_UPLOAD_ARGS=...]" && exit 1)
	@bundle exec fastlane supply \
		--aab "$(PLAYSTORE_AAB)" \
		--track production \
		--rollout "$(ROLLOUT)" \
		--skip_upload_apk \
		--skip_upload_metadata \
		--skip_upload_images \
		--skip_upload_screenshots $(PLAYSTORE_UPLOAD_ARGS)

# ---------- RECORD HIGH-RES PLAY STORE SHOTS ----------
.PHONY: playstore-screenshot-tests-generate playstore-screenshot-tests-check
## playstore-screenshot-tests-generate: Regenerate the localized Play Store Compose screenshot wrappers.
playstore-screenshot-tests-generate:
	@python3 -B scripts/translations/generate_playstore_screenshot_tests.py

## playstore-screenshot-tests-check: Fail when the generated Play Store screenshot wrappers are stale.
playstore-screenshot-tests-check:
	@python3 -B scripts/translations/generate_playstore_screenshot_tests.py --check

.PHONY: playstore-record
## playstore-record: Render and frame the configured Play Store screenshot locales.
playstore-record: playstore-screenshot-tests-generate
	@./scripts/playstore-record.sh

.PHONY: tutorial-record
## tutorial-record: Render and resize the Compose tutorial screenshots into Android density buckets.
tutorial-record:
	@./scripts/tutorial-record.sh

.PHONY: changelog
## changelog: Interactively create Play Store and optional public release notes for the current version.
changelog:
	@lua scripts/new_changelog.lua

.PHONY: snapshot-release
## snapshot-release: Create the next isolated snapshot-version revision.
snapshot-release:
	@python3 scripts/snapshot_release.py

.PHONY: commit-version
## commit-version: Commit the version bump and changelog using jj.
commit-version:
	@python3 scripts/commit_version_bump_jj.py

.PHONY: commit-version-git
## commit-version-git: Legacy git-based version/changelog commit helper.
commit-version-git:
	@python3 scripts/commit_version_bump.py

.PHONY: github-release
## github-release: Create a GitHub release from the current version and changelog using jj history.
github-release:
	@python3 scripts/create_release_jj.py

.PHONY: github-release-git
## github-release-git: Legacy git-based GitHub release helper.
github-release-git:
	@python3 scripts/create_release.py

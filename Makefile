.PHONY: deep-link-inject

deep-link-serve-local:
ifndef FILE
	@echo "Error: FILE parameter is required"
	@echo "Usage: make deep-link-inject FILE=<file_path>"
	@exit 1
endif
	@echo "Injecting deep link for file: $(FILE)"
	python3 scripts/serve-and-deep-link-lua.py $(FILE)


deep-link-inject:
ifndef FILE
	@echo "Error: FILE parameter is required"
	@echo "Usage: make deep-link-inject FILE=<file_path>"
	@exit 1
endif
	@./scripts/deep-link-inject.sh $(FILE)

validate-remote-config:
	@./scripts/validate-remote-config.sh

.PHONY: run-community-tests
run-community-tests: run-community-functions-tests run-community-graph-tests

.PHONY: run-community-functions-tests
run-community-functions-tests:
	./gradlew :data:cleanTestDebugUnitTest :data:testDebugUnitTest --tests "com.samco.trackandgraph.data.lua.community_test_runner.FunctionTestRunner"

.PHONY: run-community-graph-tests
run-community-graph-tests:
	./gradlew :data:cleanTestDebugUnitTest :data:testDebugUnitTest --tests "com.samco.trackandgraph.data.lua.community_test_runner.GraphScriptTestRunner"

.PHONY: sync-lua-to-docs
sync-lua-to-docs:
	./scripts/sync-lua-to-docs.sh

.PHONY: lua-verify-api-specs
lua-verify-api-specs:
	cd lua && lua tools/verify-api-specs.lua

.PHONY: lua-get-max-api-level
lua-get-max-api-level:
	cd lua && lua tools/get-max-api-level.lua

.PHONY: lua-validate-functions
lua-validate-functions:
	cd lua && lua tools/validate-functions.lua

.PHONY: lua-detect-changes
lua-detect-changes:
	cd lua && lua tools/detect-changes.lua

.PHONY: lua-pack-functions
lua-pack-functions:
	cd lua && lua tools/pack-functions.lua

.PHONY: lua-publish-debug
lua-publish-debug:
	cd lua && lua tools/publish-functions-debug.lua

.PHONY: lua-publish-prod
lua-publish-prod:
	cd lua && lua tools/publish-functions-prod.lua

.PHONY: lua-print-catalog
lua-print-catalog:
	cd lua && lua tools/print-catalog.lua

.PHONY: lua-test-api
lua-test-api:
	cd lua && lua src/tng/test/test_all.lua

.PHONY: validate-all
validate-all: lua-test-api validate-remote-config run-community-tests lua-verify-api-specs lua-validate-functions lua-detect-changes
	@echo "All validations passed."

.PHONY: assemble-release
assemble-release: 
	./gradlew clean assembleRelease

.PHONY: bundle-release
bundle-release:
	./gradlew clean bundleRelease

.PHONY: assemble-bundle-release
assemble-bundle-release:
	./gradlew clean assembleRelease bundleRelease

# ---------- 1) RECORD LOW-RES SNAPSHOT BASELINES ----------
.PHONY: snapshots-record
snapshots-record:
	@./scripts/snapshots-record.sh

# ---------- 2) VERIFY LOW-RES SNAPSHOTS ----------
.PHONY: snapshots-verify
snapshots-verify:
	@./scripts/snapshots-verify.sh

# ---------- 3) RECORD HIGH-RES PLAY STORE SHOTS ----------
.PHONY: playstore-record
playstore-record:
	@./scripts/playstore-record.sh

# ---------- 4) RECORD TUTORIAL IMAGES FOR APP ----------
.PHONY: tutorial-record
tutorial-record:
	@./scripts/tutorial-record.sh

.PHONY: changelog
changelog:
	@lua scripts/new_changelog.lua

.PHONY: commit-version
commit-version:
	@python3 scripts/commit_version_bump.py

.PHONY: github-release
github-release:
	@python3 scripts/create_release.py

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
	@echo "Injecting deep link for file: $(FILE)"
	# Copy the file over adb to the devices tmp directory
	adb push $(FILE) /data/local/tmp/tmp.lua
	adb shell am start -a android.intent.action.VIEW -d  "trackandgraph://lua_inject_file?path=/data/local/tmp/tmp.lua"


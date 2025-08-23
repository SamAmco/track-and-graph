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

validate-remote-config:
	cd ./configuration; source venv/bin/activate; python3 ./validate-config.py; deactivate



# --- Common config ---
HOST_ARCH := $(shell uname -m)
ifeq ($(HOST_ARCH),arm64)
  AVD_PACKAGE := system-images;android-35;google_apis;arm64-v8a
else
  AVD_PACKAGE := system-images;android-35;google_apis;x86_64
endif

EMULATOR := $(ANDROID_HOME)/emulator/emulator
AVDMANAGER := $(ANDROID_HOME)/cmdline-tools/latest/bin/avdmanager
SDKMANAGER := $(ANDROID_HOME)/cmdline-tools/latest/bin/sdkmanager

# Bail if something else is attached
check-no-devices:
	@if [ "`adb devices | grep -w 'device' | grep -v 'List'`" != "" ]; then \
	  echo "Error: another emulator or device is already running. Close it first."; \
	  exit 1; \
	fi

# Ensure AVD exists
ensure-avd-%:
ifndef ANDROID_HOME
	@echo "Error: ANDROID_HOME is not set."; exit 1
endif
	@$(EMULATOR) -list-avds | grep -Fx '$*' >/dev/null || ( \
		if [ -x "$(AVDMANAGER)" ]; then \
		  echo "==> Installing system image: $(AVD_PACKAGE)"; \
		  yes | "$(SDKMANAGER)" "$(AVD_PACKAGE)"; \
		  echo no | "$(AVDMANAGER)" create avd -n "$*" -k "$(AVD_PACKAGE)" -d "pixel_6"; \
		else \
		  echo "Error: avdmanager missing. Install Android cmdline-tools."; \
		  exit 1; \
		fi \
	)

# Boot, wait, prep device (visible emulator)
boot-and-prep-%:
	"$(EMULATOR)" -avd "$*" -gpu swiftshader_indirect -no-snapshot -no-boot-anim -no-audio &
	@adb wait-for-device
	@adb shell 'while [ "$$(getprop sys.boot_completed)" != "1" ]; do sleep 1; done'
	@adb shell settings put global hidden_api_policy_pre_p_apps 1
	@adb shell settings put global hidden_api_policy_p_apps 1
	@adb shell settings put global window_animation_scale 0
	@adb shell settings put global transition_animation_scale 0
	@adb shell settings put global animator_duration_scale 0
	@adb shell settings put system user_rotation 0  # portrait
	@adb shell settings put secure show_ime_with_hard_keyboard 1
	@adb shell cmd clipboard set text ''

# Shutdown
kill-emulator:
	@adb emu kill || true

# ---------- 1) RECORD LOW-RES SNAPSHOT BASELINES ----------
.PHONY: snapshots-record
snapshots-record: check-no-devices ensure-avd-shot-api35-low boot-and-prep-shot-api35-low
	@echo "==> Setting low-res display (tuned settings: 548x1280 @ 210dpi)"
	adb shell wm size 548x1280
	adb shell wm density 210
	@echo "==> Recording low-res baselines"
	./gradlew :app:screenshotsExecuteScreenshotTests -Precord -PdirectorySuffix=api35-low
	@$(MAKE) kill-emulator

# ---------- 2) VERIFY LOW-RES SNAPSHOTS ----------
.PHONY: snapshots-verify
snapshots-verify: check-no-devices ensure-avd-shot-api35-low boot-and-prep-shot-api35-low
	@echo "==> Setting low-res display (tuned settings: 548x1280 @ 210dpi)"
	adb shell wm size 548x1280
	adb shell wm density 210
	@echo "==> Verifying snapshots"
	./gradlew :app:screenshotsExecuteScreenshotTests -PdirectorySuffix=api35-low
	@$(MAKE) kill-emulator

# ---------- 3) RECORD HIGH-RES PLAY STORE SHOTS ----------
.PHONY: playstore-record
playstore-record: check-no-devices ensure-avd-shot-api35-hi boot-and-prep-shot-api35-hi
	@echo "==> Setting high-res display for Play Store (1080x2340 @ 420dpi)"
	adb shell wm size 1080x2340
	adb shell wm density 420
	@echo "==> Cleaning up any old screenshots"
	-adb shell rm -rf /sdcard/Download/TrackAndGraphScreenshots/ || true
	@echo "==> Running promo captures"
	./gradlew :app:connectedPromoAndroidTest -PusePromoTests=true
	# Wait a moment to ensure all files are written
	@sleep 0.3
	@echo "==> Pulling screenshots from device and copying to Fastlane phoneScreenshots"
	mkdir -p tmp/device_screenshots
	mkdir -p fastlane/metadata/android/en-GB/images/phoneScreenshots
	# Pull screenshots from device
	adb pull /sdcard/Download/TrackAndGraphScreenshots/ tmp/device_screenshots/
	# Rename and copy to fastlane directory with language suffix
	mv tmp/device_screenshots/TrackAndGraphScreenshots/1.png fastlane/metadata/android/en-GB/images/phoneScreenshots/1_en-GB.png
	mv tmp/device_screenshots/TrackAndGraphScreenshots/2.png fastlane/metadata/android/en-GB/images/phoneScreenshots/2_en-GB.png
	mv tmp/device_screenshots/TrackAndGraphScreenshots/3.png fastlane/metadata/android/en-GB/images/phoneScreenshots/3_en-GB.png
	mv tmp/device_screenshots/TrackAndGraphScreenshots/4.png fastlane/metadata/android/en-GB/images/phoneScreenshots/4_en-GB.png
	mv tmp/device_screenshots/TrackAndGraphScreenshots/5.png fastlane/metadata/android/en-GB/images/phoneScreenshots/5_en-GB.png
	mv tmp/device_screenshots/TrackAndGraphScreenshots/6.png fastlane/metadata/android/en-GB/images/phoneScreenshots/6_en-GB.png
	mv tmp/device_screenshots/TrackAndGraphScreenshots/7.png fastlane/metadata/android/en-GB/images/phoneScreenshots/7_en-GB.png
	@$(MAKE) kill-emulator

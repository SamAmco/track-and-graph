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
	# On the emulator, restart adbd as root otherwise the rm can fail
	adb root >/dev/null 2>&1 || true
	adb wait-for-device
	# Now raw deletes are reliable
	adb shell rm -rf /sdcard/Pictures/TrackAndGraphScreenshots/ || true
	@echo "==> Running promo captures"
	./gradlew :app:connectedPromoAndroidTest -PusePromoTests=true -Pandroid.testInstrumentationRunnerArguments.class=com.samco.trackandgraph.promo.PromoScreenshots
	# Wait a moment to ensure all files are written
	@sleep 0.3
	@echo "==> Pulling screenshots from device and copying to Fastlane phoneScreenshots"
	mkdir -p tmp/device_screenshots
	mkdir -p fastlane/metadata/android/en-GB/images/phoneScreenshots
	# Pull screenshots from device
	adb pull /sdcard/Pictures/TrackAndGraphScreenshots/ tmp/device_screenshots/
	# Rename and copy to fastlane directory with language suffix
	mv tmp/device_screenshots/TrackAndGraphScreenshots/1.png fastlane/metadata/android/en-GB/images/phoneScreenshots/1_en-GB.png
	mv tmp/device_screenshots/TrackAndGraphScreenshots/2.png fastlane/metadata/android/en-GB/images/phoneScreenshots/2_en-GB.png
	mv tmp/device_screenshots/TrackAndGraphScreenshots/3.png fastlane/metadata/android/en-GB/images/phoneScreenshots/3_en-GB.png
	mv tmp/device_screenshots/TrackAndGraphScreenshots/4.png fastlane/metadata/android/en-GB/images/phoneScreenshots/4_en-GB.png
	mv tmp/device_screenshots/TrackAndGraphScreenshots/5.png fastlane/metadata/android/en-GB/images/phoneScreenshots/5_en-GB.png
	mv tmp/device_screenshots/TrackAndGraphScreenshots/6.png fastlane/metadata/android/en-GB/images/phoneScreenshots/6_en-GB.png
	mv tmp/device_screenshots/TrackAndGraphScreenshots/7.png fastlane/metadata/android/en-GB/images/phoneScreenshots/7_en-GB.png
	@$(MAKE) kill-emulator

# ---------- 4) RECORD TUTORIAL IMAGES FOR APP ----------
.PHONY: tutorial-record
tutorial-record: check-no-devices ensure-avd-shot-api35-hi boot-and-prep-shot-api35-hi
	@echo "==> Setting high-res display for tutorial capture (1080x2340 @ 420dpi)"
	adb shell wm size 1080x2340
	adb shell wm density 420
	@echo "==> Cleaning up any old tutorial screenshots"
	# On the emulator, restart adbd as root otherwise the rm can fail
	adb root >/dev/null 2>&1 || true
	adb wait-for-device
	# Now raw deletes are reliable
	adb shell rm -rf /sdcard/Pictures/TutorialScreenshots/ || true
	@echo "==> Running tutorial captures"
	./gradlew :app:connectedPromoAndroidTest -PusePromoTests=true -Pandroid.testInstrumentationRunnerArguments.class=com.samco.trackandgraph.tutorial.TutorialScreenshots
	# Wait a moment to ensure all files are written
	@sleep 0.3
	@echo "==> Pulling tutorial screenshots from device and processing with ImageMagick"
	rm -rf tmp/tutorial_screenshots
	mkdir -p tmp/tutorial_screenshots
	mkdir -p app/src/main/res/drawable-mdpi
	mkdir -p app/src/main/res/drawable-hdpi
	mkdir -p app/src/main/res/drawable-xhdpi
	mkdir -p app/src/main/res/drawable-xxhdpi
	mkdir -p app/src/main/res/drawable-xxxhdpi
	# Pull screenshots from device
	adb pull /sdcard/Pictures/TutorialScreenshots tmp/tutorial_screenshots/

	@echo "==> Converting screenshots to optimal sizes for each density bucket"

	# Convert tutorial_1.png to tutorial_image_1.png for all densities
	# Source: 1080x2340 emulator capture - optimized for API 24 memory constraints
	# mdpi (25% of emulator): ~270x585 pixels, 0.6MB memory - safe for low-end devices
	magick tmp/tutorial_screenshots/TutorialScreenshots/tutorial_1.png -resize 25% app/src/main/res/drawable-mdpi/tutorial_image_1.png
	# hdpi (35% of emulator): ~378x819 pixels, 1.2MB memory - balanced size/quality
	magick tmp/tutorial_screenshots/TutorialScreenshots/tutorial_1.png -resize 35% app/src/main/res/drawable-hdpi/tutorial_image_1.png
	# xhdpi (50% of emulator): ~540x1170 pixels, 2.5MB memory - good quality, API 24 safe
	magick tmp/tutorial_screenshots/TutorialScreenshots/tutorial_1.png -resize 50% app/src/main/res/drawable-xhdpi/tutorial_image_1.png
	# xxhdpi (75% of emulator): ~810x1755 pixels, 5.7MB memory - high-end devices
	magick tmp/tutorial_screenshots/TutorialScreenshots/tutorial_1.png -resize 75% app/src/main/res/drawable-xxhdpi/tutorial_image_1.png
	# xxxhdpi (100% of emulator): 1080x2340 pixels, 10.1MB memory - flagship devices with lots of RAM
	cp tmp/tutorial_screenshots/TutorialScreenshots/tutorial_1.png app/src/main/res/drawable-xxxhdpi/tutorial_image_1.png

	# Convert tutorial_2.png to tutorial_image_2.png for all densities
	magick tmp/tutorial_screenshots/TutorialScreenshots/tutorial_2.png -resize 25% app/src/main/res/drawable-mdpi/tutorial_image_2.png
	magick tmp/tutorial_screenshots/TutorialScreenshots/tutorial_2.png -resize 35% app/src/main/res/drawable-hdpi/tutorial_image_2.png
	magick tmp/tutorial_screenshots/TutorialScreenshots/tutorial_2.png -resize 50% app/src/main/res/drawable-xhdpi/tutorial_image_2.png
	magick tmp/tutorial_screenshots/TutorialScreenshots/tutorial_2.png -resize 75% app/src/main/res/drawable-xxhdpi/tutorial_image_2.png
	cp tmp/tutorial_screenshots/TutorialScreenshots/tutorial_2.png app/src/main/res/drawable-xxxhdpi/tutorial_image_2.png

	# Convert tutorial_3.png to tutorial_image_3.png for all densities
	magick tmp/tutorial_screenshots/TutorialScreenshots/tutorial_3.png -resize 25% app/src/main/res/drawable-mdpi/tutorial_image_3.png
	magick tmp/tutorial_screenshots/TutorialScreenshots/tutorial_3.png -resize 35% app/src/main/res/drawable-hdpi/tutorial_image_3.png
	magick tmp/tutorial_screenshots/TutorialScreenshots/tutorial_3.png -resize 50% app/src/main/res/drawable-xhdpi/tutorial_image_3.png
	magick tmp/tutorial_screenshots/TutorialScreenshots/tutorial_3.png -resize 75% app/src/main/res/drawable-xxhdpi/tutorial_image_3.png
	cp tmp/tutorial_screenshots/TutorialScreenshots/tutorial_3.png app/src/main/res/drawable-xxxhdpi/tutorial_image_3.png

	@echo "==> Tutorial images generated successfully"
	@$(MAKE) kill-emulator

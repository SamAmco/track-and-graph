#!/usr/bin/env python3
"""
Create a GitHub release with the current version.
- Checks working tree is clean
- Asks if pre-release or latest
- Creates appropriate tag (rc-vX.Y.Z or vX.Y.Z)
- Pushes branch and tag
- Creates GitHub release with changelog and APK
"""

import re
import subprocess
import sys
from pathlib import Path


def run_command(cmd, capture=True, check=True):
    """Run a command and return the result"""
    if capture:
        return subprocess.run(cmd, capture_output=True, text=True, check=check)
    else:
        return subprocess.run(cmd, check=check)


def check_working_tree_clean():
    """Ensure there are no uncommitted changes"""
    result = run_command(["git", "status", "--porcelain"])
    if result.stdout.strip():
        print("Error: Working tree has uncommitted changes")
        print(result.stdout)
        sys.exit(1)
    print("✓ Working tree is clean")


def get_version_from_gradle():
    """Extract versionName and versionCode from app/build.gradle.kts"""
    gradle_file = Path(__file__).parent.parent / "app" / "build.gradle.kts"

    with open(gradle_file) as f:
        content = f.read()

    version_code_match = re.search(r'versionCode\s*=\s*(\d+)', content)
    version_name_match = re.search(r'versionName\s*=\s*"([^"]+)"', content)

    if not version_code_match or not version_name_match:
        print("Error: Could not find versionCode or versionName in app/build.gradle.kts")
        sys.exit(1)

    return version_name_match.group(1), version_code_match.group(1)


def check_tag_exists(tag):
    """Check if a tag already exists"""
    result = run_command(["git", "tag", "-l", tag])
    return bool(result.stdout.strip())


def get_changelog_content(version_code):
    """Read the changelog file for this version"""
    changelog_file = Path(__file__).parent.parent / "fastlane" / "metadata" / "android" / "en-GB" / "changelogs" / f"{version_code}.txt"

    if not changelog_file.exists():
        print(f"Error: Changelog file not found: {changelog_file}")
        sys.exit(1)

    with open(changelog_file) as f:
        return f.read().strip()


def get_current_branch():
    """Get the current git branch name"""
    result = run_command(["git", "branch", "--show-current"])
    return result.stdout.strip()


def find_apk():
    """Find the release APK file"""
    apk_dir = Path(__file__).parent.parent / "app" / "build" / "outputs" / "apk" / "release"
    apk_files = list(apk_dir.glob("*.apk"))

    if not apk_files:
        print(f"Error: No APK found in {apk_dir}")
        print("Run 'make assemble-release' first")
        sys.exit(1)

    # Return the first APK found
    return apk_files[0]


def main():
    print("==> GitHub Release Creation Script")
    print()

    # Check working tree is clean
    check_working_tree_clean()

    # Get version info
    version_name, version_code = get_version_from_gradle()
    print(f"✓ Version: {version_name} (code: {version_code})")

    # Ask if pre-release or latest
    print()
    print("Release type:")
    print("  1) Pre-release (rc-v{version})")
    print("  2) Latest release (v{version})")
    choice = input("Select [1/2]: ").strip()

    if choice == "1":
        is_prerelease = True
        tag = f"rc-v{version_name}"
    elif choice == "2":
        is_prerelease = False
        tag = f"v{version_name}"
    else:
        print("Error: Invalid choice")
        sys.exit(1)

    print(f"✓ Tag: {tag} (prerelease: {is_prerelease})")

    # Check if tag already exists
    if check_tag_exists(tag):
        print(f"Error: Tag '{tag}' already exists")
        sys.exit(1)
    print(f"✓ Tag '{tag}' does not exist")

    # Get changelog
    changelog = get_changelog_content(version_code)
    print(f"✓ Found changelog ({len(changelog)} chars)")

    # Find APK
    apk_path = find_apk()
    print(f"✓ Found APK: {apk_path.name}")

    # Get current branch
    branch = get_current_branch()
    print(f"✓ Current branch: {branch}")

    # Confirm before proceeding
    print()
    print("Ready to:")
    print(f"  - Create tag: {tag}")
    print(f"  - Push branch: {branch}")
    print(f"  - Push tag: {tag}")
    print(f"  - Create GitHub release with {apk_path.name}")
    confirm = input("Proceed? [y/N]: ").strip().lower()

    if confirm != 'y':
        print("Aborted")
        sys.exit(0)

    # Create tag
    print()
    print(f"Creating tag {tag}...")
    run_command(["git", "tag", tag], capture=False)
    print("✓ Tag created")

    # Push branch
    print(f"Pushing branch {branch}...")
    run_command(["git", "push", "origin", branch], capture=False)
    print("✓ Branch pushed")

    # Push tag
    print(f"Pushing tag {tag}...")
    run_command(["git", "push", "origin", tag], capture=False)
    print("✓ Tag pushed")

    # Create GitHub release
    print("Creating GitHub release...")
    gh_cmd = [
        "gh", "release", "create", tag,
        str(apk_path),
        "--title", tag,
        "--notes", changelog
    ]

    if is_prerelease:
        gh_cmd.append("--prerelease")
    else:
        gh_cmd.append("--latest")

    run_command(gh_cmd, capture=False)
    print("✓ GitHub release created")

    print()
    print(f"✓ Release {tag} created successfully!")


if __name__ == "__main__":
    main()

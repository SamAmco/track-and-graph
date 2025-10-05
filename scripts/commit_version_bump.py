#!/usr/bin/env python3
"""
Commit all working tree changes with a version bump message.
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


def get_version_from_gradle():
    """Extract versionName from app/build.gradle.kts"""
    gradle_file = Path(__file__).parent.parent / "app" / "build.gradle.kts"

    with open(gradle_file) as f:
        content = f.read()

    version_name_match = re.search(r'versionName\s*=\s*"([^"]+)"', content)

    if not version_name_match:
        print("Error: Could not find versionName in app/build.gradle.kts")
        sys.exit(1)

    return version_name_match.group(1)


def check_has_changes():
    """Check if there are any changes to commit"""
    result = run_command(["git", "status", "--porcelain"])
    return bool(result.stdout.strip())


def main():
    print("==> Version Bump Commit Script")
    print()

    # Get version info
    version_name = get_version_from_gradle()
    print(f"✓ Version: {version_name}")

    # Check if there are changes
    if not check_has_changes():
        print("Error: No changes to commit")
        sys.exit(1)
    print("✓ Working tree has changes")

    # Ask if pre-release
    print()
    print("Release type:")
    print("  1) Pre-release (rc-v{version})")
    print("  2) Release (v{version})")
    choice = input("Select [1/2]: ").strip()

    if choice == "1":
        tag = f"rc-v{version_name}"
    elif choice == "2":
        tag = f"v{version_name}"
    else:
        print("Error: Invalid choice")
        sys.exit(1)

    commit_message = f"Bump version and release notes for {tag}"
    print(f"✓ Commit message: {commit_message}")

    # Show files to be committed
    print()
    print("Files to be committed:")
    result = run_command(["git", "status", "--short"])
    print(result.stdout)

    # Stage all changes
    print()
    print("Staging all changes...")
    run_command(["git", "add", "-A"], capture=False)
    print("✓ Changes staged")

    # Commit
    print("Committing...")
    run_command(["git", "commit", "-m", commit_message], capture=False)
    print("✓ Committed")

    print()
    print(f"✓ Version bump committed successfully!")


if __name__ == "__main__":
    main()

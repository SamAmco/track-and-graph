#!/usr/bin/env python3
"""
Create a new changelog file for the current release version.
Extracts version from app/build.gradle.kts, creates a changelog file
in fastlane/metadata/android/en-GB/changelogs/, populates it with
git log since the last release, and opens it in neovim.
"""

import re
import subprocess
import sys
from pathlib import Path


def get_version_code_from_gradle():
    """Extract versionCode from app/build.gradle.kts"""
    gradle_file = Path(__file__).parent.parent / "app" / "build.gradle.kts"

    with open(gradle_file) as f:
        content = f.read()

    match = re.search(r'versionCode\s*=\s*(\d+)', content)
    if not match:
        print("Error: Could not find versionCode in app/build.gradle.kts")
        sys.exit(1)

    return match.group(1)


def find_previous_tag():
    """Find the most recent tag"""
    # Get all tags
    result = subprocess.run(
        ["git", "tag"],
        capture_output=True,
        text=True,
        check=True
    )

    all_tags = result.stdout.strip().split('\n')

    # Filter to only version tags (v* or rc-v*)
    version_tags = [t for t in all_tags if t.startswith('v') or t.startswith('rc-v')]

    if not version_tags:
        return None

    # Sort by stripping prefixes and parsing as version tuples
    def version_key(tag):
        # Strip rc-v or v prefix
        version = tag.replace('rc-v', '').replace('v', '', 1)
        # Split into parts and convert to integers for proper sorting
        parts = version.split('.')
        return tuple(int(p) for p in parts if p.isdigit())

    sorted_tags = sorted(version_tags, key=version_key, reverse=True)

    # Return the most recent tag
    return sorted_tags[0] if sorted_tags else None


def get_git_log_since_tag(tag):
    """Get one-line git log entries since the given tag, or diff if no commits"""
    if not tag:
        print("Error: Can not determine previous tag")
        sys.exit(1)

    result = subprocess.run(
        ["git", "log", "--oneline", f"{tag}..HEAD"],
        capture_output=True,
        text=True,
        check=True
    )

    log_output = result.stdout.strip()

    # If no commits since last tag, show the diff instead
    if not log_output:
        print("No commits since last tag, showing diff instead...")
        result = subprocess.run(
            ["git", "diff", "HEAD"],
            capture_output=True,
            text=True,
            check=True
        )
        return result.stdout.strip()

    return log_output


def create_changelog_file(version_code, git_log):
    """Create the changelog file with git log content"""
    changelog_dir = Path(__file__).parent.parent / "fastlane" / "metadata" / "android" / "en-GB" / "changelogs"
    changelog_dir.mkdir(parents=True, exist_ok=True)

    changelog_file = changelog_dir / f"{version_code}.txt"

    if changelog_file.exists():
        print(f"Warning: {changelog_file} already exists")
        response = input("Overwrite? (y/n): ")
        if response.lower() != 'y':
            print("Aborted")
            sys.exit(0)

    with open(changelog_file, 'w') as f:
        f.write(git_log)
        f.write("\n")

    return changelog_file


def open_in_neovim(file_path):
    """Open the file in neovim"""
    subprocess.run(["nvim", str(file_path)])


def main():
    print("Extracting version code from app/build.gradle.kts...")
    version_code = get_version_code_from_gradle()
    print(f"Current version code: {version_code}")

    print("\nFinding previous release tag...")
    previous_tag = find_previous_tag()
    if previous_tag:
        print(f"Previous tag: {previous_tag}")
    else:
        print("No previous tag found, will show all commits")

    print("\nGenerating git log...")
    git_log = get_git_log_since_tag(previous_tag)

    if not git_log:
        print("No commits found since last release")
        git_log = "- No changes"

    print("\nCreating changelog file...")
    changelog_file = create_changelog_file(version_code, git_log)
    print(f"Created: {changelog_file}")

    print("\nOpening in neovim...")
    open_in_neovim(changelog_file)


if __name__ == "__main__":
    main()

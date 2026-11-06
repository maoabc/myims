#!/usr/bin/env bash
set -e

# Check if gh CLI is installed
if ! command -v gh &> /dev/null; then
    echo "Error: GitHub CLI (gh) is not installed. Please install it first."
    exit 1
fi

# Find release APKs
APKS=$(find app/build/outputs/apk -name "*.apk" | grep -E "release" || true)

if [ -z "$APKS" ]; then
    echo "Error: No release APKs found under app/build/outputs/apk/."
    echo "Please run `./gradlew assembleRelease` first."
    exit 1
fi

echo "Found release APKs:"
echo "$APKS"

# Get tag from argument or input
TAG="${1:-}"
if [ -z "$TAG" ]; then
    TAG=$(git describe --tags --abbrev=0 2>/dev/null || echo "v1.0.0")
    read -p "Enter release tag (default: $TAG): " input_tag
    if [ -n "$input_tag" ]; then
        TAG="$input_tag"
    fi
fi

TITLE="${2:-Release $TAG}"
NOTES="${3:-Automated release upload via script}"

echo "Creating/updating GitHub release $TAG and uploading APKs..."

# Check if release exists
if gh release view "$TAG" &>/dev/null; then
    echo "Release $TAG already exists. Uploading assets..."
    for apk in $APKS; do
        gh release upload "$TAG" "$apk" --clobber
    done
else
    echo "Creating new release $TAG..."
    APK_ARRAY=()
    while IFS= read -r line; do
        [ -n "$line" ] && APK_ARRAY+=("$line")
    done <<< "$APKS"

    gh release create "$TAG" "${APK_ARRAY[@]}" --title "$TITLE" --notes "$NOTES"
fi

echo "Successfully uploaded APKs to GitHub Release $TAG! ~ 喵~"

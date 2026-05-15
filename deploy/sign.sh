#!/bin/bash

set -e

# =========================
# CONFIG
# =========================

KEY_ID="55C141ADEB5FF13D"

GROUP_PATH="./io/odilon"
ARTIFACT_ID="odilon-client"
VERSION="1.15.2"

TARGET_DIR="${GROUP_PATH}/${ARTIFACT_ID}/${VERSION}"

ZIP_NAME="${ARTIFACT_ID}-${VERSION}.zip"

# =========================
# START
# =========================

echo "======================================="
echo " Artifact : $ARTIFACT_ID"
echo " Version  : $VERSION"
echo " Directory: $TARGET_DIR"
echo " GPG Key  : $KEY_ID"
echo "======================================="

cd "$TARGET_DIR"

# limpiar artefactos previos
echo ""
echo "Cleaning old signatures/checksums..."
rm -f *.asc *.md5 *.sha1

# =========================
# SIGN + HASH
# =========================

echo ""
echo "Generating signatures and checksums..."

for f in *; do

    [ -f "$f" ] || continue

    # evitar firmar hashes/firma
    case "$f" in
        *.asc|*.md5|*.sha1)
            continue
            ;;
    esac

    echo "Processing $f"

    # firma ASCII
    gpg --batch --yes \
        --local-user "$KEY_ID" \
        --armor --detach-sign "$f"

    # SHA1
    shasum -a 1 "$f" | awk '{print $1}' > "$f.sha1"

    # MD5
    md5 -q "$f" > "$f.md5"

done

# =========================
# ZIP
# =========================

echo ""
echo "Creating zip..."


cd "$SCRIPT_DIR"

rm -f "$ZIP_NAME"

zip -r "$ZIP_NAME" "io" \
    -x "*.DS_Store"





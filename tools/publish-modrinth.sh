#!/usr/bin/env bash
# Creates the Elytra Compat project on Modrinth as a DRAFT and uploads v1.0.0.
# Requires a Modrinth personal access token with create/write scopes:
#   MODRINTH_TOKEN=mrp_xxx ./tools/publish-modrinth.sh
#
# The project is created as a draft (not public) so you can review it and hit
# "Publish" yourself on modrinth.com.
set -euo pipefail

: "${MODRINTH_TOKEN:?Set MODRINTH_TOKEN to a Modrinth personal access token}"
API="https://api.modrinth.com/v2"
DIR="$(cd "$(dirname "$0")/.." && pwd)"
JAR="$DIR/build/libs/elytracompat-1.0.0.jar"
ICON="$DIR/docs/modrinth-icon.png"
BODY="$(cat "$DIR/docs/modrinth-description.md")"
[ -f "$JAR" ] || { echo "Build the jar first: ./gradlew build"; exit 1; }

SUMMARY="Makes Armored Elytra, Elytra Trims and the Enderite Mod work together: armor enderite elytras, real trim rendering, decoration-safe grindstone splits, and resource-pack-aware chestplate-over-elytra icons for vanilla and enderite items."

project_data() {
  python3 - "$SUMMARY" <<PY
import json, sys, pathlib
body = pathlib.Path("$DIR/docs/modrinth-description.md").read_text()
print(json.dumps({
  "slug": "elytracompat",
  "title": "Elytra Compat (Armored Elytra + Elytra Trims + Enderite)",
  "description": sys.argv[1],
  "body": body,
  "categories": ["utility", "equipment"],
  "client_side": "required",
  "server_side": "required",
  "project_type": "mod",
  "license_id": "MIT",
  "is_draft": True,
  "initial_versions": [],
  "source_url": "https://github.com/ENC4YP7ED/elytracompat",
  "issues_url": "https://github.com/ENC4YP7ED/elytracompat/issues"
}))
PY
}

echo "Creating draft project..."
RESP=$(curl -s -X POST "$API/project" \
  -H "Authorization: $MODRINTH_TOKEN" \
  -F "data=$(project_data)")
PID=$(echo "$RESP" | python3 -c "import json,sys; print(json.load(sys.stdin).get('id',''))" 2>/dev/null || true)
if [ -z "$PID" ]; then echo "Project create failed:"; echo "$RESP"; exit 1; fi
echo "  project id: $PID"

echo "Uploading icon..."
curl -s -X PATCH "$API/project/$PID/icon?ext=png" \
  -H "Authorization: $MODRINTH_TOKEN" \
  -H "Content-Type: image/png" --data-binary "@$ICON" >/dev/null

echo "Uploading version 1.0.0..."
VDATA=$(python3 <<PY
import json
print(json.dumps({
  "name": "Elytra Compat 1.0.0",
  "version_number": "1.0.0",
  "changelog": "Initial release for Minecraft 26.2 (Fabric).",
  "dependencies": [
    {"project_id":"AuFCCYMx","dependency_type":"required"},
    {"project_id":"XpzGz7KD","dependency_type":"required"},
    {"project_id":"6lvRWqbA","dependency_type":"required"},
    {"project_id":"P7dR8mSH","dependency_type":"required"},
    {"project_id":"Ha28R6CL","dependency_type":"required"}
  ],
  "game_versions": ["26.2"],
  "version_type": "release",
  "loaders": ["fabric"],
  "featured": True,
  "project_id": "$PID",
  "file_parts": ["file"],
  "primary_file": "file"
}))
PY
)
curl -s -X POST "$API/version" \
  -H "Authorization: $MODRINTH_TOKEN" \
  -F "data=$VDATA" \
  -F "file=@$JAR;type=application/java-archive" >/dev/null

echo "Done. Review the draft at: https://modrinth.com/mod/$PID/settings"
echo "(It stays private until you press Publish / submit for review.)"

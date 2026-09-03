#!/usr/bin/env bash
set -euo pipefail

effort=$1
prompt=$2
schema=$3
output=$4
verify=${5:-true}
auth_file="$HOME/.codex/auth.json"

mkdir -p "$(dirname "$auth_file")"
printf '%s' "${CODEX_AUTH_JSON:?CODEX_AUTH_JSON is required}" > "$auth_file"
chmod 600 "$auth_file"
unset CODEX_AUTH_JSON
trap 'rm -f "$auth_file"' EXIT

codex exec \
  --model gpt-5.6-sol \
  -c model_reasoning_effort="$effort" \
  --sandbox read-only \
  --output-schema "$schema" \
  --output-last-message "$output" \
  - < "$prompt"

if [[ "$verify" == true ]] && grep -Eqi \
  'review (could not be completed|unavailable)|sandbox (initialization )?failed|bwrap:.*failed' \
  "$output"; then
  echo "Codex could not inspect the repository" >&2
  cat "$output" >&2
  exit 1
fi

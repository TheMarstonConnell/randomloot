#!/usr/bin/env bash
# Replaces the CODEX_AUTH_JSON secret with a fresh codex ChatGPT login.
#
# The login is made in a throwaway CODEX_HOME and deleted after upload, so no
# machine keeps a copy: codex-auth-refresh.yml must be the only thing that ever
# rotates this login's single-use refresh token. Never upload ~/.codex/auth.json.
set -euo pipefail

repo=TheMarstonConnell/randomloot

for cmd in codex gh jq; do
  command -v "$cmd" > /dev/null || { echo "$cmd not on PATH" >&2; exit 1; }
done
gh auth status --hostname github.com > /dev/null ||
  { echo "run 'gh auth login' first" >&2; exit 1; }

CODEX_HOME=$(mktemp -d)
export CODEX_HOME
trap 'rm -rf "${CODEX_HOME:?}"' EXIT
printf 'cli_auth_credentials_store = "file"\n' > "$CODEX_HOME/config.toml"

# Opens a browser; pass --device-auth to this script when there isn't one.
codex login "$@"

jq -e '.tokens.refresh_token' "$CODEX_HOME/auth.json" > /dev/null 2>&1 ||
  { echo "codex login did not leave a ChatGPT login in auth.json" >&2; exit 1; }
gh secret set CODEX_AUTH_JSON --repo "$repo" < "$CODEX_HOME/auth.json"
echo "CODEX_AUTH_JSON replaced; re-run the failed Codex review jobs."

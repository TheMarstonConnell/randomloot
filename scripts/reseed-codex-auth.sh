#!/usr/bin/env bash
# Replaces the CODEX_AUTH_JSON secret with a fresh codex ChatGPT login.
#
# The login is made in a throwaway CODEX_HOME and deleted after upload, so no
# machine keeps a copy: codex-auth-refresh.yml must be the only thing that ever
# rotates this login's single-use refresh token. Never upload ~/.codex/auth.json.
set -euo pipefail

repo=TheMarstonConnell/randomloot

for cmd in codex gh; do
  command -v "$cmd" > /dev/null || { echo "$cmd not on PATH" >&2; exit 1; }
done
gh auth status --hostname github.com > /dev/null ||
  { echo "run 'gh auth login' first" >&2; exit 1; }

CODEX_HOME=$(mktemp -d)
export CODEX_HOME
trap 'rm -rf "${CODEX_HOME:?}"' EXIT
printf 'cli_auth_credentials_store = "file"\n' > "$CODEX_HOME/config.toml"

# Prints a URL and a one-time code to enter in any browser, so this also
# works over SSH or on a machine without one.
codex login --device-auth

gh secret set CODEX_AUTH_JSON --repo "$repo" < "$CODEX_HOME/auth.json"
echo "CODEX_AUTH_JSON replaced; re-run the failed Codex review jobs."

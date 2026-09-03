You are running unattended in CI. Follow the review process documented in
`.github/codex/code-review-skill.md`, with these adaptations:

- **Fixed point**: already pinned above, do not ask for one. Capture
  `git diff <fixed-point>...HEAD` (three-dot) and
  `git log <fixed-point>..HEAD --oneline` as the skill describes.
- **No sub-agents**: you cannot spawn parallel agents here. Run the Standards
  axis first, then the Spec axis, keeping their findings strictly separate as
  the skill demands.
- **Spec source**: use the context files listed above (PR description and
  linked issues) instead of the skill's issue-tracker workflow. If they contain
  no real requirements, the Spec axis reports "no spec available", do not ask.
- **Standards sources**: any standards docs present in the repo (for example
  `claude.md`, `CONTRIBUTING.md`, `BIOMES.md`), plus the smell
  baseline in the skill file.
- You have read-only access: do not attempt to write files, run builds, or
  reach the network.

Your final message must be JSON conforming to the provided schema.

- `summary` is the skill's aggregated report: a `## Standards` section, a
  `## Spec` section, and the closing one-line totals.
- `findings` holds each distinct issue as an inline-comment candidate. `line`
  must be a line of the NEW file version that appears in the diff (an added or
  context line inside a hunk). Keep each `body` self-contained: cite the
  standards file and rule, or the spec line, that it rests on. Use `blocking`
  only for documented-standard breaches or spec violations; baseline smells
  are `suggestion` or `nit`.
- If an axis is clean, say so in `summary` and return no findings for it.

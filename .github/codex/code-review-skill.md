<!-- Vendored from https://github.com/mattpocock/skills (code-review skill), for use by .github/workflows/codex-review.yml. Frontmatter removed and the prose lightly rewritten; the process itself is unchanged. -->


Two-axis review of the diff between `HEAD` and a fixed point the user supplies:

- **Standards.** Does the code follow this repo's documented coding standards?
- **Spec.** Does the code do what the originating issue or spec asked?

Both axes run as parallel sub-agents so they don't pollute each other's context; this skill then aggregates their findings.

## Process

### 1. Pin the fixed point

Whatever the user said is the fixed point: a commit SHA, branch name, tag, `main`, `HEAD~5`. If they didn't give one, ask.

Capture the diff command once: `git diff <fixed-point>...HEAD`. The three dots matter, they diff against the merge-base. Also note the list of commits via `git log <fixed-point>..HEAD --oneline`.

Before going further, confirm the fixed point resolves with `git rev-parse <fixed-point>` and the diff is non-empty. A bad ref or empty diff should fail here, not inside two parallel sub-agents.

### 2. Identify the spec source

Look for the originating spec, in this order:

1. Issue references in the commit messages, such as `#123`, `Closes #45`, or GitLab's `!67`. Fetch them via the workflow in `docs/agents/issue-tracker.md`.
2. A path the user passed as an argument.
3. A spec file under `docs/`, `specs/`, or `.scratch/` matching the branch name or feature.
4. If nothing is found, ask the user where the spec is. If they say there isn't one, the **Spec** sub-agent will skip and report "no spec available".

### 3. Identify the standards sources

Anything in the repo that documents how code should be written, such as `CODING_STANDARDS.md` or `CONTRIBUTING.md`.

On top of whatever the repo documents, the Standards axis always carries the smell baseline below, a fixed set of Fowler code smells from _Refactoring_ ch.3 that applies even when a repo documents nothing. Two rules bind it:

- **The repo overrides.** A documented repo standard always wins; where it endorses something the baseline would flag, suppress the smell.
- **Always a judgement call.** Each smell is a labelled heuristic, "possible Feature Envy", never a hard violation. And like any standard here, skip anything tooling already enforces.

Each smell reads as what it is, then the fix; match each against the diff:

- **Mysterious Name.** A function, variable, or type whose name doesn't reveal what it does or holds. Fix: rename it; if no honest name comes, the design's murky.
- **Duplicated Code.** The same logic shape appears in more than one hunk or file in the change. Fix: extract the shared shape, call it from both.
- **Feature Envy.** A method that reaches into another object's data more than its own. Fix: move the method onto the data it envies.
- **Data Clumps.** The same few fields or params keep travelling together, a type wanting to be born. Fix: bundle them into one type, pass that.
- **Primitive Obsession.** A primitive or string standing in for a domain concept that deserves its own type. Fix: give the concept its own small type.
- **Repeated Switches.** The same `switch`/`if`-cascade on the same type recurs across the change. Fix: replace with polymorphism, or one map both sites share.
- **Shotgun Surgery.** One logical change forces scattered edits across many files in the diff. Fix: gather what changes together into one module.
- **Divergent Change.** One file or module is edited for several unrelated reasons. Fix: split so each module changes for one reason.
- **Speculative Generality.** Abstraction, parameters, or hooks added for needs the spec doesn't have. Fix: delete it; inline back until a real need shows.
- **Message Chains.** Long `a.b().c().d()` navigation the caller shouldn't depend on. Fix: hide the walk behind one method on the first object.
- **Middle Man.** A class or function that mostly just delegates onward. Fix: cut it, call the real target direct.
- **Refused Bequest.** A subclass or implementer that ignores or overrides most of what it inherits. Fix: drop the inheritance, use composition.

### 4. Spawn both sub-agents in parallel

**Standards sub-agent prompt.** Include:

- The full diff command and commit list.
- The list of standards-source files you found in step 3, plus the smell baseline from step 3 pasted in full. The sub-agent has no other access to it.
- The brief: "Report, per file or hunk where relevant: (a) every place the diff violates a documented standard, citing the standard by file and rule; and (b) any baseline smell you spot: name it and quote the hunk. Distinguish hard violations from judgement calls. Documented-standard breaches can be hard, but baseline smells are always judgement calls, and a documented repo standard overrides the baseline. Skip anything tooling enforces. Under 400 words."

**Spec sub-agent prompt.** Include:

- The diff command and commit list.
- The path or fetched contents of the spec.
- The brief: "Report: (a) requirements the spec asked for that are missing or partial; (b) behaviour in the diff that wasn't asked for (scope creep); (c) requirements that look implemented but where the implementation looks wrong. Quote the spec line for each finding. Under 400 words."

If the spec is missing, skip the Spec sub-agent and note this in the final report.

### 5. Aggregate

Present the two reports under `## Standards` and `## Spec` headings, verbatim or lightly cleaned. Do not merge or rerank findings. The two axes are deliberately separate, see _Why two axes_.

End with a one-line summary: total findings per axis, and the worst issue within each axis, if any. Don't pick a single winner across axes. That's the reranking the separation exists to prevent.

## Why two axes

A change can pass one axis and fail the other:

- Code that follows every standard but implements the wrong thing passes Standards and fails Spec.
- Code that does exactly what the issue asked but breaks the project's conventions passes Spec and fails Standards.

Reporting them separately stops one axis from masking the other.

You are running unattended in CI, in a read-only checkout of a pull request's
current head. A previous run of this workflow posted inline review comments on
the PR; the file named above lists the threads that are still open. Each entry
has:

- `id`: the thread id, to copy into your output exactly.
- `path` and `line`: where the comment was anchored when posted. The code may
  have moved since.
- `outdated`: true if later pushes changed the anchored line. A hint only,
  never a verdict: a complaint can be fixed without touching that line, and
  the line can change without fixing anything.
- `body`: the complaint itself, self-contained.

For each thread answer one question: does the complaint still hold in the tree
as it is now? Read the current code at and around the cited location; if the
file moved or the line shifted, search for the code the complaint describes.
Judge only whether the complaint remains true, not whether anyone addressed it
deliberately.

- `solved: true` only when you are confident the complaint no longer applies.
- When unsure, or when the code is unchanged, `solved: false`. Leaving a
  fixed thread open is a minor annoyance; resolving an unfixed one buries it.

Your final message must be JSON conforming to the provided schema, one entry
per thread.

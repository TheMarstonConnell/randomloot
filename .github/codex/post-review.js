const fs = require('fs');

const REVIEWERS = {
  codex: {
    marker: '<!-- codex-two-axis-review -->',
    axes: ['standards', 'spec'],
  },
  minimalism: {
    marker: '<!-- codex-minimalism-review -->',
    axes: ['minimalism'],
  },
};
const KIND = process.env.REVIEW_KIND || 'codex';
const { marker: MARKER, axes: AXES } = REVIEWERS[KIND];
const TITLE = `${KIND} review`;

module.exports = async ({ github, context, core }) => {
  const pr = context.payload.pull_request;
  const repo = context.repo;
  const headSha = pr.head.sha;

  const raw = fs.readFileSync(process.env.REVIEW_FILE, 'utf8');
  const review = JSON.parse(raw);

  const upsertSummary = async (body) => {
    const comments = await github.paginate(github.rest.issues.listComments, {
      ...repo, issue_number: pr.number, per_page: 100,
    });
    const existing = comments.find((c) =>
      c.user?.type === 'Bot' &&
      c.user.login === 'github-actions[bot]' &&
      c.body?.includes(MARKER)
    );
    if (existing) {
      await github.rest.issues.updateComment({ ...repo, comment_id: existing.id, body });
    } else {
      await github.rest.issues.createComment({ ...repo, issue_number: pr.number, body });
    }
  };

  // An anchor outside the PR diff makes createReview reject the whole batch,
  // so build the set of commentable NEW-side lines per file first.
  const files = await github.paginate(github.rest.pulls.listFiles, {
    ...repo, pull_number: pr.number, per_page: 100,
  });
  const anchorable = new Map();
  for (const f of files) {
    const lines = new Set();
    let line = 0;
    for (const l of (f.patch || '').split('\n')) {
      const hunk = /^@@ -\d+(?:,\d+)? \+(\d+)(?:,\d+)? @@/.exec(l);
      if (hunk) { line = Number(hunk[1]); continue; }
      if (l.startsWith('-')) continue;
      lines.add(line);
      line++;
    }
    anchorable.set(f.filename, lines);
  }

  // A finding that was fixed, resolved, and then brought back by a later push
  // deserves a fresh comment rather than silence.
  const threads = await require('./threads.js').fetchThreads({ github, context });
  const seen = new Set();
  for (const t of threads) {
    if (t.isResolved) continue;
    for (const c of t.comments.nodes) seen.add(`${c.path} ${c.body}`);
  }

  const inline = [];
  const unanchored = [];
  for (const f of review.findings) {
    const body = `**[${f.axis} / ${f.severity}]** ${f.body}`;
    if (seen.has(`${f.path} ${body}`)) continue;
    if (anchorable.get(f.path)?.has(f.line)) {
      inline.push({ path: f.path, line: f.line, side: 'RIGHT', body });
    } else {
      unanchored.push(f);
    }
  }

  if (inline.length > 0) {
    await github.rest.pulls.createReview({
      ...repo, pull_number: pr.number, commit_id: headSha,
      event: 'COMMENT', comments: inline,
      body: `Inline findings for ${headSha.slice(0, 10)}; the summary lives in the pinned comment.`,
    });
  }

  const counts = Object.fromEntries(AXES.map((axis) => [axis, 0]));
  for (const f of review.findings) counts[f.axis] = (counts[f.axis] || 0) + 1;

  let body = `${MARKER}\n## ${TITLE}\n\n`;
  body += `Reviewed ${headSha}: ${review.findings.length} finding(s):`;
  body += ` ${AXES.map((axis) => `${counts[axis]} ${axis}`).join(', ')}.\n\n`;
  body += review.summary.trim() + '\n';
  if (unanchored.length > 0) {
    body += '\n### Findings outside the diff\n\n';
    for (const f of unanchored) {
      body += `- \`${f.path}:${f.line}\` **[${f.axis} / ${f.severity}]** ${f.body}\n`;
    }
  }
  await upsertSummary(body);
};

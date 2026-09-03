const fs = require('fs');
const { fetchThread, candidates } = require('./threads.js');

const RESOLVE = `
  mutation ($threadId: ID!) {
    resolveReviewThread(input: { threadId: $threadId }) { clientMutationId }
  }`;

const REPLY = `
  mutation ($threadId: ID!, $body: String!) {
    addPullRequestReviewThreadReply(
      input: { pullRequestReviewThreadId: $threadId, body: $body }
    ) { clientMutationId }
  }`;

module.exports = async ({ github, context, core }) => {
  const dir = process.env.CTX_DIR;

  let out = null;
  try {
    out = JSON.parse(fs.readFileSync(`${dir}/resolutions.json`, 'utf8'));
  } catch {}
  if (!out || !Array.isArray(out.threads)) {
    core.warning('resolver output was not valid schema JSON; resolving nothing');
    return;
  }

  const known = new Set(
    JSON.parse(fs.readFileSync(`${dir}/threads.json`, 'utf8')).map((t) => t.id)
  );
  const headSha = context.payload.pull_request.head.sha;

  let resolved = 0;
  for (const t of out.threads) {
    if (!t.solved) continue;
    if (!known.has(t.id)) {
      core.warning(`skipping unknown thread id ${t.id}`);
      continue;
    }
    try {
      // Judging took minutes, and a person may have claimed the thread since.
      if (candidates([await fetchThread({ github, id: t.id })]).length === 0) {
        core.info(`leaving ${t.id}: no longer ours to close`);
        continue;
      }
      // A reply leaves a resolved thread resolved, and a thread GitHub refuses
      // to close must not be left claiming it was addressed.
      await github.graphql(RESOLVE, { threadId: t.id });
      await github.graphql(REPLY, {
        threadId: t.id,
        body: `Addressed as of \`${headSha}\`.`,
      });
    } catch (err) {
      core.warning(`could not close ${t.id}: ${err.message}`);
      continue;
    }
    core.info(`resolved ${t.id}: ${t.reason || 'no reason given'}`);
    resolved++;
  }
  core.info(`resolved ${resolved} thread(s)`);
};

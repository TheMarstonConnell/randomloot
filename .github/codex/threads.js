const COMMENT_PAGE = `
  pageInfo { hasNextPage endCursor }
  nodes { body path line originalLine author { login __typename } }`;

const THREAD_FIELDS = `
  id
  isResolved
  isOutdated
  comments(first: 100) { ${COMMENT_PAGE} }`;

const QUERY = `
  query ($owner: String!, $name: String!, $number: Int!, $cursor: String) {
    repository(owner: $owner, name: $name) {
      pullRequest(number: $number) {
        reviewThreads(first: 100, after: $cursor) {
          pageInfo { hasNextPage endCursor }
          nodes { ${THREAD_FIELDS} }
        }
      }
    }
  }`;

const THREAD = `
  query ($id: ID!) {
    node(id: $id) { ... on PullRequestReviewThread { ${THREAD_FIELDS} } }
  }`;

const MORE_COMMENTS = `
  query ($id: ID!, $cursor: String) {
    node(id: $id) {
      ... on PullRequestReviewThread {
        comments(first: 100, after: $cursor) { ${COMMENT_PAGE} }
      }
    }
  }`;

const FINDING_PREFIX =
  /^\*\*\[(?:(?:standards|spec) \/ (?:blocking|suggestion|nit)|minimalism \/ suggestion)\]\*\*/;

async function drainComments({ github, thread }) {
  let page = thread.comments.pageInfo;
  while (page.hasNextPage) {
    const res = await github.graphql(MORE_COMMENTS, {
      id: thread.id,
      cursor: page.endCursor,
    });
    const conn = res.node.comments;
    thread.comments.nodes.push(...conn.nodes);
    page = conn.pageInfo;
  }
  return thread;
}

async function fetchThreads({ github, context }) {
  const pr = context.payload.pull_request;
  const nodes = [];
  let cursor = null;
  let hasNext = true;
  while (hasNext) {
    const res = await github.graphql(QUERY, {
      owner: context.repo.owner,
      name: context.repo.repo,
      number: pr.number,
      cursor,
    });
    const conn = res.repository.pullRequest.reviewThreads;
    nodes.push(...conn.nodes);
    hasNext = conn.pageInfo.hasNextPage;
    cursor = conn.pageInfo.endCursor;
  }
  for (const t of nodes) await drainComments({ github, thread: t });
  return nodes;
}

async function fetchThread({ github, id }) {
  const res = await github.graphql(THREAD, { id });
  return drainComments({ github, thread: res.node });
}

function candidates(threads) {
  const found = [];
  for (const t of threads) {
    if (t.isResolved) continue;
    const comments = t.comments.nodes;
    if (comments.length === 0) continue;
    const first = comments[0];
    if (first.author?.login !== 'github-actions') continue;
    if (!FINDING_PREFIX.test(first.body)) continue;
    // A non-bot reply makes the thread theirs; an unnamed author was a person.
    if (comments.some((c) => c.author?.__typename !== 'Bot')) continue;
    found.push({
      id: t.id,
      path: first.path,
      // line is null once the anchor line is gone; originalLine remembers it.
      line: first.line ?? first.originalLine,
      outdated: t.isOutdated,
      body: first.body,
    });
  }
  return found;
}

module.exports = { fetchThreads, fetchThread, candidates };

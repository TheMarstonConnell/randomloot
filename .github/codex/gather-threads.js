const fs = require('fs');
const { fetchThreads, candidates } = require('./threads.js');

module.exports = async ({ github, context, core }) => {
  const found = candidates(await fetchThreads({ github, context }));
  fs.mkdirSync(process.env.CTX_DIR, { recursive: true });
  fs.writeFileSync(
    `${process.env.CTX_DIR}/threads.json`,
    JSON.stringify(found, null, 2)
  );
  core.setOutput('count', String(found.length));
  core.info(`${found.length} open codex thread(s) to check`);
};

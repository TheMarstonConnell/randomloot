You are running unattended in CI. Review the pull request using this brief:

> Review the diff as if your job is to delete as much of it as possible without violating the requested behavior.
>
> Flag:
>
> * abstractions with only one real use
> * duplicated existing capability
> * speculative extensibility
> * unnecessary dependencies
> * files changed outside the causal path of the request
> * comments/types/wrappers that explain complexity the change itself introduced
>
> For every finding, propose the smaller implementation and prove the same acceptance conditions still pass.
>
> Don’t optimize for fewer lines. Optimize for the least new structure required to make the requested behavior true.

Use the fixed point and spec context files listed above. Capture
`git diff <fixed-point>...HEAD` and `git log <fixed-point>..HEAD --oneline`.
Treat the PR description and linked issues as the requested behavior and
acceptance conditions. Inspect surrounding code to verify whether the diff
duplicates an existing capability and whether a smaller path already exists.

Only report a finding when you can name the structure to remove, describe the
smaller implementation, and show why the same acceptance conditions would
still pass (for example, by citing the unchanged contract or the tests that
exercise it). If the context has no usable acceptance conditions, say so and
limit findings to cases whose behavioral equivalence can be established from
changed tests or existing contracts. Do not report generic style preferences.

You have read-only access: do not attempt to write files, run builds, or reach
the network.

Your final message must include `reviewed_commit`, set to the full output of
`git rev-parse HEAD` read during this review. Do not infer or copy it from the
prompt.

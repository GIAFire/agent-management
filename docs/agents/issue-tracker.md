# Issue Tracker：GitHub

本仓库的 Issue 与 PRD 均存放在 GitHub Issues 中。所有操作使用 `gh` CLI。

## 约定

- **创建 Issue**：`gh issue create --title "..." --body "..."`。正文包含多行内容时使用 heredoc。
- **读取 Issue**：`gh issue view <编号> --comments`，并使用 `jq` 筛选评论及获取标签。
- **列出 Issue**：`gh issue list --state open --json number,title,body,labels,comments --jq '[.[] | {number, title, body, labels: [.labels[].name], comments: [.comments[].body]}]'`，按需增加 `--label` 和 `--state` 筛选条件。
- **评论 Issue**：`gh issue comment <编号> --body "..."`
- **添加或移除标签**：`gh issue edit <编号> --add-label "..."` / `--remove-label "..."`
- **关闭 Issue**：`gh issue close <编号> --comment "..."`

仓库信息从 `git remote -v` 推断；在克隆的仓库目录中执行时，`gh` 会自动完成该操作。

## 是否将 Pull Request 作为 triage 请求入口

**否。**（如果本仓库将外部 PR 视为功能请求，可将此设置改为“是”；`triage` skill 会读取该设置。）

设置为“是”时，PR 将使用与 Issue 相同的标签和状态，并使用对应的 `gh pr` 命令：

- **读取 PR**：使用 `gh pr view <编号> --comments` 查看详情和评论，使用 `gh pr diff <编号>` 查看差异。
- **列出需要 triage 的外部 PR**：执行 `gh pr list --state open --json number,title,body,labels,author,authorAssociation,comments`，仅保留 `authorAssociation` 为 `CONTRIBUTOR`、`FIRST_TIME_CONTRIBUTOR` 或 `NONE` 的 PR，排除 `OWNER`、`MEMBER` 和 `COLLABORATOR`。
- **评论、添加标签或关闭**：使用 `gh pr comment`、`gh pr edit --add-label` / `--remove-label` 和 `gh pr close`。

GitHub 的 Issue 与 PR 共用编号空间，因此单独出现的 `#42` 可能表示两者中的任意一种。先执行 `gh pr view 42`，失败后再执行 `gh issue view 42`。

## 当 skill 要求“发布到 Issue Tracker”时

创建一个 GitHub Issue。

## 当 skill 要求“获取相关 Ticket”时

执行 `gh issue view <编号> --comments`。

## Wayfinding 操作

供 `wayfinder` skill 使用。**Map** 是一个主 Issue，**Child Ticket** 是其子 Issue。

- **Map**：一个带有 `wayfinder:map` 标签的 Issue，正文包含 Notes、Decisions-so-far 和 Fog。使用 `gh issue create --label wayfinder:map` 创建。
- **Child Ticket**：作为 GitHub Sub-issue 关联至 Map 的 Issue，通过 `gh api` 调用 Sub-issues API。若仓库未启用 Sub-issues，则将 Child Ticket 加入 Map 正文的任务列表，并在 Child Ticket 正文顶部写入 `Part of #<Map 编号>`。标签使用 `wayfinder:<类型>`，其中类型为 `research`、`prototype`、`grilling` 或 `task`。Ticket 被认领后，将其分配给负责推进的开发者。
- **阻塞关系**：优先使用 GitHub 原生 Issue Dependencies，作为规范且可在界面中查看的表示。使用 `gh api --method POST repos/<所有者>/<仓库>/issues/<Child 编号>/dependencies/blocked_by -F issue_id=<Blocker 数据库 ID>` 添加依赖边。其中 `<Blocker 数据库 ID>` 是数字类型的数据库 `id`，通过 `gh api repos/<所有者>/<仓库>/issues/<编号> --jq .id` 获取，不是 `#编号` 或 `node_id`。GitHub 通过 `issue_dependencies_summary.blocked_by` 报告仍处于打开状态的阻塞项。若依赖功能不可用，则在 Child Ticket 正文顶部添加 `Blocked by: #<编号>, #<编号>`。当所有 Blocker 均已关闭时，Ticket 即解除阻塞。
- **Frontier 查询**：列出 Map 下仍处于打开状态的 Child Ticket，排除仍有打开 Blocker 或已有 Assignee 的 Ticket，并按 Map 中的排列顺序选择第一个。
- **认领**：执行 `gh issue edit <编号> --add-assignee @me`；这是会话中的第一次写操作。
- **完成**：执行 `gh issue comment <编号> --body "<答案>"`，随后执行 `gh issue close <编号>`，最后在 Map 的 Decisions-so-far 中追加上下文指针（Gist 与链接）。

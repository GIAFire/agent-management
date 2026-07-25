# 问题跟踪器：本地 Markdown

本仓库的问题和规格文档（规格文档也可称为 PRD）以 Markdown 文件的形式存放在 `.scratch/` 中。

## 约定

- 每项功能使用一个目录：`.scratch/<feature-slug>/`
- 规格文档位于 `.scratch/<feature-slug>/spec.md`
- 每个实现工单使用一个独立文件：`.scratch/<feature-slug>/issues/<NN>-<slug>.md`
- 工单编号从 `01` 开始；不得将所有工单合并到一个文件中
- 分类状态记录在每个工单文件顶部附近的 `Status:` 行中；可用角色字符串见 `triage-labels.md`
- 评论和讨论历史追加到文件底部的 `## Comments` 标题下

## 当技能要求“发布到问题跟踪器”时

在 `.scratch/<feature-slug>/` 下创建新文件；如果目录不存在，则同时创建目录。

## 当技能要求“获取相关工单”时

读取所引用路径中的文件。用户通常会直接提供文件路径或工单编号。

## 寻路操作

供 `/wayfinder` 使用。一个**地图**由一个文件表示，每个**子工单**使用一个独立文件。

- **地图**：`.scratch/<effort>/map.md`，正文用于保存“备注”“已有决策”和“未知区域”。
- **子工单**：`.scratch/<effort>/issues/NN-<slug>.md`，编号从 `01` 开始，正文记录需要解决的问题。`Type:` 行记录工单类型，可用值为 `research`、`prototype`、`grilling` 或 `task`；`Status:` 行记录 `claimed` 或 `resolved`。
- **阻塞关系**：在文件顶部附近使用 `Blocked by: NN, NN`。列出的所有工单状态均为 `resolved` 后，当前工单才算解除阻塞。
- **前沿查询**：扫描 `.scratch/<effort>/issues/`，查找尚未解决、未被阻塞且无人认领的工单；按照编号顺序选择第一个。
- **认领**：在开始任何工作前，将 `Status:` 设置为 `claimed` 并保存。
- **解决**：将答案追加到 `## Answer` 标题下，把 `Status:` 设置为 `resolved`，然后在 `map.md` 的“已有决策”部分附加上下文指针（gist 及其链接）。

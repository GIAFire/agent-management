# 领域文档

本文件说明工程技能在探索代码库时应如何读取和使用本仓库的领域文档。

## 探索代码前需要读取

- 根目录下的 **`CONTEXT.md`**；或者
- 如果根目录存在 **`CONTEXT-MAP.md`**，它会指向每个上下文的 `CONTEXT.md`。读取与当前工作主题相关的上下文文件。
- **`docs/adr/`**：读取与即将处理区域相关的 ADR。在多上下文仓库中，还应检查 `src/<context>/docs/adr/` 中与特定上下文相关的决策。

如果这些文件不存在，请**直接继续，不要提示**。不要报告文件缺失，也不要预先建议创建它们。`/domain-modeling` 技能会在术语或决策真正明确时按需创建这些文件；该技能可通过 `/grill-with-docs` 和 `/improve-codebase-architecture` 使用。

## 文件结构

单上下文仓库（适用于大多数仓库）：

```text
/
├── CONTEXT.md
├── docs/adr/
│   ├── 0001-event-sourced-orders.md
│   └── 0002-postgres-for-write-model.md
└── src/
```

多上下文仓库（根目录存在 `CONTEXT-MAP.md`）：

```text
/
├── CONTEXT-MAP.md
├── docs/adr/                          ← 系统级决策
└── src/
    ├── ordering/
    │   ├── CONTEXT.md
    │   └── docs/adr/                  ← 上下文专属决策
    └── billing/
        ├── CONTEXT.md
        └── docs/adr/
```

## 使用术语表中的词汇

当输出中需要命名领域概念时，例如问题标题、重构建议、假设或测试名称，请使用 `CONTEXT.md` 中定义的术语。不要擅自改用术语表明确避免的同义词。

如果术语表中没有所需概念，这通常意味着两种情况之一：你正在创造项目并未使用的语言，需要重新考虑；或者领域文档确实存在缺口，应记录下来并交由 `/domain-modeling` 处理。

## 标明与 ADR 的冲突

如果输出与现有 ADR 冲突，请明确指出，不要在未说明的情况下覆盖已有决策：

> _与 ADR-0007（事件溯源订单）冲突——但值得重新讨论，因为……_

# AGENTS.md

> OpenCode 智能体知识库。项目技术细节见 [PROJECT.md](./.reference/PROJECT.md)。

## 项目速览

Spring Boot 4.0.6 + GraalVM native-image 脚手架，OpenTelemetry 可观测性栈。Java 25 / Gradle 9.5.0 / base package `com.kk.demo`。详见 PROJECT.md。

## AI 工作流

本项目使用 **OpenSpec + gstack** 组合驱动 AI 辅助开发：

```
需求探索 → /opsx-propose → /autoplan → /opsx-apply → /review
   ↑                                                    ↓
   └──────────── /opsx-explore ←────────────────────────┘
```

### 阶段说明

| 阶段 | 命令 | 作用 |
|------|------|------|
| 探索 | `/opsx-explore` | 澄清需求、分析问题、思考方案 |
| 提案 | `/opsx-propose <change>` | 生成 proposal.md + design.md + tasks.md |
| 审阅 | `/autoplan` | 自动执行 CEO/设计/工程/DX 四维度审阅 |
| 实现 | `/opsx-apply` | 按 tasks.md 逐条执行实现 |
| 检视 | `/review` | 对当前分支 diff 进行代码检视 |

### 工作流规则

- **必须先 propose 再 apply**：没有 OpenSpec change 不要直接写代码
- **autoplan 前确保 design.md 已生成**：`autoplan` 会自动发现 `openspec/changes/*/design.md`
- **review 在提交前执行**：每次代码改动后运行 `/review` 做预着陆检视
- **探索模式不实现**：`/opsx-explore` 只思考不编码

## 技能路由（AI Agent 决策指南）

本章节定义 AI agent 的行为规则。当用户发起请求时，agent 必须根据当前状态（而非单纯的关键词匹配）决定调用哪个技能。

### 核心原则

1. **阶段感知优先于关键词匹配**：先检测当前处于工作流的哪个阶段，再推荐下一步动作。
2. **推荐而非自动执行**：agent 推荐下一步技能，**必须等待用户确认后**再调用。禁止自动连续触发多个技能。
3. **探索是默认入口**：任何模糊、抽象、未经分析的需求，默认入口是 `/opsx-explore`。

### 阶段检测逻辑

Agent 按以下优先级检测当前阶段：

1. **是否有未归档的活跃 change？**
   ```bash
   openspec list --json
   ```
   - 有 → 读取该 change 的状态，进入对应阶段判断
   - 无 → 视为新需求，进入需求分析阶段

2. **判断活跃 change 的当前阶段**
   | 检查项 | 阶段 | 推荐动作 |
   |--------|------|----------|
   | `tasks.md` 存在且未全部勾选 | 实现中 | 「当前有 N/M 个任务待完成。继续 `/opsx-apply`？」 |
   | `design.md` 存在，`tasks.md` 未生成或全部完成 | 设计完成 | 「设计文档已就绪。运行 `/autoplan` 进行审阅？」 |
   | `proposal.md` 存在，`design.md` 未生成 | 提案中 | 「提案已创建。继续生成设计文档？」 |
   | 仅有 `.openspec.yaml`，无 artifacts | 刚创建 | 「变更刚初始化。运行 `/opsx-propose` 生成设计文档？」 |
   | 所有 artifacts 完成，change 未归档 | 待检视 | 「实现完成。运行 `/review` 做代码检视？」 |

3. **无活跃 change 时的需求分析**
   - 用户说「我想做...」「能不能...」「需要...」等模糊需求 → 推荐 `/opsx-explore`
   - 用户说「实现...」「添加...」「修复...」等明确指令但无 change → 推荐 `/opsx-propose`
   - 用户说「看看这段代码」「审阅一下」 → 推荐 `/review`

### 工作流触发词映射

以下触发词**不是自动执行信号**，而是向用户推荐对应技能的依据：

| 用户意图 | 触发词示例 | 推荐技能 | 推荐话术 |
|----------|-----------|----------|----------|
| 探索想法 | "我想...", "能不能...", "如果...", "看看..." | `/opsx-explore` | "这个话题需要进一步澄清。运行 `/opsx-explore` 一起梳理需求？" |
| 创建提案 | "实现...", "添加...", "做...功能", "设计..." | `/opsx-propose` | "我来为你创建一个变更提案。运行 `/opsx-propose <name>`？" |
| 审阅设计 | "审阅...", "看看设计", "评估...", "合理吗" | `/autoplan` | "设计文档已生成。运行 `/autoplan` 进行四维度审阅？" |
| 执行实现 | "开始写代码", "实现任务", "按 tasks 执行" | `/opsx-apply` | "计划已审阅通过。运行 `/opsx-apply` 开始实现？" |
| 代码检视 | "review", "检视", "看看 diff", "能合入吗" | `/review` | "代码已变更。运行 `/review` 做预着陆检视？" |

### 技能调用路径

所有技能均通过项目本地安装调用：
- OpenSpec: `.opencode/skills/openspec-*/SKILL.md`
- gstack: `.opencode/skills/gstack-*/SKILL.md`

### 禁止行为

- **禁止自动连续执行**：完成 `/opsx-propose` 后，禁止自动调用 `/autoplan`。必须等待用户确认。
- **禁止跳过审阅**：`/opsx-apply` 之前必须有 `/autoplan` 或用户明确说「跳过审阅」。
- **禁止在探索模式实现**：`/opsx-explore` 期间禁止写代码或修改文件，仅允许读取和讨论。

## 目录速查

| 内容 | 位置 |
|------|------|
| 项目技术手册 | [PROJECT.md](./.reference/PROJECT.md) |
| OpenSpec 配置 | `openspec/config.yaml` |
| OpenSpec 变更存档 | `openspec/changes/archive/` |
| gstack 技能 | `.opencode/skills/gstack-*/` |
| 源码 | `src/main/java/com/kk/demo/` |
| 测试 | `src/test/java/com/kk/demo/` |
| 配置 | `src/main/resources/application*.yml` |
| 可观测性配置 | `compose.yaml`, `config/` |

## 约定

- 所有代码变更通过 OpenSpec change 管理
- gstack 技能仅本地安装（`.opencode/skills/`），不依赖全局安装
- 提交前必须运行 `/review`

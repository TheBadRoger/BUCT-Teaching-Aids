# 项目技能清单

技能安装在本仓库 `.agents/skills/`，不修改用户全局技能。核对日期：2026-10-23。仅安装技能文件、参考资料和上游许可证，不运行附带脚本、不安装应用依赖。

## 新增技能与来源

| 技能 | 适用任务 | 来源与固定提交 |
| --- | --- | --- |
| vue-best-practices | Vue 3、TypeScript、组件和组合式函数 | [vuejs-ai/skills](https://github.com/vuejs-ai/skills/tree/f5d4b3b869bb8a86ddfea148196f7db64aeefeab/skills/vue-best-practices) |
| vue-router-best-practices | 导航守卫、参数变化、页面生命周期 | [vuejs-ai/skills](https://github.com/vuejs-ai/skills/tree/f5d4b3b869bb8a86ddfea148196f7db64aeefeab/skills/vue-router-best-practices) |
| vue-testing-best-practices | Vue、Vitest、组件测试与异步测试 | [vuejs-ai/skills](https://github.com/vuejs-ai/skills/tree/f5d4b3b869bb8a86ddfea148196f7db64aeefeab/skills/vue-testing-best-practices) |
| vite | Vite 8、构建路径、环境变量、静态资源 | [antfu/skills](https://github.com/antfu/skills/tree/8886fe2d385d3644cbb47e46ae9cb1cf09307d3b/skills/vite) |
| pinia | Pinia 3、会话状态、组件外访问和测试 | [antfu/skills](https://github.com/antfu/skills/tree/2814e65fe3095742fd648aa4e09afa76daaa428e/skills/pinia) |
| docker-build-strategies | 多阶段构建、缓存、构建上下文、非 root 运行 | [Docker 官方 skills](https://github.com/docker/skills/tree/179c9cfd04c89ae50cd5d5d68b939797a8fd03d3/skills/docker-build-strategies) |
| docker-compose-patterns | 服务依赖、健康检查、环境变量和持久化 | [Docker 官方 skills](https://github.com/docker/skills/tree/179c9cfd04c89ae50cd5d5d68b939797a8fd03d3/skills/docker-compose-patterns) |

通过 Codex 内置 skill-installer 的 `install-skill-from-github.py` 安装，指定 `--repo`、`--ref`、`--path` 和 `--dest <仓库绝对路径>/.agents/skills`。固定提交避免安装时随上游默认分支变动。每个新增技能保留 `LICENSE.upstream.txt`。

## 保留与清理

保留原有 `spring-boot`、`spring-data-jpa`、`spring-security`，对应 Java 实际依赖；保留 `html-css-js`，用于后端已有静态页面及原教师 AI 页面。

移除 `spring-ai`、`spring-ai-alibaba`、`spring-cloud`、`spring-cloud-alibaba`：当前 Maven 依赖和代码未采用这些框架。AI 服务直接调用模型接口，不因服务名称包含 AI 就加载 Spring AI 技能。

不保留独立 `vitest` 技能：当前上游指向 Vitest 5 测试版，且与 Vue 测试技能覆盖范围重复；项目继续使用 Vitest 4。Pinia 技能选择明确针对 3.0.4 的历史版本，不采用面向 Pinia 4 的最新技能。

## 使用边界

- 这些文件是工作参考，不授权额外改动。用户要求和项目实际锁定版本优先，不因技能建议升级依赖、重命名编排文件、增加框架或改变部署方式。
- Vue Router 技能描述基于版本 4；本项目锁定版本为 5。公共导航模式可参考，版本相关 API 必须结合当前依赖和官方文档确认，不能据此降级项目。
- 前端沿用 Vue 3、TypeScript、Element Plus、Pinia 和现有页面风格。Docker 沿用 Java `static/js/app/` 托管方式，不引入 Nginx 或 Caddy。
- Docker 附带检查脚本仅按其用途显式运行；项目编排位于 `docker/`，不能直接照搬脚本默认根目录路径。
- 后端业务逻辑、数据库及部署范围仍以用户当前授权为准。技能安装不等于授权重构这些内容。

安装后下一轮对话可使用这些项目技能。检查范围为入口元信息、引用文件、来源版本及换行；不因技能变动重复运行应用构建或调用模型服务。

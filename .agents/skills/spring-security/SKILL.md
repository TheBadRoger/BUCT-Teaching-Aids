---
name: spring-security
description: Provides comprehensive guidance for Spring Security including authentication, authorization, OAuth2, JWT, and security best practices. Use when the user asks about Spring Security, needs to implement security in Spring applications, configure authentication, or work with security features.
license: Complete terms in LICENSE.txt
---

## When to use this skill

Use this skill whenever the user wants to:
- 用 Spring Security 配置认证、授权、会话与安全头
- 集成 OAuth2、JWT、方法安全与 CSRF

## How to use this skill

1. **配置**：SecurityFilterChain、UserDetailsService、PasswordEncoder；登录/登出与异常处理。
2. **进阶**：JWT、OAuth2 客户端/资源服务器；@PreAuthorize、CORS。
3. **参考**：https://docs.spring.io/spring-security/reference/

## Best Practices

- 密码加密、会话与 Cookie 安全；最小权限与角色设计。
- 生产用 HTTPS 与安全头；敏感路径保护。

## Keywords

spring security, 认证, 授权, JWT, OAuth2

## 能力边界

### ✅ 适用场景
- 当你需要使用此技能对应的技术栈时
- 当项目需要遵循最佳实践时
- 当需要快速上手或深入理解核心概念时

### ⚠️ 需要注意
- 复杂业务逻辑需要结合具体场景调整
- 性能优化需要根据实际数据量评估

### ❌ 不适用场景
- 不相关的技术栈或框架
- 需要完全自定义的特殊场景

## 常见陷阱 (Gotchas)

1. **版本兼容性**：注意框架版本与依赖库的兼容性，不同版本 API 可能有差异
2. **配置文件格式**：配置文件格式错误是最常见的问题，建议使用编辑器的语法检查
3. **环境变量**：确保所有必要的环境变量已正确设置，敏感信息不要硬编码
4. **依赖冲突**：多版本共存时注意依赖冲突，使用 lock 文件锁定版本
5. **性能陷阱**：大数据量场景下注意性能优化，避免 N+1 查询等常见问题

## 使用流程

### Step 1: 环境准备
确保开发环境已安装必要的依赖和工具。

### Step 2: 配置初始化
根据项目需求进行基础配置。

### Step 3: 核心功能使用
按照示例代码实现核心功能。

### Step 4: 测试验证
运行测试确保功能正常。

### Step 5: 部署上线
完成开发后进行部署和监控。

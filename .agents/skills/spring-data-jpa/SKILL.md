---
name: spring-data-jpa
description: Provides comprehensive guidance for Spring Data JPA including repositories, entity management, query methods, and database operations. Use when the user asks about Spring Data JPA, needs to work with JPA repositories, implement data access layers, or configure JPA in Spring.
license: Complete terms in LICENSE.txt
---

## When to use this skill

Use this skill whenever the user wants to:
- 用 Spring Data JPA 定义实体、Repository、查询方法与事务
- 配置数据源、JPQL、规范与审计

## How to use this skill

1. **核心**：@Entity、JpaRepository、CrudRepository；方法名查询、@Query、Pageable。
2. **进阶**：@EntityGraph、Specification、审计 @CreatedDate；多数据源与事务传播。
3. **参考**：https://spring.io/projects/spring-data-jpa

## Best Practices

- N+1 与 fetch 策略；分页与批量操作。
- 实体与 DTO 分离；事务边界明确。

## Keywords

spring data jpa, JPA, Repository, 持久化

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

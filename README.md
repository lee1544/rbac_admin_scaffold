# RBAC Admin Scaffold

一个可直接复用的 **RBAC 后台管理脚手架**，基于 Java 8 + Spring Boot 2.6.13 构建，提供组织、用户、角色、权限四个核心模块的完整后端能力。

项目采用 **按功能模块分包（package by feature）** 的结构，每个模块内部自带 `controller / dto / entity / repository / service`，便于按业务裁剪、扩展或直接复制到新项目。

---

## 1. 技术栈

| 分类 | 选型 |
| --- | --- |
| 语言 | Java 8 |
| 框架 | Spring Boot 2.6.13 |
| 持久化 | Spring Data JPA + Hibernate |
| 数据库 | H2（本地默认，内存库）/ MySQL 8（dev、prod） |
| 安全 | Spring Security + JWT（jjwt 0.9.1） |
| 密码 | BCrypt |
| 权限缓存 | Guava Cache（本地内存） |
| 参数校验 | Bean Validation |
| 构建 | Maven |

---

## 2. 目录结构

```text
rbac_admin_scaffold
├── pom.xml
├── README.md
├── docs
│   └── schema-mysql.sql              # MySQL 参考建表脚本
└── src
    ├── main
    │   ├── java/com/example/rbac
    │   │   ├── RbacAdminApplication.java
    │   │   ├── common                 # 通用能力，与业务模块无关
    │   │   │   ├── api                # ApiResponse / PageResponse
    │   │   │   ├── constant           # 系统常量、内置权限定义
    │   │   │   ├── entity             # BaseEntity（创建/更新时间）
    │   │   │   ├── event              # 权限缓存失效事件
    │   │   │   ├── exception          # 统一异常与全局异常处理
    │   │   │   └── security           # 登录上下文、权限注解、缓存、401/403 处理
    │   │   ├── bootstrap              # 启动装配与种子数据
    │   │   │   ├── DataInitializer.java
    │   │   │   └── config             # SecurityConfig / WebConfig(CORS)
    │   │   └── module                 # 业务模块（按功能分包）
    │   │       ├── auth               # 登录、登出、当前用户信息、JWT
    │   │       ├── user               # 用户管理、角色分配、密码管理
    │   │       ├── organization       # 组织管理、组织初始化
    │   │       ├── role               # 角色管理、角色授权
    │   │       └── permission         # 权限管理、权限校验切面
    │   └── resources
    │       ├── application.properties
    │       ├── application-local.properties
    │       ├── application-dev.properties
    │       └── application-prod.properties
    └── test
        └── java/com/example/rbac
            ├── AuthIntegrationTest.java
            ├── RbacAdminApplicationTests.java
            ├── common/api/PageResponseTest.java
            └── module/auth/security/JwtTokenProviderTest.java
```

### 模块内部约定

```text
module/<feature>
├── controller        # HTTP 接口，只做参数接收与响应封装
├── dto               # 请求对象，承载 Bean Validation
├── entity            # JPA 实体
├── repository        # Spring Data JPA 仓储
├── service           # 业务逻辑、事务、数据隔离
├── security          # 仅 auth 模块：JWT 相关
├── aspect            # 仅 permission 模块：@RequirePermission 切面
└── listener          # 仅 role 模块：角色权限变更时清缓存
```

---

## 3. 快速开始

### 3.1 环境要求

- JDK 8+
- Maven 3.6+（建议；只要能构建 Spring Boot 2.6 即可）

### 3.2 零配置启动（H2 内存库）

```bash
mvn spring-boot:run
```

默认激活 `local` profile，使用 H2 内存数据库，启动时自动建表并写入种子数据，无需安装任何数据库。

启动成功后：

- 服务地址：`http://localhost:8080`
- 默认账号：`admin`
- 默认密码：`admin123`

> 默认账号只在第一次启动、数据库中不存在 `admin` 时创建；后续启动不会覆盖已修改的密码。

### 3.3 登录示例

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"admin\",\"password\":\"admin123\"}"
```

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzUxMiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 604800,
    "userInfo": {
      "id": 1,
      "username": "admin",
      "realName": "超级管理员",
      "orgId": 1,
      "orgName": "默认组织",
      "roles": ["SUPER_ADMIN"],
      "permissions": ["user:view", "user:create", "..."],
      "firstLogin": false
    }
  },
  "timestamp": 1730000000000
}
```

携带令牌访问受保护接口：

```bash
curl http://localhost:8080/api/auth/userinfo \
  -H "Authorization: Bearer <token>"
```

### 3.4 使用 MySQL（dev profile）

先创建数据库：

```sql
CREATE DATABASE rbac_admin DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```

启动：

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

默认连接参数：

| 配置 | 默认值 |
| --- | --- |
| DB_URL | `jdbc:mysql://localhost:3306/rbac_admin?...` |
| DB_USERNAME | `root` |
| DB_PASSWORD | `root` |

可通过环境变量覆盖：

```bash
DB_USERNAME=your_user DB_PASSWORD=your_password mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

`dev` 默认 `spring.jpa.hibernate.ddl-auto=update`，会自动建表/更新表结构。

### 3.5 打包运行

```bash
mvn clean package
java -jar target/rbac-admin-scaffold-0.0.1-SNAPSHOT.jar
```

指定 profile：

```bash
java -jar target/rbac-admin-scaffold-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

---

## 4. 配置说明

### 4.1 Profile

| Profile | 数据库 | 用途 |
| --- | --- | --- |
| `local` | H2 内存库，`create-drop` | 本地开发、演示、测试，零配置 |
| `dev` | MySQL，`ddl-auto=update` | 联调开发环境 |
| `prod` | MySQL，`ddl-auto=validate` | 生产环境，需自行管理表结构 |

默认 profile 由 `application.properties` 指定为 `local`：

```properties
spring.profiles.active=${SPRING_PROFILES_ACTIVE:local}
```

### 4.2 主要环境变量

| 环境变量 | 说明 | 默认值 |
| --- | --- | --- |
| `SERVER_PORT` | 服务端口 | `8080` |
| `SPRING_PROFILES_ACTIVE` | 激活的 profile | `local` |
| `JWT_SECRET` | JWT 签名密钥，生产必须替换 | 内置开发密钥 |
| `JWT_EXPIRATION` | Token 有效期（秒） | `604800`（7 天） |
| `DEV_BYPASS_TOKEN` | 开发免登录令牌，空值表示关闭 | 空 |
| `CORS_ALLOWED_ORIGINS` | 允许的跨域来源，逗号分隔 | `*` |
| `DB_URL` | JDBC 连接串 | dev/local 有默认值 |
| `DB_USERNAME` | 数据库用户名 | `root` |
| `DB_PASSWORD` | 数据库密码 | `root` |
| `JPA_DDL_AUTO` | dev 环境表结构策略 | `update` |

---

## 5. 数据模型与权限模型

### 5.1 核心表

| 表名 | 说明 |
| --- | --- |
| `t_organization` | 组织 |
| `t_user` | 用户（属于一个组织） |
| `t_role` | 角色（可属于某个组织，也可为全局角色） |
| `t_user_role` | 用户-角色关联 |
| `t_permission` | 权限（以权限编码作为校验依据） |
| `t_role_permission` | 角色-权限关联 |

参考建表脚本见 [`docs/schema-mysql.sql`](docs/schema-mysql.sql)。

### 5.2 角色模型

| 角色编码 | 作用域 | 说明 |
| --- | --- | --- |
| `SUPER_ADMIN` | 全局（`org_id = null`） | 超级管理员，可跨组织管理数据 |
| `ORG_ADMIN_{orgId}` | 单个组织 | 组织管理员，可管理本组织用户、角色 |
| `ORG_USER_{orgId}` | 单个组织 | 组织普通用户，默认只有 `org:view` |

- 创建组织时，会自动创建该组织的 `ORG_ADMIN_{orgId}` 与 `ORG_USER_{orgId}` 两个系统角色，并创建组织管理员账号。
- `SUPER_ADMIN`、`ORG_ADMIN_*`、`ORG_USER_*` 为系统角色，接口层禁止删除。
- 用户只能被分配其所属组织下的角色。

### 5.3 权限编码

权限采用 `资源:动作` 命名，内置权限如下：

| 资源 | 权限编码 |
| --- | --- |
| 用户 | `user:view` `user:create` `user:update` `user:delete` `user:assign-role` `user:reset-password` |
| 组织 | `org:view` `org:create` `org:update` `org:delete` |
| 角色 | `role:view` `role:create` `role:update` `role:delete` `role:assign-permission` |
| 权限 | `permission:view` `permission:create` `permission:update` `permission:delete` |

### 5.4 权限校验流程

1. 客户端在 `Authorization: Bearer <token>` 中携带 JWT。
2. `JwtAuthenticationFilter` 解析并校验 token，同时校验：
   - 用户仍然存在；
   - 用户状态为启用；
   - 用户所属组织状态为启用。
3. 权限编码从 `UserPermissionCache`（Guava 本地缓存）读取；缓存未命中时查库并回填。
4. Controller 方法上的 `@RequirePermission("xxx")` 由 `PermissionAspect` 拦截校验。
5. 角色授权变更、用户角色变更、删除用户等操作会发布事件，清理相关用户权限缓存。

### 5.5 组织数据隔离

- 非超级管理员只能查看/操作本组织内的用户、角色和组织信息。
- 用户、角色、组织相关接口均带有组织归属校验。
- 超级管理员可通过可选参数 `orgId` 查看其他组织的用户列表/角色列表。
- 禁止非超级管理员操作超级管理员账号或全局角色。
- 默认组织 `DEFAULT_ORG` 不允许删除。

---

## 6. API 一览

所有接口统一返回：

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": 1730000000000
}
```

分页接口的 `data` 结构：

```json
{
  "items": [],
  "pageNumber": 0,
  "pageSize": 10,
  "totalElements": 0,
  "totalPages": 0,
  "first": true,
  "last": true,
  "empty": true
}
```

### 6.1 认证

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| POST | `/api/auth/login` | 公开 | 登录，返回 token 与用户信息 |
| GET | `/api/auth/userinfo` | 登录 | 当前用户信息、角色、权限 |
| POST | `/api/auth/logout` | 登录 | 登出，将 token 加入本地黑名单 |

### 6.2 用户

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| POST | `/api/users` | `user:create` | 创建用户 |
| GET | `/api/users?page=0&size=10&realname=&status=&orgId=` | `user:view` | 分页查询用户 |
| GET | `/api/users/{id}` | `user:view` | 用户详情 |
| PUT | `/api/users/{id}` | `user:update` | 更新姓名/状态 |
| PUT | `/api/users/{id}/status?status=1` | `user:update` | 启用/禁用用户 |
| PUT | `/api/users/{id}/password` | 本人 | 修改自己的密码 |
| PUT | `/api/users/{id}/reset-password` | `user:reset-password` | 重置为系统默认密码 |
| PUT | `/api/users/{id}/first-login-password` | 本人 | 首次登录/重置后设置新密码 |
| DELETE | `/api/users/{id}` | `user:delete` | 删除用户 |
| POST | `/api/users/{id}/roles` | `user:assign-role` | 分配角色 |
| GET | `/api/users/{id}/roles` | `user:view` | 查看用户角色 |

创建用户请求示例：

```json
{
  "username": "zhangsan",
  "password": "123456",
  "realName": "张三",
  "roleIds": [2]
}
```

`roleIds` 可省略，省略时自动分配该组织的 `ORG_USER_{orgId}` 角色。

### 6.3 组织

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| POST | `/api/organizations` | `org:create` | 创建组织并初始化管理员 |
| GET | `/api/organizations?page=0&size=10&orgName=` | `org:view` | 分页查询组织 |
| GET | `/api/organizations/{id}` | `org:view` | 组织详情 |
| PUT | `/api/organizations/{id}` | `org:update` | 更新组织 |
| DELETE | `/api/organizations/{id}` | `org:delete` | 删除组织及其数据 |

创建组织请求示例：

```json
{
  "orgCode": "ACME",
  "orgName": "示例公司",
  "description": "示例组织",
  "adminUsername": "acme_admin",
  "adminPassword": "admin123",
  "adminRealName": "示例管理员"
}
```

### 6.4 角色

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/roles?roleName=&orgId=` | `role:view` | 查询角色（超级管理员可指定组织） |
| GET | `/api/roles/{id}` | `role:view` | 角色详情（含权限） |
| POST | `/api/roles` | `role:create` | 创建角色 |
| PUT | `/api/roles/{id}` | `role:update` | 更新角色 |
| DELETE | `/api/roles/{id}` | `role:delete` | 删除角色 |
| POST | `/api/roles/{id}/permissions` | `role:assign-permission` | 全量覆盖角色权限 |
| GET | `/api/roles/{id}/permissions` | `role:view` | 查看角色权限 |

角色授权请求示例：

```json
{
  "permissionIds": [1, 2, 3]
}
```

### 6.5 权限

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/permissions?code=&name=&status=` | `permission:view` | 查询权限 |
| GET | `/api/permissions/{id}` | `permission:view` | 权限详情 |
| POST | `/api/permissions` | `permission:create` | 创建权限 |
| PUT | `/api/permissions/{id}` | `permission:update` | 更新权限 |
| DELETE | `/api/permissions/{id}` | `permission:delete` | 删除权限（已分配时拒绝） |

---

## 7. 扩展指南

### 7.1 新增业务模块

以 `module/product` 为例：

```text
module/product
├── controller/ProductController.java
├── dto/CreateProductRequest.java
├── entity/Product.java
├── repository/ProductRepository.java
└── service/ProductService.java
```

在 `SystemConstants.BASE_PERMISSIONS` 中登记权限编码，例如：

```java
permissions.put("product:view", "查看商品");
permissions.put("product:create", "创建商品");
```

重启后 `DataInitializer` 会自动补齐缺失权限，并将全部基础权限同步给 `SUPER_ADMIN`。

在接口上声明所需权限：

```java
@RequirePermission("product:create")
@PostMapping
public ApiResponse<Product> create(@Valid @RequestBody CreateProductRequest request) {
    // ...
}
```

### 7.2 新增接口权限

1. 在 `SystemConstants.BASE_PERMISSIONS` 增加权限编码与名称。
2. 给 Controller 方法加 `@RequirePermission("资源:动作")`。
3. 通过角色授权接口把新权限分配给需要的角色。
4. 用户权限缓存会在角色权限变更时自动失效，无需手动清理。

### 7.3 扩展组织级角色

- 组织系统角色编码使用 `ORG_ADMIN_{orgId}`、`ORG_USER_{orgId}` 约定；
- 新建业务角色时由 `RoleService.createRole(orgId, request)` 绑定组织；
- 用户角色分配会校验角色与用户属于同一组织。

### 7.4 替换本地缓存 / 黑名单

脚手架默认使用 Guava 本地缓存，适合单机或开发环境。多实例部署时建议替换：

- `UserPermissionCache` → Redis；
- `JwtTokenBlacklist` → Redis；
- 权限变更事件 → Redis Pub/Sub 或消息队列，保证多实例缓存一致。

---

## 8. 安全注意事项

> 这是脚手架，不是开箱即用的生产系统。上线前请至少完成以下事项。

1. **修改默认账号密码**：不要在生产环境保留 `admin / admin123`。
2. **替换 `JWT_SECRET`**：使用足够长、随机的密钥，并通过环境变量注入，不要提交到仓库。
3. **关闭开发绕过**：生产环境确保 `DEV_BYPASS_TOKEN` 为空；`prod` profile 已默认置空。
4. **使用 HTTPS**：JWT 通过 Authorization 头传输，必须部署在 HTTPS 之后。
5. **限制 CORS**：生产环境将 `CORS_ALLOWED_ORIGINS` 配置为明确的域名，不要使用 `*`。
6. **管理数据库账号**：生产环境使用最小权限数据库账号，并通过环境变量提供连接信息。
7. **管理表结构**：生产环境使用 `ddl-auto=validate`，建议引入 Flyway/Liquibase 管理迁移。
8. **补充审计与限流**：登录失败次数限制、操作审计日志、接口限流等需按实际业务补充。
9. **Token 吊销**：当前登出黑名单保存在进程内存，重启后失效；多实例场景请替换为集中式存储。
10. **权限缓存一致性**：跨实例部署时，角色权限变更需要广播失效事件。

---

## 9. 测试

```bash
mvn test
```

当前测试覆盖：

- Spring 上下文启动；
- 默认账号登录并获取用户信息；
- 匿名访问受保护接口返回 401；
- JWT 生成、解析与篡改拒绝；
- 分页响应元数据计算。

---

## 10. 许可

本脚手架可自由用于内部项目与二次开发，请根据实际项目补充许可证声明。

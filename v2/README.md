# 在线教育 v2（前后端分离骨架）

本目录提供一个全新、轻量的前后端分离骨架，复用现有数据库 `online_education`，便于逐步迁移和重构。

## 结构
```
v2/
  backend/   # Spring Boot 3 (port 8081)，纯 REST，使用 JdbcTemplate 读现有库
  frontend/  # 静态前端示例 (Bootstrap + Axios)，调用后端 API
```

## 后端（backend）
- 技术栈：Spring Boot 3.1、Java 17、JdbcTemplate、Validation
- 端口：8081（避免与旧版 8080 冲突）
- 配置：`v2/backend/src/main/resources/application.yml`
  - 数据库：`jdbc:mysql://localhost:3306/online_education`，用户名/密码见文件（请按需修改）
- 主要 API（示例）：
  - GET `/api/v2/courses` — 已审核课程列表
  - GET `/api/v2/courses/{id}` — 课程详情
  - GET `/api/v2/courses/{id}/videos` — 课程视频列表（读取 `course_videos`）
  - GET `/api/v2/teachers/{teacherId}/courses` — 教师课程
  - GET `/api/v2/teachers/{teacherId}/videos` — 教师视频

### 启动
```bash
cd /root/kecheng/v2/backend
mvn -DskipTests spring-boot:run
# 或打包后
mvn -DskipTests clean package
java -jar target/online-education-v2-0.1.0-SNAPSHOT.jar
```

## 前端（frontend）
- 轻量示例：单页 `index.html`（Bootstrap 5 + Axios）
- 默认调用后端 API：`http://39.106.139.93:8081/api/v2`
- 包含调试按钮：重载课程、查看教师/课程视频；课程卡片展示。

### 运行
直接用浏览器打开 `v2/frontend/index.html`，或用本地静态服务器（推荐以避免 CORS 问题，或在浏览器允许文件访问）。示例：
```bash
cd /root/kecheng/v2/frontend
python3 -m http.server 4173
# 浏览器访问 http://localhost:4173
```
如需在生产/同源部署，将前端静态资源托管到 Nginx 或 Spring Boot 静态目录，并把 `API` 地址改为相对路径（`/api/v2`）。

## 兼容现有数据库
- 仅使用 SELECT 查询，表：`courses`、`course_videos`、`users`、`grades`（简化示例）。
- 未使用 JPA 自动建表，避免 schema 冲突；可按需补充写操作 API。

## 后续可扩展方向
1) 增加认证/鉴权（JWT + 登录 API），前端存储 token。
2) 完整的 CRUD（课程、视频上传、成绩录入）——建议新增 DTO + 校验。
3) 统一错误处理与全局响应包装。
4) 前端可升级为 Vite/React/Vue，在 `frontend/` 里初始化 npm 工程并指向同一 API。
5) 日志与监控：接入 Spring Boot Actuator，自定义健康检查。

## 注意
- 当前端口为 8081，如需复用 8080，请修改 `application.yml`。
- 若数据库凭据不同，请更新 `application.yml` 或通过环境变量覆盖（`SPRING_DATASOURCE_URL` 等）。
- 如需 CORS 放行，可在 backend 增加全局 CORS 配置。

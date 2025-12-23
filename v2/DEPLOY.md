# v2 部署指南（含数据库导入）

## 文件清单
- 后端：`v2/backend`（Spring Boot 3，默认端口 8081）
- 前端：`v2/frontend`（静态 HTML/JS，Axios）
- 数据库：`v2/online_education_dump_20251210.sql`
- 一键包：`v2/online-education-v2-site-20251210.tar.gz`（包含 backend、frontend、SQL）

## 前置环境
- Java 17、Maven 3.x
- MySQL 5.7/8.0（或兼容 MariaDB）
- 可选：Python3（临时静态服务器），或 Nginx/Apache 托管前端

## 数据库导入
```bash
# 创建库（如未存在）
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS online_education CHARACTER SET utf8mb4;"
# 导入最新 dump
mysql -u root -p online_education < /root/kecheng/v2/online_education_dump_20251210.sql
```

## 后端启动
```bash
cd /root/kecheng/v2/backend
# 开发/验证：
mvn -DskipTests spring-boot:run
# 或使用已打包产物：
java -jar target/online-education-v2-0.1.0-SNAPSHOT.jar
```
- 配置：编辑 `src/main/resources/application.yml`（数据库地址、账号密码、端口），或用环境变量覆盖：
  - `SPRING_DATASOURCE_URL`、`SPRING_DATASOURCE_USERNAME`、`SPRING_DATASOURCE_PASSWORD`
  - `SERVER_PORT`（默认 8081）
- 上传目录：`app.upload-dir`（默认 `uploads`，相对启动目录）；确保有写权限。

## 前端启动（简易）
```bash
cd /root/kecheng/v2/frontend
python3 -m http.server 4173
# 浏览器访问 http://localhost:4173
```
- 前端默认 API：`http://39.106.139.93:8081/api/v2`；可在 URL 使用 `?api=http://<host>:<port>/api/v2` 覆盖。
- 生产同源部署：将前端文件托管到与后端同域端口，前端脚本里 `API` 配置改为 `/api/v2` 以免 CORS。

## 生产部署建议
1) 使用打包文件：
```bash
cd /root/kecheng/v2
tar xzf online-education-v2-site-20251210.tar.gz -C /opt/online-education
```
2) 配置 `application.yml` 或环境变量；创建上传目录：
```bash
mkdir -p /opt/online-education/backend/uploads
chmod -R 755 /opt/online-education/backend/uploads
```
3) 启动后端（systemd 示例）：
```ini
[Service]
ExecStart=/usr/bin/java -jar /opt/online-education/backend/target/online-education-v2-0.1.0-SNAPSHOT.jar
WorkingDirectory=/opt/online-education/backend
User=www-data
Restart=always
Environment=SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/online_education?useSSL=false&serverTimezone=UTC
Environment=SPRING_DATASOURCE_USERNAME=root
Environment=SPRING_DATASOURCE_PASSWORD=yourpass
Environment=SERVER_PORT=8081
```
4) 前端托管到 Nginx（示例）：
```nginx
server {
  listen 80;
  server_name example.com;
  root /opt/online-education/frontend;
  location /api/ {
    proxy_pass http://127.0.0.1:8081/api/;
  }
}
```
5) 验证：浏览器访问首页，`/auth/me` 正常返回登录状态；管理员访问 `admin.html`、教师访问 `teacher.html`。

## 快速问题排查
- 端口占用：`lsof -i :8081 -sTCP:LISTEN -P`
- CORS：确保前端同源，或在后端增加全局 CORS 放行。
- 数据库连接：检查 `application.yml` 与 MySQL 账号权限；确认库名与 dump 一致。
- 上传权限：确保运行用户对 `uploads/` 有写权限。

---
需要 Docker Compose 或进一步自动化，可在此基础上补充。
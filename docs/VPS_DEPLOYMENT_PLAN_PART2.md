#### 模块 D：SSL 证书配置（5 任务点）

**文件**: `scripts/vps-setup-ssl.sh`

- [ ] **任务 D.1**：安装 Certbot（1 点）
  - **输入**：HZUS 服务器 SSH 连接
  - **输出**：Certbot 安装完成
  - **关键步骤**：
    1. 通过 vps-ssh.sh 远程安装 certbot
    2. 安装 python3-certbot-nginx 插件
    3. 验证 certbot 版本（>= 1.0）

- [ ] **任务 D.2**：申请 SSL 证书（2 点）
  - **输入**：域名（已解析到 HZUS IP）
  - **输出**：Let's Encrypt SSL 证书
  - **关键步骤**：
    1. 创建 `scripts/vps-setup-ssl.sh` 脚本
    2. 停止 Nginx 容器（释放 80 端口）
    3. 执行 certbot certonly --standalone -d yourdomain.com
    4. 验证证书文件生成（/etc/letsencrypt/live/）
    5. 复制证书到项目 nginx/ssl/ 目录
    6. 重启 Nginx 容器

- [ ] **任务 D.3**：配置 HTTPS（2 点）
  - **输入**：SSL 证书文件
  - **输出**：Nginx HTTPS 配置
  - **关键步骤**：
    1. 修改 nginx/conf.d/minervia.conf 添加 443 端口监听
    2. 配置 SSL 证书路径（ssl_certificate、ssl_certificate_key）
    3. 启用 HTTP/2（listen 443 ssl http2）
    4. 配置 SSL 安全参数（ssl_protocols、ssl_ciphers）
    5. 添加 HTTP 到 HTTPS 重定向（301）
    6. 配置 HSTS 头（Strict-Transport-Security）

---

#### 模块 E：数据备份系统（6 任务点）

**文件**: `scripts/vps-backup.sh`

- [ ] **任务 E.1**：开发 MySQL 备份脚本（3 点）
  - **输入**：MySQL 容器连接信息
  - **输出**：自动化备份脚本
  - **关键步骤**：
    1. 创建 `scripts/vps-backup.sh` 脚本
    2. 使用 docker exec 执行 mysqldump
    3. 备份格式：minervia_YYYYMMDD_HHMMSS.sql.gz
    4. 保留最近 7 天的备份文件
    5. 添加备份完整性验证（gzip -t）
    6. 记录备份日志到 /var/log/minervia-backup.log

- [ ] **任务 E.2**：配置备份存储策略（2 点）
  - **输入**：备份文件
  - **输出**：多层备份存储
  - **关键步骤**：
    1. 本地存储：/opt/minervia/backups/
    2. 远程同步：使用 rsync 同步到本地机器
    3. 可选：配置云存储（AWS S3 / 阿里云 OSS）
    4. 设置 cron 定时任务（每天凌晨 2:00）
    5. 添加备份成功/失败通知（Bark 推送）

- [ ] **任务 E.3**：开发恢复测试脚本（1 点）
  - **输入**：备份文件
  - **输出**：恢复验证脚本
  - **关键步骤**：
    1. 创建 `scripts/vps-restore.sh` 脚本
    2. 支持从备份文件恢复数据库
    3. 添加恢复前确认提示
    4. 验证恢复后数据完整性

---

#### 模块 F：监控与日志（6 任务点）

**文件**: `scripts/vps-monitor.sh`

- [ ] **任务 F.1**：配置日志聚合（2 点）
  - **输入**：Docker 容器日志
  - **输出**：集中式日志管理
  - **关键步骤**：
    1. 配置 Docker 日志驱动（json-file + 轮转）
    2. 创建日志查看脚本 `scripts/vps-logs.sh`
    3. 支持按服务过滤日志（backend/frontend/mysql/redis）
    4. 支持按时间范围查询日志
    5. 可选：集成 Loki + Grafana（轻量级方案）

- [ ] **任务 F.2**：配置健康检查（2 点）
  - **输入**：服务健康检查端点
  - **输出**：监控脚本
  - **关键步骤**：
    1. 创建 `scripts/vps-monitor.sh` 脚本
    2. 检查 Docker 容器状态（docker ps）
    3. 检查服务健康端点（/actuator/health、frontend 首页）
    4. 检查系统资源（CPU、内存、磁盘使用率）
    5. 检查 SSL 证书过期时间
    6. 生成监控报告（JSON 格式）

- [ ] **任务 F.3**：配置告警通知（2 点）
  - **输入**：监控数据
  - **输出**：自动告警系统
  - **关键步骤**：
    1. 集成 Bark 推送通知
    2. 配置告警规则（容器停止、健康检查失败、磁盘空间不足）
    3. 设置 cron 定时监控（每 5 分钟）
    4. 添加告警去重机制（避免重复推送）
    5. 记录告警历史到日志文件

---

#### 模块 G：安全加固（7 任务点）

**文件**: `scripts/vps-security.sh`

- [ ] **任务 G.1**：配置防火墙（2 点）
  - **输入**：HZUS 服务器
  - **输出**：UFW 防火墙规则
  - **关键步骤**：
    1. 创建 `scripts/vps-security.sh` 脚本
    2. 安装并启用 UFW
    3. 配置默认策略（deny incoming, allow outgoing）
    4. 开放必需端口（22/SSH、80/HTTP、443/HTTPS）
    5. 限制 SSH 连接速率（ufw limit 22/tcp）
    6. 记录防火墙日志

- [ ] **任务 G.2**：SSH 安全加固（3 点）
  - **输入**：SSH 配置
  - **输出**：加固后的 SSH 配置
  - **关键步骤**：
    1. 禁用 root 密码登录（PermitRootLogin prohibit-password）
    2. 禁用密码认证（PasswordAuthentication no）
    3. 配置 SSH 密钥认证
    4. 修改 SSH 端口（可选，避免自动扫描）
    5. 安装 fail2ban 防止暴力破解
    6. 配置 fail2ban SSH jail（maxretry=3, bantime=3600）

- [ ] **任务 G.3**：Docker 安全配置（2 点）
  - **输入**：Docker 配置
  - **输出**：安全加固的 Docker 环境
  - **关键步骤**：
    1. 配置 Docker 用户命名空间隔离
    2. 限制容器资源（memory、cpu）
    3. 禁用容器特权模式
    4. 配置 Docker 日志大小限制
    5. 定期清理未使用的镜像和容器
    6. 扫描镜像漏洞（docker scan / Trivy）

---

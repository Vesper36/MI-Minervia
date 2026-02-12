## 4. 实施建议

### 4.1 技术选型

| 需求 | 推荐方案 | 理由 |
|------|----------|------|
| 反向代理 | Nginx 1.25 | 高性能、稳定、支持 HTTP/2 和 WebSocket |
| SSL 证书 | Let's Encrypt + Certbot | 免费、自动续期、广泛支持 |
| 备份存储 | 本地 + rsync 远程同步 | 简单可靠、成本低 |
| 监控方案 | 自定义脚本 + Bark 推送 | 轻量级、易维护、无需额外服务 |
| 日志管理 | Docker json-file + 轮转 | 内置方案、资源占用少 |
| 防火墙 | UFW | 简单易用、适合单服务器 |
| 入侵防护 | fail2ban | 成熟稳定、防止暴力破解 |

### 4.2 潜在风险

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| 部署过程中服务中断 | 高 | 1. 使用蓝绿部署策略<br>2. 准备回滚脚本<br>3. 在低峰期部署 |
| SSL 证书申请失败 | 中 | 1. 确认域名已正确解析<br>2. 确保 80 端口可访问<br>3. 准备备用证书方案 |
| 数据库迁移失败 | 高 | 1. 部署前完整备份<br>2. 在测试环境验证迁移<br>3. 准备回滚 SQL |
| 磁盘空间不足 | 中 | 1. 配置日志轮转<br>2. 定期清理 Docker 镜像<br>3. 设置磁盘告警 |
| 内存不足导致 OOM | 中 | 1. 配置容器资源限制<br>2. 调整 JVM 堆内存<br>3. 启用 swap（谨慎） |
| WebSocket 连接不稳定 | 低 | 1. 配置 Nginx 超时参数<br>2. 实现客户端重连机制<br>3. 添加心跳检测 |
| 邮件服务配置错误 | 中 | 1. 使用 SendGrid 测试 API<br>2. 验证 SMTP 连接<br>3. 配置邮件发送日志 |

### 4.3 测试策略

**单元测试**：
- 后端服务层测试（已有 JUnit + Mockito）
- 前端组件测试（已有 Vitest + Testing Library）

**集成测试**：
- API 端点测试（Testcontainers）
- 数据库迁移测试（Flyway）
- WebSocket 连接测试

**端到端测试**：
- 学生注册流程（邮箱验证 → 填写信息 → 提交申请）
- 管理员审核流程（登录 → 查看申请 → 审批）
- 学生门户访问（登录 → 查看课程 → 下载文档）

**性能测试**：
- 前端页面加载时间（< 2s）
- API 响应时间（P95 < 500ms）
- 并发用户数（支持 100+ 并发）
- WebSocket 连接数（支持 500+ 连接）

**安全测试**：
- SSL 配置检查（SSL Labs A+ 评级）
- 端口扫描（仅开放 22/80/443）
- SQL 注入测试
- XSS 攻击测试

---

## 5. 验收标准

功能完成需满足以下条件：

### 5.1 部署脚本

- [ ] vps-deploy.sh 可一键部署到 HZUS
- [ ] vps-check-env.sh 可检测服务器环境
- [ ] vps-rollback.sh 可回滚到上一版本
- [ ] 所有脚本有错误处理和日志输出

### 5.2 生产环境

- [ ] .env.prod 配置完整且安全
- [ ] Docker Compose 配置优化（资源限制、日志轮转）
- [ ] 所有容器健康检查正常
- [ ] 容器自动重启策略生效

### 5.3 Nginx 反向代理

- [ ] HTTP 请求正确转发到前端
- [ ] API 请求正确转发到后端
- [ ] WebSocket 连接稳定
- [ ] 静态资源缓存生效
- [ ] 安全头配置正确

### 5.4 SSL 证书

- [ ] HTTPS 访问正常
- [ ] HTTP 自动重定向到 HTTPS
- [ ] SSL Labs 评级 A 或以上
- [ ] 证书自动续期配置完成

### 5.5 数据备份

- [ ] MySQL 自动备份脚本运行正常
- [ ] 备份文件完整性验证通过
- [ ] 远程同步到本地机器成功
- [ ] 恢复脚本测试通过

### 5.6 监控与日志

- [ ] 日志查看脚本可正常使用
- [ ] 健康检查脚本运行正常
- [ ] 告警通知推送成功
- [ ] 监控 cron 任务配置完成

### 5.7 安全加固

- [ ] 防火墙规则配置正确
- [ ] SSH 密钥认证配置完成
- [ ] fail2ban 运行正常
- [ ] Docker 安全配置完成

### 5.8 功能测试

- [ ] 前端页面可正常访问
- [ ] 后端 API 响应正常
- [ ] 学生注册流程完整
- [ ] 管理员登录功能正常
- [ ] WebSocket 实时通信正常

### 5.9 性能测试

- [ ] 前端页面加载时间 < 2s
- [ ] API 响应时间 P95 < 500ms
- [ ] 支持 100+ 并发用户
- [ ] 无内存泄漏

### 5.10 文档

- [ ] 部署文档编写完成
- [ ] 运维手册编写完成
- [ ] 常见问题文档编写完成
- [ ] 回滚流程文档编写完成

---

## 6. 后续优化方向（可选）

Phase 2 可考虑的增强：

### 6.1 高可用性
- 配置 MySQL 主从复制
- 配置 Redis 哨兵模式
- 配置 Nginx 负载均衡（多服务器）

### 6.2 性能优化
- 配置 CDN 加速静态资源
- 启用 Redis 缓存（API 响应、会话）
- 配置数据库连接池优化
- 启用 Elasticsearch 查询缓存

### 6.3 监控增强
- 集成 Prometheus + Grafana
- 配置 APM 监控（Elastic APM / Jaeger）
- 配置日志分析（ELK Stack）
- 配置用户行为分析

### 6.4 CI/CD 自动化
- 配置 GitHub Actions 自动部署
- 配置自动化测试流水线
- 配置 Docker 镜像自动构建
- 配置自动回滚机制

### 6.5 安全增强
- 配置 WAF（Web Application Firewall）
- 配置 DDoS 防护
- 配置漏洞扫描（定期）
- 配置安全审计日志

---

## 7. 快速开始

### 7.1 部署前准备

1. 确认域名已解析到 HZUS 服务器 IP
2. 确认 SSH 密钥已配置到 HZUS
3. 确认本地已安装 vps-ssh.sh 和 vps-rsync.sh

### 7.2 一键部署

```bash
# 1. 检查服务器环境
./scripts/vps-check-env.sh HZUS

# 2. 配置生产环境变量
cp .env.example .env.prod
nano .env.prod  # 填写 JWT_SECRET、DB_PASSWORD 等

# 3. 执行部署
./scripts/vps-deploy.sh HZUS

# 4. 配置 SSL 证书
./scripts/vps-setup-ssl.sh HZUS yourdomain.com

# 5. 配置安全加固
./scripts/vps-security.sh HZUS

# 6. 配置备份
./scripts/vps-backup.sh HZUS --setup

# 7. 验证部署
./scripts/vps-verify.sh HZUS
```

### 7.3 常用运维命令

```bash
# 查看服务状态
bash ~/.agents/skills/vps-ssh-ops/scripts/vps-ssh.sh -q --target HZUS -- 'cd /opt/minervia && docker compose -f docker-compose.prod.yml ps'

# 查看日志
./scripts/vps-logs.sh HZUS backend

# 重启服务
bash ~/.agents/skills/vps-ssh-ops/scripts/vps-ssh.sh -q --target HZUS -- 'cd /opt/minervia && docker compose -f docker-compose.prod.yml restart backend'

# 备份数据库
./scripts/vps-backup.sh HZUS --now

# 回滚到上一版本
./scripts/vps-rollback.sh HZUS
```

---

## 8. 联系与支持

- 项目文档：`/Users/vesper/workspace/project/edu/docs/`
- 部署日志：`/opt/minervia/logs/deploy.log`（服务器）
- 备份目录：`/opt/minervia/backups/`（服务器）
- 监控日志：`/var/log/minervia-monitor.log`（服务器）

---

**文档版本**：1.0
**最后更新**：2026-02-11
**维护者**：Minervia DevOps Team

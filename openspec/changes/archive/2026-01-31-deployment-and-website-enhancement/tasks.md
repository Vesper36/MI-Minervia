# Tasks: 部署脚本与官网增强

## Phase 1: 部署基础设施

### 1.1 创建 .env.example 文件
- [ ] 创建 `.env.example` 包含所有环境变量
- [ ] 添加注释说明每个变量用途
- [ ] 标注必需和可选变量

### 1.2 创建 Docker 生产配置
- [ ] 创建 `docker-compose.prod.yml`
- [ ] 配置生产环境优化参数
- [ ] 添加资源限制配置

### 1.3 创建部署脚本
- [ ] 创建 `scripts/deploy.sh` 主部署脚本
- [ ] 实现环境检测功能
- [ ] 实现 Docker 部署模式
- [ ] 实现手动安装模式
- [ ] 添加错误处理和回滚

### 1.4 创建管理脚本
- [ ] 创建 `scripts/minervia.sh` 管理脚本
- [ ] 实现 status 命���
- [ ] 实现 ports 命令
- [ ] 实现 logs 命令
- [ ] 实现 start/stop/restart 命令
- [ ] 实现 backup/restore 命令

---

## Phase 2: 部署文档

### 2.1 更新部署文档
- [ ] 更新 `docs/DEPLOYMENT.md` 添加完整依赖清单
- [ ] 添加服务器配置要求
- [ ] 添加端口说明表格
- [ ] 添加部署检查清单
- [ ] 添加故障排除章节

### 2.2 创建快速开始指南
- [ ] 创建 `docs/QUICKSTART.md`
- [ ] 提供 5 分钟快速部署步骤
- [ ] 提供常见问题解答

---

## Phase 3: 官网内容增强

### 3.1 创建学校历史页面
- [ ] 创建 `frontend/src/app/[locale]/(marketing)/history/page.tsx`
- [ ] 添加 i18n 翻译内容
- [ ] 设计时间线组件

### 3.2 创建师资���绍页面
- [ ] 创建 `frontend/src/app/[locale]/(marketing)/faculty/page.tsx`
- [ ] 添加院系和教授数据
- [ ] 添加 i18n 翻译内容

### 3.3 增强课程目录页面
- [ ] 更新 `frontend/src/app/[locale]/(marketing)/programs/page.tsx`
- [ ] 添加详细课程信息
- [ ] 添加专业详情子页面

### 3.4 创建校园生活页面
- [ ] 创建 `frontend/src/app/[locale]/(marketing)/campus/page.tsx`
- [ ] 添加设施和学生组织信息
- [ ] 添加 i18n 翻译内容

### 3.5 创建新闻动态页面
- [ ] 创建 `frontend/src/app/[locale]/(marketing)/news/page.tsx`
- [ ] 添加新闻列表组件
- [ ] 添加 i18n 翻译内容

### 3.6 创建学术日历页面
- [ ] 创建 `frontend/src/app/[locale]/(marketing)/calendar/page.tsx`
- [ ] 添加日历数据
- [ ] 添加 i18n 翻译内容

### 3.7 创建 FAQ 页面
- [ ] 创建 `frontend/src/app/[locale]/(marketing)/faq/page.tsx`
- [ ] 添加常见问题数据
- [ ] 添加 i18n 翻译内容

---

## Phase 4: 前端优化

### 4.1 移动端导航
- [ ] 更新 `frontend/src/components/marketing/navbar.tsx`
- [ ] 添加移动端汉堡菜单
- [ ] 添加移动端导航抽屉

### 4.2 错误页面
- [ ] 创建 `frontend/src/app/[locale]/not-found.tsx`
- [ ] 创建 `frontend/src/app/[locale]/error.tsx`
- [ ] 添加 i18n 翻译内容

### 4.3 页脚增强
- [ ] 更新 `frontend/src/components/marketing/footer.tsx`
- [ ] 添加更多链接和信息
- [ ] 添加社交媒体链接

---

## Phase 5: 收尾工作

### 5.1 更新 README
- [ ] 更新项目 README.md
- [ ] 添加项目介绍
- [ ] 添加快速开始链接

### 5.2 Git 整理
- [ ] 检查 .gitignore 配置
- [ ] 确保无敏感信息
- [ ] 创建提交

---

## 依赖关系

```
Phase 1.1 → Phase 1.2 → Phase 1.3
Phase 1.3 → Phase 1.4
Phase 2 (独立)
Phase 3 (独立，可并行)
Phase 4 (依赖 Phase 3 部分完成)
Phase 5 (依赖所有其他 Phase)
```

## 预估工作量

| Phase | 任务数 | 复杂度 |
|-------|--------|--------|
| Phase 1 | 4 | 高 |
| Phase 2 | 2 | 中 |
| Phase 3 | 7 | 中 |
| Phase 4 | 3 | 低 |
| Phase 5 | 2 | 低 |

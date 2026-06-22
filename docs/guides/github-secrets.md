# GitHub Secrets 配置指南

## 访问配置页面

1. 打开 GitHub 仓库页面
2. 点击 **Settings** 标签
3. 在左侧菜单选择 **Secrets and variables** → **Actions**
4. 点击 **New repository secret** 按钮

## 需要配置的 Secrets

### 1. 部署相关

| Secret 名称 | 说明 | 示例值 |
|-------------|------|--------|
| `STAGING_HOST` | 测试环境服务器地址 | `your-server.com` |
| `STAGING_USER` | 服务器 SSH 用户名 | `root` |
| `STAGING_SSH_KEY` | 服务器 SSH 私钥内容 | `-----BEGIN OPENSSH PRIVATE KEY-----` |
| `PRODUCTION_HOST` | 生产环境服务器地址 | `your-server.com` |
| `PRODUCTION_USER` | 生产环境 SSH 用户名 | `root` |
| `PRODUCTION_SSH_KEY` | 生产环境 SSH 私钥内容 | `-----BEGIN OPENSSH PRIVATE KEY-----` |

### 2. 通知相关（可选）

| Secret 名称 | 说明 | 示例值 |
|-------------|------|--------|
| `SLACK_WEBHOOK_URL` | Slack 通知 Webhook | `https://hooks.slack.com/services/XXX/YYY/ZZZ` |
| `DINGTALK_WEBHOOK_URL` | 钉钉通知 Webhook | `https://oapi.dingtalk.com/robot/send?access_token=XXX` |
| `WECOM_WEBHOOK_URL` | 企业微信通知 Webhook | `https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=XXX` |

### 3. 第三方服务（可选）

| Secret 名称 | 说明 | 示例值 |
|-------------|------|--------|
| `ALIYUN_ACCESS_KEY` | 阿里云 AccessKey | `LTAI5tXXXXXX` |
| `ALIYUN_SECRET_KEY` | 阿里云 SecretKey | `XXXXXXXXX` |
| `MINIO_ACCESS_KEY` | MinIO AccessKey | `minioadmin` |
| `MINIO_SECRET_KEY` | MinIO SecretKey | `minioadmin123` |
| `JWT_SECRET` | JWT 签名密钥 | `your-jwt-secret-key` |

## 获取 Secret 值的方法

### SSH 密钥

```bash
# 生成新的 SSH 密钥
ssh-keygen -t ed25519 -C "y***@example.com"

# 查看公钥（添加到服务器）
cat ~/.ssh/id_ed25519.pub

# 查看私钥（添加到 GitHub Secrets）
cat ~/.ssh/id_ed25519
```

### Slack Webhook

1. 访问 https://api.slack.com/messaging/webhooks
2. 创建新的 Incoming Webhook
3. 复制 Webhook URL

### 钉钉 Webhook

1. 在钉钉群设置中添加自定义机器人
2. 复制 Webhook URL
3. （可选）设置安全验证（加签方式）

### 阿里云 AccessKey

1. 访问 https://ram.console.aliyun.com/
2. 创建 AccessKey
3. 复制 AccessKey ID 和 Secret

## 添加 Secret 步骤

1. 登录 GitHub 仓库
2. 进入 **Settings** → **Secrets and variables** → **Actions**
3. 点击 **New repository secret**
4. 输入 Secret 名称（如 `STAGING_HOST`）
5. 输入 Secret 值
6. 点击 **Add secret**

## 使用 Secret

在 GitHub Actions 工作流中使用：

```yaml
name: Deploy
on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Deploy to server
        uses: appleboy/ssh-action@v1.0.3
        with:
          host: ${{ secrets.STAGING_HOST }}
          username: ${{ secrets.STAGING_USER }}
          key: ${{ secrets.STAGING_SSH_KEY }}
          script: |
            cd /opt/app
            docker compose up -d
```

## 安全注意事项

1. **不要** 在代码中硬编码密钥
2. **不要** 将密钥提交到 Git 仓库
3. **不要** 在日志或错误信息中输出密钥
4. **定期** 轮换密钥
5. **使用** 最小权限原则

## 验证配置

添加完 Secrets 后：

1. 手动触发 GitHub Actions 工作流
2. 查看运行日志
3. 确认 Secrets 已正确注入

```bash
# 访问 GitHub Actions 页面
# https://github.com/<username>/<repo>/actions
```

## 故障排查

| 问题 | 解决方案 |
|------|---------|
| Secret 未找到 | 检查 Secret 名称是否正确 |
| SSH 连接失败 | 检查密钥和权限 |
| Webhook 发送失败 | 检查 URL 是否正确 |
| 权限不足 | 检查服务器权限设置 |

## 测试环境快速配置

如果你想快速测试，可以配置以下最小集合：

```
STAGING_HOST       = your-test-server.com
STAGING_USER       = ubuntu
STAGING_SSH_KEY    = (粘贴你的私钥内容)
```

这样就可以触发 CI/CD 工作流，验证自动化部署是否正常。

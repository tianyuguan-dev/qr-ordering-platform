# 监控图形界面（Prometheus + Grafana）

## 前提

- 后端已启动（如 `mvn spring-boot:run`），监听 **8080** 端口
- 本机已安装 Docker / Docker Compose

## 启动监控栈

```bash
# 在项目根目录执行
docker-compose -f docker-compose.monitoring.yml up -d
```

## 访问

| 服务     | 地址                     | 说明           |
|----------|--------------------------|----------------|
| **Grafana**  | http://localhost:3001   | 用户名 `admin`，密码 `admin` |
| **Prometheus** | http://localhost:9090 | 查询原始指标   |

## Grafana 使用

1. 打开 http://localhost:3001 ，用 `admin` / `admin` 登录（首次会要求改密码，可跳过）。
2. 左侧 **Dashboards** → 打开 **QR Ordering Platform**，即可看到：
   - 订单创建/状态变更速率
   - Outbox 积压、死信数
   - SSE 连接数
   - Outbox 发布失败速率
3. 右上角可切换时间范围、刷新间隔（默认 10s）。

## 停止

```bash
docker-compose -f docker-compose.monitoring.yml down
```

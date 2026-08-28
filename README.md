# CampusMarket

> 校园二手书 / 二手物品交易平台 —— 同校可见 + 担保支付。
>
> v0.1：单进程多模块后端（Maven 多模块 + Spring Boot 3.3 + JPA + H2）。
> Spring Cloud / Nacos / Feign / 多进程 在 v0.2 引入。

## 项目结构

```
campus-market/
├── scripts/
│   └── env.sh                      # source 后即可使用 java / mvn
├── backend/                        # Maven 多模块后端
│   ├── pom.xml                     # parent
│   ├── campus-common/              # 通用：API/错误码/JWT/鉴权/Trace
│   ├── campus-user/                # 用户与学校域（注册/登录/JWT）
│   ├── campus-listing/             # 商品发布/检索/状态机
│   ├── campus-trade/               # 钱包/订单/担保/评价
│   └── campus-app/                 # 单进程启动器（含 HappyPathTest）
├── frontend/                       # v0.2 引入 Vue 3
├── deploy/                         # v0.2 引入 docker-compose.yml
└── docs/                           # 接入 OpenSpec 后维护 ADR
```

## 本地启动

```bash
# 1) 配环境（一次性）
source ./scripts/env.sh

# 2) 编 + 跑 happy-path 集成测试
mvn -B -ntp -f backend/pom.xml test

# 3) 启动后端（端口 8080，单进程）
java -jar backend/campus-app/target/campus-app-0.1.0-SNAPSHOT.jar
```

服务器启动后访问：

```bash
curl -s http://localhost:8080/api/v1/schools | jq
# 期望：{"code":"OK","message":"success","data":[{"id":1,"name":"Demo University","domain":"demo.edu"}],...}
```

## Docker 一键启动

```bash
mvn -B -ntp -f backend/pom.xml -DskipTests install
docker compose -f deploy/docker-compose.yml up -d
# 浏览器打开 http://localhost:8080/
```

## 发布（GitHub Release + GHCR image）

推送 tag 触发 GitHub Actions（自带 docker daemon），自动构建 + 推 image + 创建 Release：

```bash
git tag v0.1.2 && git push origin v0.1.2
# 几分钟后：
#   image:  ghcr.io/bbbbbmy/campus-market:v0.1.2  (also :latest)
#   release: https://github.com/bbbbbmy/CampusMarket/releases/tag/v0.1.2
#   jar:    campus-app-0.1.0-SNAPSHOT.jar （附在 release）
```

ghcr.io 镜像默认 private，去仓 Settings → Packages 改 public 才能 pull。
CI 详细：每次 push / PR 都触发 `backend-ci` job 跑后端单测，提交前能看绿灯。

## OpenSpec / 设计文档

设计与需求规范留在 [`bbbbbmy/vibecoding`](https://github.com/bbbbbmy/vibecoding) 仓的
`openspec/changes/add-campus-secondhand-marketplace/`，含：

- `proposal.md` —— Why / What / Impact / Non-Goals
- `design.md` —— 架构、Schema、技术栈
- `tasks.md` —— 实施任务清单（按本 README 完成情况勾选）
- `specs/user/`、`specs/listing/`、`specs/trade/` —— ADDED 增量需求

## API 速查（v0.1）

| 方法 | 路径 | 鉴权 | 说明 |
|---|---|---|---|
| GET | `/api/v1/schools` | 公开 | 拉取所有 ACTIVE 学校 |
| GET | `/api/v1/categories` | 公开 | 拉取分类（教材 / 课外书 / 电子数码 / 生活用品） |
| POST | `/api/v1/auth/register` | 公开 | 注册 |
| POST | `/api/v1/auth/login` | 公开 | 登录 → JWT |
| GET | `/api/v1/auth/me` | JWT | 当前用户 |
| GET | `/api/v1/wallet` | JWT | 钱包余额 |
| POST | `/api/v1/wallet/top-up` | JWT | 充值（v0.1 虚拟） |
| POST | `/api/v1/listings` | JWT | 发新商品 |
| GET | `/api/v1/listings?keyword=...&categoryId=...&minPrice=...&maxPrice=...&condition=...` | JWT | 同校搜索（**强制覆盖 schoolId = 当前 token.schoolId**） |
| GET | `/api/v1/listings/{id}` | JWT | 详情 |
| POST | `/api/v1/listings/{id}/off-shelf` | JWT | 下架 |
| POST | `/api/v1/orders` `{listingId}` | JWT | 下单（自动担保） |
| GET | `/api/v1/orders/{id}` | JWT | 订单详情 |
| POST | `/api/v1/orders/{id}/ship` | JWT（卖家） | 发货 |
| POST | `/api/v1/orders/{id}/confirm` | JWT（买家） | 确认收货 + 放款 |
| POST | `/api/v1/orders/{id}/cancel` | JWT | 取消并退款 |
| POST | `/api/v1/reviews` `{orderId,rating,content}` | JWT | 评价 |

错误响应：`{code, message, traceId}`，`code` 参见 `campus-common/.../ErrorCode.java`。

## 端到端测试

`campus-app/src/test/java/com/campus/HappyPathTest.java` 跑通完整闭环：

```
注册 → 充值 → 发布 → 搜索 → 下单（PAID_ESCROW）→ 发货 → 确认（CONFIRMED）→ 评价
```

断言：

- Order.status = CONFIRMED
- 卖家钱包 balance 收到全部金额，frozen 清零
- 买家钱包 frozen 清零，balance 扣减对应金额

## 演示账号

本地首次启动时，`campus-user` 模块的 `DataSeed` 自动创建一个学校：

| 字段 | 值 |
|---|---|
| 学校名 | Demo University |
| domain（邮箱后缀） | `@demo.edu` |

所有用户邮箱必须 **以 `@demo.edu` 结尾**（验证邮箱域跟所选学校匹配）。注册时密码必须 **≥8 位且含字母 + 数字**；昵称 2–24 字。

### 推荐 demo 账号（两角色）

```
卖家 Alice（发书 / 收钱）：   email=alice@demo.edu   password=alice12345   nickname=Alice
买家 Bob  （下单 / 评价）：  email=bob@demo.edu     password=bobpass1234  nickname=Bob
```

闭环步骤：

1. 浏览器打开 http://localhost:8080/ 进入 UI
2. 用 Alice 账号登录 → “发布商品” tab 发书 “高数教材 4500 cents”
3. 注销 → 用 Bob 账号登录 → “充值 +5000 cents” → “浏览市场” 下单
4. 切回 Alice → “我的 → 订单” → 「发货」
5. 切回 Bob  → “我的 → 订单” → 「确认收货（受手续费提醒后写评价）」
6. Bob 钱包 frozen=0、balance=500c；Alice 钱包 balance=4500c

### README 与 UI 代码同步

UI HTML 单文件：`backend/campus-app/src/main/resources/static/index.html`
Spring Boot 默认从 `static/` 路径 serve；`http://localhost:8080/` 即页面入口。

## v0.1.1 已实施 / 已跳

### ✅ v0.1 (4d11055) + v0.1.1 (本轮)

- 学校域注册 + JWT 签发 + 鉴权拦截器
- 同校可见强过滤（`schoolId` 强制覆盖）
- 商品发布 / 检索（学校 + keyword + 类别 + 价格 + 成色）
- 商品状态机：ON_SALE / RESERVED / SOLD / OFF_SHELF
- 担保钱包（PESSIMISTIC_WRITE 锁 + @Version 乐观锁）
- 订单状态机：CREATED → PAID_ESCROW → SHIPPED → CONFIRMED；取消路径退款
- 评价（双向，1–5 星 + 文本）
- 收藏（幂等添加 / 取消 + 分页 mine 列表）
- 自动定时器：30 分钟未付款取消 + 7 天未确认放款（`@EnableScheduling` + `TradeScheduler`）
- 单元测试套：`UnitTests.java` 23 用例（Auth / Listing / Order + Scheduler）
- 端到端 Happy-PathTest（含收藏闭环）

- gateway-service（Spring Cloud Gateway + Sentinel 限流）
- file-service（MinIO / 图片上传）
- notification-service（订单事件 → 站内通知）
- admin 服务（管理后台 + RBAC）
- docker-compose（MySQL 8 + Redis 7 + Nacos 2.3）
- 真实支付通道（支付宝 / 微信）
- IM 聊天
- 前端 Vue 3 + Vite + Element Plus
- 中文 LIKE 检索（H2 + Chinese collation 这块的已知 quirk；v0.2 切 MySQL + 显式 collation）

## 工程取舍说明

**v0.1 是单进程**：4 个业务模块（user / listing / trade / common）+ 1 个 app 启动器，
都跑在同一个 Spring Boot 进程里。**这违背了"spring boot 微服务"的字面要求**，
但符合"端到端冒烟要通 + 无 Docker 环境可验证"的现实约束。
v0.2 切多进程时：

1. 每个模块变成独立 executable jar
2. Nacos 作注册 / 配置中心
3. service 间同步调用走 OpenFeign
4. 当前 `WalletApi` / `ListingApi` 接口直接升级为 Feign client

业务代码不变，只动启动器与 pom。

## 提交流程

```bash
git add -A && git commit -m "v0.1: 4 个业务模块 + happy-path 集成测试"

# 推到 GitHub（需先网页创建空仓 github.com/bbbbbmy/campus-market）
git remote add origin git@github.com:bbbbbmy/campus-market.git
git push -u origin main
```

## License

MIT

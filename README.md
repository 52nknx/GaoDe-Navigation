# 高德地图导航系统

这是一个基于高德地图的 Web 导航演示项目。项目由一个 Spring Boot 后端和一个原生 HTML 前端组成，后端负责转发高德开放平台 Web 服务接口，前端负责地图展示、起终点输入、路线绘制、坐标信息展示和路线导出。

## 项目功能

- 展示高德地图并支持缩放、比例尺等地图控件
- 支持浏览器定位，定位失败时默认使用北京天安门作为起点
- 支持输入起点、终点地址或经纬度坐标
- 支持点击地图设置起点，并通过逆地理编码显示地址
- 支持驾车路线规划和步行路线规划
- 在地图上绘制路线、起点标记和终点标记
- 展示起终点经纬度、直线距离等坐标信息
- 支持复制坐标信息
- 支持将路线导出为 JSON 或 KML 文件
- 支持路线规划成功后的浏览器语音播报

## 技术栈

- Java 8
- Spring Boot 2.7.15
- Gradle Wrapper
- Spring Web
- fastjson2
- 高德地图 JS API v2.0
- 高德开放平台 Web 服务 API
- Bootstrap 5
- Bootstrap Icons

## 项目结构

```text
.
├── index.html
├── build.gradle
├── settings.gradle
├── gradlew
├── gradlew.bat
└── src
    ├── main
    │   ├── java/com/example/amapnav
    │   │   ├── AmapnavApplication.java
    │   │   ├── config/RestTemplateConfig.java
    │   │   └── controller/AmapApiController.java
    │   └── resources/application.properties
    └── test/java/com/example/amapnav/AmapnavApplicationTests.java
```

## 运行环境

请先准备：

- JDK 8 或更高版本
- 可以访问高德开放平台接口的网络环境
- 高德开放平台账号
- 高德 Web 服务 API Key
- 高德地图 JS API Key

## API Key 配置方式

项目需要配置两类高德 Key。

### 1. 后端 Web 服务 Key

后端通过 `src/main/resources/application.properties` 中的 `gaode.key` 调用高德 Web 服务接口。默认配置会从环境变量 `GAODE_KEY` 读取：

```properties
gaode.key=${GAODE_KEY:}
```

推荐在本地启动前设置环境变量。

Windows PowerShell：

```powershell
$env:GAODE_KEY="你的高德Web服务Key"
```

macOS 或 Linux：

```bash
export GAODE_KEY="你的高德Web服务Key"
```

该 Key 会用于地址编码、逆地理编码、驾车路线、步行路线和交通状态查询等后端接口。

### 2. 前端 JS API Key

前端地图配置使用本地 `config.js` 文件。仓库中提供了示例文件：

```text
config.example.js
```

复制一份作为本地配置：

```powershell
Copy-Item config.example.js config.js
```

macOS 或 Linux：

```bash
cp config.example.js config.js
```

然后编辑 `config.js`：

```javascript
window.AppConfig = {
  API_BASE_URL: "http://127.0.0.1:8080/api",
  AMAP_JS_KEY: "你的高德JSAPIKey"
};
```

`config.js` 已加入 `.gitignore`，不会被提交到仓库。

> 注意：不要在公开仓库中提交真实可用的 API Key。如果 Key 已经公开过，建议到高德开放平台控制台重置或删除旧 Key。

## 后端运行步骤

在项目根目录执行：

```powershell
.\gradlew.bat bootRun
```

如果是在 macOS 或 Linux 环境中，执行：

```bash
./gradlew bootRun
```

启动成功后，后端服务默认运行在：

```text
http://127.0.0.1:8080
```

端口配置位于：

```properties
server.port=8080
```

## 前端打开方式

后端启动后，打开项目根目录下的 `index.html`。

可以直接双击打开，也可以使用本地静态服务打开，例如：

```bash
python -m http.server 5500
```

然后访问：

```text
http://127.0.0.1:5500/index.html
```

前端页面当前会请求本地后端地址：

```text
http://127.0.0.1:8080/api
```

因此使用前端功能前，需要先启动 Spring Boot 后端服务。

## 后端接口说明

所有后端接口统一以 `/api` 开头。

### 输入提示

```http
GET /api/suggest
```

请求参数：

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `keywords` | 是 | 搜索关键词 |
| `city` | 否 | 城市名称或城市编码 |

示例：

```text
http://127.0.0.1:8080/api/suggest?keywords=天安门&city=北京
```

### 逆地理编码

```http
GET /api/geocode/reverse
```

请求参数：

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `location` | 是 | 经纬度坐标，格式为 `经度,纬度` |

示例：

```text
http://127.0.0.1:8080/api/geocode/reverse?location=116.39748,39.90882
```

### 驾车路线规划

```http
GET /api/route/driving
```

请求参数：

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `origin` | 是 | 起点地址或坐标 |
| `destination` | 是 | 终点地址或坐标 |
| `waypoints` | 否 | 途经点坐标，多个途经点按高德接口格式传入 |

示例：

```text
http://127.0.0.1:8080/api/route/driving?origin=北京市东城区天安门广场&destination=北京市海淀区中关村
```

### 步行路线规划

```http
GET /api/route/walking
```

请求参数：

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `origin` | 是 | 起点地址或坐标 |
| `destination` | 是 | 终点地址或坐标 |

示例：

```text
http://127.0.0.1:8080/api/route/walking?origin=116.39748,39.90882&destination=116.407526,39.90403
```

### 矩形区域实时交通状态

```http
GET /api/traffic/status
```

请求参数：

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `rect` | 是 | 矩形区域坐标，格式按高德交通态势接口要求传入 |

示例：

```text
http://127.0.0.1:8080/api/traffic/status?rect=116.351147,39.966309;116.357134,39.968727
```

## 使用流程

1. 设置后端环境变量 `GAODE_KEY`。
2. 复制 `config.example.js` 为 `config.js`，填写前端 JS API Key。
3. 启动 Spring Boot 后端服务。
4. 打开 `index.html`。
5. 输入起点和终点。
6. 选择出行方式。
7. 点击“搜索路线”。
8. 查看地图路线、坐标信息，并按需导出 JSON 或 KML 文件。

## 常见问题

### 前端提示“路线搜索失败，请检查后端服务”

请确认 Spring Boot 后端已经启动，并且可以访问：

```text
http://127.0.0.1:8080
```

### 地图无法加载

请检查 `config.js` 中的 `AMAP_JS_KEY` 是否正确，以及当前网络是否可以访问高德地图 JS API。

### 地址解析失败

请检查后端环境变量 `GAODE_KEY` 是否可用，且该 Key 是否开通了对应的高德 Web 服务接口权限。

### 浏览器定位失败

浏览器定位可能受权限、网络、浏览器安全策略影响。定位失败时，系统会默认使用北京天安门作为起点，也可以手动输入起点或点击地图设置起点。

## 说明

该项目适合作为高德地图 API、路线规划、地理编码和前后端接口调用的学习示例。当前实现偏课程作业和演示用途，如需部署到生产环境，建议进一步完善 API Key 安全、接口参数校验、错误响应格式、前端配置化和测试覆盖。

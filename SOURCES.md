# sources.json — 影视采集源清单（自维护）

本文件是「锤锤影视」的**可导入采集源清单**，格式为 `[{"name","api","type":"maccms"}]`，填入 app：

> 源管理 → 右上「**批量导入**」→ 粘贴本文件内容（默认"追加"，去重；全部重置用"覆盖"）。

当前 10 个源均为**本机实测可用**的苹果 CMS 采集接口（`MaccmsAdapter` 适配），已通过
`?ac=list`（分类）+ `?ac=videolist`（列表）+ `?ac=detail`（播放地址）三连验证。

> ⚠️ 这些是第三方资源站，内容版权与可用性由第三方承担，仅供自用研究（项目为空壳定位）。
> 仅包含常规影视源；成人向源已剔除。

## 使用方法

### 本地（模拟器 / 同一 WiFi 真机）
把本文件内容粘贴到 app「批量导入」即可。也可直接用下面的 URL 方式（见公网一节）。

### 公网发布（一次维护，所有设备自动拉最新，可定时探活）——推荐

1. **推送到 GitHub**（本仓库尚未配置 remote）：
   ```bash
   git remote add origin git@github.com:<你的用户名>/<仓库名>.git
   git push -u origin master
   ```
2. **开启 GitHub Pages**：仓库 → Settings → Pages → Source 选 `Deploy from a branch` →
   分支 `master` / 目录 `/ (root)` → Save。稍等部署后：
   ```
   https://<你的用户名>.github.io/<仓库名>/sources.json
   ```
3. **在 app「批量导入」用 URL 方式**，填上面这个公网地址 → 导入 → 得到全部源。

### 定时自维护（自动剔除失效源）
`.github/workflows/update-sources.yml` 已配置：
- **每天 06:30 UTC（北京 14:30）** 自动跑 `probe-sources.ps1`；
- 探活脚本请求每个源的 `?ac=list`，保留返回 `class`/`list` 的存活源，剔除死源并回写 `sources.json`；
- 有变化时自动 `commit + push`（`chore(sources): prune dead sources`，由 GitHub Actions bot 提交）。
- 也可在 Actions 页手动触发 `workflow_dispatch`，或推送 `sources.json` 时自动触发。

## 本地重跑探活（可选）

```powershell
pwsh probe-sources.ps1
```

输出 `ALIVE/DEAD` 与 `SUMMARY: total=… alive=… dead=…`，并回写存活源。

## 文件

| 文件 | 作用 |
|------|------|
| `sources.json` | 采集源清单（app 可导入） |
| `probe-sources.ps1` | 探活并回写清清单 |
| `.github/workflows/update-sources.yml` | 每天定时跑探活 + 有变化自动提交 |

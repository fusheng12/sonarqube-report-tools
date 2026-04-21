# SonarQube 报告生成工具

基于 [sonar-cnes-report](https://github.com/cnescatlab/sonar-cnes-report) 的图形化包装工具，面向普通用户，支持批量导出多个项目、多个分支的代码质量分析报告。

## 功能特性

- **图形化操作** — 无需命令行，三步完成报告导出
- **批量导出** — 一次勾选多个项目，一键全部导出
- **多分支支持** — 每个项目可选择多个分支分别生成报告
- **多种格式** — 支持 Word (.docx)、Excel (.xlsx)、CSV、Markdown
- **自动打包** — 每个项目的报告自动压缩为 zip，命名为 `日期-时间-项目名.zip`
- **配置持久化** — 服务器地址和 Token 自动保存，下次启动无需重新配置
- **内嵌 JDK** — 自带 JDK 21 运行环境，无需安装任何依赖

## 目录结构

```
sonar-report-tool/
├── jdk-21/                            # 内嵌 JDK 21 运行环境
├── lib/
│   └── sonar-cnes-report-5.0.3.jar    # cnes-report 报告生成引擎
├── target/
│   └── sonar-report-tool.jar          # GUI 工具主程序
├── output/                            # 报告输出目录
├── config/                            # 用户配置（自动生成）
├── start.bat                          # 启动脚本
├── build.bat                          # 编译脚本（开发用）
└── dist.bat                           # 打包分发脚本（开发用）
```

## 使用方法

### 1. 启动工具

双击 `start.bat`，打开图形界面。

### 2. 配置服务器连接

在"服务器配置"页签中：

| 字段 | 说明 |
|------|------|
| SonarQube 地址 | 服务器完整地址，如 `http://10.30.24.21:9000` |
| 用户 Token | 在 SonarQube 的 **My Account → Security** 中生成 |

填写后点击"测试连接"，验证成功后配置自动保存。下次启动自动加载，可直接进入下一步。

### 3. 选择项目和分支

切换到"项目管理"页签：

1. 点击"获取项目列表" — 自动加载所有项目及其分支信息
2. 勾选需要导出的项目
3. **双击**"分支"列可选择要导出的分支（默认选中主分支）

### 4. 导出报告

切换到"导出报告"页签：

| 配置项 | 说明 |
|--------|------|
| 报告作者 | 填写在报告中署名的作者 |
| 语言 | 报告语言：en_US / fr_FR |
| 输出目录 | 报告文件输出路径，可通过"浏览"按钮选择 |
| 报告格式 | Word / Excel / CSV / Markdown，按需勾选 |

点击"开始导出"，进度条和日志区域会实时显示导出进度。导出完成后，每个项目会在输出目录下生成一个 zip 文件，如：

```
output/
├── 20260421-1430-mes-cloud-base.zip
├── 20260421-1431-ird-epp.zip
├── mes-cloud-base/          # 临时目录（可删除）
│   └── dev/
└── ird-epp/                 # 临时目录（可删除）
    └── epm-loc-uat/
```

## 兼容性

| cnes-report | SonarQube 9.9 (LTS) | SonarQube 10.5 | SonarQube 25.1 |
|:-----------:|:-------------------:|:---------------:|:---------------:|
| 5.0.x       | -                   | ✓               | ✓               |

## 开发

### 前置条件

- JDK 21+

### 编译

```
双击 build.bat
或手动执行：
  jdk-21\bin\javac -source 21 -target 21 -encoding UTF-8 -d target\classes src\main\java\com\sonar\tools\**\*.java
  jdk-21\bin\jar cfe target\sonar-report-tool.jar com.sonar.tools.App -C target\classes .
```

### 打包分发

```
双击 dist.bat
输出：dist/sonar-report-tool.zip
```

将 zip 文件发送给用户，解压后双击 `start.bat` 即可使用。

## 许可证

- 本工具：按需声明
- sonar-cnes-report：[GPL-3.0](https://github.com/cnescatlab/sonar-cnes-report/blob/master/LICENSE)

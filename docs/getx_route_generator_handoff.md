# xPlugin GetX Route Generator V1 交接与扩展指南

> 文档用途：将本文直接提供给 GPT，作为后续完善 GetX Route 代码生成器的项目上下文。
>
> 当前基线：`xPlugin` 仓库 `main` 分支，基线提交 `125fb30 GetX Router`。
>
> 重要约束：接手后应先阅读本文和实际代码，先给出影响范围与方案，再修改代码。

## 1. 项目背景

`xPlugin` 是一个运行在 IntelliJ IDEA / Android Studio 内的插件，用于通过右键菜单一键生成 Android、Flutter 等项目脚手架代码。

本次新增的能力是 Flutter GetX Route 生成器，入口为：

```text
New → xView → flutter → GetX Route
```

GetX Route 的主代码独立维护在：

```text
src/main/kotlin/com/frame/xplugins/getx
```

这样可以避免 GetX 逻辑与原有 MVVM、MVI 等生成器相互耦合。

原始需求文档位于：

```text
/Users/guoql/Downloads/getx_route_generator_requirements_v1.md
```

如果后续 GPT 不在同一台机器上，应同时上传原始需求文档与本文。

## 2. V1 当前状态

GetX Route V1 已经完成并可实际生成文件，当前覆盖：

- GetView、StatelessWidget、StatefulWidget 三类 Page。
- Controller 新建、复用、不使用三种模式。
- Repository 新建、复用、不使用三种模式。
- Binding 新建、追加、不使用三种模式。
- 可选创建 `widgets/` 目录。
- 基于 Dart PSI 扫描已有 Controller、Repository、Binding。
- 生成前预览 CREATE、REUSE、MODIFY 文件。
- 文件冲突、类冲突和依赖组合校验。
- Binding 最小范围追加，不整体重排已有文件。
- 整次创建操作作为一个 IDE Write Command，可一次 Undo。
- 生成代码使用“末尾参数不加逗号”的项目格式规范。
- 主要声明带中文 Dart 文档注释。

实现阶段可对应为：

| 阶段 | 已完成内容 |
| --- | --- |
| P0 | 菜单入口、Dart 可选依赖、GetX 独立包、命名与路径基础能力 |
| P1 | 配置 Dialog、模式联动、模板生成、文件预览 |
| P2 | Dart PSI 扫描、现有类发现和候选复用 |
| P3 | 文件写入、Binding 追加、冲突校验、单次 Undo |
| P4 | 自动化测试、格式规范、中文注释、插件打包验证 |

## 3. 已确认的产品与交互规则

### 3.1 菜单入口

GetX Action 直接注册在主 `plugin.xml` 的 Flutter 分组中：

```text
New
└── xView
    └── flutter
        └── GetX Route
```

Action 在存在 Project 时保持可见、可点击。这样处理是因为部分 Android Studio 2025.3 场景下，嵌套 New 菜单的 `update()` 阶段不能稳定取得当前目录。

真正的目录解析和合法性检查放在点击 Action 后执行。如果当前目录不属于 Flutter `lib`，则弹出明确错误提示，不生成文件。

### 3.2 目标目录

用户右键选中的目录是生成 Route 文件夹的父目录。

合法目录必须满足：

- 目录本身是 `lib`；或
- 目录位于某个 `lib` 目录之下。

目录上下文按以下顺序兜底解析：

1. `CommonDataKeys.VIRTUAL_FILE`
2. `PSI_ELEMENT` 中的 `PsiDirectory`
3. `VIRTUAL_FILE_ARRAY`
4. `LangDataKeys.IDE_VIEW`

### 3.3 命名输入

Route 名称支持 ASCII：

- snake_case
- camelCase
- PascalCase
- 连字符
- 空格

输入会统一转换为：

- 文件和目录：`snake_case`
- Dart 类型：`PascalCase`

前缀默认是 `tk`，允许清空。

以下输入拒绝生成：

- Unicode 名称
- 路径字符
- 以数字开头
- 不能稳定转换为 Dart 标识符的内容

### 3.4 模式联动

Controller：

- Create：创建新 Controller。
- Existing：复用项目中已有 Controller。
- None：不使用 Controller。

Repository：

- Create：创建新 Repository。
- Existing：复用项目中已有 Repository。
- None：不使用 Repository。

Binding：

- Create：创建新 Binding 文件。
- Append：向已有 Binding 文件追加内容。
- None：不使用 Binding。

已确认的联动规则：

- Controller 为 None 时，Repository 和 Binding 必须同时为 None。
- Controller 为 None 且 Page 原来选择 GetView 时，Page 自动切换为 StatelessWidget。
- 新建 Controller 时必须生成或追加 Binding。
- 复用 Controller 但不使用 Binding 时，Repository 必须为 None。
- 已有 Controller 构造参数必须与 Repository 选择兼容。

### 3.5 业务域推荐

若选择路径位于：

```text
lib/features/<domain>/...
```

则 `<domain>` 作为当前业务域，用于优先推荐同域的 Repository 和 Binding。

若不在 `features` 结构中，则使用当前 Route 名称作为推荐域。

推荐只影响默认选中项，不改变用户的最终选择。

## 4. 生成路径规则

以用户选中的目录为 `<target>`，Route 名称规范化后为 `<route_name>`：

```text
<target>/<route_name>/
├── <prefix_><route_name>_page.dart
├── <prefix_><route_name>_controller.dart     # 按配置创建
└── widgets/                                  # 按配置创建
```

Repository 固定放在 Flutter 模块的：

```text
lib/repositories
```

Binding 固定放在 Flutter 模块的：

```text
lib/bindings
```

预览区域用三种状态明确显示写入影响：

- `CREATE`：新建文件或目录。
- `REUSE`：引用已有类或文件，不修改。
- `MODIFY`：向已有 Binding 文件追加内容。

## 5. Dart 代码格式规范

### 5.1 核心原则

所有 GetX 生成模板中，每一层调用、集合或参数列表的最后一项都不加 trailing comma。

这样做的目的是让 Dart formatter 尽可能利用代码编辑器宽度，而不是因为最后一个逗号强制换行。

注意：

- 非最后一个参数之间仍需保留逗号。
- 不只处理最外层参数，内部 `Get.find<T>()` 等嵌套调用的最后一个参数也不加逗号。
- 不应在后续优化中恢复 Dart 官方常见的“末尾逗号强制多行”风格。

Repository 注册示例：

```dart
Get.lazyPut<CourseRepository>(
  () => CourseRepository(Get.find<TkApi>()),
  fenix: true
);
```

Controller 注册示例：

```dart
Get.lazyPut<CourseController>(
  () => CourseController(Get.find<CourseRepository>())
);
```

Page 中简单组件保持紧凑：

```dart
return const Scaffold(body: SizedBox());
```

测试中使用以下语义检查禁止末尾逗号：

```regex
,\s*[)\]}]
```

### 5.2 注释规范

生成的主要声明应带简洁中文 Dart 文档注释，包括：

- Page
- Controller
- Repository
- `_registerRepository()` 辅助方法
- 每一个 Binding 类

一个 Binding 文件允许存在多个 `Bindings` 实现类，每个类都要有独立说明，不允许只在文件顶部写一条笼统注释。

Binding 注释需要明确服务的 Page，例如：

```dart
/// [CourseDetailPage] 的依赖绑定。
class CourseDetailBinding extends Bindings {
  // ...
}
```

## 6. Binding 追加策略

Binding 是 V1 中最需要保持克制的写入区域。

### 6.1 文件结构

新建 Binding 文件时，推荐顺序为：

1. imports
2. `_registerRepository()` 辅助方法
3. 一个或多个 Binding class

### 6.2 追加已有文件

追加已有 Binding 时只做必要变更：

- 只补缺失的 import。
- `_registerRepository()` 不存在时才补充。
- 只追加一个新的 Binding 类。
- 不整体重写原文件。
- 不对整个文件执行 Reformat。
- 不自动 Optimize Imports 或重排用户原有 import。
- 只格式化本次插入的代码范围。

这样可以减少无关 diff，并保护一个文件中已有的多个 Binding class。

### 6.3 Helper 一致性校验

如果已有 `_registerRepository()`，写入前必须校验它是否包含本次期望的 Repository 注册逻辑。

不能仅因为方法同名就直接复用，否则可能生成一个引用了 Repository、但运行时没有完成依赖注册的 Binding。

## 7. PSI 扫描与复用逻辑

扫描器基于 Dart PSI，而不是根据文件名猜测。

扫描范围为当前 Flutter 模块的 `lib`，识别规则包括：

- Controller：类的父类为 `GetxController`。
- Repository：位于 `lib/repositories`，且类名以 `Repository` 结尾。
- Binding 文件：位于 `lib/bindings`，且包含 `Bindings` 子类。
- Controller 构造信息：解析未命名构造函数参数，并识别 `this.field` 对应的字段类型。

扫描结果通过 `SmartPsiElementPointer` 保存，降低 PSI 更新后持有失效元素的风险。

Dialog 打开前会执行一次扫描，用于显示候选项；真正写入前还会重新扫描和校验，避免用户打开 Dialog 后项目文件发生变化造成过期写入。

## 8. 写入、冲突与 Undo

整次生成操作放在一个 `WriteCommandAction` 中，包含：

- 重新扫描项目状态。
- 重新校验配置和文件冲突。
- 校验 Binding 追加目标。
- 创建目录和文件。
- 修改已有 Binding。
- 格式化本次新增内容。

因此用户执行一次 IDE Undo，应能撤销本次完整生成操作。

写入前需要阻止：

- 新建文件已经存在。
- 目标 Dart 类已经存在。
- 选择的已有类或文件已经失效。
- Binding 中已经存在同名类。
- Controller、Repository、Binding 模式组合不合法。
- 已有 Controller 构造函数与选择的 Repository 不兼容。

目录和文件通过 PSI / VFS API 创建；已有 Binding 通过 Document 做最小范围编辑。

## 9. 代码结构与职责

### 9.1 Kotlin 主代码

```text
src/main/kotlin/com/frame/xplugins/getx/
├── action/
│   └── GetXRouteAction.kt
├── dialog/
│   └── GetXRouteDialog.kt
├── generator/
│   └── GetXCodeGenerator.kt
├── model/
│   └── GetXModels.kt
├── naming/
│   └── DartNamingConverter.kt
├── path/
│   └── FlutterProjectPathResolver.kt
├── scanner/
│   └── GetXProjectScanner.kt
├── service/
│   ├── GetXRouteExecutor.kt
│   └── GetXRouteGenerationService.kt
├── validation/
│   └── GetXRouteValidator.kt
└── writer/
    └── GetXRouteWriter.kt
```

职责说明：

| 模块 | 职责 |
| --- | --- |
| Action | 解析右键目录、校验 Flutter 上下文、触发扫描、打开 Dialog、执行生成 |
| Dialog | 收集配置、处理模式联动、展示候选与文件预览 |
| Models | 模式枚举、扫描候选、生成配置、计划和派生名称/路径 |
| Naming | Route 名称和前缀的校验与 snake_case / PascalCase 转换 |
| Path | 查找 Flutter `lib`、解析选择目录与业务域 |
| Scanner | 使用 Dart PSI 扫描可复用的类、文件和构造函数 |
| Validator | 校验组合、构造参数、文件与类冲突、目标是否仍然有效 |
| GenerationService | 根据配置生成 CREATE / REUSE / MODIFY 计划 |
| CodeGenerator | 读取模板并替换占位符，生成最终 Dart 文本 |
| Writer | 创建新文件、追加 Binding、格式化新增范围 |
| Executor | 将重新扫描、校验和写入包在一次 Write Command 中 |

### 9.2 Dart 模板

```text
src/main/resources/code/getx/
├── binding/
│   ├── register_repository.dart.tpl
│   ├── with_repository.dart.tpl
│   └── without_repository.dart.tpl
├── controller/
│   ├── with_repository.dart.tpl
│   └── without_repository.dart.tpl
├── page/
│   ├── get_view.dart.tpl
│   ├── stateful.dart.tpl
│   ├── stateful_with_controller.dart.tpl
│   ├── stateless.dart.tpl
│   └── stateless_with_controller.dart.tpl
└── repository/
    └── repository.dart.tpl
```

后续新增模板变量时，需要同步检查：

1. `GetXCodeGenerator`
2. 对应 `.tpl`
3. `GetXTemplateFormattingTest`
4. 文件预览和冲突校验是否需要新增路径

### 9.3 插件注册

主配置文件：

```text
src/main/resources/META-INF/plugin.xml
```

关键点：

- 插件 ID：`com.frame.plugin`
- GetX Action ID：`com.frame.xplugins.getx.route`
- Action 文案：`GetX Route`
- Dart 插件使用可选依赖：`<depends optional="true">Dart</depends>`
- GetX Action 直接挂在 `com.load.plugin.flutter` 分组下
- 主分组 `com.load.plugin` 以 `xView` 名称加入 `NewXml`

## 10. 自动化测试

GetX 相关测试位于：

```text
src/test/kotlin/com/frame/xplugins/getx
```

当前包括：

| 测试文件 | 覆盖内容 |
| --- | --- |
| `GetXRouteActionRegistrationTest.kt` | Action 注册、菜单层级和 Dart 依赖 |
| `GetXTemplateFormattingTest.kt` | 模板渲染、中文注释、禁止最后一位逗号 |
| `DartNamingConverterTest.kt` | 合法命名、转换结果和非法输入 |
| `FlutterProjectPathResolverTest.kt` | `lib` 和 Route 路径推导 |
| `GetXProjectScannerTest.kt` | Dart PSI 扫描和候选识别 |
| `GetXRouteWriterTest.kt` | 新建、追加、最小修改和 Undo 相关行为 |

当前共 15 个 GetX 测试，最近一次执行全部通过。

## 11. 构建与验证

项目当前的 Gradle Wrapper 缺少 `gradle-wrapper.jar`，因此直接执行 `./gradlew` 会失败。当前可使用本机缓存的 Gradle 7.6：

```bash
GRADLE_BIN=/Users/guoql/.gradle/wrapper/dists/gradle-7.6-bin/9l9tetv7ltxvx3i8an4pb86ye/gradle-7.6/bin/gradle
```

运行测试：

```bash
"$GRADLE_BIN" test --no-daemon
```

构建指定版本插件：

```bash
"$GRADLE_BIN" buildPlugin --no-daemon -PpluginVersion=1.0.4
```

当前插件包：

```text
/Users/guoql/Downloads/xPlugin-1.0.4.zip
```

构建目录中的原始产物：

```text
/Users/guoql/codeRepo/github/xPlugin/build/distributions/xPlugin-1.0.4.zip
```

`build.gradle` 当前关键配置：

- Kotlin：1.9.23
- Gradle IntelliJ Plugin：1.17.3
- IntelliJ Platform：IC 2024.1
- Dart 插件：`Dart:241.15845`
- JUnit：4.13.2
- JVM target：17
- `sinceBuild`：241
- `untilBuild`：253.*
- 源码默认版本仍为 1.0.1，打包时可通过 `-PpluginVersion` 覆盖

最近一次目标验证结果：

- `git diff --check`：通过。
- GetX 自动化测试：15 个全部通过。
- 示例 Flutter 项目 `dart analyze`：通过，无问题。
- 插件包 1.0.4：构建成功。

`verifyPlugin` 仍会报告两个已有的插件元数据问题，与 GetX 生成逻辑无关：

- 插件名称包含 `plugin`。
- 插件描述少于 40 个字符。

## 12. 示例项目与验收样例

用于验证 Binding 追加和格式规范的示例文件：

```text
/Users/guoql/codeRepo/offcn/new--tk/ai_tiku_flutter/lib/bindings/tk_course_binding.dart
```

该文件体现了以下最终决策：

- 一个 Binding 文件可以包含多个 Binding class。
- 每个 Binding class 都有独立中文注释。
- `_registerRepository()` 有用途说明。
- `Get.find<TkApi>()`、`Get.find<CourseRepository>()` 等内部调用不带最后一位逗号。
- 外层 `Get.lazyPut` 的最后一个参数也不带逗号。
- 文件可通过 `dart analyze`。

## 13. 当前明确不属于 V1 的能力

以下内容不要误认为已经实现：

- 自动修改 GetX 全局路由表。
- 自动生成 route constants 或 GetPage 列表。
- 自动推断项目任意 API 基类。
- 为多个 Repository 自动生成复杂 Controller 构造函数。
- 可视化配置模板风格或全局持久化设置。
- 自动移动、重命名或删除已有 Route。
- 跨 Flutter module 选择目标。
- 对整个已有 Binding 文件做统一格式化或 imports 排序。

其中“GetX Router”是功能名称，但 V1 的核心是 Page、Controller、Repository、Binding 脚手架生成，不应在没有新需求确认时擅自扩展为自动编辑全局路由注册。

## 14. 已知限制与技术债

### 14.1 API 类型仍是项目约定

当前 Repository 模板和注册逻辑以项目中的 `TkApi` 为既定依赖，并引用对应 API 文件。若未来支持不同项目，建议将 API 类型和 import 变成：

- Dialog 可选择项；或
- 项目级配置；或
- PSI 扫描并推荐。

调整前需要先确认默认策略，避免破坏现有 `tk` 项目的便捷性。

### 14.2 Gradle Wrapper 不完整

仓库缺少 `gradle-wrapper.jar`，导致标准 `./gradlew` 流程不可用。后续可以单独修复，但这属于构建基础设施变更，应与 GetX 功能修改分开提交。

### 14.3 版本默认值未同步

源码默认插件版本仍为 1.0.1，目前 1.0.4 依赖构建参数覆盖。后续应确认版本管理规则，再决定是否更新默认版本或引入统一的版本来源。

### 14.4 插件元数据告警

插件名和描述不满足 JetBrains Marketplace 的推荐规则。若准备正式发布，需要单独处理，但不要在普通 GetX 功能迭代中顺手改品牌信息。

### 14.5 IDE 兼容范围

当前声明兼容到 `253.*`，并针对 Android Studio 2025.3 的菜单上下文问题做过兜底。升级 IntelliJ Platform 或 Gradle IntelliJ Plugin 时，需要重点回归：

- New 菜单中的 Action 可见性与点击状态。
- Dart PSI API 是否变化。
- Dialog UI API 是否废弃。
- CodeStyleManager 的局部格式化行为。
- WriteCommandAction 和 Undo 行为。

## 15. 后续扩展建议

以下只是建议路线，不是已确认需求。每项都应先与用户确认，再实施。

### 15.1 优先级 A：完善现有能力

- 增加更多端到端 Fixture，覆盖复杂 Binding 文件和 import 冲突。
- 增加已有 Controller 多参数、可选参数和命名参数的兼容性测试。
- 补充 Dialog 联动状态测试。
- 明确并统一错误提示文案。
- 恢复 Gradle Wrapper，固定可复现构建流程。

### 15.2 优先级 B：项目级可配置

- API 类型与 import 可配置。
- 默认前缀可配置。
- Repository、Binding 目录可配置。
- Page 基类或公共 Widget 模板可配置。
- 是否生成 `widgets/` 的默认值可配置。

配置应优先存为项目级设置，避免不同 Flutter 项目互相污染。

### 15.3 优先级 C：路由注册

如要真正扩展全局 GetX Router 注册，建议先设计独立能力：

- 识别项目当前路由表结构。
- 区分 routes 常量、GetPage、Bindings 的组织方式。
- 支持预览将修改的具体位置。
- 使用 PSI 做结构化写入。
- 保持最小 diff 和一次 Undo。
- 对重复 route name、重复 path、重复 page 做冲突校验。

不要直接用字符串搜索向任意 Dart 文件末尾追加路由。

### 15.4 优先级 D：多依赖与更复杂架构

- Controller 注入多个 Repository。
- Repository 注入多个 API 或 DataSource。
- 支持抽象 Repository 与实现类。
- 支持按 feature 放置 Binding / Repository。
- 支持团队自定义模板包。

这一阶段会改变当前数据模型和 Dialog 复杂度，适合先单独产出设计文档。

## 16. 后续修改必须遵守的边界

交给 GPT 继续开发时，建议明确以下不可破坏项：

1. GetX 主代码继续放在 `com.frame.xplugins.getx`。
2. 不为实现 GetX 功能重构原有 MVVM / MVI 生成器。
3. 使用 Dart PSI 识别结构，不以文件名或正则替代核心语义判断。
4. Dialog 打开时可扫描一次，但写入前必须重新扫描和校验。
5. 已有 Binding 只做最小范围修改。
6. 不整体格式化已有 Binding，不自动重排 imports。
7. 一次生成保持为一个 Write Command，支持一次 Undo。
8. 所有嵌套层级的最后一位逗号都要去掉。
9. 多个 Binding class 必须分别提供明确中文注释。
10. 新增模板或模式必须同步补测试和预览逻辑。
11. 修改前检查工作区，保留用户已有未提交更改。
12. 验证至少包括 `git diff --check`、目标测试和插件构建。

## 17. 建议的 GPT 接手流程

```text
1. 完整阅读本文和 getx_route_generator_requirements_v1.md。
2. 扫描当前 git 状态、GetX 包、模板、plugin.xml、build.gradle 和测试。
3. 对照本文确认实际代码是否已经变化，不把本文当作替代源码的事实来源。
4. 复述新需求影响的模式、文件和兼容风险。
5. 先给出最小改动方案与验证计划，等待用户确认。
6. 按小步骤实现，每一步都保持可编译、可回归。
7. 运行目标测试、git diff --check 和 buildPlugin。
8. 如涉及生成的 Dart，使用真实 Flutter 样例执行 dart analyze。
9. 最后列出改动文件、行为变化、产物路径和未解决限制。
```

## 18. 可直接复制给 GPT 的开场 Prompt

```text
这是一个 IntelliJ IDEA / Android Studio 插件项目，用来生成 Android、Flutter 脚手架代码。

请先完整阅读：
1. docs/getx_route_generator_handoff.md
2. getx_route_generator_requirements_v1.md
3. src/main/kotlin/com/frame/xplugins/getx 下的现有实现
4. src/main/resources/code/getx 下的模板
5. src/test/kotlin/com/frame/xplugins/getx 下的测试

先检查 git 状态并保护已有修改。不要立即大规模改代码。

请先输出：
1. 你对当前 GetX Route V1 行为的理解
2. 新需求与现有实现的差异
3. 最小改动方案
4. 预计新增和修改的文件
5. 风险、兼容性和测试计划

必须保留以下约束：
- GetX 主代码独立维护在 com.frame.xplugins.getx
- Dart 结构识别使用 PSI
- 写入前重新扫描和校验
- 已有 Binding 只做最小范围追加
- 整次操作支持一次 Undo
- 所有参数列表最后一项不加 trailing comma，包括嵌套 Get.find
- 每个 Binding class 都有独立中文 Dart 文档注释

等待我确认方案后再执行修改。

本次的新需求是：
【在这里填写下一阶段需求】
```

## 19. 快速索引

| 内容 | 路径 |
| --- | --- |
| 原始需求 | `/Users/guoql/Downloads/getx_route_generator_requirements_v1.md` |
| 插件配置 | `src/main/resources/META-INF/plugin.xml` |
| 构建配置 | `build.gradle` |
| GetX Kotlin 主代码 | `src/main/kotlin/com/frame/xplugins/getx` |
| GetX Dart 模板 | `src/main/resources/code/getx` |
| GetX 测试 | `src/test/kotlin/com/frame/xplugins/getx` |
| Flutter Binding 样例 | `/Users/guoql/codeRepo/offcn/new--tk/ai_tiku_flutter/lib/bindings/tk_course_binding.dart` |
| 当前插件包 | `/Users/guoql/Downloads/xPlugin-1.0.4.zip` |

---

维护本文时，请把“已实现事实”“已确认规则”和“未来建议”分开描述，避免后续 GPT 将候选方案误判为当前行为。

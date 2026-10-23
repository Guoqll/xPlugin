{{IMPORTS}}

/// {{PAGE_CLASS}} 使用 Navigator 页面级依赖作用域展示数据状态。
class {{PAGE_CLASS}} extends StatefulWidget {
  const {{PAGE_CLASS}}({super.key});

  @override
  State<{{PAGE_CLASS}}> createState() => _{{PAGE_CLASS}}State();
}

class _{{PAGE_CLASS}}State extends State<{{PAGE_CLASS}}>
    with PageControllerMixin<{{PAGE_CLASS}}, {{CONTROLLER_CLASS}}>{{BUSINESS_MIXIN_COMPOSITION}} {
  @override
  {{CONTROLLER_CLASS}} createController() {
{{CONTROLLER_CREATION_BODY}}
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: PageRefreshView(
        enabled: controller.enableRefresh,
        onRefresh: controller.onRefresh,
        child: Obx(
          () => PageStateView(
            state: controller.pageState.value,
            onRetry: controller.loadData,
            builder: _buildContent,
          ),
        ),
      ),
    );
  }

  Widget _buildContent(BuildContext context) {
{{CONTENT_BODY}}
  }
}

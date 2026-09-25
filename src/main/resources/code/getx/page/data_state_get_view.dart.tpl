import 'package:flutter/material.dart';
import 'package:get/get.dart';

import '{{PAGE_STATE_WIDGETS_IMPORT}}';
import '{{CONTROLLER_IMPORT}}';

/// {{PAGE_CLASS}} 数据状态路由页面。
class {{PAGE_CLASS}} extends GetView<{{CONTROLLER_CLASS}}> {
  const {{PAGE_CLASS}}({super.key});

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

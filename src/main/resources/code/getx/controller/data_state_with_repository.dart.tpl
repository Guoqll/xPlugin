import 'package:flutter/foundation.dart';
import 'package:get/get.dart';

import '{{PAGE_STATE_MIXIN_IMPORT}}';
import '{{REPOSITORY_IMPORT}}';

/// {{CONTROLLER_CLASS}} 负责当前路由的页面状态与业务逻辑。
class {{CONTROLLER_CLASS}} extends GetxController with PageStateMixin {
  final {{REPOSITORY_CLASS}} _repository;

  {{CONTROLLER_CLASS}}(this._repository);

  bool get enableRefresh => {{ENABLE_REFRESH}};

{{BUSINESS_STATE}}
  /// Controller 创建时调用。
  /// 适合初始化变量、读取路由参数、注册监听等。
  @override
  void onInit() {
    super.onInit();
  }

  /// 页面首次渲染完成后调用。
  /// 默认在这里加载页面数据。
  @override
  void onReady() {
    super.onReady();

    loadData();
  }

  /// Controller 销毁前调用。
  /// Timer、Worker、Stream 等资源应在这里释放。
  @override
  void onClose() {
    super.onClose();
  }

  /// 首次加载页面数据。
  Future<void> loadData() async {
    showPageLoading();

    try {
{{LOAD_DATA_BODY}}
      showPageSuccess();
    } catch (e, stackTrace) {
      debugPrint('加载页面数据失败: $e\n$stackTrace');
      showPageError('加载失败，请稍后重试');
    }
  }

  /// 下拉刷新页面数据。
  Future<void> onRefresh() async {
    try {
{{REFRESH_BODY}}
      showPageSuccess();
    } catch (e, stackTrace) {
      debugPrint('刷新页面数据失败: $e\n$stackTrace');
    }
  }
{{PRIVATE_HELPERS}}
}

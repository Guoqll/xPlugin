import 'package:flutter/widgets.dart';
import 'package:get/get.dart';

/// 为每个 StatefulWidget 页面实例管理独立的 GetX Controller 生命周期。
mixin PageControllerMixin<W extends StatefulWidget, C extends GetxController> on State<W> {
  late final C _controller;
  late final String _controllerTag;

  C get controller => _controller;

  @protected
  String get controllerTag => _controllerTag;

  @protected
  C createController();

  @mustCallSuper
  @override
  void initState() {
    super.initState();

    _controllerTag = '$C-${UniqueKey()}';
    _controller = Get.put<C>(
      createController(),
      tag: _controllerTag,
    );
  }

  @mustCallSuper
  @override
  void dispose() {
    if (Get.isRegistered<C>(tag: _controllerTag)) {
      Get.delete<C>(tag: _controllerTag);
    }

    super.dispose();
  }
}

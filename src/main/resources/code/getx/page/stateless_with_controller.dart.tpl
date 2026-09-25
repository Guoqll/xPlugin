import 'package:flutter/material.dart';
import 'package:get/get.dart';

import '{{CONTROLLER_IMPORT}}';

/// {{PAGE_CLASS}} 路由页面。
class {{PAGE_CLASS}} extends StatelessWidget {
  const {{PAGE_CLASS}}({super.key});

  {{CONTROLLER_CLASS}} get controller => Get.find<{{CONTROLLER_CLASS}}>();

  @override
  Widget build(BuildContext context) {
    return const Scaffold(body: SizedBox());
  }
}

import 'package:flutter/material.dart';
import 'package:get/get.dart';

import '{{CONTROLLER_IMPORT}}';

{{BUSINESS_MIXIN_IMPORT}}/// {{PAGE_CLASS}} 路由页面。
class {{PAGE_CLASS}} extends StatefulWidget {
  const {{PAGE_CLASS}}({super.key});

  @override
  State<{{PAGE_CLASS}}> createState() => _{{PAGE_CLASS}}State();
}

class _{{PAGE_CLASS}}State extends State<{{PAGE_CLASS}}>{{BUSINESS_MIXIN_DECLARATION}} {
  {{CONTROLLER_CLASS}} get controller => Get.find<{{CONTROLLER_CLASS}}>();

  @override
  Widget build(BuildContext context) {
    return const Scaffold(body: SizedBox());
  }
}

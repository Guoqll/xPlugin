import 'package:flutter/material.dart';

{{BUSINESS_MIXIN_IMPORT}}/// {{PAGE_CLASS}} 路由页面。
class {{PAGE_CLASS}} extends StatefulWidget {
  const {{PAGE_CLASS}}({super.key});

  @override
  State<{{PAGE_CLASS}}> createState() => _{{PAGE_CLASS}}State();
}

class _{{PAGE_CLASS}}State extends State<{{PAGE_CLASS}}>{{BUSINESS_MIXIN_DECLARATION}} {
  @override
  Widget build(BuildContext context) {
    return const Scaffold(body: SizedBox());
  }
}

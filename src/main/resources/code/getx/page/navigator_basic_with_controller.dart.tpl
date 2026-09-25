{{IMPORTS}}

/// {{PAGE_CLASS}} 使用 Navigator 页面级依赖作用域。
class {{PAGE_CLASS}} extends StatefulWidget {
  const {{PAGE_CLASS}}({super.key});

  @override
  State<{{PAGE_CLASS}}> createState() => _{{PAGE_CLASS}}State();
}

class _{{PAGE_CLASS}}State extends State<{{PAGE_CLASS}}>
    with PageControllerMixin<{{PAGE_CLASS}}, {{CONTROLLER_CLASS}}> {
  @override
  {{CONTROLLER_CLASS}} createController() {
{{CONTROLLER_CREATION_BODY}}
  }

  @override
  Widget build(BuildContext context) {
    return const Scaffold(body: SizedBox());
  }
}

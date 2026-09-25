/// [{{PAGE_CLASS}}] 的依赖绑定。
class {{BINDING_CLASS}} extends Bindings {
  @override
  void dependencies() {
    Get.lazyPut<{{CONTROLLER_CLASS}}>({{CONTROLLER_CLASS}}.new);
  }
}

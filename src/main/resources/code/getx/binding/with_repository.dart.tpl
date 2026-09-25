/// [{{PAGE_CLASS}}] 的依赖绑定。
class {{BINDING_CLASS}} extends Bindings {
  @override
  void dependencies() {
    {{REGISTER_REPOSITORY_METHOD}}();

    Get.lazyPut<{{CONTROLLER_CLASS}}>(
      () => {{CONTROLLER_CLASS}}(Get.find<{{REPOSITORY_CLASS}}>()),
    );
  }
}

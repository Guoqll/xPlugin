/// 注册并复用 [{{REPOSITORY_CLASS}}]，供当前 Binding 文件中的多个路由使用。
void {{REGISTER_REPOSITORY_METHOD}}() {
  if (Get.isRegistered<{{REPOSITORY_CLASS}}>()) {
    return;
  }

  Get.lazyPut<{{REPOSITORY_CLASS}}>(
    () => {{REPOSITORY_EXPRESSION}},
    fenix: true,
  );
}

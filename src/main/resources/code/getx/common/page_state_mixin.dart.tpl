import 'package:get/get.dart';

/// 页面基础加载状态。
enum PageStatus {
  loading,
  success,
  error,
}

/// 页面基础状态，只描述加载、成功和错误。
class PageState {
  const PageState._(this.status, {this.errorMessage});

  const PageState.loading() : this._(PageStatus.loading);

  const PageState.success() : this._(PageStatus.success);

  const PageState.error([String? message])
      : this._(PageStatus.error, errorMessage: message);

  final PageStatus status;
  final String? errorMessage;
}

/// 为页面 Controller 提供基础状态切换能力。
mixin PageStateMixin on GetxController {
  final pageState = const PageState.loading().obs;

  void showPageLoading() {
    pageState.value = const PageState.loading();
  }

  void showPageSuccess() {
    pageState.value = const PageState.success();
  }

  void showPageError([String? message]) {
    pageState.value = PageState.error(message);
  }
}

import 'example_state.dart';
import 'package:flutter/cupertino.dart';
import '../../models/banner_entity.dart';
import '../../net/repertory/wan_repertory.dart';
import '../../../core/mixin/net_mixin.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

part 'example_view_model.g.dart';

/*@riverpod
WanRepertory wanRepertory(WanRepertoryRef ref) {
  return WanRepertory();
}*/

@riverpod
class ExampleViewModel extends _$ExampleViewModel with NetMixin {
  WanRepertory get _repo => ref.read(wanRepertoryProvider);

  @override
  ExampleState build(String pageId) {
    ref.onDispose(() {
      //自动清理资源
      cancelAllRequests();
      debugPrint('>>>>>>>>>>>>>>>>>>>>>>ExampleViewModel($pageId) disposed, requests cancelled.');
    });

    // 执行初始化操作
    Future.microtask(() => fetchDataSimple());

    return const ExampleState();
  }

  /// =========== 业务逻辑方法 ===========
  void increment() {
    state = state.copyWith(
      count: state.count + 1,
      lastUpdated: DateTime.now().millisecondsSinceEpoch,
    );
  }

  /// =========== 页面特定逻辑 ===========
  void fetchDataSimple() {
    const key = 'banner';

    state = state.copyWith(bannerAsyncValue: const AsyncValue.loading());

    // 直接调用 Mixin 中的 executeRequest
    executeRequest<BannerEntity>(
      key: key,
      request: (token) => _repo.fetchBanner('', cancelToken: token),
      onSuccess: (res) {
        state = state.copyWith(bannerAsyncValue: AsyncValue.data(res));
        debugPrint('>>>>>>>>>>>>>>>>>>>>>>fetch-banner-on-success');
      },
      onError: (error) {
        state = state.copyWith(bannerAsyncValue: AsyncValue.error(error, StackTrace.current));
        debugPrint('>>>>>>>>>>>>>>>>>>>>>>fetch-banner-on-error');
      },
      onFinally: () {
        debugPrint('>>>>>>>>>>>>>>>>>>>>>>fetch-banner-on-finally');
      },
    );
  }
}

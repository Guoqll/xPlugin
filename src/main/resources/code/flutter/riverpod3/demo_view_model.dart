import 'demo_state.dart';
import 'package:flutter/cupertino.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

part 'demo_view_model.g.dart';

@riverpod
class DemoViewModel extends _$DemoViewModel {
  /// 1. build 方法现在是异步的，本身就是初始化的入口
  @override
  FutureOr<DemoState> build(String pageId) async {
    ref.onDispose(() {
      debugPrint('>>>>>>>>>>>>>>>>>>>>>>DemoViewModel($pageId) disposed.');
    });

    // 直接在此处进行初始数据请求，Riverpod 会自动将状态置为 AsyncLoading
    return await _fetchData();
  }

  /// 模拟获取数据 (在更严格的架构中，这里应该调用 Repository)
  Future<DemoState> _fetchData() async {
    await Future.delayed(const Duration(seconds: 1));
    return DemoState(lastUpdated: DateTime.now().millisecondsSinceEpoch);
  }

  /// =========== 业务逻辑方法 ===========

  /// 增加计数
  void increment() {
    // 因为 state 是 AsyncValue<DemoState>，需要获取当前数据并更新
    final currentState = state.valueOrNull;
    if (currentState != null) {
      // 使用 AsyncData 包装新状态
      state = AsyncData(currentState.copyWith(
        count: currentState.count + 1,
        lastUpdated: DateTime.now().millisecondsSinceEpoch,
      ));
    }
  }

  /// 刷新数据
  Future<void> refresh() async {
    // 设置为加载中，但保留之前的数据 (hasValue 依然为 true，这样可以避免 UI 闪烁)
    state = const AsyncLoading<DemoState>().copyWithPrevious(state);

    // 使用 AsyncValue.guard 自动捕获异常并转换为 AsyncError 状态
    state = await AsyncValue.guard(() async {
      return await _fetchData();
    });

    // ✨ 3.0 优化：安全的异步状态更新
    // 类似于 Context.mounted，防止 Provider 已被 Dispose 但还在强行赋值引发 Crash
    /*if (ref.mounted) {
      state = result;
    }*/
  }
}

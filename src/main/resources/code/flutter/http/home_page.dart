/*
 *        小猫咪出没，永无BUG！
 *           /\_/\
 *          ( o.o )
 *          > ^ <
 *         /       \
 *        |         |
 *        |         |
 *        |         |
 */

import '^simple^_state.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

class ^Simple^Page extends ConsumerStatefulWidget {
  final String? title;

  const ^Simple^Page({super.key, this.title});

  @override
  ConsumerState<^Simple^Page> createState() => _^Simple^PageState();
}

class _^Simple^PageState extends ConsumerState<^Simple^Page> {
  @override
  Widget build(BuildContext context) {
    final AsyncValue<^Simple^Model> model = ref.watch(^simple^StateProvider);
    return Center(
      child: switch (model) {
        AsyncData(:final value) => _loadWidgetByData(value),
        AsyncError() => _loadWidgetByHttpError(),
        _ => _loadingWidget(),
      },
    );
  }

  ///加载框
  Widget _loadingWidget() {
    return CircularProgressIndicator(
      valueColor: AlwaysStoppedAnimation<Color>(Theme.of(context).colorScheme.inversePrimary),
    );
  }

  ///加载网络异常界面布局
  Widget _loadWidgetByHttpError() {
    return const Text('Oops, something unexpected happened');
  }

  ///根据服务端数据加载界面布局
  Widget _loadWidgetByData(^Simple^Model value) {
    return Text('ok===>${value.bannerList.length}');
  }
}

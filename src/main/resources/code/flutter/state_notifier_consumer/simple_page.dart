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

  _handlerState(^Simple^Model? previous, ^Simple^Model next) {
      switch (next.mStateType) {
          case ^Simple^StateType.defaultState:
          break;
          }
  }

  @override
  Widget build(BuildContext context) {
      final ^Simple^Model model = ref.watch(^simple^Provider);
      ref.listen(^simple^Provider, (previous, next) {
          _handlerState(previous, next);
      });
      return Scaffold(
        appBar: AppBar(
          title: Text(widget.title ?? "^Simple^ Page"),
          backgroundColor: Theme.of(context).colorScheme.inversePrimary,
        ),
        body: Center(
          child: Text("this is ^Simple^ Page \n count is： ${model.count}"),
        ),
        floatingActionButton: FloatingActionButton(
          child: const Icon(Icons.add),
          onPressed: () => {ref.read(^simple^Provider.notifier).increment()},
        ),
      );
  }
}

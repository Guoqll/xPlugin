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

class _^Simple^PageState extends ConsumerState<^Simple^Page> with ^Simple^Controller{

  _handlerState() {
      ref.listen(^simple^Provider, (previous, next) {
          switch (next.mStateType) {
              case ^Simple^StateType.defaultState:
              break;
              case ^Simple^StateType.requestLoadingState:
              break;
              case ^Simple^StateType.requestSuccessState:
              break;
              case ^Simple^StateType.requestFailState:
              break;
          }
      });
  }

  @override
  Widget build(BuildContext context) {
      final ^Simple^Model model = ref.watch(^simple^Provider);
      _handlerState();
      return Scaffold(
        appBar: AppBar(
          title: Text(widget.title ?? "^Simple^ Page"),
          backgroundColor: Color(0xFFF2F2F3),
        ),
        body: _stateContent(model),
      );
  }

  Widget _stateContent(^Simple^Model model){
      return Center(
          child: switch (model.mStateType) {
                ^Simple^StateType.requestLoadingState => loadingWidget(),
                ^Simple^StateType.requestFailState => httpErrorWidget(),
                ^Simple^StateType.requestSuccessState => _realContentWidget(model.mStateResult),
                _ => Text("this is ^Simple^ Page"),
          },
        );
  }

  Widget _realContentWidget(^Simple^Model  value) {
      return const Text("this is ^Simple^ Page");
  }


}

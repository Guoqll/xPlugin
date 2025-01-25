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

import 'package:flutter_riverpod/flutter_riverpod.dart';

/// #、state type
enum ^Simple^StateType {
  defaultState,
  requestLoadingState,
  requestSuccessState,
  requestFailState,
}

/// 1、data model
class ^Simple^Model {

  ^Simple^StateType mStateType;
  dynamic mStateResult;

  ^Simple^Model({required this.mStateType, required this.mStateResult});

  ^Simple^Model copyWith({^Simple^StateType? stateType, dynamic? stateResult}) {
  return ^Simple^Model(
     mStateType: stateType ?? mStateType,
     mStateResult: stateResult ?? mStateResult,
    );
  }
}

/// 2、state notifier
class ^Simple^State extends StateNotifier<^Simple^Model> {
  ^Simple^State() : super(^Simple^Model(mStateType: ^Simple^StateType.defaultState,mStateResult: null));

  void recycle() {
    state = state.copyWith(stateType: ^Simple^StateType.defaultState);
  }
}

/// 3、provider
final ^simple^Provider = StateNotifierProvider<^Simple^State, ^Simple^Model>((ref) {
  return ^Simple^State();
});

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
}

/// 1、data model
class ^Simple^Model {
  int count;
  ^Simple^StateType mStateType;
  dynamic mStateResult;

  ^Simple^Model({required this.count, required this.mStateType,required this.mStateResult});

  ^Simple^Model copyWith({int? count,^Simple^StateType? stateType,dynamic? stateResult}) {
    return ^Simple^Model(
      count: count ?? this.count,
      mStateType: stateType ?? this.mStateType,
      mStateResult: stateResult ?? mStateResult,
    );
  }
}

/// 2、state notifier
class ^Simple^State extends StateNotifier<^Simple^Model> {
  ^Simple^State() : super(^Simple^Model(count: 0,mStateType: ^Simple^StateType.defaultState,mStateResult: null));

  void increment() {
    state = state.copyWith(count: state.count + 1);
  }

  void decrement() {
    state = state.copyWith(count: state.count - 1);
  }
}

/// 3、provider
final ^simple^Provider = StateNotifierProvider<^Simple^State, ^Simple^Model>((ref) {
  return ^Simple^State();
});

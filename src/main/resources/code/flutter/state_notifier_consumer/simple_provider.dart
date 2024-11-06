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

/// 1、data model
class ^Simple^Model {
  int count;

  ^Simple^Model({required this.count});

  ^Simple^Model copyWith({int? count}) {
    return ^Simple^Model(
      count: count ?? this.count,
    );
  }
}

/// 2、state notifier
class ^Simple^State extends StateNotifier<^Simple^Model> {
  ^Simple^State() : super(^Simple^Model(count: 0));

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

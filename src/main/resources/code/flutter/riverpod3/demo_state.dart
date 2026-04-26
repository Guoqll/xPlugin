import 'package:freezed_annotation/freezed_annotation.dart';

part 'demo_state.g.dart';
part 'demo_state.freezed.dart';

@freezed
class DemoState with _$DemoState {
  const factory DemoState({
    @Default(0) int count,
    @Default(-1) int lastUpdated,
  }) = _DemoState;

  factory DemoState.fromJson(Map<String, dynamic> json) => _$DemoStateFromJson(json);
}
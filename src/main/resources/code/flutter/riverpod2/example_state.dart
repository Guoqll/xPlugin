import '../../models/banner_entity.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

part 'example_state.g.dart';
part 'example_state.freezed.dart';

@freezed
class ExampleState with _$ExampleState {
  const factory ExampleState({
    @Default(0) int count,

    @JsonKey(includeFromJson: false)
    @Default(AsyncValue<BannerEntity?>.data(null))
    AsyncValue<BannerEntity?> bannerAsyncValue,

    @Default(-1) int lastUpdated,
  }) = _ExampleState;

  factory ExampleState.fromJson(Map<String, dynamic> json) => _$ExampleStateFromJson(json);
}

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
import 'package:riverpod_annotation/riverpod_annotation.dart';
import 'package:freezed_annotation/freezed_annotation.dart';
import 'package:offcn_book/models/banner_entity.dart';

import '../../http/http_result.dart';
import '../../http/wan_api.dart';
import '../../models/article_entity.dart';

///todo: must run the command: dart run build_runner watch
part '^simple^_state.freezed.dart';

part '^simple^_state.g.dart';

@freezed
class ^Simple^Model with _$^Simple^Model {
  factory ^Simple^Model({
    required List<BannerData> bannerList,
    required ArticleData articleData,
  }) = _^Simple^Model;
}

@riverpod
Future<^Simple^Model> ^simple^State(Ref ref) async {
  final result = await Future.wait([
    WanApi().getBannerData(),
    WanApi().getArticleList("0"),
  ]);

  HttpResult<List<BannerData>> bannerResult = result[0] as HttpResult<List<BannerData>>;
  HttpResult<ArticleData> articleResult = result[1] as HttpResult<ArticleData>;

  return ^Simple^Model(bannerList: bannerResult.data!, articleData: articleResult.data!);
}

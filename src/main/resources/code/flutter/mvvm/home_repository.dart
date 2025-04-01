import 'package:flutter_example/apis/wan_api.dart';

/// home-repository
abstract class HomeRepository {
  Future<dynamic> getBannerData();
}

/// home-repository-impl
class HomeRepositoryImpl extends HomeRepository {
  late WanApi _api;

  HomeRepositoryImpl() {
    _api = WanApi();
  }

  @override
  Future<dynamic> getBannerData() async {
    Future<dynamic> result = await _api.getBannerList();
    return result;
  }
}


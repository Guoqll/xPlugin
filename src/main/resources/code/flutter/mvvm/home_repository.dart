
/// home-repository
abstract class HomeRepository {
  Future<dynamic> getBannerData();
}

/// home-repository-impl
class HomeRepositoryImpl extends HomeRepository {
  late SimpleApi _api;

  HomeRepositoryImpl() {
    _api = SimpleApi();
  }

  @override
  Future<dynamic> getBannerData() async {
    HttpResult<List<BannerData>> result = await _api.getAreaList();
    return result;
  }
}


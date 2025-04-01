import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import 'home_repository.dart';

/// home-state
part 'home_state.freezed.dart';

@freezed
class HomeState with _$HomeState {
  const factory HomeState({
    @Default(0) int counter,
    @Default(0) int selectedTab,
    @Default(false) bool isLoading,
    String? errorMessage,
  }) = _HomeState;
}

enum AppTab { home, explore, profile }

/// home-view model-provider
final homeViewModelProvider = StateNotifierProvider<HomeViewModel, HomeState>((ref) {
  return HomeViewModel();
});

class HomeViewModel extends StateNotifier<HomeState> {
  late HomeRepository _homeRepo;

  HomeViewModel() : super(const HomeState()) {
    _homeRepo = HomeRepositoryImpl();
  }

  void incrementCounter() {
    state = state.copyWith(counter: state.counter + 1);
  }

  void changeTab(AppTab newTab) {
    state = state.copyWith(selectedTab: state.selectedTab + 1);
  }

  Future<void> simulateLoading() async {
    state = state.copyWith(isLoading: true);
    await _homeRepo.getBannerData();
    state = state.copyWith(isLoading: false);
  }
}

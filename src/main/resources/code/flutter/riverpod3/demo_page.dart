import 'demo_view_model.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

class DemoPage extends ConsumerStatefulWidget {
  final String title;
  final String pageId;

  const DemoPage({super.key, required this.pageId, this.title = 'demo-page'});

  @override
  ConsumerState<DemoPage> createState() => _DemoPageState();
}

class _DemoPageState extends ConsumerState<DemoPage> {
  /// 1. 统一获取 Provider 实例
  DemoViewModelProvider get _provider => demoViewModelProvider(widget.pageId);

  @override
  Widget build(BuildContext context) {
    // 监听全局状态，交由 .when() 处理
    final asyncState = ref.watch(_provider);

    return Scaffold(
      appBar: AppBar(
        title: Text(widget.title),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            // 【写数据】：触发全局刷新
            onPressed: () => ref.read(_provider.notifier).refresh(),
          ),
        ],
      ),

      /// 2. 拥抱 Riverpod 原生的异步状态处理
      body: asyncState.when(
        // skipLoadingOnReload: true 保证静默刷新时(有旧数据)，不会闪烁全屏 Loading
        skipLoadingOnReload: true,
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (error, stack) => Center(child: Text('Error: $error')),
        data: (_) => _buildContent(),
      ),
      floatingActionButton: FloatingActionButton(
        // 【写数据】：触发业务逻辑操作
        onPressed: () => ref.read(_provider.notifier).increment(),
        child: const Icon(Icons.add),
      ),
    );
  }

  /// 3. 主体内容区
  Widget _buildContent() {
    return Center(
      // 使用局部的 Consumer 配合 select
      child: Consumer(
        builder: (context, ref, _) {
          // 【读数据】：实现极致的局部刷新，只监听 count
          final count = ref.watch(_provider.select((s) => s.valueOrNull?.count ?? 0));

          return Text(
            'Count: $count',
            style: Theme.of(context).textTheme.headlineMedium?.copyWith(
                  fontWeight: FontWeight.bold,
                ),
          );
        },
      ),
    );
  }
}

import 'package:flutter/material.dart';

import '../../mixins/page_state_mixin.dart';

/// 根据页面基础状态显示 Loading、Error 或成功内容。
class PageStateView extends StatelessWidget {
  const PageStateView({
    super.key,
    required this.state,
    required this.builder,
    this.onRetry,
  });

  final PageState state;
  final WidgetBuilder builder;
  final Future<void> Function()? onRetry;

  @override
  Widget build(BuildContext context) {
    return switch (state.status) {
      PageStatus.loading => const PageLoadingView(),
      PageStatus.error => PageErrorView(
          message: state.errorMessage,
          onRetry: onRetry,
        ),
      PageStatus.success => builder(context),
    };
  }
}

/// 页面加载中的默认展示。
class PageLoadingView extends StatelessWidget {
  const PageLoadingView({super.key});

  @override
  Widget build(BuildContext context) {
    return const Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          SizedBox(
            width: 28,
            height: 28,
            child: CircularProgressIndicator(strokeWidth: 2.4),
          ),
          SizedBox(height: 12),
          Text('正在加载...'),
        ],
      ),
    );
  }
}

/// 页面加载失败时的默认展示。
class PageErrorView extends StatelessWidget {
  const PageErrorView({
    super.key,
    this.message,
    this.onRetry,
  });

  final String? message;
  final Future<void> Function()? onRetry;

  @override
  Widget build(BuildContext context) {
    return PagePlaceholderView(
      icon: Icons.error_outline,
      title: '加载失败',
      message: message,
      actionText: onRetry == null ? null : '重新加载',
      onAction: onRetry,
    );
  }
}

/// 通用占位展示，具体显示时机由业务页面决定。
class PagePlaceholderView extends StatelessWidget {
  const PagePlaceholderView({
    super.key,
    required this.icon,
    required this.title,
    this.message,
    this.actionText,
    this.onAction,
  });

  final IconData icon;
  final String title;
  final String? message;
  final String? actionText;
  final Future<void> Function()? onAction;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, size: 48, color: Theme.of(context).colorScheme.outline),
            const SizedBox(height: 16),
            Text(title, style: Theme.of(context).textTheme.titleMedium),
            if (message != null) ...[
              const SizedBox(height: 8),
              Text(
                message!,
                textAlign: TextAlign.center,
                style: Theme.of(context).textTheme.bodyMedium,
              ),
            ],
            if (actionText != null && onAction != null) ...[
              const SizedBox(height: 16),
              FilledButton(onPressed: onAction, child: Text(actionText!)),
            ],
          ],
        ),
      ),
    );
  }
}

/// 使用 Flutter 原生 RefreshIndicator 提供可选下拉刷新能力。
class PageRefreshView extends StatelessWidget {
  const PageRefreshView({
    super.key,
    required this.onRefresh,
    required this.child,
    this.enabled = true,
  });

  final bool enabled;
  final Future<void> Function() onRefresh;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    if (!enabled) {
      return child;
    }

    return RefreshIndicator(
      onRefresh: onRefresh,
      child: child,
    );
  }
}

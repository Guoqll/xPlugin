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

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

class ^Simple^Page extends StatefulWidget {
  final String? title;

  const ^Simple^Page({super.key, this.title});

  @override
  State<^Simple^Page> createState() => _^Simple^PageState();
}

class _^Simple^PageState extends State<^Simple^Page> {
  String title = "";
  final ^Simple^ViewModel mViewModel = ^Simple^ViewModel();

  @override
  void initState() {
    super.initState();
    if (widget.title != null) {
      title = widget.title!;
    } else {
      title = "this is ^Simple^Page";
    }
  }

  @override
  Widget build(BuildContext context) {
    return ChangeNotifierProvider(
        create: (context) => mViewModel,
        child: Scaffold(
            appBar: AppBar(
              title: Text(title),
              backgroundColor: Theme.of(context).colorScheme.inversePrimary,
            ),
            body: Consumer<^Simple^ViewModel>(builder: (context, model, child) {
              return Center(
                child: Text("$title \ncount is： ${model.mData.count}"),
              );
            }),
            floatingActionButton: FloatingActionButton(
              child: const Icon(Icons.add),
              onPressed: () => mViewModel.updateCount(),
            )));
  }
}

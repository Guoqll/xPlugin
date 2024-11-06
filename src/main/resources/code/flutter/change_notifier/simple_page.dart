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

  final ^Simple^State mViewModel = ^Simple^State();

  @override
  Widget build(BuildContext context) {
    return ChangeNotifierProvider(
        create: (context) => mViewModel,
        child: Scaffold(
            appBar: AppBar(
              title: Text(widget.title ?? "^Simple^ Page"),
              backgroundColor: Theme.of(context).colorScheme.inversePrimary,
            ),
            body: Consumer<^Simple^State>(builder: (context, model, child) {
              return Center(
                child: Text("this is ^Simple^ Page \ncount is： ${model.mData.count}"),
              );
            }),
            floatingActionButton: FloatingActionButton(
              child: const Icon(Icons.add),
              onPressed: () => mViewModel.updateCount(),
            )));
  }
}

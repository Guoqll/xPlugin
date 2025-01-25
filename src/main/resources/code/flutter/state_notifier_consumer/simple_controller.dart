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
import 'package:flutter_riverpod/flutter_riverpod.dart';

mixin ^Simple^Controller on ConsumerState<^Simple^Page> {

  @override
  initState() {
    super.initState();
    WidgetsBinding.instance.endOfFrame.then((value) {
      ///todo: Load data after first frame
    });
  }

  @override
  void dispose() {
    super.dispose();
  }

  Widget loadingWidget() {
    return CircularProgressIndicator(
      valueColor: AlwaysStoppedAnimation<Color>(Theme.of(context).colorScheme.inversePrimary),
    );
  }

  Widget httpErrorWidget() {
    return const Text('Oops, something unexpected happened');
  }



}

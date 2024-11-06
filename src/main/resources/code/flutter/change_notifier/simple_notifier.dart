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

import 'package:flutter/cupertino.dart';

class ^Simple^Model {
  int count;

  ^Simple^Model(this.count);
}

class ^Simple^State extends ChangeNotifier {
  ^Simple^Model mData = ^Simple^Model(110);

  void updateCount() {
    mData.count++;
    notifyListeners();
  }
}

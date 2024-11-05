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
  final String name;
  int count;

  ^Simple^Model(this.name, this.count);
}

class ^Simple^ViewModel extends ChangeNotifier {
  ^Simple^Model mData = ^Simple^Model("Guoqll", 110);

  void updateCount() {
    mData.count++;
    notifyListeners();
  }
}

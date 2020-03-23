import 'dart:async';

import 'package:flutter/services.dart';

class Music {
  static const MethodChannel _channel =
      const MethodChannel('music');

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }
}

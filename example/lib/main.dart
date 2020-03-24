import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:music/music.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _platformVersion = 'Unknown';

  @override
  void initState() {
    super.initState();
    MusicWrapper.singleton.initState();
    MusicWrapper.singleton.getMusicStateStream().listen((event) {
      if (!mounted) return;
      setState(() {
        _platformVersion =
            'is Playing now position is ${event.data['position']}';
      });
    });
//    initPlatformState();
  }

  @override
  void dispose() {
    super.dispose();
    MusicWrapper.singleton.dispose();
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initPlatformState() async {
    String platformVersion;
    // Platform messages may fail, so we use a try/catch PlatformException.
    try {
      platformVersion = await MusicWrapper.singleton.playSong('111',
          "https://webfs.yun.kugou.com/202003241405/b115cd03e8cc3e479f7b5a2158546f1a/G164/M01/1F/09/RIcBAF1FXz6AImQhAC0SISFl4Mw962.mp3");
    } on PlatformException {
      platformVersion = 'Failed to play music.';
    }

    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
    if (!mounted) return;

    setState(() {
      _platformVersion = platformVersion;
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: GestureDetector(
          child: Center(
            child: Text('Running on: $_platformVersion\n'),
          ),
          onTap: () {
            initPlatformState();
          },
        ),
      ),
    );
  }
}

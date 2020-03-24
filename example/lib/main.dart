import 'dart:math';

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
  double progress = 0;
  double maxProgress = 1;
  String _duration = '0s';
  bool isChange = false;

  @override
  void initState() {
    super.initState();
    MusicWrapper.singleton.initState();
    MusicWrapper.singleton.getMusicStateStream().listen((event) {
      if (!mounted || isChange ) return;
      setState(() {
        maxProgress = event.data['duration'] * 1.0;
        progress = min(event.data['position'] * 1.0, maxProgress);
        _duration = '${progress~/1000}s';
        print('$progress   $maxProgress  $_duration');

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
        body: Center(
          child: Column(
            children: <Widget>[
              FlatButton(
                child: Text('点击播放'),
                onPressed: () {
                  initPlatformState();
                },
              ),
              Slider(
                value: progress,
                label: _duration,
                min: 0.0,
                max: maxProgress,
                divisions: 1000000,
                activeColor: Colors.blue,
                onChanged: (value){
                  setState(() {
                    progress = value;
                    _duration = '${progress~/1000}s';
                  });
                },onChangeEnd: (value){
                  setState(() {
                    MusicWrapper.singleton.seekTo(value.toInt());
                    isChange = false;
                  });
              },onChangeStart: (value){
                isChange = true;
              },
              )
            ],
          ),
        ),
      ),
    );
  }
}

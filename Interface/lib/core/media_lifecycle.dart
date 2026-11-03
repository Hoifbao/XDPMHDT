import 'dart:async';

import 'package:flutter/material.dart';

final mediaRouteObserver = RouteObserver<ModalRoute<void>>();

mixin MediaLifecycle<T extends StatefulWidget> on State<T>
    implements RouteAware {
  Future<void> pauseMedia();
  ModalRoute<void>? _mediaRoute;
  late final _MediaAppObserver _appObserver;

  @override
  void initState() {
    super.initState();
    _appObserver = _MediaAppObserver(pauseMedia);
    WidgetsBinding.instance.addObserver(_appObserver);
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    final route = ModalRoute.of<void>(context);
    if (route != _mediaRoute) {
      mediaRouteObserver.unsubscribe(this);
      _mediaRoute = route;
      if (route != null) mediaRouteObserver.subscribe(this, route);
    }
  }

  @override
  void didPushNext() {
    unawaited(pauseMedia());
  }

  @override
  void didPop() {
    unawaited(pauseMedia());
  }

  @override
  void didPush() {}
  @override
  void didPopNext() {}
  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(_appObserver);
    mediaRouteObserver.unsubscribe(this);
    super.dispose();
  }
}

class _MediaAppObserver extends WidgetsBindingObserver {
  final Future<void> Function() pause;
  _MediaAppObserver(this.pause);
  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state != AppLifecycleState.resumed) unawaited(pause());
  }
}

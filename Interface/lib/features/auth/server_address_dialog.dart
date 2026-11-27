import 'package:flutter/material.dart';

import 'auth_service.dart';

class ServerAddressDialog extends StatefulWidget {
  const ServerAddressDialog({super.key});

  @override
  State<ServerAddressDialog> createState() => _ServerAddressDialogState();
}

class _ServerAddressDialogState extends State<ServerAddressDialog> {
  final _address = TextEditingController(text: AuthService.baseUrl);
  Uri? _verified;
  String? _error;
  bool _busy = false;

  @override
  void dispose() {
    _address.dispose();
    super.dispose();
  }

  Future<void> _check() async {
    final address = AuthService.parseServerAddress(_address.text);
    if (address == null) {
      setState(() => _error = 'Nhập link HTTPS gốc của máy chủ.');
      return;
    }
    FocusScope.of(context).unfocus();
    setState(() {
      _busy = true;
      _error = null;
      _verified = null;
    });
    final connected = await AuthService.canConnectToServer(address);
    if (!mounted) return;
    setState(() {
      _busy = false;
      _verified = connected ? address : null;
      _error = connected
          ? null
          : 'Không kết nối được với Smart Heritage. Kiểm tra link và thử lại.';
    });
  }

  Future<void> _save() async {
    final address = _verified;
    if (address == null) return;
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      await AuthService.saveServerAddress(address);
      if (mounted) Navigator.of(context).pop(true);
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _busy = false;
        _error = 'Không lưu được địa chỉ. Hãy thử lại.';
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: const Text('Địa chỉ máy chủ'),
      scrollable: true,
      content: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          TextField(
            controller: _address,
            enabled: !_busy,
            keyboardType: TextInputType.url,
            autocorrect: false,
            decoration: InputDecoration(
              labelText: 'Link HTTPS',
              hintText: 'https://…',
              errorText: _error,
              errorMaxLines: 3,
            ),
            onChanged: (_) => setState(() {
              _verified = null;
              _error = null;
            }),
          ),
          if (_verified != null && _error == null)
            const Padding(
              padding: EdgeInsets.only(top: 10),
              child: Text('Kết nối thành công.'),
            ),
          if (_busy)
            const Padding(
              padding: EdgeInsets.only(top: 10),
              child: LinearProgressIndicator(),
            ),
        ],
      ),
      actions: [
        TextButton(
          onPressed: _busy ? null : () => Navigator.of(context).pop(),
          child: const Text('Hủy'),
        ),
        TextButton(
          onPressed: _busy ? null : _check,
          child: const Text('Kiểm tra kết nối'),
        ),
        FilledButton(
          onPressed: _busy || _verified == null ? null : _save,
          child: const Text('Lưu'),
        ),
      ],
    );
  }
}

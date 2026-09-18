import 'package:flutter_secure_storage/flutter_secure_storage.dart';

class SecureStore {
  const SecureStore();
  static const _storage = FlutterSecureStorage();
  static const tokenKey = 'vertal.jwt';
  static const privateKeyKey = 'vertal.private_key';
  static const publicKeyKey = 'vertal.public_key';
  static const deviceIdKey = 'vertal.device_id';
  static const serverKeyKey = 'vertal.server_key';
  Future<String?> read(String key) => _storage.read(key: key);
  Future<void> write(String key, String value) => _storage.write(key: key, value: value);
  Future<void> delete(String key) => _storage.delete(key: key);
}
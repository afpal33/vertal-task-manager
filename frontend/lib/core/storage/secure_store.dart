import 'package:flutter_secure_storage/flutter_secure_storage.dart';

class SecureStore {
  const SecureStore();
  static const androidOptions = AndroidOptions(
    keyCipherAlgorithm:
        KeyCipherAlgorithm.RSA_ECB_OAEPwithSHA_256andMGF1Padding,
    storageCipherAlgorithm: StorageCipherAlgorithm.AES_GCM_NoPadding,
  );
  static const _storage = FlutterSecureStorage(aOptions: androidOptions);
  static const tokenKey = 'vertal.jwt';
  static const privateKeyKey = 'vertal.private_key';
  static const publicKeyKey = 'vertal.public_key';
  static const deviceIdKey = 'vertal.device_id';
  static const serverKeyKey = 'vertal.server_key';
  Future<String?> read(String key) => _storage.read(key: key);
  Future<void> write(String key, String value) =>
      _storage.write(key: key, value: value);
  Future<void> delete(String key) => _storage.delete(key: key);
}

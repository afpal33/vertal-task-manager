import 'dart:io';

import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:vertal/core/network/api_client.dart';
import 'package:vertal/core/storage/secure_store.dart';

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();
  const verificationChannel = MethodChannel('vertal/privacy_verification');

  testWidgets('la clave privada se conserva mediante Android Keystore', (
    tester,
  ) async {
    const store = SecureStore();
    const value = 'private-key-rnf02-verification';
    await store.write(SecureStore.privateKeyKey, value);

    expect(await store.read(SecureStore.privateKeyKey), value);
    final aliases = await verificationChannel.invokeListMethod<String>(
      'secureStorageKeyAliases',
    );
    expect(aliases, isNotEmpty);

    await store.delete(SecureStore.privateKeyKey);
    expect(await store.read(SecureStore.privateKeyKey), isNull);
  });

  testWidgets('el cliente solo se comunica con el servidor configurado', (
    tester,
  ) async {
    var serverUrl = const String.fromEnvironment('RNF02_SERVER_URL');
    if (serverUrl.isEmpty) {
      final server = await HttpServer.bind(InternetAddress.loopbackIPv4, 0);
      addTearDown(() => server.close(force: true));
      server.listen((request) async {
        await request.drain<void>();
        request.response
          ..statusCode = HttpStatus.ok
          ..headers.contentType = ContentType.json
          ..write('{"status":"UP"}');
        await request.response.close();
      });
      serverUrl = 'http://${server.address.address}:${server.port}';
    }
    final client = ApiClient(baseUrl: serverUrl);

    await client.get('/actuator/health');
    await expectLater(
      client.get('https://telemetry.example/collect'),
      throwsA(isA<ApiException>()),
    );
  });
}

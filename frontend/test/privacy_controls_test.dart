import 'dart:io';

import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:vertal/core/network/api_client.dart';
import 'package:vertal/core/storage/secure_store.dart';

void main() {
  group('controles de privacidad', () {
    test('la clave privada usa cifrado respaldado por Android Keystore', () {
      final options = SecureStore.androidOptions.toMap();

      expect(
        options['keyCipherAlgorithm'],
        KeyCipherAlgorithm.RSA_ECB_OAEPwithSHA_256andMGF1Padding.name,
      );
      expect(
        options['storageCipherAlgorithm'],
        StorageCipherAlgorithm.AES_GCM_NoPadding.name,
      );
    });

    test(
      'el cliente bloquea destinos ajenos al servidor configurado',
      () async {
        var sent = false;
        final client = ApiClient(
          baseUrl: 'https://vertal.local',
          client: MockClient((_) async {
            sent = true;
            return http.Response('{}', 200);
          }),
        );

        await expectLater(
          client.get('https://telemetry.example/collect'),
          throwsA(isA<ApiException>()),
        );
        expect(sent, isFalse);
      },
    );

    test('el tráfico HTTP real se limita al servidor configurado', () async {
      final received = <String>[];
      final server = await HttpServer.bind(InternetAddress.loopbackIPv4, 0);
      addTearDown(() => server.close(force: true));
      server.listen((request) async {
        received.add('${request.method} ${request.uri.path}');
        await request.drain<void>();
        request.response
          ..statusCode = HttpStatus.ok
          ..headers.contentType = ContentType.json
          ..write('{}');
        await request.response.close();
      });
      final client = ApiClient(
        baseUrl: 'http://${server.address.address}:${server.port}',
      );

      await client.get('/api/tasks');
      await client.post('/api/teams', {'nombre': 'Privacidad'});
      await client.put('/api/tasks/1', {'titulo': 'Actualizada'});
      await client.delete('/api/tasks/1');
      await expectLater(
        client.get('https://telemetry.example/collect'),
        throwsA(isA<ApiException>()),
      );

      expect(received, [
        'GET /api/tasks',
        'POST /api/teams',
        'PUT /api/tasks/1',
        'DELETE /api/tasks/1',
      ]);
    });

    test('el cliente no sigue redirecciones del servidor', () async {
      final client = ApiClient(
        baseUrl: 'https://vertal.local',
        client: MockClient(
          (_) async => http.Response(
            '',
            302,
            headers: {'location': 'https://telemetry.example/collect'},
          ),
        ),
      );

      await expectLater(
        client.get('/api/tasks'),
        throwsA(
          isA<ApiException>().having(
            (error) => error.message,
            'message',
            contains('redirigir'),
          ),
        ),
      );
    });

    test('toda petición autenticada identifica el dispositivo', () async {
      late http.Request request;
      final client = ApiClient(
        baseUrl: 'https://vertal.local',
        token: 'jwt-test',
        client: MockClient((incoming) async {
          request = incoming;
          return http.Response('[]', 200);
        }),
      )..deviceId = 'device-test';

      await client.get('/api/tasks');

      expect(request.headers['x-device-id'], 'device-test');
    });

    test('los manifiestos no incluyen SDK de telemetría o analítica', () {
      final frontend = Directory.current;
      final repository = frontend.parent;
      final manifests = [
        File('${frontend.path}/pubspec.yaml').readAsStringSync(),
        File('${frontend.path}/pubspec.lock').readAsStringSync(),
        File('${repository.path}/backend/pom.xml').readAsStringSync(),
      ].join('\n').toLowerCase();
      const prohibited = [
        'firebase_analytics',
        'firebase-crashlytics',
        'sentry',
        'appcenter',
        'datadog',
        'newrelic',
        'new-relic',
        'amplitude',
        'mixpanel',
        'segment-analytics',
      ];

      for (final dependency in prohibited) {
        expect(manifests, isNot(contains(dependency)), reason: dependency);
      }
    });

    test('el código de producción centraliza las conexiones en ApiClient', () {
      final lib = Directory('${Directory.current.path}/lib');
      final networkUsers = lib
          .listSync(recursive: true)
          .whereType<File>()
          .where((file) => file.path.endsWith('.dart'))
          .where((file) {
            final source = file.readAsStringSync();
            return source.contains("package:http/") ||
                source.contains("import 'dart:io'") ||
                source.contains('import "dart:io"');
          })
          .map((file) => file.path.replaceFirst('${lib.path}/', ''))
          .toList();

      expect(networkUsers, ['core/network/api_client.dart']);
    });

    test('Android impide respaldar las credenciales del dispositivo', () {
      final manifest = File(
        '${Directory.current.path}/android/app/src/main/AndroidManifest.xml',
      ).readAsStringSync();

      expect(manifest, contains('android:allowBackup="false"'));
      expect(manifest, contains('android:fullBackupContent="false"'));
    });
  });
}

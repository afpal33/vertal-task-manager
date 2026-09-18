import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:vertal/core/network/api_client.dart';
import 'package:vertal/core/models/models.dart';
import 'package:vertal/core/security/credential_service.dart';

void main() {
  test('genera una credencial local con claves distintas', () async {
    final credential = await CredentialService().generate();

    expect(credential.privateKey, contains('PRIVATE KEY'));
    expect(credential.publicKey, contains('PUBLIC KEY'));
    expect(credential.privateKey, isNot(credential.publicKey));
  });

  test('interpreta roles y estados del contrato del backend', () {
    final user = User.fromJson({
      'id': 4,
      'nombreUsuario': 'maria',
      'nombreCompleto': 'Maria Lopez',
      'rol': 'MANAGER',
      'activo': true,
    });
    final task = Task.fromJson({
      'id': 8,
      'titulo': 'Revisar API',
      'descripcion': 'Validar endpoints',
      'estado': 'EN_PROGRESO',
      'equipoId': 2,
      'creadorId': 4,
    });

    expect(user.role, UserRole.manager);
    expect(task.status, TaskStatus.inProgress);
    expect(task.teamId, 2);
  });

  test('convierte errores REST en mensajes de API', () async {
    final client = ApiClient(
      baseUrl: 'http://test.local',
      client: MockClient((_) async => http.Response('{"message":"Acceso denegado"}', 403)),
    );

    expect(client.get('/api/tasks'), throwsA(isA<ApiException>().having((error) => error.message, 'message', 'Acceso denegado')));
  });

  test('envía JWT y cuerpo JSON en operaciones protegidas', () async {
    late http.Request request;
    final client = ApiClient(
      baseUrl: 'http://test.local',
      token: 'jwt-test',
      client: MockClient((incoming) async {
        request = incoming;
        return http.Response('{"id":1}', 201);
      }),
    );

    await client.post('/api/teams', {'nombre': 'Mobile'});

    expect(request.headers['authorization'], 'Bearer jwt-test');
    expect(request.headers['content-type'], contains('application/json'));
    expect(request.body, '{"nombre":"Mobile"}');
  });

  test('interpreta solicitud y recordatorio del backend', () {
    final request = LinkRequest.fromJson({
      'id': 3,
      'nombreUsuarioSolicitado': 'ana',
      'estado': 'PENDIENTE',
      'fechaSolicitud': '2026-09-18T12:00:00Z',
    });
    final reminder = Reminder.fromJson({
      'id': 5,
      'tareaId': 9,
      'usuarioId': 3,
      'fechaHora': '2026-09-19T12:00:00Z',
      'activo': true,
    });

    expect(request.status, 'PENDIENTE');
    expect(request.username, 'ana');
    expect(reminder.taskId, 9);
    expect(reminder.active, isTrue);
  });
}
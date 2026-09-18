import 'package:flutter/foundation.dart';
import 'config/app_config.dart';
import 'models/models.dart';
import 'network/api_client.dart';
import 'security/credential_service.dart';
import 'storage/secure_store.dart';
import 'notifications/notification_service.dart';

enum AppStage { welcome, credential, server, link, pending, login, ready }

class AppController extends ChangeNotifier {
  AppController({ApiClient? client, SecureStore? store}) : _store = store ?? const SecureStore(), _api = client ?? ApiClient(baseUrl: AppConfig.defaultBaseUrl);
  final SecureStore _store; final ApiClient _api; final _credential = CredentialService(); final _notifications = NotificationService();
  AppStage stage = AppStage.welcome; User? user; String? error; bool busy = false; String serverUrl = AppConfig.defaultBaseUrl; String? deviceId; String? publicKey; String? linkStatus;
  List<Team> teams = []; List<Task> tasks = []; List<LinkRequest> requests = []; List<User> users = [];

  Future<void> initialize() async { await _notifications.initialize(); deviceId = await _store.read(SecureStore.deviceIdKey); publicKey = await _store.read(SecureStore.publicKeyKey); final token = await _store.read(SecureStore.tokenKey); if (token != null && deviceId != null) { _api.token = token; try { user = User.fromJson(await _api.get('/api/auth/me')); stage = AppStage.ready; await refresh(); return; } catch (_) { await logout(); } } stage = publicKey == null ? AppStage.welcome : AppStage.server; notifyListeners(); }
  Future<void> generateCredential() async { await _run(() async { final credential = await _credential.generate(); deviceId = 'device-${DateTime.now().millisecondsSinceEpoch}'; publicKey = credential.publicKey; await _store.write(SecureStore.privateKeyKey, credential.privateKey); await _store.write(SecureStore.publicKeyKey, credential.publicKey); await _store.write(SecureStore.deviceIdKey, deviceId!); stage = AppStage.credential; }); }
  void continueAfterCredential() { stage = AppStage.server; error = null; notifyListeners(); }
  void setServer(String value) { serverUrl = value.trim().replaceAll(RegExp(r'/$'), ''); _api.baseUrl = serverUrl; _api.token = null; stage = AppStage.link; error = null; notifyListeners(); }
  Future<void> requestLink(String username, String deviceName) async { await _run(() async { _api.token = null; final response = await _api.post('/api/auth/linking/request', {'nombreUsuarioSolicitado': username, 'nombreDispositivo': deviceName, 'identificadorDispositivo': deviceId, 'clavePublicaDispositivo': publicKey}); linkStatus = response['estado']; final serverPublicKey = response['clavePublicaServidor'] as String?; if (serverPublicKey != null) await _store.write(SecureStore.serverKeyKey, serverPublicKey); stage = linkStatus == 'PENDIENTE' ? AppStage.pending : AppStage.login; }); }
  Future<void> requestChallenge() async { await _run(() async { final response = await _api.post('/api/auth/challenge', {'identificadorDispositivo': deviceId}); final privateKey = await _store.read(SecureStore.privateKeyKey); if (privateKey == null) throw const ApiException(400, 'No existe una credencial local.'); final signature = await _credential.sign(response['challenge'], privateKey); final login = await _api.post('/api/auth/login', {'challengeId': response['challengeId'], 'identificadorDispositivo': deviceId, 'firma': signature}); await _store.write(SecureStore.tokenKey, login['token']); _api.token = login['token']; user = User.fromJson(login['usuario']); stage = AppStage.ready; await refresh(); }); }
  Future<void> refresh() async { if (stage != AppStage.ready) return; final teamData = await _api.get('/api/teams'); final taskData = await _api.get('/api/tasks'); teams = (teamData as List).map((e) => Team.fromJson(e)).toList(); tasks = (taskData as List).map((e) => Task.fromJson(e)).toList(); if (user?.role == UserRole.admin) { requests = ((await _api.get('/api/admin/linking/requests')) as List).map((e) => LinkRequest.fromJson(e)).toList(); users = ((await _api.get('/api/admin/users')) as List).map((e) => User.fromJson(e)).toList(); } notifyListeners(); }
  Future<void> createTeam(String name) async { await _run(() async { await _api.post('/api/teams', {'nombre': name}); await refresh(); }); }
  Future<void> addMember(int teamId, int userId) async { await _run(() async { await _api.post('/api/teams/$teamId/members', {'usuarioId': userId}); await refresh(); }); }
  Future<void> removeMember(int teamId, int userId) async { await _run(() async { await _api.delete('/api/teams/$teamId/members/$userId'); await refresh(); }); }
  Future<void> createTask(String title, String description, int teamId, DateTime? dueDate) async { await _run(() async { await _api.post('/api/tasks', {'titulo': title, 'descripcion': description, 'equipoId': teamId, if (dueDate != null) 'fechaVencimiento': dueDate.toUtc().toIso8601String()}); await refresh(); }); }
  Future<void> updateTaskStatus(Task task, TaskStatus status) async { await _run(() async { await _api.put('/api/tasks/${task.id}/status', {'estado': taskStatusValue(status)}); await refresh(); }); }
  Future<void> completeTask(Task task) => updateTaskStatus(task, TaskStatus.completed);
  Future<void> assignTask(Task task, int userId) async { await _run(() async { await _api.post('/api/tasks/${task.id}/assignments', {'usuarioId': userId}); }); }
  Future<void> deleteTask(Task task) async { await _run(() async { await _api.delete('/api/tasks/${task.id}'); await refresh(); }); }
  Future<void> updateTask(Task task, String title, String description, DateTime? dueDate) async { await _run(() async { await _api.put('/api/tasks/${task.id}', {'titulo': title, 'descripcion': description, 'equipoId': task.teamId, if (dueDate != null) 'fechaVencimiento': dueDate.toUtc().toIso8601String()}); await refresh(); }); }
  Future<List<Reminder>> reminders(Task task) async { final data = await _api.get('/api/tasks/${task.id}/reminders') as List; return data.map((e) => Reminder.fromJson(e)).toList(); }
  Future<void> createReminder(Task task, DateTime date) async { await _run(() async { final reminder = await _api.post('/api/tasks/${task.id}/reminders', {'fechaHora': date.toUtc().toIso8601String()}); await _notifications.schedule(reminder['id'] as int, task.title, 'Recordatorio de tarea', date); }); }
  Future<void> deleteReminder(Reminder reminder) async { await _run(() async { await _api.delete('/api/reminders/${reminder.id}'); await _notifications.cancel(reminder.id); }); }
  Future<void> assignRole(User target, UserRole role) async { await _run(() async { await _api.put('/api/admin/users/${target.id}/role', {'rol': roleValue(role)}); await refresh(); }); }
  Future<void> deactivateUser(User target) async { await _run(() async { await _api.delete('/api/admin/users/${target.id}'); await refresh(); }); }
  Future<void> approve(LinkRequest request, String name) async { await _run(() async { await _api.post('/api/admin/linking/requests/${request.id}/approve', {'nombreCompleto': name, 'rol': 'USUARIO_NORMAL'}); await refresh(); }); }
  Future<void> reject(LinkRequest request) async { await _run(() async { await _api.post('/api/admin/linking/requests/${request.id}/reject'); await refresh(); }); }
  Future<void> logout() async { await _store.delete(SecureStore.tokenKey); _api.token = null; user = null; stage = AppStage.login; notifyListeners(); }
  Future<void> _run(Future<void> Function() action) async { busy = true; error = null; notifyListeners(); try { await action(); } on ApiException catch (e) { if (e.statusCode == 401) { await logout(); error = 'La sesión expiró. Inicia sesión de nuevo.'; } else { error = e.message; } } catch (_) { error = 'No se pudo conectar con el servidor.'; } finally { busy = false; notifyListeners(); } }
}

String roleValue(UserRole role) => switch (role) { UserRole.admin => 'ADMINISTRADOR_SISTEMAS', UserRole.manager => 'MANAGER', UserRole.normal => 'USUARIO_NORMAL' };
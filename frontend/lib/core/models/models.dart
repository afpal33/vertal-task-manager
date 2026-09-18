enum UserRole { admin, manager, normal }
enum TaskStatus { pending, inProgress, completed }

UserRole roleFrom(String? value) => switch (value) { 'ADMINISTRADOR_SISTEMAS' => UserRole.admin, 'MANAGER' => UserRole.manager, _ => UserRole.normal };
TaskStatus taskStatusFrom(String? value) => switch (value) { 'EN_PROGRESO' => TaskStatus.inProgress, 'COMPLETADA' => TaskStatus.completed, _ => TaskStatus.pending };
String taskStatusValue(TaskStatus value) => switch (value) { TaskStatus.pending => 'PENDIENTE', TaskStatus.inProgress => 'EN_PROGRESO', TaskStatus.completed => 'COMPLETADA' };

class User {
  const User({required this.id, required this.username, required this.name, required this.role, required this.active});
  final int id; final String username; final String name; final UserRole role; final bool active;
  factory User.fromJson(Map<String, dynamic> json) => User(id: json['id'], username: json['nombreUsuario'], name: json['nombreCompleto'], role: roleFrom(json['rol']), active: json['activo'] ?? true);
}

class Team {
  const Team({required this.id, required this.name, required this.creatorId});
  final int id; final String name; final int creatorId;
  factory Team.fromJson(Map<String, dynamic> json) => Team(id: json['id'], name: json['nombre'], creatorId: json['creadorId']);
}

class Task {
  const Task({required this.id, required this.title, required this.description, required this.status, required this.teamId, required this.creatorId, this.dueDate});
  final int id; final String title; final String description; final TaskStatus status; final int teamId; final int creatorId; final DateTime? dueDate;
  factory Task.fromJson(Map<String, dynamic> json) => Task(id: json['id'], title: json['titulo'], description: json['descripcion'] ?? '', status: taskStatusFrom(json['estado']), teamId: json['equipoId'], creatorId: json['creadorId'], dueDate: json['fechaVencimiento'] == null ? null : DateTime.parse(json['fechaVencimiento']));
}

class LinkRequest {
  const LinkRequest({required this.id, required this.username, required this.status, required this.date});
  final int id; final String username; final String status; final DateTime date;
  factory LinkRequest.fromJson(Map<String, dynamic> json) => LinkRequest(id: json['id'], username: json['nombreUsuarioSolicitado'], status: json['estado'], date: DateTime.parse(json['fechaSolicitud']));
}

class Reminder {
  const Reminder({required this.id, required this.taskId, required this.userId, required this.date, required this.active});
  final int id; final int taskId; final int userId; final DateTime date; final bool active;
  factory Reminder.fromJson(Map<String, dynamic> json) => Reminder(id: json['id'], taskId: json['tareaId'], userId: json['usuarioId'], date: DateTime.parse(json['fechaHora']), active: json['activo'] ?? true);
}
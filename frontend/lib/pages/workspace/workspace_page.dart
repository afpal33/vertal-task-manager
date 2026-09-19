import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:vertal/core/app_controller.dart';
import 'package:vertal/core/models/models.dart';
import 'package:vertal/widgets/common/app_widgets.dart';

class WorkspacePage extends StatefulWidget {
  const WorkspacePage({super.key, required this.controller});
  final AppController controller;

  @override
  State<WorkspacePage> createState() => _WorkspacePageState();
}

class _WorkspacePageState extends State<WorkspacePage> {
  int selectedIndex = 0;

  @override
  Widget build(BuildContext context) {
    final isAdmin = widget.controller.user?.role == UserRole.admin;
    final pages = <Widget>[
      TasksView(app: widget.controller),
      TeamsView(app: widget.controller),
      if (isAdmin) AdminView(app: widget.controller),
      ProfileView(app: widget.controller),
    ];
    if (selectedIndex >= pages.length) selectedIndex = 0;
    return Scaffold(
      body: SafeArea(child: pages[selectedIndex]),
      bottomNavigationBar: NavigationBar(
        selectedIndex: selectedIndex,
        onDestinationSelected: (index) => setState(() => selectedIndex = index),
        destinations: [
          const NavigationDestination(icon: Icon(Icons.checklist_rounded), label: 'Tareas'),
          const NavigationDestination(icon: Icon(Icons.groups_outlined), label: 'Equipos'),
          if (isAdmin) const NavigationDestination(icon: Icon(Icons.admin_panel_settings_outlined), label: 'Admin'),
          const NavigationDestination(icon: Icon(Icons.person_outline), label: 'Perfil'),
        ],
      ),
    );
  }
}

class TasksView extends StatelessWidget {
  const TasksView({super.key, required this.app});
  final AppController app;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Tareas'),
        actions: [IconButton(onPressed: app.refresh, tooltip: 'Actualizar', icon: const Icon(Icons.refresh))],
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => showTaskDialog(context, app),
        icon: const Icon(Icons.add),
        label: const Text('Nueva tarea'),
      ),
      body: RefreshIndicator(
        onRefresh: app.refresh,
        child: app.tasks.isEmpty
            ? ListView(children: const [SizedBox(height: 180), EmptyState(icon: Icons.checklist_outlined, title: 'Sin tareas todavía', message: 'Crea una tarea dentro de un equipo para empezar a coordinar el trabajo.')])
            : ListView.separated(
                padding: const EdgeInsets.fromLTRB(16, 12, 16, 100),
                itemCount: app.tasks.length,
                separatorBuilder: (_, index) => const SizedBox(height: 10),
                itemBuilder: (context, index) => TaskCard(task: app.tasks[index], app: app),
              ),
      ),
    );
  }
}

class TaskCard extends StatelessWidget {
  const TaskCard({super.key, required this.task, required this.app});
  final Task task;
  final AppController app;

  @override
  Widget build(BuildContext context) {
    final color = statusColor(task.status);
    return Card(
      child: InkWell(
        borderRadius: BorderRadius.circular(12),
        onTap: () => showModalBottomSheet<void>(
          context: context,
          isScrollControlled: true,
          builder: (_) => TaskDetails(task: task, app: app),
        ),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              Container(
                width: 42,
                height: 42,
                decoration: BoxDecoration(color: color.withValues(alpha: .12), borderRadius: BorderRadius.circular(10)),
                child: Icon(statusIcon(task.status), color: color),
              ),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(task.title, maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 16)),
                    const SizedBox(height: 5),
                    Text(task.description.isEmpty ? 'Sin descripción' : task.description, maxLines: 1, overflow: TextOverflow.ellipsis, style: TextStyle(color: Colors.blueGrey.shade600)),
                    const SizedBox(height: 8),
                    StatusPill(statusLabel(task.status), color: color),
                    const SizedBox(height: 4),
                    Text(task.assigneeName == null ? 'Sin asignar' : 'Asignada a ${task.assigneeName}', style: TextStyle(color: Colors.blueGrey.shade600, fontSize: 12)),
                  ],
                ),
              ),
              const Icon(Icons.chevron_right),
            ],
          ),
        ),
      ),
    );
  }
}

class TeamsView extends StatelessWidget {
  const TeamsView({super.key, required this.app});
  final AppController app;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Equipos'), actions: [IconButton(onPressed: app.refresh, icon: const Icon(Icons.refresh))]),
      floatingActionButton: FloatingActionButton.extended(onPressed: () => showTeamDialog(context, app), icon: const Icon(Icons.add), label: const Text('Nuevo equipo')),
      body: app.teams.isEmpty
          ? const EmptyState(icon: Icons.groups_outlined, title: 'Sin equipos', message: 'Crea un equipo para que las tareas sean siempre colaborativas.')
          : ListView.separated(
              padding: const EdgeInsets.all(16),
              itemCount: app.teams.length,
              separatorBuilder: (_, index) => const SizedBox(height: 10),
              itemBuilder: (context, index) {
                final team = app.teams[index];
                return Card(
                  child: ListTile(
                    leading: const CircleAvatar(child: Icon(Icons.groups_outlined)),
                    title: Text(team.name, style: const TextStyle(fontWeight: FontWeight.w800)),
                    subtitle: Text('Equipo #${team.id}'),
                    trailing: const Icon(Icons.chevron_right),
                    onTap: () => showMembersDialog(context, app, team),
                  ),
                );
              },
            ),
    );
  }
}

class AdminView extends StatelessWidget {
  const AdminView({super.key, required this.app});
  final AppController app;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Administración'), actions: [IconButton(onPressed: app.refresh, icon: const Icon(Icons.refresh))]),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          AdminSection(
            title: 'Solicitudes de vinculación',
            child: app.requests.isEmpty
                ? const Text('No hay solicitudes pendientes.')
                : Column(
                    children: app.requests.map((request) {
                      return Card(
                        margin: const EdgeInsets.only(bottom: 8),
                        child: ListTile(
                          leading: const Icon(Icons.pending_actions),
                          title: Text(request.username),
                          subtitle: Text(DateFormat('d MMM y, HH:mm').format(request.date)),
                          trailing: PopupMenuButton<String>(
                            onSelected: (value) async {
                              if (value == 'approve') await approveDialog(context, app, request);
                              if (value == 'reject') await app.reject(request);
                            },
                            itemBuilder: (_) => const [PopupMenuItem(value: 'approve', child: Text('Aprobar')), PopupMenuItem(value: 'reject', child: Text('Rechazar'))],
                          ),
                        ),
                      );
                    }).toList(),
                  ),
          ),
          const SizedBox(height: 24),
          AdminSection(
            title: 'Usuarios aprobados',
            child: app.users.isEmpty
                ? const Text('No hay usuarios cargados.')
                : Column(
                    children: app.users.map((user) {
                      return ListTile(
                        contentPadding: EdgeInsets.zero,
                        leading: CircleAvatar(child: Text(user.name.isEmpty ? '?' : user.name[0].toUpperCase())),
                        title: Text(user.name),
                        subtitle: Text('@${user.username} · ${roleLabel(user.role)}'),
                        trailing: PopupMenuButton<String>(
                          onSelected: (value) {
                            if (value == 'deactivate') {
                              app.deactivateUser(user);
                            } else {
                              app.assignRole(user, UserRole.values.firstWhere((role) => role.name == value));
                            }
                          },
                          itemBuilder: (_) => [
                            ...UserRole.values.map((role) => PopupMenuItem(value: role.name, child: Text(roleLabel(role)))),
                            const PopupMenuItem(value: 'deactivate', child: Text('Desactivar usuario')),
                          ],
                        ),
                      );
                    }).toList(),
                  ),
          ),
        ],
      ),
    );
  }
}

class AdminSection extends StatelessWidget {
  const AdminSection({super.key, required this.title, required this.child});
  final String title;
  final Widget child;
  @override
  Widget build(BuildContext context) => Column(crossAxisAlignment: CrossAxisAlignment.start, children: [SectionTitle(title), const SizedBox(height: 8), Card(child: Padding(padding: const EdgeInsets.all(14), child: child))]);
}

class ProfileView extends StatelessWidget {
  const ProfileView({super.key, required this.app});
  final AppController app;

  @override
  Widget build(BuildContext context) {
    final user = app.user;
    return Scaffold(
      appBar: AppBar(title: const Text('Tu cuenta')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Card(child: Padding(padding: const EdgeInsets.all(20), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [const Icon(Icons.verified_user_outlined, size: 34), const SizedBox(height: 18), Text(user?.name ?? 'Usuario', style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w800)), const SizedBox(height: 4), Text('@${user?.username ?? ''}'), const SizedBox(height: 16), StatusPill(roleLabel(user?.role ?? UserRole.normal), color: Theme.of(context).colorScheme.primary)]))),
          const SizedBox(height: 16),
          ListTile(leading: const Icon(Icons.dns_outlined), title: const Text('Servidor'), subtitle: Text(app.serverUrl)),
          const SizedBox(height: 16),
          OutlinedButton.icon(onPressed: app.logout, icon: const Icon(Icons.logout), label: const Text('Cerrar sesión')),
        ],
      ),
    );
  }
}

class TaskDetails extends StatefulWidget {
  const TaskDetails({super.key, required this.task, required this.app});
  final Task task;
  final AppController app;
  @override
  State<TaskDetails> createState() => _TaskDetailsState();
}

class _TaskDetailsState extends State<TaskDetails> {
  late TaskStatus status = widget.task.status;
  List<Reminder> reminders = [];
  bool get canManage =>
      widget.app.user?.role == UserRole.manager ||
      widget.app.user?.id == widget.task.creatorId ||
      widget.app.user?.id == widget.task.assigneeId;

  @override
  void initState() { super.initState(); loadReminders(); }
  Future<void> loadReminders() async { try { reminders = await widget.app.reminders(widget.task); if (mounted) setState(() {}); } catch (_) {} }

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(20, 12, 20, 24),
        child: SingleChildScrollView(
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Center(child: Container(width: 42, height: 4, decoration: BoxDecoration(color: Colors.black12, borderRadius: BorderRadius.circular(3)))),
            const SizedBox(height: 22),
            Text(widget.task.title, style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w800)),
            const SizedBox(height: 8),
            Text(widget.task.description.isEmpty ? 'Sin descripción' : widget.task.description),
            const SizedBox(height: 8),
            ListTile(contentPadding: EdgeInsets.zero, leading: const Icon(Icons.person_outline), title: const Text('Asignada a'), subtitle: Text(widget.task.assigneeName == null ? 'Sin asignar' : '${widget.task.assigneeName} (@${widget.task.assigneeUsername})')),
            const SizedBox(height: 22),
            DropdownButtonFormField<TaskStatus>(initialValue: status, decoration: const InputDecoration(labelText: 'Estado'), items: TaskStatus.values.map((value) => DropdownMenuItem(value: value, child: Text(statusLabel(value)))).toList(), onChanged: (value) async { if (value == null) return; setState(() => status = value); await widget.app.updateTaskStatus(widget.task, value); }),
            if (widget.task.dueDate != null) ListTile(contentPadding: EdgeInsets.zero, leading: const Icon(Icons.event_outlined), title: const Text('Vencimiento'), subtitle: Text(DateFormat('d MMM y, HH:mm').format(widget.task.dueDate!))),
            SectionTitle('Recordatorios', action: () => addReminder(context)),
            if (reminders.isEmpty) const Text('No hay recordatorios configurados.') else ...reminders.map((reminder) => ListTile(contentPadding: EdgeInsets.zero, leading: const Icon(Icons.notifications_none), title: Text(DateFormat('d MMM y, HH:mm').format(reminder.date)), trailing: IconButton(icon: const Icon(Icons.delete_outline), onPressed: () async { await widget.app.deleteReminder(reminder); await loadReminders(); }))),
            if (canManage) ...[
              const SizedBox(height: 10),
              OutlinedButton.icon(onPressed: () => assignTask(context), icon: const Icon(Icons.person_add_alt_1_outlined), label: const Text('Asignar tarea')),
              OutlinedButton.icon(onPressed: () => editTask(context), icon: const Icon(Icons.edit_outlined), label: const Text('Editar tarea')),
              TextButton.icon(onPressed: () async { await widget.app.deleteTask(widget.task); if (context.mounted) Navigator.pop(context); }, icon: const Icon(Icons.delete_outline), label: const Text('Eliminar tarea', style: TextStyle(color: Colors.red))),
            ],
          ]),
        ),
      ),
    );
  }

  Future<void> addReminder(BuildContext context) async {
    final date = await showDatePicker(context: context, firstDate: DateTime.now(), lastDate: DateTime.now().add(const Duration(days: 365)), initialDate: DateTime.now().add(const Duration(days: 1)));
    if (date == null || !context.mounted) return;
    final time = await showTimePicker(context: context, initialTime: TimeOfDay.now());
    if (time == null || !context.mounted) return;
    final value = DateTime(date.year, date.month, date.day, time.hour, time.minute);
    if (value.isBefore(DateTime.now())) { showMessage(context, 'El recordatorio debe estar en el futuro.'); return; }
    await widget.app.createReminder(widget.task, value);
    await loadReminders();
  }

  Future<void> editTask(BuildContext context) async {
    final title = TextEditingController(text: widget.task.title);
    final description = TextEditingController(text: widget.task.description);
    final save = await showDialog<bool>(context: context, builder: (_) => AlertDialog(title: const Text('Editar tarea'), content: Column(mainAxisSize: MainAxisSize.min, children: [TextField(controller: title, decoration: const InputDecoration(labelText: 'Título')), TextField(controller: description, decoration: const InputDecoration(labelText: 'Descripción'))]), actions: [TextButton(onPressed: () => Navigator.pop(context), child: const Text('Cancelar')), FilledButton(onPressed: () => Navigator.pop(context, true), child: const Text('Guardar'))]));
    if (save == true) await widget.app.updateTask(widget.task, title.text.trim(), description.text.trim(), widget.task.dueDate);
  }

  Future<void> assignTask(BuildContext context) async {
    final username = TextEditingController();
    final assigned = await showDialog<bool>(context: context, builder: (_) => AlertDialog(title: const Text('Asignar tarea'), content: TextField(controller: username, decoration: const InputDecoration(labelText: 'Nombre de usuario')), actions: [TextButton(onPressed: () => Navigator.pop(context), child: const Text('Cancelar')), FilledButton(onPressed: () => Navigator.pop(context, username.text.trim().isNotEmpty), child: const Text('Asignar'))]));
    if (assigned == true) await widget.app.assignTask(widget.task, username.text.trim());
  }
}

Future<void> showTaskDialog(BuildContext context, AppController app) async {
  final title = TextEditingController();
  final description = TextEditingController();
  int? teamId;
  final created = await showDialog<bool>(context: context, builder: (_) => StatefulBuilder(builder: (context, setDialogState) => AlertDialog(title: const Text('Nueva tarea colaborativa'), content: SingleChildScrollView(child: Column(mainAxisSize: MainAxisSize.min, children: [TextField(controller: title, decoration: const InputDecoration(labelText: 'Título')), TextField(controller: description, decoration: const InputDecoration(labelText: 'Descripción')), const SizedBox(height: 12), DropdownButtonFormField<int>(decoration: const InputDecoration(labelText: 'Equipo'), items: app.teams.map((team) => DropdownMenuItem(value: team.id, child: Text(team.name))).toList(), onChanged: (value) => setDialogState(() => teamId = value))])), actions: [TextButton(onPressed: () => Navigator.pop(context), child: const Text('Cancelar')), FilledButton(onPressed: title.text.trim().isEmpty || teamId == null ? null : () => Navigator.pop(context, true), child: const Text('Crear'))])));
  if (created == true && teamId != null) await app.createTask(title.text.trim(), description.text.trim(), teamId!, null);
}

Future<void> showTeamDialog(BuildContext context, AppController app) async { final name = TextEditingController(); final created = await showDialog<bool>(context: context, builder: (_) => AlertDialog(title: const Text('Nuevo equipo'), content: TextField(controller: name, autofocus: true, decoration: const InputDecoration(labelText: 'Nombre del equipo')), actions: [TextButton(onPressed: () => Navigator.pop(context), child: const Text('Cancelar')), FilledButton(onPressed: () => Navigator.pop(context, name.text.trim().isNotEmpty), child: const Text('Crear'))])); if (created == true) await app.createTeam(name.text.trim()); }
Future<void> showMembersDialog(BuildContext context, AppController app, Team team) async {
  final username = TextEditingController();
  await showDialog<void>(context: context, builder: (_) => AlertDialog(title: Text(team.name), content: TextField(controller: username, decoration: const InputDecoration(labelText: 'Nombre de usuario')), actions: [
    TextButton(onPressed: () => Navigator.pop(context), child: const Text('Cerrar')),
    TextButton(onPressed: () async {
      final value = username.text.trim();
      if (value.isNotEmpty) await app.removeMember(team.id, value);
      if (context.mounted) Navigator.pop(context);
    }, child: const Text('Retirar')),
    FilledButton(onPressed: () async {
      final value = username.text.trim();
      if (value.isNotEmpty) await app.addMember(team.id, value);
      if (context.mounted) Navigator.pop(context);
    }, child: const Text('Agregar miembro'))
  ]));
}
Future<void> approveDialog(BuildContext context, AppController app, LinkRequest request) async { final name = TextEditingController(text: request.username); final approved = await showDialog<bool>(context: context, builder: (_) => AlertDialog(title: const Text('Aprobar vinculación'), content: TextField(controller: name, decoration: const InputDecoration(labelText: 'Nombre completo')), actions: [TextButton(onPressed: () => Navigator.pop(context), child: const Text('Cancelar')), FilledButton(onPressed: () => Navigator.pop(context, true), child: const Text('Aprobar'))])); if (approved == true) await app.approve(request, name.text.trim()); }

Color statusColor(TaskStatus status) => switch (status) { TaskStatus.pending => const Color(0xFFB7791F), TaskStatus.inProgress => const Color(0xFF0E7490), TaskStatus.completed => const Color(0xFF2F855A) };
IconData statusIcon(TaskStatus status) => switch (status) { TaskStatus.pending => Icons.radio_button_unchecked, TaskStatus.inProgress => Icons.timelapse, TaskStatus.completed => Icons.check_circle_outline };
String statusLabel(TaskStatus status) => switch (status) { TaskStatus.pending => 'Pendiente', TaskStatus.inProgress => 'En progreso', TaskStatus.completed => 'Completada' };
String roleLabel(UserRole role) => switch (role) { UserRole.admin => 'Administrador', UserRole.manager => 'Manager', UserRole.normal => 'Usuario normal' };
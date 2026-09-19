import 'package:flutter/material.dart';
import 'package:vertal/core/app_controller.dart';
import 'package:vertal/widgets/common/app_widgets.dart';

class AuthFlowPage extends StatefulWidget {
  const AuthFlowPage({super.key, required this.controller});
  final AppController controller;

  @override
  State<AuthFlowPage> createState() => _AuthFlowPageState();
}

class _AuthFlowPageState extends State<AuthFlowPage> {
  final _server = TextEditingController();
  final _username = TextEditingController();
  final _deviceName = TextEditingController();
  final _fullName = TextEditingController();
  final _bootstrapToken = TextEditingController();

  AppController get app => widget.controller;

  @override
  void dispose() {
    _server.dispose();
    _username.dispose();
    _deviceName.dispose();
    _fullName.dispose();
    _bootstrapToken.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Center(
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 460),
            child: SingleChildScrollView(
              padding: const EdgeInsets.all(24),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const BrandMark(large: true),
                  const SizedBox(height: 48),
                  _content(context),
                  if (app.error != null) ...[
                    const SizedBox(height: 18),
                    _ErrorBanner(message: app.error!),
                  ],
                  if (app.busy) ...[
                    const SizedBox(height: 24),
                    const LinearProgressIndicator(),
                  ],
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _content(BuildContext context) {
    return switch (app.stage) {
      AppStage.welcome => _welcome(context),
      AppStage.credential => _credential(context),
      AppStage.server => _serverStep(context),
      AppStage.link => _linkStep(context),
      AppStage.pending => _pending(context),
      AppStage.login => _login(context),
      AppStage.ready => const SizedBox.shrink(),
    };
  }

  Widget _welcome(BuildContext context) => _step(
        'Tu espacio de trabajo, bajo tu control.',
        'Vincula este dispositivo a la instalación privada de tu organización. No usamos contraseñas ni proveedores externos.',
        FilledButton.icon(
          onPressed: app.generateCredential,
          icon: const Icon(Icons.key_outlined),
          label: const Text('Generar credencial local'),
        ),
      );

  Widget _credential(BuildContext context) => _step(
        'Credencial lista',
        'La clave privada permanece protegida en este dispositivo y nunca se envía al servidor.',
        Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            _InfoTile(label: 'Dispositivo', value: app.deviceId ?? 'Local'),
            const SizedBox(height: 16),
            FilledButton(onPressed: app.continueAfterCredential, child: const Text('Continuar')),
          ],
        ),
      );

  Widget _serverStep(BuildContext context) => _step(
        'Conecta tu servidor',
        'Escribe la dirección de la instalación self-hosted de tu organización.',
        Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            TextFormField(controller: _server, keyboardType: TextInputType.url, decoration: const InputDecoration(labelText: 'URL del servidor', hintText: 'http://10.0.2.2:8080')),
            const SizedBox(height: 16),
            FilledButton(onPressed: () => app.setServer(_server.text.isEmpty ? app.serverUrl : _server.text), child: const Text('Continuar')),
          ],
        ),
      );

  Widget _linkStep(BuildContext context) => _step(
        'Solicita acceso',
        'Un Administrador del Sistema debe aprobar esta solicitud antes de que puedas entrar.',
        Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            TextFormField(controller: _username, decoration: const InputDecoration(labelText: 'Nombre de usuario')),
            const SizedBox(height: 12),
            TextFormField(controller: _deviceName, decoration: const InputDecoration(labelText: 'Nombre de este dispositivo', hintText: 'Teléfono de Ana')),
            const SizedBox(height: 16),
            FilledButton(onPressed: () => app.requestLink(_username.text.trim(), _deviceName.text.trim()), child: const Text('Enviar solicitud')),
            const SizedBox(height: 28),
            const Divider(),
            const SizedBox(height: 18),
            Text('¿Es el primer administrador?', style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w800)),
            const SizedBox(height: 6),
            Text('Usa el token mostrado por setup.sh. Solo funciona para vincular el primer dispositivo administrador.', style: Theme.of(context).textTheme.bodySmall),
            const SizedBox(height: 12),
            TextFormField(controller: _fullName, decoration: const InputDecoration(labelText: 'Nombre completo del administrador')),
            const SizedBox(height: 12),
            TextFormField(controller: _bootstrapToken, obscureText: true, decoration: const InputDecoration(labelText: 'Token de bootstrap')),
            const SizedBox(height: 12),
            OutlinedButton.icon(onPressed: () => app.bootstrapAdmin(_bootstrapToken.text.trim(), _fullName.text.trim(), _deviceName.text.trim()), icon: const Icon(Icons.admin_panel_settings_outlined), label: const Text('Configurar administrador inicial')),
          ],
        ),
      );

  Widget _pending(BuildContext context) => _step(
        'Solicitud enviada',
        'Tu solicitud está pendiente de aprobación. Cuando el administrador la apruebe, vuelve aquí para iniciar sesión.',
        Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const _StateCard(icon: Icons.hourglass_top_rounded, title: 'Pendiente', color: Color(0xFFB7791F)),
            const SizedBox(height: 16),
            OutlinedButton(onPressed: () => setState(() => app.stage = AppStage.login), child: const Text('Intentar iniciar sesión')),
          ],
        ),
      );

  Widget _login(BuildContext context) => _step(
        'Inicia sesión con tu credencial',
        'El servidor enviará un desafío que se firma localmente. No necesitas recordar ninguna contraseña.',
        Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            _InfoTile(label: 'Dispositivo', value: app.deviceId ?? 'No configurado'),
            const SizedBox(height: 16),
            FilledButton.icon(onPressed: app.requestChallenge, icon: const Icon(Icons.login), label: const Text('Solicitar acceso')),
            const SizedBox(height: 8),
            TextButton(onPressed: () => setState(() => app.stage = AppStage.server), child: const Text('Cambiar servidor')),
          ],
        ),
      );

  Widget _step(String title, String subtitle, Widget child) => Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text(title, style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w800)), const SizedBox(height: 10), Text(subtitle, style: Theme.of(context).textTheme.bodyLarge?.copyWith(color: Colors.blueGrey.shade700)), const SizedBox(height: 28), child]);
}

class _InfoTile extends StatelessWidget {
  const _InfoTile({required this.label, required this.value});
  final String label;
  final String value;
  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          children: [
            const Icon(Icons.verified_user_outlined),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(label, style: Theme.of(context).textTheme.labelMedium),
                  const SizedBox(height: 3),
                  Text(value, overflow: TextOverflow.ellipsis, style: const TextStyle(fontWeight: FontWeight.w700)),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _StateCard extends StatelessWidget {
  const _StateCard({required this.icon, required this.title, required this.color});
  final IconData icon;
  final String title;
  final Color color;
  @override
  Widget build(BuildContext context) => Card(color: color.withValues(alpha: .1), child: Padding(padding: const EdgeInsets.all(18), child: Row(children: [Icon(icon, color: color), const SizedBox(width: 12), Text(title, style: TextStyle(color: color, fontWeight: FontWeight.w800))])));
}

class _ErrorBanner extends StatelessWidget {
  const _ErrorBanner({required this.message});
  final String message;
  @override
  Widget build(BuildContext context) => Container(width: double.infinity, padding: const EdgeInsets.all(14), decoration: BoxDecoration(color: Theme.of(context).colorScheme.errorContainer, borderRadius: BorderRadius.circular(10)), child: Text(message, style: TextStyle(color: Theme.of(context).colorScheme.onErrorContainer)));
}
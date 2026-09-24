import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:vertal/core/notifications/notification_service.dart';

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();
  const verificationChannel = MethodChannel('vertal/privacy_verification');

  testWidgets('publica efectivamente una notificación en Android', (
    tester,
  ) async {
    const notificationId = 99001;
    final service = NotificationService();
    await service.initialize();

    await service.showNow(
      notificationId,
      'Prueba de recordatorio',
      'Verificación funcional de Vertal',
    );
    await Future<void>.delayed(const Duration(seconds: 1));
    final activeIds = await verificationChannel.invokeListMethod<int>(
      'activeNotificationIds',
    );

    expect(activeIds, contains(notificationId));
    await service.cancel(notificationId);
  });
}

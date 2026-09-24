import 'package:flutter/foundation.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:vertal/core/notifications/notification_service.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  const channel = MethodChannel('dexterous.com/flutter/local_notifications');
  final calls = <MethodCall>[];

  setUp(() {
    FlutterLocalNotificationsPlatform.instance =
        AndroidFlutterLocalNotificationsPlugin();
    debugDefaultTargetPlatformOverride = TargetPlatform.android;
    calls.clear();
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (call) async {
          calls.add(call);
          if (call.method == 'initialize' ||
              call.method == 'requestNotificationsPermission') {
            return true;
          }
          return null;
        });
  });

  tearDown(() {
    debugDefaultTargetPlatformOverride = null;
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, null);
  });

  test('programa el recordatorio en el canal local de Android', () async {
    final service = NotificationService();
    final date = DateTime.now().add(const Duration(hours: 1));

    await service.initialize();
    await service.schedule(21, 'Tarea', 'Recordatorio de tarea', date);

    expect(calls.map((call) => call.method), contains('zonedSchedule'));
    final scheduled = calls.firstWhere(
      (call) => call.method == 'zonedSchedule',
    );
    expect((scheduled.arguments as Map)['id'], 21);
    expect((scheduled.arguments as Map)['title'], 'Tarea');
  });
}

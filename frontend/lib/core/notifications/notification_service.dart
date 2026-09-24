import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:timezone/data/latest.dart' as tz;
import 'package:timezone/timezone.dart' as tz;

class NotificationService {
  NotificationService({FlutterLocalNotificationsPlugin? plugin})
    : _plugin = plugin ?? FlutterLocalNotificationsPlugin();
  final FlutterLocalNotificationsPlugin _plugin;
  Future<void> initialize() async {
    tz.initializeTimeZones();
    const settings = AndroidInitializationSettings('@mipmap/ic_launcher');
    await _plugin.initialize(const InitializationSettings(android: settings));
  }

  Future<void> schedule(
    int id,
    String title,
    String body,
    DateTime date,
  ) async {
    await _plugin
        .resolvePlatformSpecificImplementation<
          AndroidFlutterLocalNotificationsPlugin
        >()
        ?.requestNotificationsPermission();
    await _plugin.zonedSchedule(
      id,
      title,
      body,
      _tz(date),
      const NotificationDetails(
        android: AndroidNotificationDetails(
          'vertal_reminders',
          'Recordatorios',
          channelDescription: 'Recordatorios de tareas',
          importance: Importance.high,
        ),
      ),
      androidScheduleMode: AndroidScheduleMode.inexactAllowWhileIdle,
    );
  }

  Future<void> cancel(int id) => _plugin.cancel(id);

  Future<void> showNow(int id, String title, String body) async {
    await _plugin
        .resolvePlatformSpecificImplementation<
          AndroidFlutterLocalNotificationsPlugin
        >()
        ?.requestNotificationsPermission();
    await _plugin.show(
      id,
      title,
      body,
      const NotificationDetails(
        android: AndroidNotificationDetails(
          'vertal_reminders',
          'Recordatorios',
          channelDescription: 'Recordatorios de tareas',
          importance: Importance.high,
        ),
      ),
    );
  }

  tz.TZDateTime _tz(DateTime date) => tz.TZDateTime.from(date, tz.local);
}

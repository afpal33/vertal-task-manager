class AppConfig {
  static const defaultBaseUrl = String.fromEnvironment(
    'VERTAL_API_URL',
    defaultValue: 'http://10.0.2.2:8080',
  );
}
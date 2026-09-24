import 'dart:async';
import 'dart:convert';
import 'package:http/http.dart' as http;

class ApiException implements Exception {
  final int statusCode;
  final String message;
  const ApiException(this.statusCode, this.message);
  @override
  String toString() => message;
}

class ApiClient {
  ApiClient({
    required this.baseUrl,
    this.token,
    http.Client? client,
    this.timeout = const Duration(seconds: 15),
  }) : _client = client ?? http.Client();
  String baseUrl;
  String? token;
  String? deviceId;
  final http.Client _client;
  final Duration timeout;

  Future<dynamic> get(String path) => _request('GET', path);
  Future<dynamic> post(String path, [Map<String, dynamic>? body]) =>
      _request('POST', path, body);
  Future<dynamic> put(String path, [Map<String, dynamic>? body]) =>
      _request('PUT', path, body);
  Future<dynamic> delete(String path) => _request('DELETE', path);

  static Uri validateBaseUrl(String value) {
    final server = Uri.tryParse(value);
    if (server == null ||
        !server.hasAuthority ||
        server.host.isEmpty ||
        (server.scheme != 'http' && server.scheme != 'https') ||
        server.userInfo.isNotEmpty ||
        server.query.isNotEmpty ||
        server.fragment.isNotEmpty) {
      throw const ApiException(400, 'La dirección del servidor no es válida.');
    }
    return server;
  }

  Future<dynamic> _request(
    String method,
    String path, [
    Map<String, dynamic>? body,
  ]) async {
    final uri = _resolve(path);
    final headers = {
      'Content-Type': 'application/json',
      if (token != null) 'Authorization': 'Bearer $token',
      if (deviceId != null) 'X-Device-Id': deviceId!,
    };
    late final http.Response response;
    try {
      final request = http.Request(method, uri)
        ..headers.addAll(headers)
        ..followRedirects = false;
      if (body != null) request.body = jsonEncode(body);
      response = await http.Response.fromStream(
        await _client.send(request).timeout(timeout),
      );
    } on TimeoutException {
      throw const ApiException(
        408,
        'El servidor tardó demasiado en responder.',
      );
    }
    if (response.statusCode >= 300 && response.statusCode < 400) {
      throw const ApiException(
        502,
        'El servidor intentó redirigir la conexión a otro destino.',
      );
    }
    dynamic decoded;
    try {
      decoded = response.body.isEmpty ? null : jsonDecode(response.body);
    } catch (_) {
      decoded = null;
    }
    if (response.statusCode < 200 || response.statusCode >= 300) {
      final message = decoded is Map ? decoded['message']?.toString() : null;
      throw ApiException(
        response.statusCode,
        message ?? 'No se pudo completar la operación.',
      );
    }
    return decoded;
  }

  Uri _resolve(String path) {
    final server = validateBaseUrl(baseUrl);
    final endpoint = Uri.tryParse(path);
    if (endpoint == null ||
        !path.startsWith('/') ||
        endpoint.hasScheme ||
        endpoint.hasAuthority) {
      throw const ApiException(
        400,
        'La aplicación bloqueó una conexión fuera del servidor configurado.',
      );
    }
    final uri = Uri.parse('${baseUrl.replaceAll(RegExp(r'/$'), '')}$path');
    if (uri.scheme != server.scheme ||
        uri.host != server.host ||
        uri.port != server.port) {
      throw const ApiException(
        400,
        'La aplicación bloqueó una conexión fuera del servidor configurado.',
      );
    }
    return uri;
  }
}

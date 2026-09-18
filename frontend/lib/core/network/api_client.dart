import 'dart:convert';
import 'package:http/http.dart' as http;

class ApiException implements Exception {
  final int statusCode;
  final String message;
  const ApiException(this.statusCode, this.message);
  @override String toString() => message;
}

class ApiClient {
  ApiClient({required this.baseUrl, this.token, http.Client? client}) : _client = client ?? http.Client();
  String baseUrl;
  String? token;
  final http.Client _client;

  Future<dynamic> get(String path) => _request('GET', path);
  Future<dynamic> post(String path, [Map<String, dynamic>? body]) => _request('POST', path, body);
  Future<dynamic> put(String path, [Map<String, dynamic>? body]) => _request('PUT', path, body);
  Future<dynamic> delete(String path) => _request('DELETE', path);

  Future<dynamic> _request(String method, String path, [Map<String, dynamic>? body]) async {
    final headers = {'Content-Type': 'application/json', if (token != null) 'Authorization': 'Bearer $token'};
    final uri = Uri.parse('$baseUrl$path');
    final response = switch (method) {
      'GET' => await _client.get(uri, headers: headers),
      'POST' => await _client.post(uri, headers: headers, body: body == null ? null : jsonEncode(body)),
      'PUT' => await _client.put(uri, headers: headers, body: body == null ? null : jsonEncode(body)),
      _ => await _client.delete(uri, headers: headers),
    };
    dynamic decoded;
    try { decoded = response.body.isEmpty ? null : jsonDecode(response.body); } catch (_) { decoded = null; }
    if (response.statusCode < 200 || response.statusCode >= 300) {
      final message = decoded is Map ? decoded['message']?.toString() : null;
      throw ApiException(response.statusCode, message ?? 'No se pudo completar la operación.');
    }
    return decoded;
  }
}
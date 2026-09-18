import 'dart:convert';
import 'dart:math';
import 'dart:typed_data';
import 'package:basic_utils/basic_utils.dart';
import 'package:pointycastle/export.dart';

class LocalCredential {
  const LocalCredential({required this.privateKey, required this.publicKey});
  final String privateKey;
  final String publicKey;
}

class CredentialService {
  Future<LocalCredential> generate() async {
    final secureRandom = FortunaRandom();
    secureRandom.seed(KeyParameter(Uint8List.fromList(List.generate(32, (_) => Random.secure().nextInt(256)))));
    final generator = RSAKeyGenerator()..init(ParametersWithRandom(RSAKeyGeneratorParameters(BigInt.parse('65537'), 2048, 64), secureRandom));
    final pair = generator.generateKeyPair();
    final publicKey = pair.publicKey as RSAPublicKey;
    final privateKey = pair.privateKey as RSAPrivateKey;
    return LocalCredential(
      privateKey: CryptoUtils.encodeRSAPrivateKeyToPem(privateKey),
      publicKey: CryptoUtils.encodeRSAPublicKeyToPem(publicKey),
    );
  }

  Future<String> sign(String challenge, String privateKeyPem) async {
    final privateKey = CryptoUtils.rsaPrivateKeyFromPem(privateKeyPem);
    final signature = CryptoUtils.rsaSign(privateKey, Uint8List.fromList(utf8.encode(challenge)));
    return base64Encode(signature);
  }
}
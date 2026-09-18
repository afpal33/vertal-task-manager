import 'package:flutter/material.dart';

class AppTheme {
  static ThemeData light() {
    const ink = Color(0xFF1F2933); const teal = Color(0xFF0E7490); const mist = Color(0xFFF3F7F6);
    return ThemeData(useMaterial3: true, scaffoldBackgroundColor: mist, colorScheme: ColorScheme.fromSeed(seedColor: teal, brightness: Brightness.light), fontFamily: 'sans', appBarTheme: const AppBarTheme(backgroundColor: mist, foregroundColor: ink, elevation: 0), cardTheme: CardThemeData(color: Colors.white, elevation: 0, margin: EdgeInsets.zero, shape: RoundedRectangleBorder(borderRadius: BorderRadius.all(Radius.circular(12)))), inputDecorationTheme: InputDecorationTheme(filled: true, fillColor: Colors.white, border: OutlineInputBorder(borderRadius: BorderRadius.all(Radius.circular(10)), borderSide: BorderSide.none), enabledBorder: OutlineInputBorder(borderRadius: BorderRadius.all(Radius.circular(10)), borderSide: BorderSide(color: Color(0xFFD9E4E2))), focusedBorder: OutlineInputBorder(borderRadius: BorderRadius.all(Radius.circular(10)), borderSide: BorderSide(color: teal, width: 2))), elevatedButtonTheme: ElevatedButtonThemeData(style: ElevatedButton.styleFrom(backgroundColor: teal, foregroundColor: Colors.white, minimumSize: const Size.fromHeight(50), shape: const RoundedRectangleBorder(borderRadius: BorderRadius.all(Radius.circular(10))))));
  }
}
/*
 * Copyright 2026 Fabrizio.root
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import 'package:flutter/material.dart';
import 'package:vertal/core/app_controller.dart';
import 'package:vertal/pages/auth/auth_flow_page.dart';
import 'package:vertal/pages/workspace/workspace_page.dart';
import 'package:vertal/presentation/theme/app_theme.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});
  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  late final AppController controller;
  late final Future<void> initialization;

  @override
  void initState() {
    super.initState();
    controller = AppController();
    initialization = controller.initialize();
  }

  @override
  void dispose() {
    controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Vertal',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.light(),
      home: FutureBuilder<void>(
        future: initialization,
        builder: (context, snapshot) {
          if (snapshot.connectionState != ConnectionState.done) {
            return const Scaffold(body: Center(child: CircularProgressIndicator()));
          }
          return AnimatedBuilder(
            animation: controller,
            builder: (context, _) => controller.stage == AppStage.ready
                ? WorkspacePage(controller: controller)
                : AuthFlowPage(controller: controller),
          );
        },
      ),
    );
  }
}

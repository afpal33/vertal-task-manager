// frontend/lib/core/presentation/pages/main_wrapper_page.dart

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
import 'package:vertal/core/presentation/pages/profile.dart';
import 'package:vertal/core/presentation/widgets/bottom_nav.dart';
import 'package:vertal/core/presentation/pages/task_widget.dart';

class MainWrapperPage extends StatefulWidget {
  final screens = const [ProfilePreview(), TaskWidget()];
  const MainWrapperPage({super.key});

  @override
  State<MainWrapperPage> createState() =>
      _MainWrapperPage();
}

class _MainWrapperPage extends State<MainWrapperPage> {
  int _currentIndex = 0;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: widget.screens[_currentIndex],
      appBar: AppBar(
        title: Text('Vertal'),
        titleTextStyle: TextStyle(
          color: Colors.white60,
          fontSize: 30,
        ),
        backgroundColor: Colors.lightBlue,
      ),
      bottomNavigationBar: BottomNav(
        currentIndex: _currentIndex,
        onTap: (index) {
          setState(() {
            _currentIndex = index;
          });
        },
      ),
    );
  }
}

# Project Roadmap - Laiza Redesign

This roadmap details the milestones for the **Laiza** Business Management System. Each milestone represents a core module to be developed one at a time.

---

## 📍 Current Status
*   **Target Version**: 1.0.0 (Release Candidate)
*   **Current Milestone**: All Milestones Completed Successfully

---

## 🗺️ redelivery Milestones

### Milestone 1: Architectural Base & Custom UI System
*   [x] Refactor folder layout to support package `com.laiza`.
*   [x] Setup premium Material Design 3 theme system (curated palettes, typography, shapes).
*   [x] Create shared visual widgets: custom buttons, fields, loading screens, alert dialogs.
*   [x] Setup basic Navigation Compose Graph (Splash, Auth, Admin, Employee routes).

### Milestone 2: Database Schema & Dependency Injection
*   [x] Implement Room entities according to `DATABASE_SCHEMA.md`.
*   [x] Define Room DAOs supporting transactional operations.
*   [x] Initialize `LaizaDatabase` and setup versioning migrations.
*   [x] Bind Hilt modules for database providers and core repositories.

### Milestone 3: Authentication & Splash Screen
*   [x] Configure Firebase Authentication client.
*   [x] Implement `AuthRepositoryImpl` with Mobile+Password mapper (`<phone>@laiza.com`).
*   [x] Build session manager cache using Preferences DataStore.
*   [x] Design premium Splash screen checking active sessions.
*   [x] Create Login screen (validated inputs, role redirection).

### Milestone 4: Attendance & CameraX Location Module
*   [x] Build Runtime Permissions Handler (Camera, GPS Location).
*   [x] Implement CameraX scanner flow (supporting Front/Back camera selection).
*   [x] Implement Fused Location Client coordinate capture and geocoded address helper.
*   [x] Integrate check-in/out calculations (Late Minutes, Left Early, Working Hours).
*   [x] Implement `WorkManager` daily photo purge task.

### Milestone 5: Employee Management & Activity Logging (Admin)
*   [x] Admin screen to add, edit, and search employees (by name/phone).
*   [x] Detailed Employee Profile screen (attendance, payment histories, late sheets).
*   [x] Implement global Activity Logging repository capturing operations.
*   [x] Build Admin logs viewer screen.

### Milestone 6: Salary & Payments Module
*   [x] Implement Payments database transaction processor (Pay, Advance, Extra, Deduct).
*   [x] Admin panel to record salary payments and advances.
*   [x] Employee view of their individual salary ledger.
*   [x] Add dynamic calculations of earnings/dues on top statistics cards.

### Milestone 7: Inventory Management (Admin)
*   [x] Build Raw Material ledger (add, update, delete for Admin).
*   [x] Build Finished Product creator (requires multi-select raw materials + quantity recipes).
*   [x] Build atomic transaction database logic (reduces raw stock, increases product stock).
*   [x] Design stock status dashboards highlighting critical items under minimum limits.

### Milestone 8: Unified Dashboard & Notification Shell
*   [x] Build Admin Dashboard screen with metrics (employee status, stock counts, pending balances).
*   [x] Build Employee Dashboard screen (attendance status, notifications, profile).
*   [x] Implement Notification triggers (Salary Updated, Advance Added, Stock Assigned).
*   [x] Perform system end-to-end local test.

### Milestone 9: Premium UI/UX Refinement & Android 15 Compatibility
*   [x] Realign Welcome Screen elements into a vertical hierarchy and add Breathing logo and Swipe Arrow hint animations.
*   [x] Polished Login inputs, role selector tabs, and main buttons with press-scaling micro-interactions.
*   [x] Optimize bottom navigation by eliminating allocations and transitioning to a layout weight-based sliding spacer capsule.
*   [x] Implement shared-axis horizontal slide & fade page transitions for all application screens.
*   [x] Upgrade CameraX dependencies and disable legacy JNI packaging to guarantee Android 15 16 KB page-size compatibility.


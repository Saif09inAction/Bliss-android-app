# Changelog

All notable changes to the **Laiza** project will be documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

---

## [2.1.0] - 2026-07-17
### Added
*   Redesigned Welcome onboarding screen with centered visual hierarchy, infinite breathing scale animation on Logo, and idle sliding hint animations on swipe arrow handle.
*   Polished Login screen role tabs with press-scaling micro-interactions and added touch-scale animations to primary login button.
*   Optimized Bottom Navigation bar by extracting list allocations and converting to a weight-based sliding spacer pill layout, completely eliminating layout passes and BoxWithConstraints redraw lag.
*   Implemented shared-axis horizontal slide & fade transitions for app screen navigation inside NavGraph.kt and container screens.
*   Upgraded CameraX library dependencies to version 1.4.2, aligning JNI C++ libraries to 16 KB page sizes.
*   Configured jniLibs.useLegacyPackaging = false inside app build.gradle.kts to solve the Android 15 compatibility warning.

## [2.0.0-alpha07] - 2026-07-16
### Added
*   Implemented live dashboard metrics calculations (active workers, low stock count, today's attendance percentage, and net pending dues) inside `DashboardViewModel`.
*   Bound `AdminDashboardScreen` metrics to live database flows.
*   Built dynamic employee `DashboardScreen` displaying check-in status, attendance counters, and live unread notifications.
*   Wired `DashboardScreen` to bottom nav bar in `MainContainerScreen`.

## [2.0.0-alpha06] - 2026-07-16
### Added
*   Implemented `InventoryViewModel` managing material updates, deletions, and assembly logs.
*   Built `InventoryScreen` for Admin view displaying raw material directories and finished products.
*   Added low-stock highlights and alert banners mapping items under minimum limits.
*   Added an "Assemble Run" recipe selection dialog to book product quantities and reduce component stocks.
*   Wired `InventoryScreen` destination inside `AdminContainerScreen`.

## [2.0.0-alpha05] - 2026-07-16
### Added
*   Implemented `SalaryViewModel` for employees to load their personal statements from repositories.
*   Built `SalaryLedgerScreen` for Employee view displaying contract rates, payouts, balance remaining, and advance taken balances.
*   Wired `SalaryLedgerScreen` to the Bottom Navigation Bar tab inside `MainContainerScreen`.

## [2.0.0-alpha04] - 2026-07-16
### Added
*   Implemented `EmployeeViewModel` handling worker search queries, profile details, and payout logs.
*   Built `EmployeeListScreen` for Admin search listings, with a quick-action employee creation dialog.
*   Built `EmployeeProfileScreen` summarizing monthly contracts, salary payouts, and remaining balances, split across attendance and transaction histories.
*   Added an inline transaction registry Dialog in the profile screen to book payouts or advance loans.
*   Built `AuditLogsScreen` to view chronological system activity logging trails.
*   Configured drawer and NavHost routes in `AdminContainerScreen` to link the new admin screens.

## [2.0.0-alpha03] - 2026-07-16
### Added
*   Implemented `MarkAttendanceUseCase` to calculate late minutes and left-early checkouts against configured settings.
*   Added reverse geocoding to `LocationHelper` using coordinates to retrieve physical address strings.
*   Built fullscreen CameraX selfie verification page (`AttendanceCameraScreen.kt`) with front/back camera switcher.
*   Created preview verification details card screen (`AttendancePreviewScreen.kt`) checking GPS coordinate precision.
*   Added spring-animated punch success checkmark page (`AttendanceSuccessScreen.kt`).
*   Wired `AttendanceImageCleanupWorker` background task to delete selfie images older than 10 days and update Room records.
*   Configured Jetpack Compose bottom navigation to route the Attendance tab to the new `AttendanceHomeScreen`.

## [2.0.0-alpha02] - 2026-07-16
### Added
*   Rebuilt the local Room database with 9 core entities (`EmployeeEntity`, `AttendanceEntity`, `AttendanceSettingsEntity`, `PaymentEntity`, `RawMaterialEntity`, `FinishedProductEntity`, `FinishedProductComponentEntity`, `ActivityLogEntity`, `NotificationEntity`).
*   Created abstract repository interfaces under `domain/repository` and implemented database-backed sources under `data/repository`.
*   Mapped Hilt bindings for Room database DAOs in `DatabaseModule.kt` and repository injections in `RepositoryModule.kt`.
*   Implemented Firebase Authentication mapping phone numbers to format `[phone]@laiza.com` for SMS-free login.
*   Updated Splash Screen and Login Screen to align branding with a Business Management System, adding Mobile Number validation and Phone input masks.
*   Purged legacy ERP files and stubbed dashboards and attendance paths with clean compile-ready M3 placeholders.

## [2.0.0-alpha01] - 2026-07-16
### Added
*   Complete redesign planning for the **Laiza Business Management System** based on custom client requirements.
*   Wrote `DATABASE_SCHEMA.md` specifying tables for Room database (Offline-first, Firebase-ready schema).
*   Wrote `API_CONTRACTS.md` defining Repository contracts for Auth, Employees, Attendance, Inventory, and Payments.
*   Updated `ROADMAP.md` mapping out 8 progressive milestones for single-module development.
*   Updated `PROJECT_MEMORY.md` reflecting stack settings, new role restrictions (Admin and Employee), mobile number authentication, and M3 premium styling guidelines.

# Project Memory - Laiza

## Project Overview
*   **App Name**: Laiza
*   **Description**: A custom Business Management System built for a women's purse manufacturing company to streamline daily operations: employee management, salary and transaction tracking, attendance, stock control, raw material tracking, and finished product assembly.
*   **Target Audience**: Owner (Admin) and Workers (Employees).

---

## Technical Stack
*   **Language**: Kotlin
*   **UI System**: Jetpack Compose (Material Design 3)
*   **Architecture**: Clean Architecture + MVVM
*   **Dependency Injection**: Hilt
*   **Navigation**: Navigation Compose
*   **Database**: Room Database (Offline-first, easily migratable to Firebase Firestore)
*   **Authentication**: Firebase Authentication (using mobile number & password mapped behind the scenes to an email)
*   **Session Management**: Preferences DataStore
*   **Background Jobs**: WorkManager (specifically for purging local attendance photos older than 10 days)
*   **OS Services**: CameraX (front/back capture), Google Location Services (Fused Location Client for coordinates and address)

---

## Directory Structure
```
app/src/main/java/com/laiza
│
├── core/                   # Cross-cutting systems
│   ├── di/                 # Hilt Modules (DatabaseModule, AuthModule, RepositoryModule)
│   ├── local/              # Room Database, DAOs, Entities
│   ├── navigation/         # NavHost, NavGraph, Screen definitions
│   ├── theme/              # Material Design 3 Styling, custom color palettes
│   └── utils/              # CameraHelper, LocationHelper, DateUtils, ImagePurgeWorker
│
├── domain/                 # Pure Business Rules (No Android Framework dependencies)
│   ├── model/              # Pure models (Employee, Attendance, Payment, Stock)
│   ├── repository/         # Repository contracts defining access limits
│   └── usecase/            # Business operations (Login, AddEmployee, MarkAttendance, AssembleProduct)
│
├── data/                   # Adapters & Implementations
│   ├── repository/         # Repositories executing Room & Firebase logic
│   └── source/             # Room Local source & Firebase Auth source
│
└── presentation/           # UI Presentation layer
    ├── auth/               # Splash Screen, Login Screen
    ├── admin/              # Admin Dashboard, Employee List/Profile, Payments, Logs UI
    ├── employee/           # Employee Dashboard, Salary Summary, Attendance screen
    └── common/             # Reusable M3 widgets (AppTextField, GradientCard, AppButton)
```

---

## Architectural Decisions
1.  **Strict Repository Separation**: Presentation and Domain layers depend only on Domain Repository interfaces. Data adapters are bound via Hilt. This ensures migrating from Room to Firebase Firestore in the future requires *zero* changes to ViewModels or Compositions.
2.  **Firebase Auth Phone/Password Bridge**: Firebase Authentication does not offer native password-based credentials for phone numbers without SMS OTP. To meet the user requirement, the `AuthRepositoryImpl` maps the mobile input transparently to `<phone>@laiza.com` when executing Firebase Auth queries.
3.  **Automatic Inventory Reduction**: Creating a finished product decrements raw material records and increments product counts in a single atomic database transaction.
4.  **WorkManager Photo Purge**: Attendance selfies are stored in the local file system. A Daily `WorkManager` task checks timestamps and deletes image files older than 10 days while keeping metadata.

---

## UI Design Guidelines (Rich Aesthetics)
*   **Core Colors**: Professional deep slate dark theme or rich warm colors (e.g., Deep plum `#4C1D95` primary, Rose gold `#F43F5E` secondary, Warm neutral canvas `#FAF9F6`).
*   **Visual Highlights**: Subtle glassmorphism sheets (`Surface` with alpha and border strokes), smooth gradient cards, modern Google Fonts (Inter/Outfit), and smooth micro-animations.
*   **Layout**: Balanced paddings, large readable text fields, explicit badges for stock levels (red alert for minimum stock), and visual feedback during transactions.

---

## Reusable Components Plan
*   `LaizaButton`: Elegant button with state changes and rounded corners.
*   `LaizaTextField`: Styled fields with validation support.
*   `GradientHeader`: Banner showing user greetings or dashboard balances.
*   `StatsCard`: Sleek card for tracking indicators (Total Employees, Present count).
*   `ActivityLogItem`: Grid list row documenting actions with timestamps.

---

## Module Status
*   **Aesthetics Redesign**: Completed (Centering Welcome screen, breathing logo, bounce indicators, tactile press animations)
*   **Android 15 Compatibility**: Resolved (Upgraded CameraX to `1.4.2` and set `useLegacyPackaging = false` to align JNI native libraries to 16 KB page sizes)
*   **Performance Optimization**: Completed (Switched navigation rail calculations to weight-based Spacer sliding systems to avoid BoxWithConstraints subcompositions and inline allocations)


# Nihonix — Software Requirements Specification

Updated: 2026-11-16. Source: `nihonix.docx`, Android source and the supplied backend audit.

**Section 02 security update (2026-11-16):** [Authorization matrix](planning/remaining-work/evidence/authorization-matrix.md) records 82 routes and [security evidence](planning/remaining-work/evidence/security-review.md) covers liveness-only public AI health, authenticated quota compatibility, ownership/deletion failure-and-retry checks and dependency triage. D02/OQ-015 is Accepted by the user on 2026-11-16: all catalog reads require login per BR-001. Coordinated backend guards and Android session/cache/background-sync changes are tracked in the [D02 ledger](planning/remaining-work/evidence/catalog-authentication.md). Dependency vulnerabilities and infrastructure retention remain follow-up work; local tests do not establish whole-product security acceptance.

**Sprint 09 update (draft, 2026-11-16):** Acceptance evidence for AC-001–AC-017 is indexed in [Sprint 09 acceptance](sprints/sprint-09-acceptance.md): 15 criteria have local evidence and 2 are partial (AC-003, AC-015). No criterion is accepted on a shared environment; shared deployment/HTTPS, real SMTP/Cloudinary/FCM/Gemini providers, physical devices and measured NFR targets remain open. FR/AC dispositions below were reconciled with the Sprint 02–09 reports on this date.

**Sprint 08 update:** Engaged-time activity, timezone-aware streak/calendar, FCM device registration and reminder scheduling are implemented and locally verified (60 engaged seconds qualify a streak day; the daily goal uses daily_goal_minutes). The reminder scheduler is off by default and no real notification has been delivered. [Contract](contracts/sprint-08-api.md) · [Report](sprints/sprint-08-report.md) · [Firebase setup](troubleshooting/firebase-fcm.md).

**Sprint 07 update:** AI quota, private fill-in/choice practice, durable submissions/results and explicit generated-card preview/save are implemented. User-approved policy: flashcards2/day, shared practice3/day, max15, reset00:00 Vietnam. Local fixtures cover concurrency/replay/refunds/rollback; local verification passed. FR-078 completion records were delivered here; Sprint08 added the activity/streak aggregation. [Contract](contracts/sprint-07-api.md) · [Ledger](sprints/sprint-07-implementation.md).

**Sprint 06 update:** Personal folders/sets/cards, atomic bulk edits/deletion, offline review and account-private synchronization are implemented. User-selected conflict handling preserves local edits and offers server copy or save-copy. Room chunk storage avoids large-row limits; AI FR-069 remains Sprint07. [Contract](contracts/sprint-06-api.md) · [Verification and rollout](sprints/sprint-06-report.md).

**Sprint 05 update:** Persistent public catalog/ETag caching, authoritative deletion inventory, Room/WorkManager outbox and manual learned/unlearned progress with topic/level summaries are implemented and locally tested. [Contract](contracts/sprint-05-api.md) · [Verification/rollout limits](sprints/sprint-05-report.md). Offline covers previously fetched content; flashcard/activity producers and shared rollout remain separate.

**Sprint 04 update:** Vocabulary sets/items and grammar sub-topics/patterns now have online list/detail flows, multilingual search, filters, pagination and rich Japanese examples. See [test evidence](sprints/sprint-04-report.md) and [API contract](contracts/sprint-04-api.md). Offline FR-036/FR-044 and learning progress remain Sprint 05; shared-environment acceptance is pending.

**Sprint 03 update:** Level counts, complete topic bundles and startup configuration/health/maintenance/version behavior are implemented and locally tested. [Sprint 03 evidence](sprints/sprint-03-report.md) and [canonical contracts](contracts/sprint-03-api.md) supersede older roadmap/configuration observations. The historical public-read policy is superseded by D02 (2026-11-16): all catalog reads require login; catalog mutations require Admin. FR-027 remains Sprint 05. Shared deployment and production HTTPS acceptance remain open.

**Sprint 02 update:** Android/backend account and profile changes are implemented and locally tested. Verification, recovery, refresh rotation/revocation, profile editing, avatars, preferences and deletion now have evidence in the [Sprint 02 report](sprints/sprint-02-report.md) and [API contract](contracts/sprint-02-api.md). These supersede older source-review notes for FR-001–FR-020. FR-008 device cleanup and reminder delivery were completed locally in Sprint 08. Shared backend migration and actual SMTP/Cloudinary acceptance remain pending; whole-product acceptance is not claimed.

**Sprint 01 update:** Security/environment changes, Android/backend build results and live read-only API checks are recorded in the [execution report](sprints/sprint-01-report.md). The [backend audit](contracts/backend-audit.md) and [offline design](architecture/offline-sync.md) supersede the initial review's missing-backend and proposed architecture assumptions. Unchanged initial source-review notes remain historical observations, not claims about the latest code.

This document retains the original product requirements, feature numbering, FR-001–FR-105, BR-001–BR-019, NFR-001–NFR-026, and AC-001–AC-017. Requirements using “must”, “should”, or future journeys describe target behavior unless marked as implemented. Source-review notes describe current behavior and take precedence over inherited statements about the existing application. The unchanged text/table conversion is [nihonix-original.md](nihonix-original.md).

**Status convention:** “Implemented in client” means the inspected source contains client logic and wiring; it does not mean the backend or end-to-end behavior passed testing. “Partial” means some required behavior exists. “Planned” means no implementation was found in the inspected app. “Unverified” means external/backend evidence is needed. “Implemented and locally tested” means source plus automated or emulator evidence on a developer machine (unit, instrumented, isolated PostgreSQL fixtures, local backend); it is not shared-environment acceptance.

**Current scope:** secure registration/email verification, username/password login, recovery, session rotation/revocation, profile editing/avatar/deletion/settings, JLPT selection/update, topic browsing/counts/bundles, and startup configuration/health/maintenance/version behavior are implemented and locally tested. Online vocabulary/grammar browsing and details are delivered in Sprint 04. Persistent offline study and vocabulary/grammar progress synchronization are delivered locally in Sprint 05. Manual flashcards/review/synchronization are delivered locally in Sprint06. AI/quota and practice are implemented in Sprint07; streak/calendar and FCM notifications are implemented locally in Sprint08; Sprint09 records acceptance evidence. Shared deployment/provider acceptance remains pending; see the execution reports above.

## 1. Introduction

### 1.1. Purpose

Nihonix là ứng dụng mobile hỗ trợ người dùng học tiếng Nhật theo lộ trình từ N5 đến N1.

Ứng dụng cung cấp hệ thống nội dung học tập có cấu trúc, bao gồm từ vựng, ngữ pháp, flashcard và bài luyện tập. Người dùng có thể theo dõi tiến độ học, duy trì chuỗi ngày học (streak), cá nhân hóa lịch học và sử dụng AI để tạo nội dung phục vụ việc học.

Ứng dụng hướng đến trải nghiệm học tập cá nhân, có khả năng hoạt động offline đối với nội dung đã đồng bộ và tự động đồng bộ dữ liệu khi có kết nối mạng.

### 1.2. Project Scope

> **Source review (updated Sprint 09):** The Android app module and the sibling NestJS backend deliver F1–F13 locally; see each feature section and [Sprint 09 acceptance](sprints/sprint-09-acceptance.md) for what is and is not accepted. Capabilities not delivered remain target scope (for example SM-2 scheduling and a home dashboard).

| Hạng mục | Mô tả |
| --- | --- |
| Product | Nihonix |
| Platform | Android Mobile |
| Application type | Japanese Learning Application |
| Target users | Người học tiếng Nhật N5–N1 |
| Backend | RESTful API |
| Authentication | JWT |
| Content | Vocabulary, Grammar, Topics |
| AI | AI-generated flashcards and exercises |
| Offline | Local content caching and data synchronization |
| Notification | Firebase Cloud Messaging (FCM) |

### 1.3. Objectives

Cung cấp lộ trình học tiếng Nhật theo cấp độ JLPT N5–N1.

Tổ chức nội dung học thành các Level và Topic.

Hỗ trợ học từ vựng và ngữ pháp.

Cho phép người dùng tạo và quản lý flashcard cá nhân.

Tạo bài luyện tập bằng AI.

Theo dõi tiến độ học tập và thống kê kết quả.

Duy trì động lực học thông qua streak và nhắc nhở.

Hỗ trợ học offline và đồng bộ dữ liệu giữa các phiên sử dụng.

### 1.4. Out of Scope

Các chức năng sau không thuộc phạm vi phiên bản này:

Spaced Repetition System (SM-2).

Social Login (Google, Facebook).

Bookmark và ghi chú cá nhân.

Achievements và bảng xếp hạng.

Thanh toán và subscription.

Luyện nghe, luyện viết và luyện Kanji chuyên biệt.

Web Admin UI.

Đa ngôn ngữ giao diện ứng dụng.

## 2. User Roles & Permissions

### 2.1. User Roles

| Role | Description |
| --- | --- |
| Guest | Người chưa đăng nhập |
| Authenticated User | Người dùng đã đăng nhập bằng tài khoản |
| Admin | Quản trị viên quản lý nội dung học tập |

### 2.2. Permission Matrix

> **Source review:** The matrix below is the target authorization policy. Current navigation starts at login without a stored access token and topics with one. Public level/topic Retrofit requests explicitly omit authentication; this conflicts with the blanket guest prohibition and BR-001. No guest browsing entry is exposed in the current navigation. Admin authorization and ownership enforcement cannot be established without backend source. See [AuthNavGraph](../app/src/main/kotlin/com/nihonix/mobile/feature/auth/presentation/navigation/AuthNavGraph.kt) and [TopicsLevelsApiService](../app/src/main/kotlin/com/nihonix/mobile/feature/topics/data/api/TopicsLevelsApiService.kt), [TopicsLevelsRepositoryImpl](../app/src/main/kotlin/com/nihonix/mobile/feature/topics/data/repository/TopicsLevelsRepositoryImpl.kt).

| Feature | Guest | User | Admin |
| --- | --- | --- | --- |
| Register / Login | ✓ | ✓ | ✓ |
| View learning content | – | ✓ | ✓ |
| View levels and topics | – | ✓ | ✓ |
| Learn vocabulary | – | ✓ | ✓ |
| Learn grammar | – | ✓ | ✓ |
| Manage personal flashcards | – | ✓ | ✓ |
| Generate AI content | – | ✓ | ✓ |
| Practice exercises | – | ✓ | ✓ |
| Track learning progress | – | ✓ | ✓ |
| Manage profile and settings | – | ✓ | ✓ |
| Manage devices | – | ✓ | ✓ |
| Manage learning content | – | – | ✓ |

Business rules:

Người dùng phải đăng nhập mới được truy cập nội dung học tập.

Tài khoản chưa xác minh email không được phép đăng nhập.

Người dùng chỉ được truy cập và chỉnh sửa dữ liệu cá nhân thuộc tài khoản của mình.

Admin có quyền quản lý nội dung học tập thông qua các API được phân quyền.

## 3. System Overview

### 3.1. Application Architecture

> **Source review:** Current architecture: Compose screens → ViewModels/StateFlow → domain repository interfaces → repository implementations → Retrofit/OkHttp REST API. ServiceLocator and AppViewModelFactory wire dependencies. Sprint 01 token storage uses EncryptedSharedPreferences and fails closed; user JSON uses ordinary private SharedPreferences. No Room, WorkManager, Hilt, AI or FCM integration is present. The following client-server diagram describes target product architecture.

Nihonix sử dụng kiến trúc Client–Server.

Android Application

UI · ViewModel · Repository · Local Database

HTTPS · REST API · JWT

Nihonix Backend

Authentication · Learning · Progress · AI · Sync

Database

User data · Learning content

AI Services

Flashcards · Practice generation

FCM

Push notifications

Scheduler

Daily learning reminders

### 3.2. Main Application Modules

> **Source review (updated Sprint 09):** Authentication, profile/settings, roadmap, vocabulary/grammar, offline/progress, flashcards, practice/AI quota, activity/streak, notifications and application configuration all have feature implementations (Sprints 01–08). JLPT level mutation belongs to the topics feature. Shared-environment acceptance is open.

| Module | Responsibilities |
| --- | --- |
| Authentication | Register, login, email verification, password recovery |
| User Profile | Profile, avatar, learning preferences |
| Learning Roadmap | JLPT levels, topics and learning content |
| Vocabulary | Vocabulary items and vocabulary sets |
| Grammar | Grammar sub-topics and grammar patterns |
| Flashcards | Personal folders, sets and cards |
| AI Practice | Fill-in-the-blank and multiple-choice exercises |
| Learning Progress | Track completed learning activities |
| Streak & Calendar | Daily learning activity and statistics |
| Offline & Synchronization | Cache learning content and synchronize changes |
| Notifications | Learning reminders through FCM |
| AI Quota | Monitor AI usage limits |

## 4. Functional Requirements

### 4.1. F1 – Authentication & Account Management

> **Source review (updated Sprint 09):** Current flow: register with username, email, password (8 characters, 72 bytes maximum, aligned with reset) and optional full name → email verification code (expiring, resend cooldown, attempt limits) → sign in. Login submits username/password and rejects unverified accounts. Recovery uses request code → verify code → reset with a purpose-scoped temporary token. Refresh tokens rotate; replay is rejected and logout revokes the session, which also removes the device's push binding. Evidence: [Sprint 02](sprints/sprint-02-report.md), [Sprint 08](sprints/sprint-08-report.md) and the Sprint 09 fixtures. Real SMTP delivery and shared-environment acceptance are pending. The original verification flows below describe the target behavior.

Description: Allow users to register, authenticate, verify email and manage their account credentials.

| ID | Functional Requirement | Priority | Client status | Evidence / remaining work |
| --- | --- | --- | --- | --- |
| FR-001 | Users can register with username, email, password and optional full name. | High | Implemented and locally tested | Registration requires username; full name optional; password policy aligned with the backend ([Sprint 02](sprints/sprint-02-report.md)). [AuthApiService](../app/src/main/kotlin/com/nihonix/mobile/feature/auth/data/api/AuthApiService.kt), [AuthRepositoryImpl](../app/src/main/kotlin/com/nihonix/mobile/feature/auth/data/repository/AuthRepositoryImpl.kt) |
| FR-002 | System sends an email verification code after registration. | High | Implemented and locally tested | Registration issues an expiring email code with resend cooldown and attempt limits ([Sprint 02](sprints/sprint-02-report.md)); SMTP was a fake provider, real delivery pending. Original gap: [backend audit](contracts/backend-audit.md). |
| FR-003 | Users can verify their email using a verification code. | High | Implemented and locally tested | Verify-email API and Android verification screen (Sprint 02; Sprint 09 emulator journey). [AuthApiService](../app/src/main/kotlin/com/nihonix/mobile/feature/auth/data/api/AuthApiService.kt), [AuthRepositoryImpl](../app/src/main/kotlin/com/nihonix/mobile/feature/auth/data/repository/AuthRepositoryImpl.kt) |
| FR-004 | Users can log in with username and password; verified-email enforcement remains required. | High | Implemented and locally tested | Username/password login; the backend rejects unverified accounts (Sprint 02 `test:sprint02`). [AuthApiService](../app/src/main/kotlin/com/nihonix/mobile/feature/auth/data/api/AuthApiService.kt), [AuthRepositoryImpl](../app/src/main/kotlin/com/nihonix/mobile/feature/auth/data/repository/AuthRepositoryImpl.kt) |
| FR-005 | System must reject login attempts from unverified accounts. | High | Implemented and locally tested | Backend rejects unverified login (Sprint 02 fixture); Android routes to verification. Existing accounts must verify after the migration. |
| FR-006 | System provides access token and refresh token. | High | Implemented and locally tested | Encrypted token storage with 401 refresh and rotation; backend rotation, replay rejection and logout revocation verified by the Sprint 02 and Sprint 09 fixtures. [TokenStorage](../app/src/main/kotlin/com/nihonix/mobile/core/storage/TokenStorage.kt) |
| FR-007 | System supports token refresh and rotation. | High | Implemented and locally tested | Encrypted token storage with 401 refresh and rotation; backend rotation, replay rejection and logout revocation verified by the Sprint 02 and Sprint 09 fixtures. [TokenStorage](../app/src/main/kotlin/com/nihonix/mobile/core/storage/TokenStorage.kt) |
| FR-008 | Users can log out and unregister their device token. | High | Implemented and locally tested | Logout revokes the validated server session, which removes the device push binding, and always clears the local session and suppresses push locally ([Sprint 08](sprints/sprint-08-report.md)). Offline logout cannot revoke the server session until connectivity or expiry. [AuthApiService](../app/src/main/kotlin/com/nihonix/mobile/feature/auth/data/api/AuthApiService.kt), [AuthRepositoryImpl](../app/src/main/kotlin/com/nihonix/mobile/feature/auth/data/repository/AuthRepositoryImpl.kt) |
| FR-009 | Users can request a password reset code. | High | Implemented and locally tested | Password recovery uses email, code, purpose-scoped temporary reset token, then new password; code reuse is rejected (Sprint 02 fixture). [AuthApiService](../app/src/main/kotlin/com/nihonix/mobile/feature/auth/data/api/AuthApiService.kt), [AuthRepositoryImpl](../app/src/main/kotlin/com/nihonix/mobile/feature/auth/data/repository/AuthRepositoryImpl.kt) |
| FR-010 | Users can verify the reset code and set a new password. | High | Implemented and locally tested | Password recovery uses email, code, purpose-scoped temporary reset token, then new password; code reuse is rejected (Sprint 02 fixture). [AuthApiService](../app/src/main/kotlin/com/nihonix/mobile/feature/auth/data/api/AuthApiService.kt), [AuthRepositoryImpl](../app/src/main/kotlin/com/nihonix/mobile/feature/auth/data/repository/AuthRepositoryImpl.kt) |
| FR-011 | Users can resend verification or reset codes. | Medium | Implemented and locally tested | Resend with cooldown for registration verification and password recovery (Sprint 02). [AuthApiService](../app/src/main/kotlin/com/nihonix/mobile/feature/auth/data/api/AuthApiService.kt), [AuthRepositoryImpl](../app/src/main/kotlin/com/nihonix/mobile/feature/auth/data/repository/AuthRepositoryImpl.kt) |

Main flow – Registration:

User opens the registration screen.

User enters email and password.

Application submits registration information.

Backend creates the account and sends an email verification code.

User enters the verification code.

Backend verifies the code and activates the account.

User can proceed to login.

Main flow – Login:

User enters email and password.

Application sends login request.

Backend validates credentials and email verification status.

Backend returns authentication tokens.

Application stores authentication data securely.

Application navigates to the main screen.

Business rules:

Unverified accounts cannot access authenticated features.

Access token must be attached to authenticated API requests.

Refresh token must be handled according to the backend's token rotation mechanism.

Logout must remove the registered FCM device token.

Authentication errors must be displayed with understandable messages.

### 4.2. F2 – User Profile & Settings

> **Source review (updated Sprint 09):** Profile viewing/editing, avatar upload, confirmed account deletion, learning settings (push, reminder, time, timezone, daily goal) and JLPT level are implemented and locally tested ([Sprint 02](sprints/sprint-02-report.md), [Sprint 08](sprints/sprint-08-report.md)). Real Cloudinary upload and the deletion retention/backup policy remain to be accepted.

Description: Allow users to manage their personal information and learning preferences.

| ID | Functional Requirement | Priority | Client status | Evidence / remaining work |
| --- | --- | --- | --- | --- |
| FR-012 | Users can view their profile. | High | Implemented and locally tested | Full profile fetched using POST auth/me (Sprint 02). [AuthApiService](../app/src/main/kotlin/com/nihonix/mobile/feature/auth/data/api/AuthApiService.kt), [AuthRepositoryImpl](../app/src/main/kotlin/com/nihonix/mobile/feature/auth/data/repository/AuthRepositoryImpl.kt) |
| FR-013 | Users can update their personal information. | High | Implemented and locally tested | Owner-scoped profile update with validation and an Android edit-profile screen ([Sprint 02](sprints/sprint-02-report.md)). |
| FR-014 | Users can upload or update their avatar. | Medium | Implemented and locally tested | Photo-picker upload, content validation and old-avatar cleanup (Sprint 02). Real Cloudinary upload acceptance pending; a fake storage provider was used. |
| FR-015 | Users can delete their account. | Medium | Implemented and locally tested | Confirmed deletion; the backend invalidates sessions and cascades owned data (Sprint 02 and Sprint 09 fixtures). Retention/backup policy review pending. |
| FR-016 | Users can view and update their current JLPT level. | High | Implemented and locally tested | Authenticated GET/PATCH users/me/level; N5–N1 validation and cached-user update. [TopicsLevelsApiService](../app/src/main/kotlin/com/nihonix/mobile/feature/topics/data/api/TopicsLevelsApiService.kt), [TopicsLevelsRepositoryImpl](../app/src/main/kotlin/com/nihonix/mobile/feature/topics/data/repository/TopicsLevelsRepositoryImpl.kt) |
| FR-017 | Users can view learning settings. | High | Implemented and locally tested | GET users/me/settings, shown in the profile screen (Sprint 02; Sprint 09 emulator journey matched the server). |
| FR-018 | Users can configure push notification preferences. | Medium | Implemented and locally tested | push_enabled and reminder_enabled saved and honored by the app and scheduler (Sprint 02, Sprint 08). |
| FR-019 | Users can configure daily learning goals. | Medium | Implemented and locally tested | daily_goal_minutes validated and saved; drives goal completion and reminder suppression (Sprint 02, Sprint 08). |
| FR-020 | Users can configure reminder time and timezone. | Medium | Implemented and locally tested | reminder_time and IANA timezone validated and saved (Sprint 02, Sprint 08). |

User settings include:

| Setting | Description |
| --- | --- |
| push_enabled | Enable or disable push notifications |
| reminder_enabled | Enable or disable learning reminders |
| reminder_time | Preferred reminder time |
| timezone | User's timezone |
| daily_goal_minutes | Daily learning goal in minutes |

Business rules:

Each user can only manage their own profile and settings.

Account deletion must remove or anonymize associated personal data according to the backend's deletion policy.

Avatar uploads must validate file type and size.

Changing the user's JLPT level must not automatically delete existing learning progress.

### 4.3. F3 – Learning Roadmap (Level & Topic)

> **Source review:** Current behavior includes N5→N1 ordering, All-level filtering, level detail, topic ordering, local title/slug search, refresh/retry, and a dialog to update the user's JLPT level. Search filters the fetched topic list; it is not vocabulary search or server-side pagination. Topic detail shows metadata and localized names; no vocabulary/grammar bundle or persistent cache exists.

Description: Provide a structured learning roadmap from JLPT N5 to N1.

Bunpo: Learn Japanese for Android - Download

Passing JLPT—The Ultimate Study Companion (Bunpo App Review) | Most Wandered

PORO - Japanese Grammar - Education App | MWM

12

| ID | Functional Requirement | Priority | Client status | Evidence / remaining work |
| --- | --- | --- | --- | --- |
| FR-021 | Users can view all available JLPT levels. | High | Implemented and locally tested | Level list/detail and topic list/detail with persistent cache; verified against a local backend ([Sprint 03](sprints/sprint-03-report.md) fixture, [Sprint 05](sprints/sprint-05-report.md) cache, Sprint 09 emulator run). Shared data not verified. [TopicsLevelsApiService](../app/src/main/kotlin/com/nihonix/mobile/feature/topics/data/api/TopicsLevelsApiService.kt), [TopicsLevelsRepositoryImpl](../app/src/main/kotlin/com/nihonix/mobile/feature/topics/data/repository/TopicsLevelsRepositoryImpl.kt) |
| FR-022 | System displays the number of topics, vocabulary items and grammar content per level. | High | Implemented and locally tested | Count DTO/UI and consistent PostgreSQL aggregate queries; [Sprint 03 evidence](sprints/sprint-03-report.md). Shared rollout pending. |
| FR-023 | Users can view detailed information about a selected level. | High | Implemented and locally tested | Level list/detail and topic list/detail with persistent cache; verified against a local backend ([Sprint 03](sprints/sprint-03-report.md) fixture, [Sprint 05](sprints/sprint-05-report.md) cache, Sprint 09 emulator run). Shared data not verified. [TopicsLevelsApiService](../app/src/main/kotlin/com/nihonix/mobile/feature/topics/data/api/TopicsLevelsApiService.kt), [TopicsLevelsRepositoryImpl](../app/src/main/kotlin/com/nihonix/mobile/feature/topics/data/repository/TopicsLevelsRepositoryImpl.kt) |
| FR-024 | Users can browse topics belonging to a level. | High | Implemented and locally tested | Level list/detail and topic list/detail with persistent cache; verified against a local backend ([Sprint 03](sprints/sprint-03-report.md) fixture, [Sprint 05](sprints/sprint-05-report.md) cache, Sprint 09 emulator run). Shared data not verified. [TopicsLevelsApiService](../app/src/main/kotlin/com/nihonix/mobile/feature/topics/data/api/TopicsLevelsApiService.kt), [TopicsLevelsRepositoryImpl](../app/src/main/kotlin/com/nihonix/mobile/feature/topics/data/repository/TopicsLevelsRepositoryImpl.kt) |
| FR-025 | Users can view topic details. | High | Implemented and locally tested | Level list/detail and topic list/detail with persistent cache; verified against a local backend ([Sprint 03](sprints/sprint-03-report.md) fixture, [Sprint 05](sprints/sprint-05-report.md) cache, Sprint 09 emulator run). Shared data not verified. [TopicsLevelsApiService](../app/src/main/kotlin/com/nihonix/mobile/feature/topics/data/api/TopicsLevelsApiService.kt), [TopicsLevelsRepositoryImpl](../app/src/main/kotlin/com/nihonix/mobile/feature/topics/data/repository/TopicsLevelsRepositoryImpl.kt) |
| FR-026 | Users can retrieve a complete topic bundle. | High | Implemented and locally tested | Versioned bundle includes vocabulary sets/items and grammar groups/patterns; Android content tabs and retry/empty states. [Contract](contracts/sprint-03-api.md). Offline persistence deferred. |
| FR-027 | Application supports caching topic content for offline access. | High | Implemented and locally tested | Room/ETag cache and account-scoped durable progress; see [Sprint 05 contract](contracts/sprint-05-api.md) and [verification](sprints/sprint-05-report.md). Offline scope is previously fetched content; shared rollout pending. |

Content hierarchy:

JLPT Level

N5 · N4 · N3 · N2 · N1

Topic

Learning unit

Vocabulary

Sets and items

Grammar

Sub-topics and patterns

Main flow:

User opens the learning roadmap.

Application retrieves available JLPT levels.

User selects a level.

Application displays topics belonging to that level.

User selects a topic.

Application loads the topic bundle.

User accesses vocabulary and grammar content.

Business rules:

Learning content is available only to authenticated users.

A topic belongs to a specific learning level.

A topic bundle should contain the topic information, vocabulary, vocabulary sets and grammar content required by the topic.

The application should use ETag to determine whether cached content needs updating.

Offline mode should use previously downloaded content instead of requiring a network connection.

### 4.4. F4 – Vocabulary Learning

> **Sprint 04/05 source review:** Vocabulary sets/items have list/detail/search/filter/page flows, rich Japanese content, persistent public cache and explicit learned/unlearned actions. Shared rollout is pending.

Description: Allow users to browse and study Japanese vocabulary.

Flippon - Japanese Vocabulary - Education App | MWM

#分享 自己開發的 AI 單字卡 App - 日語板 | Dcard

QUARTET Vocab & Kanji - Education App | MWM

10

| ID | Functional Requirement | Priority | Client status | Evidence / remaining work |
| --- | --- | --- | --- | --- |
| FR-028 | Users can view vocabulary items. | High | Implemented and locally tested | Lists/details, filters/search, paging and Japanese content have [Sprint 04 evidence](sprints/sprint-04-report.md); shared-environment acceptance pending. |
| FR-029 | Users can filter vocabulary by topic. | High | Implemented and locally tested | Lists/details, filters/search, paging and Japanese content have [Sprint 04 evidence](sprints/sprint-04-report.md); shared-environment acceptance pending. |
| FR-030 | Users can search vocabulary by keyword. | High | Implemented and locally tested | Lists/details, filters/search, paging and Japanese content have [Sprint 04 evidence](sprints/sprint-04-report.md); shared-environment acceptance pending. |
| FR-031 | Users can view vocabulary details. | High | Implemented and locally tested | Lists/details, filters/search, paging and Japanese content have [Sprint 04 evidence](sprints/sprint-04-report.md); shared-environment acceptance pending. |
| FR-032 | Users can browse vocabulary sets. | High | Implemented and locally tested | Lists/details, filters/search, paging and Japanese content have [Sprint 04 evidence](sprints/sprint-04-report.md); shared-environment acceptance pending. |
| FR-033 | Users can view vocabulary sets by topic. | High | Implemented and locally tested | Lists/details, filters/search, paging and Japanese content have [Sprint 04 evidence](sprints/sprint-04-report.md); shared-environment acceptance pending. |
| FR-034 | Users can view vocabulary set details. | High | Implemented and locally tested | Lists/details, filters/search, paging and Japanese content have [Sprint 04 evidence](sprints/sprint-04-report.md); shared-environment acceptance pending. |
| FR-035 | Application supports pagination for vocabulary lists. | Medium | Implemented and locally tested | Lists/details, filters/search, paging and Japanese content have [Sprint 04 evidence](sprints/sprint-04-report.md); shared-environment acceptance pending. |
| FR-036 | Application caches vocabulary content for offline learning. | High | Implemented and locally tested | Room/ETag cache and account-scoped durable progress; see [Sprint 05 contract](contracts/sprint-05-api.md) and [verification](sprints/sprint-05-report.md). Offline scope is previously fetched content; shared rollout pending. |

Vocabulary information should support the fields provided by backend data, such as:

Japanese word.

Reading (Hiragana/Katakana).

Kanji representation, when available.

Meaning or translation.

Example sentences.

Related learning topic.

Other pronunciation or explanation metadata if available.

Lưu ý: Đây là các trường nội dung mong muốn ở giao diện; cần đối chiếu schema thực tế trước khi cố định data model.

### 4.5. F5 – Grammar Learning

> **Sprint 04/05 source review:** Grammar groups/patterns have list/detail/search/filter/page flows, examples/conjugation, persistent public cache and explicit learned/unlearned actions. Shared rollout is pending.

Description: Allow users to study Japanese grammar through structured grammar topics and patterns.

The Best Bunpro Alternative App for Japanese Grammar (2025)

Bunpo! - Une application pour apprendre la grammaire japonaise !

6

| ID | Functional Requirement | Priority | Client status | Evidence / remaining work |
| --- | --- | --- | --- | --- |
| FR-037 | Users can browse grammar sub-topics. | High | Implemented and locally tested | Lists/details, filters/search, paging and Japanese content have [Sprint 04 evidence](sprints/sprint-04-report.md); shared-environment acceptance pending. |
| FR-038 | Users can filter grammar sub-topics by JLPT level. | High | Implemented and locally tested | Lists/details, filters/search, paging and Japanese content have [Sprint 04 evidence](sprints/sprint-04-report.md); shared-environment acceptance pending. |
| FR-039 | Users can view grammar sub-topic details. | High | Implemented and locally tested | Lists/details, filters/search, paging and Japanese content have [Sprint 04 evidence](sprints/sprint-04-report.md); shared-environment acceptance pending. |
| FR-040 | Users can browse grammar patterns. | High | Implemented and locally tested | Lists/details, filters/search, paging and Japanese content have [Sprint 04 evidence](sprints/sprint-04-report.md); shared-environment acceptance pending. |
| FR-041 | Users can search and filter grammar patterns. | Medium | Implemented and locally tested | Lists/details, filters/search, paging and Japanese content have [Sprint 04 evidence](sprints/sprint-04-report.md); shared-environment acceptance pending. |
| FR-042 | Users can view grammar pattern details. | High | Implemented and locally tested | Lists/details, filters/search, paging and Japanese content have [Sprint 04 evidence](sprints/sprint-04-report.md); shared-environment acceptance pending. |
| FR-043 | Users can view grammar patterns belonging to a sub-topic. | High | Implemented and locally tested | Lists/details, filters/search, paging and Japanese content have [Sprint 04 evidence](sprints/sprint-04-report.md); shared-environment acceptance pending. |
| FR-044 | Application supports offline grammar content. | High | Implemented and locally tested | Room/ETag cache and account-scoped durable progress; see [Sprint 05 contract](contracts/sprint-05-api.md) and [verification](sprints/sprint-05-report.md). Offline scope is previously fetched content; shared rollout pending. |

Grammar content should support:

Grammar pattern.

Meaning and explanation.

Usage instructions.

Example sentences.

Related JLPT level.

Related grammar sub-topic.

### 4.6. F6 – Offline Content & Synchronization

> **Sprint 05 source review:** Room cache/ETag restoration, authoritative deletion invalidation and WorkManager outbox now exist. Offline scope is previously fetched projections; see the contract for limits and rollout gates.

Description: Allow users to download learning content and access it without an internet connection.

| ID | Functional Requirement | Priority | Client status | Evidence / remaining work |
| --- | --- | --- | --- | --- |
| FR-045 | Application can retrieve synchronization metadata. | High | Implemented and locally tested | Room/ETag cache and account-scoped durable progress; see [Sprint 05 contract](contracts/sprint-05-api.md) and [verification](sprints/sprint-05-report.md). Offline scope is previously fetched content; shared rollout pending. |
| FR-046 | Application can compare local and remote content versions. | High | Implemented and locally tested | Room/ETag cache and account-scoped durable progress; see [Sprint 05 contract](contracts/sprint-05-api.md) and [verification](sprints/sprint-05-report.md). Offline scope is previously fetched content; shared rollout pending. |
| FR-047 | Application can download content when the local version is outdated. | High | Implemented and locally tested | Room/ETag cache and account-scoped durable progress; see [Sprint 05 contract](contracts/sprint-05-api.md) and [verification](sprints/sprint-05-report.md). Offline scope is previously fetched content; shared rollout pending. |
| FR-048 | Application supports ETag-based cache validation. | High | Implemented and locally tested | Room/ETag cache and account-scoped durable progress; see [Sprint 05 contract](contracts/sprint-05-api.md) and [verification](sprints/sprint-05-report.md). Offline scope is previously fetched content; shared rollout pending. |
| FR-049 | Application handles HTTP 304 responses without downloading unchanged content. | High | Implemented and locally tested | Room/ETag cache and account-scoped durable progress; see [Sprint 05 contract](contracts/sprint-05-api.md) and [verification](sprints/sprint-05-report.md). Offline scope is previously fetched content; shared rollout pending. |
| FR-050 | Application can access previously downloaded learning content offline. | High | Implemented and locally tested | Room/ETag cache and account-scoped durable progress; see [Sprint 05 contract](contracts/sprint-05-api.md) and [verification](sprints/sprint-05-report.md). Offline scope is previously fetched content; shared rollout pending. |
| FR-051 | Application can synchronize pending user data when connectivity is restored. | High | Implemented and locally tested | Room/ETag cache and account-scoped durable progress; see [Sprint 05 contract](contracts/sprint-05-api.md) and [verification](sprints/sprint-05-report.md). Offline scope is previously fetched content; shared rollout pending. |

Supported synchronization entities:

Level.

Topic.

Vocabulary item.

Vocabulary set.

Grammar sub-topic.

Grammar pattern.

Content synchronization flow:

Application checks network availability.

Application requests the complete authoritative catalog inventory (all six collections); browsing remains filterable by level.

Backend returns entity keys/content versions; resource responses carry ETags. Inventory absence represents deletion.

Application compares remote ETags with local cached versions.

Application downloads new or modified entities.

Application updates local storage.

Unchanged entities remain cached.

Important technical requirements:

Use If-None-Match when requesting cached resources.

Handle 304 Not Modified.

Avoid downloading unchanged content.

Synchronization should not block users from accessing existing offline content.

Cache invalidation must be handled when an entity is updated.

Synchronization must handle network interruption and retry safely.

### 4.7. F7 – Learning Progress

> **Sprint 05 source review:** Explicit learned/unlearned vocabulary/grammar actions persist atomically with outbox edits; backend receipts, deltas and current-catalog summaries are implemented. Opening content alone does not mark completion.

Description: Track the user's learning progress across vocabulary, grammar and other supported learning items.

| ID | Functional Requirement | Priority | Client status | Evidence / remaining work |
| --- | --- | --- | --- | --- |
| FR-052 | Application records learning progress. | High | Implemented and locally tested | Room/ETag cache and account-scoped durable progress; see [Sprint 05 contract](contracts/sprint-05-api.md) and [verification](sprints/sprint-05-report.md). Offline scope is previously fetched content; shared rollout pending. |
| FR-053 | Application can submit multiple progress updates in one batch. | High | Implemented and locally tested | Room/ETag cache and account-scoped durable progress; see [Sprint 05 contract](contracts/sprint-05-api.md) and [verification](sprints/sprint-05-report.md). Offline scope is previously fetched content; shared rollout pending. |
| FR-054 | Application retrieves progress updates from the backend. | High | Implemented and locally tested | Room/ETag cache and account-scoped durable progress; see [Sprint 05 contract](contracts/sprint-05-api.md) and [verification](sprints/sprint-05-report.md). Offline scope is previously fetched content; shared rollout pending. |
| FR-055 | Application supports incremental progress synchronization. | High | Implemented and locally tested | Room/ETag cache and account-scoped durable progress; see [Sprint 05 contract](contracts/sprint-05-api.md) and [verification](sprints/sprint-05-report.md). Offline scope is previously fetched content; shared rollout pending. |
| FR-056 | Users can view learning progress summaries. | High | Implemented and locally tested | Room/ETag cache and account-scoped durable progress; see [Sprint 05 contract](contracts/sprint-05-api.md) and [verification](sprints/sprint-05-report.md). Offline scope is previously fetched content; shared rollout pending. |
| FR-057 | Users can view progress by JLPT level and topic. | High | Implemented and locally tested | Room/ETag cache and account-scoped durable progress; see [Sprint 05 contract](contracts/sprint-05-api.md) and [verification](sprints/sprint-05-report.md). Offline scope is previously fetched content; shared rollout pending. |
| FR-058 | Application preserves pending progress updates while offline. | High | Implemented and locally tested | Room/ETag cache and account-scoped durable progress; see [Sprint 05 contract](contracts/sprint-05-api.md) and [verification](sprints/sprint-05-report.md). Offline scope is previously fetched content; shared rollout pending. |

Progress data requirements:

Item identifier.

Item type.

Completion or learning status.

Client update timestamp.

Server update timestamp, when provided.

Business rules:

Progress updates must be idempotent.

Backend applies Last-Write-Wins (LWW) using client_updated_at as specified by the synchronization contract.

Application must retain unsynchronized progress locally.

Progress synchronization must not create duplicated records.

Progress summary should reflect the latest accepted data.

### 4.8. F8 – Personal Flashcards

> **Source review (Sprint06):** Manual folder/set/card CRUD, bulk actions, review, Room persistence and owner-scoped CAS/receipt/delta synchronization are implemented. AI generation/quota and stable preview/explicit-save are connected in Sprint07; actual-provider/shared rollout remains open. Legacy unowned backend rows are preserved and hidden.

Description: Allow users to create and manage personal flashcard folders, sets and cards.

【英語】「WordHolic」を使って英単語を効率よく暗記してみた - 理系社会人から始める英語学習

StudyPod - Flashcards app | Create Flashcards online

Flashwise - Smarter Studying

10

| ID | Functional Requirement | Priority | Client status | Evidence / remaining work |
| --- | --- | --- | --- | --- |
| FR-059 | Users can create flashcard folders. | High | Implemented and locally tested | Private CRUD/bulk/review and durable offline sync; see [Sprint06 report](sprints/sprint-06-report.md). Shared rollout remains pending. |
| FR-060 | Users can view their folders. | High | Implemented and locally tested | Private CRUD/bulk/review and durable offline sync; see [Sprint06 report](sprints/sprint-06-report.md). Shared rollout remains pending. |
| FR-061 | Users can view folder details. | High | Implemented and locally tested | Private CRUD/bulk/review and durable offline sync; see [Sprint06 report](sprints/sprint-06-report.md). Shared rollout remains pending. |
| FR-062 | Users can update folders. | High | Implemented and locally tested | Private CRUD/bulk/review and durable offline sync; see [Sprint06 report](sprints/sprint-06-report.md). Shared rollout remains pending. |
| FR-063 | Users can delete folders. | High | Implemented and locally tested | Private CRUD/bulk/review and durable offline sync; see [Sprint06 report](sprints/sprint-06-report.md). Shared rollout remains pending. |
| FR-064 | Users can create flashcard sets and multiple cards. | High | Implemented and locally tested | Private CRUD/bulk/review and durable offline sync; see [Sprint06 report](sprints/sprint-06-report.md). Shared rollout remains pending. |
| FR-065 | Users can bulk create, update and delete cards. | High | Implemented and locally tested | Private CRUD/bulk/review and durable offline sync; see [Sprint06 report](sprints/sprint-06-report.md). Shared rollout remains pending. |
| FR-066 | Users can view flashcard sets by folder. | High | Implemented and locally tested | Private CRUD/bulk/review and durable offline sync; see [Sprint06 report](sprints/sprint-06-report.md). Shared rollout remains pending. |
| FR-067 | Users can view cards belonging to a set. | High | Implemented and locally tested | Private CRUD/bulk/review and durable offline sync; see [Sprint06 report](sprints/sprint-06-report.md). Shared rollout remains pending. |
| FR-068 | Users can delete a flashcard set. | High | Implemented and locally tested | Private CRUD/bulk/review and durable offline sync; see [Sprint06 report](sprints/sprint-06-report.md). Shared rollout remains pending. |
| FR-069 | Users can generate flashcards using AI. | Medium | Implemented; locally tested | Sprint07 implementation and deterministic local tests; see contract/ledger. Actual-provider/shared rollout pending. |
| FR-070 | Users can synchronize flashcard changes. | High | Implemented and locally tested | Private CRUD/bulk/review and durable offline sync; see [Sprint06 report](sprints/sprint-06-report.md). Shared rollout remains pending. |

Content hierarchy:

Flashcard Folder

Flashcard Set

Flashcards

Front · Back

Main flow – Create Flashcards:

User opens the Flashcards section.

User creates or selects a folder.

User creates a flashcard set.

User adds multiple flashcards.

Application submits the set and cards in bulk.

User can view and manage the created cards.

AI Flashcard Generation:

User selects AI generation.

User provides the required generation input.

Application sends a generation request.

Backend checks the AI usage quota.

Backend generates flashcard content.

User reviews the generated cards.

User saves the cards to a selected set.

Business rules:

Every folder, set and card must belong to an authenticated user.

Users cannot access or modify another user's flashcards.

Flashcard CRUD operations must follow the bulk API contract.

The application must not assume that individual card CRUD endpoints exist.

AI generation is limited to 2 requests per day, with a maximum of 15 cards per request.

Flashcard synchronization must handle deleted records through deleted_ids.

### 4.9. F9 – AI Practice

> **Sprint07 source review:** Both exercise types, private history, durable drafts/submissions and backend results are implemented and locally tested. Completion events are recorded once; activity/streak aggregation remains Sprint08.

Description: Generate and complete AI-powered Japanese exercises.

renshuu.org

Learn real Japanese — Kotrin

アークアカデミー新宿校 - JPSchool-navi

8

| ID | Functional Requirement | Priority | Client status | Evidence / remaining work |
| --- | --- | --- | --- | --- |
| FR-071 | Users can generate fill-in-the-blank exercises. | High | Implemented; locally tested | Sprint07 implementation and deterministic local tests; see contract/ledger. Actual-provider/shared rollout pending. |
| FR-072 | Users can generate multiple-choice exercises. | High | Implemented; locally tested | Sprint07 implementation and deterministic local tests; see contract/ledger. Actual-provider/shared rollout pending. |
| FR-073 | Users can browse generated practice sets. | High | Implemented; locally tested | Sprint07 implementation and deterministic local tests; see contract/ledger. Actual-provider/shared rollout pending. |
| FR-074 | Users can filter practice sets by topic, type and status. | Medium | Implemented; locally tested | Sprint07 implementation and deterministic local tests; see contract/ledger. Actual-provider/shared rollout pending. |
| FR-075 | Users can view practice set details. | High | Implemented; locally tested | Sprint07 implementation and deterministic local tests; see contract/ledger. Actual-provider/shared rollout pending. |
| FR-076 | Users can submit their answers. | High | Implemented; locally tested | Sprint07 implementation and deterministic local tests; see contract/ledger. Actual-provider/shared rollout pending. |
| FR-077 | Application displays the practice results returned by backend. | High | Implemented; locally tested | Sprint07 implementation and deterministic local tests; see contract/ledger. Actual-provider/shared rollout pending. |
| FR-078 | Application records learning activity after completing a practice set. | High | Implemented and locally tested | Unique transactional completion event; Sprint08 aggregates completion counts and the captured timezone date into the calendar and streak without duplicating timed seconds ([report](sprints/sprint-08-report.md)). |
| FR-079 | Application synchronizes practice data when necessary. | Medium | Implemented; locally tested | Sprint07 implementation and deterministic local tests; see contract/ledger. Actual-provider/shared rollout pending. |

Supported exercise types:

Fill in the Blank: User completes a missing word or grammar expression.

Multiple Choice: User selects an answer from available options.

Main flow:

User selects a practice type.

User selects a topic or learning scope if supported.

Application requests AI-generated exercises.

Backend validates AI quota.

Backend returns a practice set.

User completes the questions.

Application submits answers.

Backend evaluates the submission.

Application displays the result.

Backend records the completed learning activity.

Business rules:

Fill-in-the-blank generation is limited to 3 requests per day.

Each generation request supports a maximum of 15 questions.

Multiple-choice generation follows the applicable AI quota policy.

Completing a practice set should automatically create a learning activity.

The application should prevent duplicate submission when the same completion request is retried, according to the backend contract.

### 4.10. F10 – Learning Streak & Calendar

> **Source review (updated Sprint 09):** Implemented and locally verified in Sprint 08: foreground engaged-time recording, immutable offline activity events, server streak/minutes/goal and monthly calendar ([report](sprints/sprint-08-report.md), [contract](contracts/sprint-08-api.md)). A Sprint 09 emulator run matched app and server totals. Physical-device, multi-device and shared-environment acceptance remain open.

Description: Encourage consistent learning through daily activity tracking and calendar visualization.

ポケット語学の使い方 | ポケット語学 【NHK出版】

Keep the chain. — Lingora Mandarin Language Learning App

브런치

6

| ID | Functional Requirement | Priority | Client status | Evidence / remaining work |
| --- | --- | --- | --- | --- |
| FR-080 | Users can view their current learning streak. | High | Implemented and locally tested | Server streak by captured-timezone days; 60 engaged seconds qualify a day ([Sprint 08](sprints/sprint-08-report.md)). The Sprint 09 emulator journey matched the server. |
| FR-081 | Users can view their longest learning streak if supported by backend. | Medium | Implemented and locally tested | Longest streak returned by the backend and shown in the activity screen ([Sprint 08](sprints/sprint-08-report.md)). |
| FR-082 | Users can view learning minutes for the current day. | High | Implemented and locally tested | Today's minutes against daily_goal_minutes from accepted server activity; pending time appears after synchronization ([Sprint 08](sprints/sprint-08-report.md)). |
| FR-083 | Application can record learning activities. | High | Implemented and locally tested | Foreground engaged-time producer on vocabulary/grammar detail, flashcard review and practice; immutable UUID events; practice completions counted without double timed credit ([Sprint 08](sprints/sprint-08-report.md)). |
| FR-084 | Application supports offline activity recording. | High | Implemented and locally tested | Room v6 outbox keeps UUID, occurrence time and timezone; same-APK force-stop restart test passes ([Sprint 08](sprints/sprint-08-report.md)). Process death can lose the last unchecked 15-second interval. |
| FR-085 | Application can synchronize pending learning activities. | High | Implemented and locally tested | WorkManager sync to idempotent server batches with replay, conflict and late-event handling ([Sprint 08](sprints/sprint-08-report.md)). |
| FR-086 | Users can view their learning calendar by month. | High | Implemented and locally tested | Monthly calendar from the backend with cache and pending/error/retry/discard states ([Sprint 08](sprints/sprint-08-report.md)). |
| FR-087 | Application displays daily learning activity on the calendar. | High | Implemented and locally tested | Per-day seconds/minutes, qualified and goal flags and practice completions; Sprint 09 emulator journey showed the studied day ([Sprint 08](sprints/sprint-08-report.md)). |

Business rules:

Learning streaks must be calculated using the user's configured timezone.

Learning activity records must include duration and occurrence timestamp.

Activity synchronization must be idempotent.

Offline activities must preserve their original occurrence timestamps.

The daily learning goal is based on daily_goal_minutes.

Timezone changes must be handled consistently by backend streak calculation.

### 4.11. F11 – Device Registration & Push Notifications

> **Source review (updated Sprint 09):** Implemented and locally verified in Sprint 08: Firebase Messaging, explicit permission/preference controls, owner/session-bound device registration, logout cleanup, data-only payload handling and a scheduler that is off by default. The reminder provider was a fake in all tests; no real notification has been delivered. ACCESS_LOCAL_NETWORK permission is unrelated to notification permission.

Description: Register mobile devices and send learning reminders.

| ID | Functional Requirement | Priority | Client status | Evidence / remaining work |
| --- | --- | --- | --- | --- |
| FR-088 | Application requests notification permission when appropriate. | Medium | Implemented and locally tested | Requested from profile controls; denial never blocks study ([Sprint 08](sprints/sprint-08-report.md)). |
| FR-089 | Application retrieves the FCM registration token. | High | Implemented and locally tested | Firebase Messaging token retrieval; the emulator obtained a real token in the Sprint 09 journey ([Sprint 08](sprints/sprint-08-report.md)). |
| FR-090 | Application registers or updates the device token. | High | Implemented and locally tested | Idempotent PUT devices/:installationId bound to owner and validated session; rebinding and rotation tested ([Sprint 08](sprints/sprint-08-report.md)). |
| FR-091 | Application removes the device token during logout. | High | Implemented and locally tested | Logout revokes the session and cascades the device binding; local push is suppressed immediately. Offline logout cannot revoke the server session until connectivity or expiry ([Sprint 08](sprints/sprint-08-report.md)). |
| FR-092 | Users can enable or disable push notifications. | Medium | Implemented and locally tested | Preference saved on the server and honored by the app and scheduler; Sprint 09 emulator journey matched ([Sprint 08](sprints/sprint-08-report.md)). |
| FR-093 | Users can configure daily reminder time. | Medium | Implemented and locally tested | Reminder time and timezone saved; scheduler honors local time and DST ([Sprint 08](sprints/sprint-08-report.md)). |
| FR-094 | Backend sends reminders based on user timezone and settings. | High | Implemented; locally tested with a fake provider | One claim per owner/local date, goal/opt-out/session checks, invalid-token cleanup, no retry after ambiguous provider acceptance. Scheduler is off by default (FCM_REMINDERS_ENABLED); no real notification has been delivered ([Sprint 08](sprints/sprint-08-report.md)). |
| FR-095 | Application handles notification payload and deeplink navigation. | Medium | Implemented and locally tested | Data-only payload; taps open only allowlisted destinations for the current owner, session and binding ([Sprint 08](sprints/sprint-08-report.md)). Physical-device cold/warm launch not yet verified. |

Reminder rules:

Notifications are sent only when reminders are enabled.

Notifications must respect the configured timezone.

Notifications should only be sent when the user has not reached the daily learning goal.

FCM payload must contain a notification type and deeplink.

Application should route the user to the corresponding screen when a supported notification is tapped.

### 4.12. F12 – AI Usage & Quota Management

> **Sprint07 source review:** Server quota ledger and native allowance/reset/429 states are implemented. User approved2 flashcard requests/day and3 shared practice requests/day, max15, reset00:00 Vietnam; actual-provider/shared rollout remains open.

Description: Provide transparency about AI feature usage and enforce usage limits.

| ID | Functional Requirement | Priority | Client status | Evidence / remaining work |
| --- | --- | --- | --- | --- |
| FR-096 | Users can view AI usage information. | High | Implemented; locally tested | Sprint07 implementation and deterministic local tests; see contract/ledger. Actual-provider/shared rollout pending. |
| FR-097 | Application displays flashcard generation quota. | High | Implemented; locally tested | Sprint07 implementation and deterministic local tests; see contract/ledger. Actual-provider/shared rollout pending. |
| FR-098 | Application displays practice generation quota. | High | Implemented; locally tested | Sprint07 implementation and deterministic local tests; see contract/ledger. Actual-provider/shared rollout pending. |
| FR-099 | Application handles quota exceeded responses. | High | Implemented; locally tested | Sprint07 implementation and deterministic local tests; see contract/ledger. Actual-provider/shared rollout pending. |
| FR-100 | Application displays the quota reset time. | Medium | Implemented; locally tested | Sprint07 implementation and deterministic local tests; see contract/ledger. Actual-provider/shared rollout pending. |

Quota information:

Number of flashcard generation requests used.

Maximum flashcard generation requests.

Number of practice generation requests used.

Maximum practice generation requests.

Quota reset timestamp.

Business rules:

Backend is the source of truth for AI quotas.

The application must not rely only on local counters.

When backend returns 429 AI_QUOTA_EXCEEDED, the application must display a clear message.

Quota limits must be configurable or consistent with the backend policy.

### 4.13. F13 – Application Configuration & System

> **Source review:** Only shared API error handling is present. Backend health, remote configuration, maintenance and minimum-version checks are planned.

Description: Retrieve application configuration and handle system-level states.

| ID | Functional Requirement | Priority | Client status | Evidence / remaining work |
| --- | --- | --- | --- | --- |
| FR-101 | Application can check backend health. | Medium | Implemented and locally tested | Diagnostic health action; existing liveness contract retained. [Sprint 03 evidence](sprints/sprint-03-report.md). Shared rollout pending. |
| FR-102 | Application can retrieve application configuration. | High | Implemented and locally tested | Validated configuration and environment-specific TTL cache. [Sprint 03 evidence](sprints/sprint-03-report.md). Shared rollout pending. |
| FR-103 | Application can detect maintenance mode. | High | Implemented and locally tested | Maintenance gate and cached-restriction fallback. [Sprint 03 evidence](sprints/sprint-03-report.md). Shared rollout pending. |
| FR-104 | Application can detect minimum supported application version. | High | Implemented and locally tested | Numeric versionCode gate, safe update destination and retry. [Sprint 03 evidence](sprints/sprint-03-report.md). Shared rollout pending. |
| FR-105 | Application handles server errors consistently. | High | Partial | Shared error envelopes, server request IDs and roadmap/config retry states verified locally; future modules and whole-product acceptance remain open. |

Configuration requirements:

Minimum supported app version.

Maintenance mode status.

Any additional configuration fields exposed by /app/config.

System behavior:

If the current application version is below the minimum supported version, the user should be informed that an update is required.

If maintenance mode is active, the application should display an appropriate maintenance screen.

Network and server errors should be handled consistently.

## 5. Main Application Navigation & User Flow

### 5.1. Main Navigation

> **Source review:** Current destinations are login, registration, password recovery, topics/levels, topic detail and profile. Successful login opens topics; registration returns to login; logout clears navigation to login. The multi-tab navigation table below is target design, not current navigation. [AuthNavGraph](../app/src/main/kotlin/com/nihonix/mobile/feature/auth/presentation/navigation/AuthNavGraph.kt)

Ứng dụng sử dụng Bottom Navigation để điều hướng giữa các khu vực chính.

Langura - Language Learning App by Rina Damayanti for kiwari on Dribbble

dribbble.com

| Navigation | Description |
| --- | --- |
| Home | Dashboard, learning overview and daily goal |
| Learn | JLPT roadmap, vocabulary and grammar |
| Flashcards | Personal flashcard folders and sets |
| Practice | AI-generated exercises |
| Profile | User information, settings and statistics |

### 5.2. Home Dashboard

> **Source review:** Planned: topics/levels is the current landing screen. No home dashboard, daily goal, streak or practice shortcuts are implemented.

Home là màn hình tổng quan, cung cấp thông tin học tập quan trọng.

Required components:

Welcome information.

Current JLPT level.

Daily learning goal.

Today's learning progress.

Current learning streak.

Quick access to vocabulary and grammar.

Recent or recommended learning topics, if supported by available content.

Quick access to AI Practice.

Learning calendar shortcut.

### 5.3. Core User Journey

> **Source review:** Only registration/login, level selection, topic exploration and profile viewing currently exist. Study/practice/progress/habit steps remain target journeys.

Main Learning Journey

Create Account / LoginAuthenticate and verify email

Select JLPT LevelChoose N5–N1 learning level

Explore TopicsBrowse vocabulary and grammar topics

Study Learning ContentRead vocabulary and grammar materials

Practice & ReviewUse personal flashcards and AI exercises

Track ProgressUpdate learning progress and daily activity

Maintain Learning HabitTrack streaks, goals and calendar

### 5.4. Offline Learning Journey

> **Source review:** Planned end-to-end journey. Current lists are network-backed; retaining data in a ViewModel during a screen session does not provide persistent offline learning.

User accesses learning content while connected to the internet.

Application downloads and caches the selected content.

User loses network connectivity.

Application continues displaying cached vocabulary, grammar and topics.

User performs supported learning activities.

Application stores pending progress and activity updates locally.

When connectivity returns, the application synchronizes pending changes.

Application updates local data using the latest server responses.

Important: Offline learning does not imply that AI generation, login or other server-dependent operations must work offline.

## 6. Data Management & Synchronization Requirements

### 6.1. Local Data Storage

> **Source review:** Current persistence is limited to auth tokens and cached user JSON (including current level). No Room/DataStore content or learning database exists. The table below specifies future storage requirements.

The Android application should maintain local data for:

| Data | Local Storage | Offline Access |
| --- | --- | --- |
| Authentication session | Secure storage | Existing session subject to token validity |
| User profile | Local database/cache | Read cached profile |
| Learning levels | Local database | Yes |
| Topics | Local database | Yes |
| Vocabulary | Local database | Yes |
| Grammar | Local database | Yes |
| Flashcard folders | Local database | Yes |
| Flashcard sets and cards | Local database | Yes |
| Learning progress | Local database | Yes |
| Learning activities | Local database | Yes |
| Learning calendar | Cache/local database | Previously synchronized data |
| AI quota | Cache | Read-only, potentially stale |

### 6.2. Synchronization Strategy

> **Source review:** All synchronization strategies in this section are planned. No ETag/If-None-Match request headers or sync endpoints exist in the current service interfaces.

The application must distinguish between two categories of data.

A. Server-owned content

Examples: Levels, topics, vocabulary and grammar.

Backend is the source of truth.

Local database stores cached copies.

ETag is used to detect changes.

Unchanged content must not be downloaded again unnecessarily.

B. User-generated data

Examples: Flashcards, learning progress and learning activities.

Application stores local changes.

Pending changes are synchronized with the backend.

Synchronization must support retry.

Duplicate updates must be avoided.

Deleted data must be represented appropriately.

### 6.3. Conflict Resolution

> **Source review:** Conflict policies are target backend contracts requiring confirmation; no client implementation exists.

| Data Type | Conflict Resolution |
| --- | --- |
| Learning content | Server version takes precedence |
| Learning progress | Last-Write-Wins using client_updated_at |
| Learning activities | Idempotent activity submission |
| Flashcard folders | Follow backend update contract |
| Flashcard sets | Follow backend update contract |
| Flashcards | Bulk synchronization and deletion tracking |

Additional requirement: Backend must provide a consistent synchronization contract for flashcards. The mobile application must not assume that an entity can be synchronized using an endpoint that does not exist.

### 6.4. Synchronization Triggers

> **Source review:** These are proposed triggers. Current refresh reloads remote data on demand; it is not background synchronization.

Application should trigger synchronization:

After successful login.

When the application starts, subject to cache policy.

When the user opens a learning content section.

When network connectivity is restored.

After completing a learning activity.

After modifying personal flashcards.

When the user manually requests synchronization, if this action is provided in the UI.

Synchronization should avoid unnecessary network requests and duplicate submissions.

## 7. Business Rules & System Constraints

### 7.1. Authentication Rules

> **Source review:** BR-001 conflicts with public level/topic request annotations. BR-002 and ownership enforcement are backend requirements that are not verified here. JWT attachment exists for authenticated requests.

| Rule ID | Description |
| --- | --- |
| BR-001 | Only authenticated users can access learning content. |
| BR-002 | Unverified accounts cannot log in. |
| BR-003 | Authenticated APIs require a valid JWT. |
| BR-004 | Users can only access their own personal data. |

### 7.2. AI Usage Limits

> **Source review (updated Sprint 09):** The user-approved quota (flashcards 2/day, shared fill-in/choice practice 3/day, maximum 15 items, reset 00:00 Asia/Ho_Chi_Minh) is enforced by the backend with reservations, refunds and replay receipts ([Sprint 07](sprints/sprint-07-report.md)). Verified with a fake provider only.

| Feature | Daily Limit | Maximum per Request |
| --- | --- | --- |
| Flashcard Generation | 2 requests | 15 cards |
| Fill-in-the-Blank | 3 requests | 15 questions |
| Multiple Choice | Follow backend quota policy | Follow backend policy |

Quota must be enforced on the backend, not just in the Android application.

### 7.3. Learning Rules

> **Source review (updated Sprint 09):** Progress ([Sprint 05](sprints/sprint-05-report.md)) and duration, streak/timezone and goal rules ([Sprint 08](sprints/sprint-08-report.md)) are implemented and locally tested. Legacy count-only activity rows are preserved without invented durations.

| Rule ID | Description |
| --- | --- |
| BR-005 | Learning roadmap contains JLPT levels N5–N1. |
| BR-006 | Each topic belongs to a learning level. |
| BR-007 | Progress must be associated with the corresponding learning item. |
| BR-008 | Learning activities must include duration and occurrence timestamp. |
| BR-009 | Streak calculation must respect the user's timezone. |
| BR-010 | Daily goal completion is calculated using learning minutes. |

### 7.4. Offline Rules

> **Source review (updated Sprint 09):** Persistent content and account-scoped pending-change storage exist (Room, WorkManager) and are locally tested ([Sprint 05](sprints/sprint-05-report.md)–[Sprint 08](sprints/sprint-08-report.md)). Physical-device, Doze and multi-device behavior are not yet verified.

| Rule ID | Description |
| --- | --- |
| BR-011 | Only previously downloaded content is available offline. |
| BR-012 | Offline changes must be stored locally until synchronized. |
| BR-013 | Content synchronization must use ETag validation where supported. |
| BR-014 | Progress synchronization must be idempotent. |
| BR-015 | Server-owned learning content must not be overwritten by local changes. |

### 7.5. Notification Rules

> **Source review (updated Sprint 09):** FCM integration and a reminder scheduler exist ([Sprint 08](sprints/sprint-08-report.md)); the scheduler is disabled unless FCM_REMINDERS_ENABLED=true and was exercised with a fake provider only.

| Rule ID | Description |
| --- | --- |
| BR-016 | Notifications must respect user preferences. |
| BR-017 | Reminder scheduling must use the configured timezone. |
| BR-018 | Users who have completed their daily goal should not receive the corresponding reminder. |
| BR-019 | Logout must unregister the current device token. |

## 8. Non-Functional Requirements

### 8.1. Performance

| ID | Requirement |
| --- | --- |
| NFR-001 | Application should provide responsive navigation between screens. |
| NFR-002 | Cached learning content should load without requiring network requests. |
| NFR-003 | Large vocabulary and grammar lists should support pagination or incremental loading. |
| NFR-004 | Long-running AI requests must provide loading and processing feedback. |
| NFR-005 | Background synchronization must not block the main UI thread. |

### 8.2. Security

> **Source review — Sprint 01:** Release requires an explicit deployed HTTPS API URL and blocks cleartext; HTTP exceptions exist only for three local debug hosts. Request-body logging was removed. Secure token storage now fails closed without an ordinary-preferences fallback, excludes tokens/profile from backup, and exposes session loss to navigation. The production URL remains unavailable. Backend authorization/ownership findings and test limits are documented in the [execution report](sprints/sprint-01-report.md) and [backend audit](contracts/backend-audit.md).

| ID | Requirement |
| --- | --- |
| NFR-006 | All backend communication must use HTTPS. |
| NFR-007 | JWT tokens must be stored using secure Android storage mechanisms. |
| NFR-008 | Passwords must never be stored in plain text. |
| NFR-009 | Personal user data must be protected from unauthorized access. |
| NFR-010 | API authorization must be enforced by the backend. |
| NFR-011 | Sensitive information must not be exposed in application logs. |

### 8.3. Reliability

| ID | Requirement |
| --- | --- |
| NFR-012 | Application should recover from temporary network interruptions. |
| NFR-013 | Pending offline data must survive application restarts. |
| NFR-014 | Synchronization requests should support safe retry. |
| NFR-015 | Application must handle expired tokens and authentication failures. |
| NFR-016 | The application must gracefully handle API errors and unexpected responses. |

### 8.4. Usability

| ID | Requirement |
| --- | --- |
| NFR-017 | UI must follow consistent design guidelines. |
| NFR-018 | Navigation must be intuitive and predictable. |
| NFR-019 | Loading, empty, error and offline states must be clearly communicated. |
| NFR-020 | Forms must provide appropriate input validation. |
| NFR-021 | Learning content must be readable on different Android screen sizes. |

### 8.5. Maintainability

> **Source review:** Feature/layer package separation and shared repositories/networking are present in one Gradle app module. Manual dependency wiring exists; Hilt and separate Gradle feature modules are not required to describe the current implementation.

| ID | Requirement |
| --- | --- |
| NFR-022 | Application should use modular architecture. |
| NFR-023 | UI, business logic and data access must be separated. |
| NFR-024 | API communication must be centralized. |
| NFR-025 | Application should support dependency injection. |
| NFR-026 | Shared components and common business logic should be reusable. |

## 9. Technical Requirements

### 9.1. Android Platform

The source-reviewed implementation baseline supersedes the proposed stack in the Word document.

| Category | Current implementation |
| --- | --- |
| Platform | Native Android; minSdk 26, compileSdk 37, targetSdk 37 |
| Application | com.nihonix.mobile; versionCode 1, versionName 1.0 |
| Build | Single :app module; AGP 9.5.1; Kotlin Compose plugin 2.2.10; Java compatibility 11 |
| UI | Jetpack Compose / Material 3; Compose BOM 2026.02.01; no XML screen layouts |
| Navigation | Navigation Compose 2.8.5 |
| Architecture | Feature packages with presentation/data/domain separation; MVVM; repository interfaces |
| Async | Kotlin coroutines 1.9.0, Flow and StateFlow |
| Networking | Retrofit 2.11.0 / OkHttp 4.12.0 |
| Serialization | Gson 2.11.0 |
| Dependency wiring | ServiceLocator and AppViewModelFactory; no Hilt |
| Session storage | EncryptedSharedPreferences / MasterKey; no plaintext fallback; storage failure rejects sign-in |
| User storage | Gson JSON in private SharedPreferences |
| Content database | Room 2.8.5 (public catalog cache, account-private outbox/flashcards/practice/activity data) |
| Background synchronization | WorkManager 2.12.0 (network-constrained, backoff, periodic recovery, independent producers) |
| Push / images / AI | Firebase Messaging (BOM 35.2.0); no Coil; AI generation runs on the backend only |

Evidence: [app/build.gradle.kts](../app/build.gradle.kts), [version catalog](../gradle/libs.versions.toml), [ServiceLocator](../app/src/main/kotlin/com/nihonix/mobile/core/di/ServiceLocator.kt), [TokenStorage](../app/src/main/kotlin/com/nihonix/mobile/core/storage/TokenStorage.kt). Versions are recorded from this repository, not recommendations to upgrade.

### 9.2. Architecture

Current layers:

```text
MainActivity / Compose NavHost / Compose screens
    -> ViewModels + StateFlow<Resource-derived UI state>
    -> AuthRepository / TopicsLevelsRepository interfaces
    -> AuthRepositoryImpl / TopicsLevelsRepositoryImpl
    -> Retrofit API services -> OkHttp -> REST backend
    -> TokenStorage / UserStorage (SharedPreferences)
```

Domain packages hold models and repository contracts. There is no separate use-case layer. ServiceLocator initializes shared dependencies with application context; AppViewModelFactory constructs ViewModels. Repository flows run remote work on Dispatchers.IO. API response envelopes map to Resource.Loading, Success, Empty or Error and then screen state.

The future content/offline architecture must add a persistent content store and durable pending operations before offline guarantees can be claimed. Hilt, Room and WorkManager from the original proposal are future options rather than installed components.

Evidence: [ServiceLocator](../app/src/main/kotlin/com/nihonix/mobile/core/di/ServiceLocator.kt), [AuthApiService](../app/src/main/kotlin/com/nihonix/mobile/feature/auth/data/api/AuthApiService.kt), [AuthRepositoryImpl](../app/src/main/kotlin/com/nihonix/mobile/feature/auth/data/repository/AuthRepositoryImpl.kt), [TopicsLevelsApiService](../app/src/main/kotlin/com/nihonix/mobile/feature/topics/data/api/TopicsLevelsApiService.kt), [TopicsLevelsRepositoryImpl](../app/src/main/kotlin/com/nihonix/mobile/feature/topics/data/repository/TopicsLevelsRepositoryImpl.kt).

### 9.3. API Integration Standards

Debug default base URL: `http://10.0.2.2:3010/api/`. Release requires `NIHONIX_RELEASE_API_BASE_URL` as a Gradle property or environment variable with an explicit HTTPS URL ending in `/`. No production host has been supplied. Main-client connect/read/write timeouts remain 30 seconds; refresh uses a separate OkHttp client.

Retrofit uses Gson and a shared envelope containing success, message, data, errors and statusCode. AuthInterceptor adds a Bearer token except on requests marked No-Authentication. TokenAuthenticator serializes refresh attempts, can reuse a token refreshed by another request, persists returned access/refresh tokens and limits repeated 401 retries. Refresh failure clears tokens and user data.

Current public methods include auth entry/recovery operations and GET level/topic routes. Authenticated methods include logout, profile retrieval, and user-level retrieval/update. See Appendix A for exact paths and methods.

Target requirements still include production HTTPS, consistent authorization, robust DTO/error mapping and safe handling of expired sessions. Pagination, delta synchronization, ETag/If-None-Match, and 304 handling remain planned. Displaying a topic's etag does not satisfy those requirements.

Evidence: [ApiConstants](../app/src/main/kotlin/com/nihonix/mobile/core/network/ApiConstants.kt), [NetworkModule](../app/src/main/kotlin/com/nihonix/mobile/core/network/NetworkModule.kt), [AuthInterceptor](../app/src/main/kotlin/com/nihonix/mobile/core/network/interceptor/AuthInterceptor.kt), [TokenAuthenticator](../app/src/main/kotlin/com/nihonix/mobile/core/network/authenticator/TokenAuthenticator.kt), [safeApiCall](../app/src/main/kotlin/com/nihonix/mobile/core/network/util/SafeApiCall.kt).

### 9.4. Error Handling

> **Source review:** The table below specifies target behavior. Current safeApiCall maps HTTP/network/envelope failures to Resource.Error; TokenAuthenticator retries authenticated 401 responses with token refresh and clears the session if refresh fails. There is no specialized 304 cache path, quota workflow or offline queue.

| HTTP Status / Code | Application Behavior |
| --- | --- |
| 400 | Display validation errors |
| 401 | Attempt token refresh when appropriate |
| 403 | Handle authorization errors and EMAIL_NOT_VERIFIED |
| 404 | Display not-found state |
| 409 | Handle conflict based on API contract |
| 429 | Display AI quota exceeded message |
| 500 | Display generic server error |
| Network unavailable | Use cached data and queue supported operations |
| 304 | Keep existing cached content |

## 10. API Integration & Traceability

> **Source review:** Original endpoint groups below are target mappings, not proof that backend APIs exist. Actual client contracts are listed in Appendix A. In particular current profile loading uses POST /auth/me, not GET /users/me.

Bảng dưới đây ánh xạ từng module trong SRS với API backend đã cung cấp.

| Feature | API Group | Main Endpoints |
| --- | --- | --- |
| F1 Authentication | /auth | login, register, verify-email, refresh, logout, password recovery |
| F2 User Profile | /users | me, avatar, level, settings |
| F3 Learning Roadmap | /levels, /topics | levels, topics, topic bundle |
| F4 Vocabulary | /vocabulary-items, /vocabulary-sets | list, detail, search, sets |
| F5 Grammar | /grammar-sub-topics, /grammar-patterns | list, detail, filter |
| F6 Offline Sync | /entity-sync, /sync | check, entity, manifest |
| F7 Progress | /progress | batch, delta, summary |
| F8 Flashcards | /flashcard-folders, /flashcards | folders, sets, bulk, sync |
| F9 AI Practice | /practice | generate, sets, submit |
| F10 Streak | /learning-streaks | streak, activity, calendar |
| F11 Notifications | /devices | register, delete token |
| F12 AI Quota | /ai | health, quota |
| F13 System | /, /health, /app/config | system health, application configuration |

### 10.1. Backend Dependencies

> **Source review — Sprint 01:** The backend has been audited at commit b981b845b6e41add43e0f8f4901d5c38943635ce and its local public/protected endpoints smoke-checked. The dependency table below remains the original target list; consult the [backend audit](contracts/backend-audit.md) for confirmed gaps, existing implementations and limits.

Các API dưới đây cần hoàn thiện trước hoặc trong quá trình tích hợp mobile.

| Priority | Backend Requirement | Related Feature |
| --- | --- | --- |
| Critical | Gắn flashcard folders, sets, cards với user_id và kiểm tra quyền sở hữu | F8 |
| Critical | Bảo vệ các API quản lý nội dung bằng Admin authorization | F3–F5 |
| Critical | Hoàn thiện email verification và chặn login chưa xác minh | F1 |
| Critical | Hoàn thiện progress batch, delta synchronization | F7 |
| High | Hoàn thiện ETag cho toàn bộ nội dung học tập | F3–F6 |
| High | Bổ sung vocabulary pagination và search | F4 |
| High | Bổ sung grammar pagination và synchronization | F5–F6 |
| High | Hoàn thiện flashcard synchronization và deleted IDs | F8 |
| High | Hoàn thiện activity tracking và timezone-based streak | F10 |
| High | Hoàn thiện FCM device registration và reminder scheduler | F11 |
| High | Bổ sung AI quota và enforcement | F9, F12 |
| Medium | Bổ sung user settings và daily learning goal | F2 |
| Medium | Bổ sung app configuration và maintenance mode | F13 |

### 10.2. API Implementation Status

> **Source review:** Ready/Pending/New below are the original backend planning vocabulary. No backend readiness classification was verified. This SRS uses Implemented in client / Partial / Planned / Unverified for source-review status.

Quy ước trạng thái trong quá trình triển khai mobile:

Ready Backend endpoint đã hoàn thiện, có thể tích hợp sau khi xác nhận request/response schema.

Pending Backend endpoint tồn tại nhưng cần sửa hoặc bổ sung.

New Backend endpoint cần phát triển mới.

Không được đánh dấu một chức năng là hoàn thiện chỉ vì endpoint đã tồn tại. Cần kiểm tra thêm authentication, authorization, validation, response schema và business logic.

## 11. Testing Requirements

> **Source review — Sprint 01:** Android and backend build/unit-test results, lint and limited API/device smoke checks are now recorded in the [execution report](sprints/sprint-01-report.md). Existing tests are not proof that all acceptance criteria pass; authenticated journeys, ownership, real permission/storage failures and offline behavior still require integration verification.

### 11.1. Unit Testing

Kiểm thử các thành phần:

ViewModels.

Use Cases.

Repository implementations.

Authentication and token management.

Data mapping.

Progress calculation and synchronization logic.

Flashcard management logic.

Input validation.

### 11.2. Integration Testing

Kiểm thử:

Retrofit API integration.

JWT authentication and token refresh.

Local database operations.

Remote and local data synchronization.

ETag cache validation.

FCM token registration.

AI quota handling.

Offline operation and retry behavior.

### 11.3. UI Testing

Kiểm thử các luồng:

Registration and login.

Email verification.

Learning level selection.

Topic browsing.

Vocabulary learning.

Grammar learning.

Flashcard creation and management.

AI practice completion.

Learning progress display.

Profile and settings.

Offline mode.

### 11.4. Acceptance Criteria

| ID | Acceptance Criteria | Source-review disposition (not test result) |
| --- | --- | --- |
| AC-001 | User can register, verify email and log in successfully. | Local evidence: [Sprint 02](sprints/sprint-02-report.md) fixture and Sprint 09 emulator journey; real SMTP and shared-environment demo pending. See [acceptance matrix](sprints/sprint-09-acceptance.md). |
| AC-002 | Unverified users cannot access authenticated features. | Local evidence: backend rejects unverified login ([Sprint 02](sprints/sprint-02-report.md)); route-guard audit and anonymous-401 fixture in [Sprint 09](sprints/sprint-09-acceptance.md). Shared-environment demo pending. |
| AC-003 | User can browse JLPT levels from N5 to N1. | Partial: browsing and N5–N1 counts tested locally ([Sprint 03](sprints/sprint-03-report.md)); real N5–N1 dataset on a shared environment unverified; D02 authenticated-only policy accepted. |
| AC-004 | User can access topics, vocabulary and grammar content. | Local evidence: [Sprint 03](sprints/sprint-03-report.md), [Sprint 04](sprints/sprint-04-report.md) and Sprint 09 emulator run. Shared data and performance measurement pending. |
| AC-005 | User can access previously synchronized content offline. | Implemented; local automated evidence in [Sprint 05 report](sprints/sprint-05-report.md). Shared rollout pending. |
| AC-006 | Application can synchronize content using ETag. | Implemented; local automated evidence in [Sprint 05 report](sprints/sprint-05-report.md). Shared rollout pending. |
| AC-007 | Learning progress is preserved across application restarts. | Implemented; local automated evidence in [Sprint 05 report](sprints/sprint-05-report.md). Shared rollout pending. |
| AC-008 | Progress synchronization does not create duplicated records. | Implemented; local automated evidence in [Sprint 05 report](sprints/sprint-05-report.md). Shared rollout pending. |
| AC-009 | User can create and manage personal flashcard folders and sets. | Local automated CRUD/offline/conflict/restart evidence in [Sprint06](sprints/sprint-06-report.md); shared multi-device acceptance pending. |
| AC-010 | AI can generate supported flashcards and exercises within quota. | Local deterministic Sprint07 evidence in [report](sprints/sprint-07-report.md); configured-provider/shared acceptance pending. |
| AC-011 | User can complete exercises and view results. | Local transactional grading, replay and native UI evidence in [Sprint07](sprints/sprint-07-report.md); shared acceptance pending. |
| AC-012 | Learning activities are recorded and synchronized correctly. | Local evidence: [Sprint 08](sprints/sprint-08-report.md) tests and Sprint 09 emulator run (app and server totals matched). Physical-device, multi-device and shared-environment acceptance pending. |
| AC-013 | Streak and calendar reflect the user's learning activities and timezone. | Local evidence: Sprint 08 midnight/DST/late-event/timezone-change tests; Sprint 09 emulator run (UTC only). Real-timezone device check and shared acceptance pending. |
| AC-014 | User can configure daily learning goals and reminder preferences. | Local evidence: Sprint 08 tests; Sprint 09 emulator run saved settings that matched the server. Shared acceptance pending. |
| AC-015 | Push notifications are handled correctly. | Partial: payload/owner/session handling, registration (real FCM token) and reminder logic tested locally with a fake provider ([Sprint 08](sprints/sprint-08-report.md)). Real delivery, Doze and scheduler enablement not verified. |
| AC-016 | Application handles expired authentication sessions and API errors. | Local evidence: Sprint 02 refresh tests and a Sprint 09 emulator run where a revoked session produced a real 401 and returned the app to sign-in ([acceptance matrix](sprints/sprint-09-acceptance.md)). Shared-environment and poor-network checks pending. |
| AC-017 | Users cannot access or modify another user's personal data. | Local evidence: per-feature owner fixtures (Sprints 02, 05–08) and the Sprint 09 cross-feature ownership/role fixture (`npm run test:sprint09`). Shared-environment check pending. |

## 12. Development Scope & Implementation Phases

> **Source review:** The phases below remain a target delivery plan. Foundation/auth and core browsing are partially implemented. Profile settings, email verification, vocabulary/grammar and the later learning/AI/offline/habit phases remain incomplete.

Đề xuất chia quá trình phát triển thành các phase để hạn chế phụ thuộc vào những API backend chưa hoàn thiện.

| Phase | Scope | Features |
| --- | --- | --- |
| Phase 1 | Foundation & Authentication | F1, F2 |
| Phase 2 | Core Learning | F3, F4, F5 |
| Phase 3 | Personal Learning | F7, F8 |
| Phase 4 | AI Learning | F9, F12 |
| Phase 5 | Offline & Synchronization | F6 |
| Phase 6 | Learning Habit & Notifications | F10, F11 |
| Phase 7 | System Integration & Testing | F13 + all features |

Phase 1 – Foundation & Authentication

Initialize Android project.

Configure application architecture.

Implement API client and dependency injection.

Implement authentication.

Implement token management.

Implement user profile and settings.

Deliverable: User can register, verify email, log in and manage their profile.

Phase 2 – Core Learning

Implement JLPT roadmap.

Implement level and topic browsing.

Implement vocabulary learning.

Implement grammar learning.

Integrate content caching.

Deliverable: User can browse and study Japanese learning content.

Phase 3 – Personal Learning

Implement personal flashcard folders.

Implement flashcard sets and cards.

Implement bulk flashcard operations.

Implement learning progress tracking.

Deliverable: User can create personal learning materials and track progress.

Phase 4 – AI Learning

Implement AI flashcard generation.

Implement fill-in-the-blank exercises.

Implement multiple-choice exercises.

Implement practice submission.

Integrate AI quota management.

Deliverable: User can generate and complete AI-powered learning exercises.

Phase 5 – Offline & Synchronization

Implement local database.

Implement content synchronization.

Implement ETag validation.

Implement progress synchronization.

Implement flashcard synchronization.

Handle offline operations and retry.

Deliverable: User can continue studying with cached content and synchronize supported data.

Phase 6 – Learning Habit & Notifications

Implement learning streak.

Implement learning calendar.

Implement daily learning goal.

Implement FCM registration.

Implement learning reminders.

Deliverable: User can track learning habits and receive personalized reminders.

Phase 7 – System Integration & Testing

Integrate all modules.

Perform end-to-end testing.

Fix API integration issues.

Optimize performance.

Validate offline and synchronization scenarios.

Prepare release build.

Deliverable: Complete Android application ready for release testing.

## 13. Risks & Open Questions

> **Source review:** All original open questions remain unresolved without backend evidence. Additional source-derived issues are listed in Appendix C.

Một số vấn đề cần được xác nhận trước khi triển khai để tránh mobile phải tự suy diễn business logic.

| ID | Question / Risk | Related Feature |
| --- | --- | --- |
| OQ-001 | Chính xác những trường dữ liệu nào được trả về trong vocabulary item? | F4 |
| OQ-002 | Grammar pattern có những trường nội dung và ví dụ nào? | F5 |
| OQ-003 | Topic bundle trả về grammar và vocabulary theo cấu trúc nào? | F3 |
| OQ-004 | Flashcard deletion có hỗ trợ soft delete và đồng bộ deleted IDs không? | F8 |
| OQ-005 | Flashcard sync có xử lý conflict khi cùng một set được sửa trên nhiều thiết bị không? | F8 |
| OQ-006 | Progress item types và trạng thái học được định nghĩa cụ thể như thế nào? | F7 |
| OQ-007 | Chính xác điều kiện nào được tính là hoàn thành một learning activity? | F7, F10 |
| OQ-008 | Cách tính streak khi timezone thay đổi là gì? | F10 |
| OQ-009 | AI quota reset theo timezone, UTC hay một mốc thời gian cố định? | F9, F12 |
| OQ-010 | Multiple-choice và fill-in-the-blank có chung quota hay không? | F9 |
| OQ-011 | API /app/config trả về cấu trúc version và maintenance flag như thế nào? | F13 |
| OQ-012 | Backend có hỗ trợ pagination cursor thống nhất giữa các endpoint không? | F4–F7 |
| OQ-013 | Chính sách giữ hoặc xóa dữ liệu khi người dùng xóa tài khoản là gì? | F2 |
| OQ-014 | AI-generated content có được lưu tự động hay phải qua bước xác nhận của người dùng? | F8, F9 |

Các câu hỏi trên cần được giải quyết bằng cách đối chiếu backend implementation, Swagger và các tài liệu thiết kế liên quan trước khi chốt contract cho từng feature.

## 14. Reference Documents

> **Source review:** The named backend/reference documents below were not found among project Markdown files and were not consulted. Actual evidence for this review is linked in the implementation notes and appendices.

Các tài liệu cần sử dụng làm nguồn tham chiếu trong quá trình phát triển:

| Reference | Purpose |
| --- | --- |
| backend_scope_final.md | Phạm vi backend và business requirements |
| mobile_srs.md | Mobile requirements hiện có |
| BACKEND_EXISTING_IMPLEMENTATION.md | Đối chiếu implementation thực tế |
| Swagger /api-docs | API endpoints, request/response schemas |
| API List – Nihonix | Feature mapping và backend implementation status |

## 15. Definition of Done

> **Source review:** These are completion gates, not a claim that any feature has passed all gates. Client source presence alone does not establish Definition of Done.

Một feature được xem là hoàn thành khi đáp ứng đầy đủ:

UI hoàn thiện theo thiết kế và responsive với các kích thước màn hình Android.

Business logic được triển khai đúng yêu cầu.

API integration hoạt động đúng contract.

Loading, success, empty và error states được xử lý.

Authentication và authorization được xử lý đúng.

Local persistence được triển khai nếu feature yêu cầu.

Offline behavior được kiểm thử nếu feature hỗ trợ offline.

Unit tests và integration tests được thực hiện phù hợp.

Không có lỗi nghiêm trọng ảnh hưởng đến feature.

Tài liệu kỹ thuật được cập nhật.

## Appendix A. Implemented client API contracts

The table below preserves the initial conversion baseline. Later account/catalog/progress contracts are in Sprint02–05 documents; [Sprint06](contracts/sprint-06-api.md) adds authenticated POST flashcards/sync/batch and GET flashcards/sync/changes. Statements below about missing service methods describe the initial review only and are superseded by the dated sprint reports.

Paths are relative to `/api/`. “Public” records the client's No-Authentication annotation; backend accessibility is unverified.

| Method | Path | Client authentication | Purpose |
| --- | --- | --- | --- |
| POST | auth/register | Public | username, email, password, optional fullName |
| POST | auth/login | Public | username/password -> user and token pair |
| POST | auth/refresh | Public | refresh token -> rotated token pair |
| POST | auth/logout | Bearer | Submit refresh token; local cleanup always occurs |
| POST | auth/me | Bearer | Fetch profile |
| POST | auth/forgot-password | Public | Request recovery code by email |
| POST | auth/verify-reset-code | Public | Verify email/code -> temporary reset token |
| POST | auth/reset-password | Public | Reset password using temporary token |
| POST | auth/resend-code | Public | Resend code by email |
| GET | levels | Public | List levels |
| GET | levels/{code} | Public | Level detail |
| GET | topics?levelCode={code} | Public | List/filter topics; omit query for all levels |
| GET | topics/{identifier} | Public | Topic detail |
| GET | users/me/level | Bearer | Read current user's JLPT level |
| PATCH | users/me/level | Bearer | Update levelCode |

Evidence: [AuthApiService](../app/src/main/kotlin/com/nihonix/mobile/feature/auth/data/api/AuthApiService.kt), [AuthRepositoryImpl](../app/src/main/kotlin/com/nihonix/mobile/feature/auth/data/repository/AuthRepositoryImpl.kt), [TopicsLevelsApiService](../app/src/main/kotlin/com/nihonix/mobile/feature/topics/data/api/TopicsLevelsApiService.kt), [TopicsLevelsRepositoryImpl](../app/src/main/kotlin/com/nihonix/mobile/feature/topics/data/repository/TopicsLevelsRepositoryImpl.kt). There is no client verify-email, topic-bundle, vocabulary, grammar, flashcard, progress, sync, practice, device, AI quota, health or app-config service method.

The Topic model includes id, slug, levelCode, localized title (vi/en/jp), icon metadata, order, optional etag and optional embedded level. JlptLevel includes code, localized name and optional updatedAt. Localized content fields do not imply a translated application UI.

## Appendix B. Verification and existing tests

The initial conversion used static review. Sprint 01 then changed Android security/configuration and ran builds, lint, 36 Android unit tests, 4 backend unit tests, read-only local API smoke checks and an updated debug-app startup check on emulator-5556. See the [execution report](sprints/sprint-01-report.md) for exact evidence and limits; full authenticated/offline/permission/Keystore acceptance remains open.

Existing unit test source includes:

- [AuthViewModelTest](../app/src/test/kotlin/com/nihonix/mobile/AuthViewModelTest.kt): fake-repository login/registration validation and results, password recovery and resend flow.
- [TopicsLevelsViewModelTest](../app/src/test/kotlin/com/nihonix/mobile/TopicsLevelsViewModelTest.kt): fake-repository level selection/update, topic filtering/search, empty/error states and detail fallback.
- [ApiResponseDtoTest](../app/src/test/kotlin/com/nihonix/mobile/ApiResponseDtoTest.kt): response-envelope parsing and reset-verification message extraction.
- [TopicsLevelsDtoTest](../app/src/test/kotlin/com/nihonix/mobile/TopicsLevelsDtoTest.kt): level/topic/user-level payloads and localized text.

This paragraph describes the Sprint 01 baseline (generated application-context instrumented check only). Since then the suite has grown to unit tests plus 72 instrumented tests (Sprint 09 run: 0 failures, 4 phase-only restart tests run separately) and isolated PostgreSQL fixtures for Sprints 02–09. Shared-environment API authorization, physical-device behavior and measured non-functional targets still need verification. The original testing requirements in Section 11 remain acceptance work.

Documentation validation checks that every original FR, BR, NFR and AC ID is retained, table rows render consistently, and relative source links point to project files.

## Appendix C. Source-derived decisions and remaining work

| ID | Issue / decision needed | Related requirements |
| --- | --- | --- |
| OQ-015 | Resolve public level/topic API annotations versus authenticated-only learning policy; current UI has no guest entry. **Status (2026-11-16): Accepted** — user requires login for all catalog reads per BR-001; writes remain Admin-only. See [D02 implementation/evidence](planning/remaining-work/evidence/catalog-authentication.md). | BR-001, Section 2 |
| OQ-016 | Provide verify-email contract/screen and verify backend rejection of unverified accounts. **Status: resolved locally** ([Sprint 02](sprints/sprint-02-report.md)); real SMTP and shared-environment demo pending. | FR-002–FR-005 |
| OQ-017 | Standardize credential contract: current login uses username, not email. Registration minimum password length is 6; reset minimum is 8. **Status: resolved** — username login retained; registration and reset both use the 8-character/72-byte policy ([Sprint 02](sprints/sprint-02-report.md)). | FR-001, FR-004, FR-010 |
| OQ-018 | Configure a production HTTPS endpoint and cleartext policy; current default is emulator HTTP. **Status: partly resolved** — release requires an HTTPS URL and forbids cleartext; a deployed endpoint does not exist yet and no release build has been run. | NFR-006 |
| OQ-019 | Remove ordinary-preferences token fallback or define a secure failure strategy. **Status: resolved** ([Sprint 01](sprints/sprint-01-report.md)): encrypted storage only; storage failure rejects sign-in. | NFR-007 |
| OQ-020 | Gate/redact BODY network logging so credentials and token payloads are not exposed. **Status: resolved** — request logging removed; Sprint 09 static review found no `Log.*` or HTTP logging in app code. | NFR-011 |
| OQ-021 | Verify session-expiration navigation and retry behavior with real 401/refresh failures; local token presence is the initial login check. **Status: verified locally** — Sprint 09 emulator run: revoked session, refresh got 401, app returned to sign-in. Shared-environment and poor-network checks pending. | NFR-015, AC-016 |
| OQ-022 | Define persistent content/pending-write schema and retry/conflict contracts before promising offline learning. **Status: resolved** ([Sprint 05](sprints/sprint-05-report.md), [Sprint 06](sprints/sprint-06-report.md)); physical-device and Doze behavior unverified. | FR-027, FR-045–FR-058 |
| OQ-023 | Confirm whether API 37 local-network permission should gate the whole app in production; currently denial blocks the NavHost. **Status: resolved for debug** ([Sprint 01](sprints/sprint-01-report.md)): the permission is requested only for development local-network URLs and does not block production HTTPS. Not exercised against a release build. | Platform / startup |
| OQ-024 | Confirm backend DTOs, authorization and original quota limits using backend source or OpenAPI; none was supplied. **Status: resolved** — backend source audited ([backend audit](contracts/backend-audit.md)); quota limits set by the user and enforced in Sprint 07; authorization checked by the Sprint 09 route audit and fixture. | Section 10, OQ-001–OQ-014 |

Evidence for startup behavior: [MainActivity](../app/src/main/kotlin/com/nihonix/mobile/MainActivity.kt), [AuthNavGraph](../app/src/main/kotlin/com/nihonix/mobile/feature/auth/presentation/navigation/AuthNavGraph.kt). The observations above document the inspected implementation; they do not change application code or assert backend defects.


[System Notification] Background shell session 'shell-167' (`npm run build; if ($LASTEXITCODE -eq 0) { npm test -- --runInBand }`) finished with exit status 0. Full output was saved to `C:\Users\Ngoc Anh\.android\studio\agent\conversations\Nihonix-862d30a798452eabf70fc92c85f47f\2679c0f5-d461-47ba-bcba-f89d3ac348c3\session\agent\shell-logs\shell-167.log`. Call `interact_shell` with `sessionId="shell-167"` and `action="status"` if you need to inspect its final output.
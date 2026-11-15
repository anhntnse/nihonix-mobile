# Nihonix — Software Requirements Specification

Updated: 2026-11-15. Source: `nihonix.docx`, Android source and the supplied backend audit.

**Sprint 07 update:** AI quota, private fill-in/choice practice, durable submissions/results and explicit generated-card preview/save are implemented. User-approved policy: flashcards2/day, shared practice3/day, max15, reset00:00 Vietnam. Local fixtures cover concurrency/replay/refunds/rollback; local verification passed. FR-078 completion records are delivered; activity/streak aggregation remains Sprint08. [Contract](contracts/sprint-07-api.md) · [Ledger](sprints/sprint-07-implementation.md).

**Sprint 06 update:** Personal folders/sets/cards, atomic bulk edits/deletion, offline review and account-private synchronization are implemented. User-selected conflict handling preserves local edits and offers server copy or save-copy. Room chunk storage avoids large-row limits; AI FR-069 remains Sprint07. [Contract](contracts/sprint-06-api.md) · [Verification and rollout](sprints/sprint-06-report.md).

**Sprint 05 update:** Persistent public catalog/ETag caching, authoritative deletion inventory, Room/WorkManager outbox and manual learned/unlearned progress with topic/level summaries are implemented and locally tested. [Contract](contracts/sprint-05-api.md) · [Verification/rollout limits](sprints/sprint-05-report.md). Offline covers previously fetched content; flashcard/activity producers and shared rollout remain separate.

**Sprint 04 update:** Vocabulary sets/items and grammar sub-topics/patterns now have online list/detail flows, multilingual search, filters, pagination and rich Japanese examples. See [test evidence](sprints/sprint-04-report.md) and [API contract](contracts/sprint-04-api.md). Offline FR-036/FR-044 and learning progress remain Sprint 05; shared-environment acceptance is pending.

**Sprint 03 update:** Level counts, complete topic bundles and startup configuration/health/maintenance/version behavior are implemented and locally tested. [Sprint 03 evidence](sprints/sprint-03-report.md) and [canonical contracts](contracts/sprint-03-api.md) supersede older roadmap/configuration observations. Public catalog reads remain compatible pending D02 policy; catalog mutations require Admin. FR-027 remains Sprint 05. Shared deployment and production HTTPS acceptance remain open.

**Sprint 02 update:** Android/backend account and profile changes are implemented and locally tested. Verification, recovery, refresh rotation/revocation, profile editing, avatars, preferences and deletion now have evidence in the [Sprint 02 report](sprints/sprint-02-report.md) and [API contract](contracts/sprint-02-api.md). These supersede older source-review notes for FR-001–FR-020. FR-008 device registration/cleanup and reminder delivery remain Sprint 08. Shared backend migration and actual SMTP/Cloudinary acceptance remain pending; whole-product acceptance is not claimed.

**Sprint 01 update:** Security/environment changes, Android/backend build results and live read-only API checks are recorded in the [execution report](sprints/sprint-01-report.md). The [backend audit](contracts/backend-audit.md) and [offline design](architecture/offline-sync.md) supersede the initial review's missing-backend and proposed architecture assumptions. Unchanged initial source-review notes remain historical observations, not claims about the latest code.

This document retains the original product requirements, feature numbering, FR-001–FR-105, BR-001–BR-019, NFR-001–NFR-026, and AC-001–AC-017. Requirements using “must”, “should”, or future journeys describe target behavior unless marked as implemented. Source-review notes describe current behavior and take precedence over inherited statements about the existing application. The unchanged text/table conversion is [nihonix-original.md](nihonix-original.md).

**Status convention:** “Implemented in client” means the inspected source contains client logic and wiring; it does not mean the backend or end-to-end behavior passed testing. “Partial” means some required behavior exists. “Planned” means no implementation was found in the inspected app. “Unverified” means external/backend evidence is needed.

**Current scope:** secure registration/email verification, username/password login, recovery, session rotation/revocation, profile editing/avatar/deletion/settings, JLPT selection/update, topic browsing/counts/bundles, and startup configuration/health/maintenance/version behavior are implemented and locally tested. Online vocabulary/grammar browsing and details are delivered in Sprint 04. Persistent offline study and vocabulary/grammar progress synchronization are delivered locally in Sprint 05. Manual flashcards/review/synchronization are delivered locally in Sprint06. AI/quota and practice are implemented in Sprint07; streak/calendar and notifications remain later-sprint work. Shared deployment/provider acceptance remains pending; see the execution reports above.

## 1. Introduction

### 1.1. Purpose

Nihonix là ứng dụng mobile hỗ trợ người dùng học tiếng Nhật theo lộ trình từ N5 đến N1.

Ứng dụng cung cấp hệ thống nội dung học tập có cấu trúc, bao gồm từ vựng, ngữ pháp, flashcard và bài luyện tập. Người dùng có thể theo dõi tiến độ học, duy trì chuỗi ngày học (streak), cá nhân hóa lịch học và sử dụng AI để tạo nội dung phục vụ việc học.

Ứng dụng hướng đến trải nghiệm học tập cá nhân, có khả năng hoạt động offline đối với nội dung đã đồng bộ và tự động đồng bộ dữ liệu khi có kết nối mạng.

### 1.2. Project Scope

> **Source review:** Current delivery is a single Android app module with auth and topics features. All other product capabilities in this scope table remain target scope.

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

> **Source review:** Only authentication, read-only profile, and level/topic browsing currently have feature implementations. JLPT level mutation belongs to the topics feature. Other rows are planned modules.

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

> **Source review:** Current flow: register with username (minimum 3 characters), email, password (minimum 6 characters), optional full name → return to login. Login submits username/password and saves tokens and user. Email verification and rejection of unverified accounts remain target requirements. Recovery uses request code → verify six-character code → reset with temporary token and password of at least 8 characters; reconcile the registration/reset password policy. No FCM cleanup exists on logout. The original verification flows below describe the target behavior.

Description: Allow users to register, authenticate, verify email and manage their account credentials.

| ID | Functional Requirement | Priority | Client status | Evidence / remaining work |
| --- | --- | --- | --- | --- |
| FR-001 | Users can register with username, email, password and optional full name. | High | Partial | Registration also requires username; optional full name. [AuthApiService](../app/src/main/kotlin/com/nihonix/mobile/feature/auth/data/api/AuthApiService.kt), [AuthRepositoryImpl](../app/src/main/kotlin/com/nihonix/mobile/feature/auth/data/repository/AuthRepositoryImpl.kt) |
| FR-002 | System sends an email verification code after registration. | High | Planned | Audited AuthService.register creates an active account without a registration-verification email flow; see [backend audit](contracts/backend-audit.md). |
| FR-003 | Users can verify their email using a verification code. | High | Planned | No verify-email API method or verification screen. [AuthApiService](../app/src/main/kotlin/com/nihonix/mobile/feature/auth/data/api/AuthApiService.kt), [AuthRepositoryImpl](../app/src/main/kotlin/com/nihonix/mobile/feature/auth/data/repository/AuthRepositoryImpl.kt) |
| FR-004 | Users can log in with username and password; verified-email enforcement remains required. | High | Partial | Username/password login exists; verified-account policy is not demonstrated by mobile source. [AuthApiService](../app/src/main/kotlin/com/nihonix/mobile/feature/auth/data/api/AuthApiService.kt), [AuthRepositoryImpl](../app/src/main/kotlin/com/nihonix/mobile/feature/auth/data/repository/AuthRepositoryImpl.kt) |
| FR-005 | System must reject login attempts from unverified accounts. | High | Planned | Audited login has no email-verification gate; backend and Android verification flow are required in Sprint 02. |
| FR-006 | System provides access token and refresh token. | High | Implemented in client | Token pair storage and 401 refresh/rotation exist; backend behavior unverified. [TokenStorage](../app/src/main/kotlin/com/nihonix/mobile/core/storage/TokenStorage.kt) |
| FR-007 | System supports token refresh and rotation. | High | Implemented in client | Token pair storage and 401 refresh/rotation exist; backend behavior unverified. [TokenStorage](../app/src/main/kotlin/com/nihonix/mobile/core/storage/TokenStorage.kt) |
| FR-008 | Users can log out and unregister their device token. | High | Partial | Logout attempts server revocation and always clears local session; FCM unregistration absent. [AuthApiService](../app/src/main/kotlin/com/nihonix/mobile/feature/auth/data/api/AuthApiService.kt), [AuthRepositoryImpl](../app/src/main/kotlin/com/nihonix/mobile/feature/auth/data/repository/AuthRepositoryImpl.kt) |
| FR-009 | Users can request a password reset code. | High | Implemented in client | Password recovery uses email, six-character code, temporary reset token, then new password. [AuthApiService](../app/src/main/kotlin/com/nihonix/mobile/feature/auth/data/api/AuthApiService.kt), [AuthRepositoryImpl](../app/src/main/kotlin/com/nihonix/mobile/feature/auth/data/repository/AuthRepositoryImpl.kt) |
| FR-010 | Users can verify the reset code and set a new password. | High | Implemented in client | Password recovery uses email, six-character code, temporary reset token, then new password. [AuthApiService](../app/src/main/kotlin/com/nihonix/mobile/feature/auth/data/api/AuthApiService.kt), [AuthRepositoryImpl](../app/src/main/kotlin/com/nihonix/mobile/feature/auth/data/repository/AuthRepositoryImpl.kt) |
| FR-011 | Users can resend verification or reset codes. | Medium | Partial | Resend API wired in password recovery; registration verification flow absent. [AuthApiService](../app/src/main/kotlin/com/nihonix/mobile/feature/auth/data/api/AuthApiService.kt), [AuthRepositoryImpl](../app/src/main/kotlin/com/nihonix/mobile/feature/auth/data/repository/AuthRepositoryImpl.kt) |

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

> **Source review:** Only profile viewing and current JLPT level viewing/updating exist. Profile editing, avatar upload, account deletion and learning/reminder settings are planned.

Description: Allow users to manage their personal information and learning preferences.

| ID | Functional Requirement | Priority | Client status | Evidence / remaining work |
| --- | --- | --- | --- | --- |
| FR-012 | Users can view their profile. | High | Implemented in client | Profile fetched using POST auth/me. [AuthApiService](../app/src/main/kotlin/com/nihonix/mobile/feature/auth/data/api/AuthApiService.kt), [AuthRepositoryImpl](../app/src/main/kotlin/com/nihonix/mobile/feature/auth/data/repository/AuthRepositoryImpl.kt) |
| FR-013 | Users can update their personal information. | High | Planned | No corresponding feature implementation found; original requirement retained as target scope. |
| FR-014 | Users can upload or update their avatar. | Medium | Planned | No corresponding feature implementation found; original requirement retained as target scope. |
| FR-015 | Users can delete their account. | Medium | Planned | No corresponding feature implementation found; original requirement retained as target scope. |
| FR-016 | Users can view and update their current JLPT level. | High | Implemented in client | Authenticated GET/PATCH users/me/level; N5–N1 validation and cached-user update. [TopicsLevelsApiService](../app/src/main/kotlin/com/nihonix/mobile/feature/topics/data/api/TopicsLevelsApiService.kt), [TopicsLevelsRepositoryImpl](../app/src/main/kotlin/com/nihonix/mobile/feature/topics/data/repository/TopicsLevelsRepositoryImpl.kt) |
| FR-017 | Users can view learning settings. | High | Planned | No corresponding feature implementation found; original requirement retained as target scope. |
| FR-018 | Users can configure push notification preferences. | Medium | Planned | No corresponding feature implementation found; original requirement retained as target scope. |
| FR-019 | Users can configure daily learning goals. | Medium | Planned | No corresponding feature implementation found; original requirement retained as target scope. |
| FR-020 | Users can configure reminder time and timezone. | Medium | Planned | No corresponding feature implementation found; original requirement retained as target scope. |

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
| FR-021 | Users can view all available JLPT levels. | High | Implemented in client | Level list/detail and topic list/detail wired to screens; live data not verified. [TopicsLevelsApiService](../app/src/main/kotlin/com/nihonix/mobile/feature/topics/data/api/TopicsLevelsApiService.kt), [TopicsLevelsRepositoryImpl](../app/src/main/kotlin/com/nihonix/mobile/feature/topics/data/repository/TopicsLevelsRepositoryImpl.kt) |
| FR-022 | System displays the number of topics, vocabulary items and grammar content per level. | High | Implemented and locally tested | Count DTO/UI and consistent PostgreSQL aggregate queries; [Sprint 03 evidence](sprints/sprint-03-report.md). Shared rollout pending. |
| FR-023 | Users can view detailed information about a selected level. | High | Implemented in client | Level list/detail and topic list/detail wired to screens; live data not verified. [TopicsLevelsApiService](../app/src/main/kotlin/com/nihonix/mobile/feature/topics/data/api/TopicsLevelsApiService.kt), [TopicsLevelsRepositoryImpl](../app/src/main/kotlin/com/nihonix/mobile/feature/topics/data/repository/TopicsLevelsRepositoryImpl.kt) |
| FR-024 | Users can browse topics belonging to a level. | High | Implemented in client | Level list/detail and topic list/detail wired to screens; live data not verified. [TopicsLevelsApiService](../app/src/main/kotlin/com/nihonix/mobile/feature/topics/data/api/TopicsLevelsApiService.kt), [TopicsLevelsRepositoryImpl](../app/src/main/kotlin/com/nihonix/mobile/feature/topics/data/repository/TopicsLevelsRepositoryImpl.kt) |
| FR-025 | Users can view topic details. | High | Implemented in client | Level list/detail and topic list/detail wired to screens; live data not verified. [TopicsLevelsApiService](../app/src/main/kotlin/com/nihonix/mobile/feature/topics/data/api/TopicsLevelsApiService.kt), [TopicsLevelsRepositoryImpl](../app/src/main/kotlin/com/nihonix/mobile/feature/topics/data/repository/TopicsLevelsRepositoryImpl.kt) |
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
| FR-078 | Application records learning activity after completing a practice set. | High | Partial | Unique transactional completion event; activity/streak/calendar aggregation remains Sprint08. |
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

> **Source review:** Planned: no activity recording, streak calculation, or calendar integration exists.

Description: Encourage consistent learning through daily activity tracking and calendar visualization.

ポケット語学の使い方 | ポケット語学 【NHK出版】

Keep the chain. — Lingora Mandarin Language Learning App

브런치

6

| ID | Functional Requirement | Priority | Client status | Evidence / remaining work |
| --- | --- | --- | --- | --- |
| FR-080 | Users can view their current learning streak. | High | Planned | No corresponding feature implementation found; original requirement retained as target scope. |
| FR-081 | Users can view their longest learning streak if supported by backend. | Medium | Planned | No corresponding feature implementation found; original requirement retained as target scope. |
| FR-082 | Users can view learning minutes for the current day. | High | Planned | No corresponding feature implementation found; original requirement retained as target scope. |
| FR-083 | Application can record learning activities. | High | Planned | No corresponding feature implementation found; original requirement retained as target scope. |
| FR-084 | Application supports offline activity recording. | High | Planned | No corresponding feature implementation found; original requirement retained as target scope. |
| FR-085 | Application can synchronize pending learning activities. | High | Planned | No corresponding feature implementation found; original requirement retained as target scope. |
| FR-086 | Users can view their learning calendar by month. | High | Planned | No corresponding feature implementation found; original requirement retained as target scope. |
| FR-087 | Application displays daily learning activity on the calendar. | High | Planned | No corresponding feature implementation found; original requirement retained as target scope. |

Business rules:

Learning streaks must be calculated using the user's configured timezone.

Learning activity records must include duration and occurrence timestamp.

Activity synchronization must be idempotent.

Offline activities must preserve their original occurrence timestamps.

The daily learning goal is based on daily_goal_minutes.

Timezone changes must be handled consistently by backend streak calculation.

### 4.11. F11 – Device Registration & Push Notifications

> **Source review:** Planned: no Firebase Messaging dependency, messaging service, device registration API or notification permission request exists. ACCESS_LOCAL_NETWORK permission is unrelated to notification permission.

Description: Register mobile devices and send learning reminders.

| ID | Functional Requirement | Priority | Client status | Evidence / remaining work |
| --- | --- | --- | --- | --- |
| FR-088 | Application requests notification permission when appropriate. | Medium | Planned | No corresponding feature implementation found; original requirement retained as target scope. |
| FR-089 | Application retrieves the FCM registration token. | High | Planned | No corresponding feature implementation found; original requirement retained as target scope. |
| FR-090 | Application registers or updates the device token. | High | Planned | No corresponding feature implementation found; original requirement retained as target scope. |
| FR-091 | Application removes the device token during logout. | High | Planned | No corresponding feature implementation found; original requirement retained as target scope. |
| FR-092 | Users can enable or disable push notifications. | Medium | Planned | No corresponding feature implementation found; original requirement retained as target scope. |
| FR-093 | Users can configure daily reminder time. | Medium | Planned | No corresponding feature implementation found; original requirement retained as target scope. |
| FR-094 | Backend sends reminders based on user timezone and settings. | High | Planned | No corresponding feature implementation found; original requirement retained as target scope. |
| FR-095 | Application handles notification payload and deeplink navigation. | Medium | Planned | No corresponding feature implementation found; original requirement retained as target scope. |

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

> **Source review:** Quota limits are retained from the source document as proposals. No client or backend evidence confirms enforcement, reset policy, or shared quotas.

| Feature | Daily Limit | Maximum per Request |
| --- | --- | --- |
| Flashcard Generation | 2 requests | 15 cards |
| Fill-in-the-Blank | 3 requests | 15 questions |
| Multiple Choice | Follow backend quota policy | Follow backend policy |

Quota must be enforced on the backend, not just in the Android application.

### 7.3. Learning Rules

> **Source review:** Level/topic models exist, but progress, duration, streak/timezone and goal rules remain planned.

| Rule ID | Description |
| --- | --- |
| BR-005 | Learning roadmap contains JLPT levels N5–N1. |
| BR-006 | Each topic belongs to a learning level. |
| BR-007 | Progress must be associated with the corresponding learning item. |
| BR-008 | Learning activities must include duration and occurrence timestamp. |
| BR-009 | Streak calculation must respect the user's timezone. |
| BR-010 | Daily goal completion is calculated using learning minutes. |

### 7.4. Offline Rules

> **Source review:** Offline rules remain target requirements; there is no persistent content or pending-change storage.

| Rule ID | Description |
| --- | --- |
| BR-011 | Only previously downloaded content is available offline. |
| BR-012 | Offline changes must be stored locally until synchronized. |
| BR-013 | Content synchronization must use ETag validation where supported. |
| BR-014 | Progress synchronization must be idempotent. |
| BR-015 | Server-owned learning content must not be overwritten by local changes. |

### 7.5. Notification Rules

> **Source review:** Notification rules remain target requirements; no FCM integration or scheduler exists.

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
| Content database | Not implemented; Room/DataStore remain candidate future choices |
| Background synchronization | Not implemented; WorkManager remains a future choice |
| Push / images / AI | No FCM, Coil, or AI integration dependency declared |

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
| AC-001 | User can register, verify email and log in successfully. | Partial: registration/login exist; email verification missing |
| AC-002 | Unverified users cannot access authenticated features. | Unverified: backend enforcement required |
| AC-003 | User can browse JLPT levels from N5 to N1. | Client browsing implemented; verify real N5–N1 dataset |
| AC-004 | User can access topics, vocabulary and grammar content. | Partial: topics exist; vocabulary/grammar missing |
| AC-005 | User can access previously synchronized content offline. | Implemented; local automated evidence in [Sprint 05 report](sprints/sprint-05-report.md). Shared rollout pending. |
| AC-006 | Application can synchronize content using ETag. | Implemented; local automated evidence in [Sprint 05 report](sprints/sprint-05-report.md). Shared rollout pending. |
| AC-007 | Learning progress is preserved across application restarts. | Implemented; local automated evidence in [Sprint 05 report](sprints/sprint-05-report.md). Shared rollout pending. |
| AC-008 | Progress synchronization does not create duplicated records. | Implemented; local automated evidence in [Sprint 05 report](sprints/sprint-05-report.md). Shared rollout pending. |
| AC-009 | User can create and manage personal flashcard folders and sets. | Local automated CRUD/offline/conflict/restart evidence in [Sprint06](sprints/sprint-06-report.md); shared multi-device acceptance pending. |
| AC-010 | AI can generate supported flashcards and exercises within quota. | Local deterministic Sprint07 evidence in [report](sprints/sprint-07-report.md); configured-provider/shared acceptance pending. |
| AC-011 | User can complete exercises and view results. | Local transactional grading, replay and native UI evidence in [Sprint07](sprints/sprint-07-report.md); shared acceptance pending. |
| AC-012 | Learning activities are recorded and synchronized correctly. | Planned: implementation and acceptance testing required |
| AC-013 | Streak and calendar reflect the user's learning activities and timezone. | Planned: implementation and acceptance testing required |
| AC-014 | User can configure daily learning goals and reminder preferences. | Planned: implementation and acceptance testing required |
| AC-015 | Push notifications are handled correctly. | Planned: implementation and acceptance testing required |
| AC-016 | Application handles expired authentication sessions and API errors. | Partial: refresh/error handling exists; integration testing required |
| AC-017 | Users cannot access or modify another user's personal data. | Unverified: backend enforcement required |

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

The instrumented test is the generated application-context check; it does not validate learning/authentication journeys. Repository, authenticator concurrency/rotation, secure-storage failure, real API authorization and full UI/offline integration still need verification. The original testing requirements in Section 11 remain acceptance work.

Documentation validation checks that every original FR, BR, NFR and AC ID is retained, table rows render consistently, and relative source links point to project files.

## Appendix C. Source-derived decisions and remaining work

| ID | Issue / decision needed | Related requirements |
| --- | --- | --- |
| OQ-015 | Resolve public level/topic API annotations versus authenticated-only learning policy; current UI has no guest entry. | BR-001, Section 2 |
| OQ-016 | Provide verify-email contract/screen and verify backend rejection of unverified accounts. | FR-002–FR-005 |
| OQ-017 | Standardize credential contract: current login uses username, not email. Registration minimum password length is 6; reset minimum is 8. | FR-001, FR-004, FR-010 |
| OQ-018 | Configure a production HTTPS endpoint and cleartext policy; current default is emulator HTTP. | NFR-006 |
| OQ-019 | Remove ordinary-preferences token fallback or define a secure failure strategy. | NFR-007 |
| OQ-020 | Gate/redact BODY network logging so credentials and token payloads are not exposed. | NFR-011 |
| OQ-021 | Verify session-expiration navigation and retry behavior with real 401/refresh failures; local token presence is the initial login check. | NFR-015, AC-016 |
| OQ-022 | Define persistent content/pending-write schema and retry/conflict contracts before promising offline learning. | FR-027, FR-045–FR-058 |
| OQ-023 | Confirm whether API 37 local-network permission should gate the whole app in production; currently denial blocks the NavHost. | Platform / startup |
| OQ-024 | Confirm backend DTOs, authorization and original quota limits using backend source or OpenAPI; none was supplied. | Section 10, OQ-001–OQ-014 |

Evidence for startup behavior: [MainActivity](../app/src/main/kotlin/com/nihonix/mobile/MainActivity.kt), [AuthNavGraph](../app/src/main/kotlin/com/nihonix/mobile/feature/auth/presentation/navigation/AuthNavGraph.kt). The observations above document the inspected implementation; they do not change application code or assert backend defects.


[System Notification] Background shell session 'shell-167' (`npm run build; if ($LASTEXITCODE -eq 0) { npm test -- --runInBand }`) finished with exit status 0. Full output was saved to `C:\Users\Ngoc Anh\.android\studio\agent\conversations\Nihonix-862d30a798452eabf70fc92c85f47f\2679c0f5-d461-47ba-bcba-f89d3ac348c3\session\agent\shell-logs\shell-167.log`. Call `interact_shell` with `sessionId="shell-167"` and `action="status"` if you need to inspect its final output.
> Source: `C:\Users\Ngoc Anh\Downloads\nihonix.docx`. Text and tables converted on 2026-11-14; original wording and requirement IDs retained. Word-specific layout, diagrams, and automatic list numbering are not reproduced. For the source-reviewed SRS, see [SRS.md](SRS.md).

# NIHОNIX – SOFTWARE REQUIREMENTS SPECIFICATION (SRS)

## 1. Introduction

### 1.1. Purpose

Nihonix là ứng dụng mobile hỗ trợ người dùng học tiếng Nhật theo lộ trình từ N5 đến N1.

Ứng dụng cung cấp hệ thống nội dung học tập có cấu trúc, bao gồm từ vựng, ngữ pháp, flashcard và bài luyện tập. Người dùng có thể theo dõi tiến độ học, duy trì chuỗi ngày học (streak), cá nhân hóa lịch học và sử dụng AI để tạo nội dung phục vụ việc học.

Ứng dụng hướng đến trải nghiệm học tập cá nhân, có khả năng hoạt động offline đối với nội dung đã đồng bộ và tự động đồng bộ dữ liệu khi có kết nối mạng.

### 1.2. Project Scope

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

Description: Allow users to register, authenticate, verify email and manage their account credentials.

| ID | Functional Requirement | Priority |
| --- | --- | --- |
| FR-001 | Users can register using email and password. | High |
| FR-002 | System sends an email verification code after registration. | High |
| FR-003 | Users can verify their email using a verification code. | High |
| FR-004 | Users can log in using verified credentials. | High |
| FR-005 | System must reject login attempts from unverified accounts. | High |
| FR-006 | System provides access token and refresh token. | High |
| FR-007 | System supports token refresh and rotation. | High |
| FR-008 | Users can log out and unregister their device token. | High |
| FR-009 | Users can request a password reset code. | High |
| FR-010 | Users can verify the reset code and set a new password. | High |
| FR-011 | Users can resend verification or reset codes. | Medium |

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

Description: Allow users to manage their personal information and learning preferences.

| ID | Functional Requirement | Priority |
| --- | --- | --- |
| FR-012 | Users can view their profile. | High |
| FR-013 | Users can update their personal information. | High |
| FR-014 | Users can upload or update their avatar. | Medium |
| FR-015 | Users can delete their account. | Medium |
| FR-016 | Users can view and update their current JLPT level. | High |
| FR-017 | Users can view learning settings. | High |
| FR-018 | Users can configure push notification preferences. | Medium |
| FR-019 | Users can configure daily learning goals. | Medium |
| FR-020 | Users can configure reminder time and timezone. | Medium |

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

Description: Provide a structured learning roadmap from JLPT N5 to N1.

Bunpo: Learn Japanese for Android - Download

Passing JLPT—The Ultimate Study Companion (Bunpo App Review) | Most Wandered

PORO - Japanese Grammar - Education App | MWM

12

| ID | Functional Requirement | Priority |
| --- | --- | --- |
| FR-021 | Users can view all available JLPT levels. | High |
| FR-022 | System displays the number of topics, vocabulary items and grammar content per level. | High |
| FR-023 | Users can view detailed information about a selected level. | High |
| FR-024 | Users can browse topics belonging to a level. | High |
| FR-025 | Users can view topic details. | High |
| FR-026 | Users can retrieve a complete topic bundle. | High |
| FR-027 | Application supports caching topic content for offline access. | High |

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

Description: Allow users to browse and study Japanese vocabulary.

Flippon - Japanese Vocabulary - Education App | MWM

#分享 自己開發的 AI 單字卡 App - 日語板 | Dcard

QUARTET Vocab & Kanji - Education App | MWM

10

| ID | Functional Requirement | Priority |
| --- | --- | --- |
| FR-028 | Users can view vocabulary items. | High |
| FR-029 | Users can filter vocabulary by topic. | High |
| FR-030 | Users can search vocabulary by keyword. | High |
| FR-031 | Users can view vocabulary details. | High |
| FR-032 | Users can browse vocabulary sets. | High |
| FR-033 | Users can view vocabulary sets by topic. | High |
| FR-034 | Users can view vocabulary set details. | High |
| FR-035 | Application supports pagination for vocabulary lists. | Medium |
| FR-036 | Application caches vocabulary content for offline learning. | High |

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

Description: Allow users to study Japanese grammar through structured grammar topics and patterns.

The Best Bunpro Alternative App for Japanese Grammar (2025)

Bunpo! - Une application pour apprendre la grammaire japonaise !

6

| ID | Functional Requirement | Priority |
| --- | --- | --- |
| FR-037 | Users can browse grammar sub-topics. | High |
| FR-038 | Users can filter grammar sub-topics by JLPT level. | High |
| FR-039 | Users can view grammar sub-topic details. | High |
| FR-040 | Users can browse grammar patterns. | High |
| FR-041 | Users can search and filter grammar patterns. | Medium |
| FR-042 | Users can view grammar pattern details. | High |
| FR-043 | Users can view grammar patterns belonging to a sub-topic. | High |
| FR-044 | Application supports offline grammar content. | High |

Grammar content should support:

Grammar pattern.

Meaning and explanation.

Usage instructions.

Example sentences.

Related JLPT level.

Related grammar sub-topic.

### 4.6. F6 – Offline Content & Synchronization

Description: Allow users to download learning content and access it without an internet connection.

| ID | Functional Requirement | Priority |
| --- | --- | --- |
| FR-045 | Application can retrieve synchronization metadata. | High |
| FR-046 | Application can compare local and remote content versions. | High |
| FR-047 | Application can download content when the local version is outdated. | High |
| FR-048 | Application supports ETag-based cache validation. | High |
| FR-049 | Application handles HTTP 304 responses without downloading unchanged content. | High |
| FR-050 | Application can access previously downloaded learning content offline. | High |
| FR-051 | Application can synchronize pending user data when connectivity is restored. | High |

Supported synchronization entities:

Level.

Topic.

Vocabulary item.

Vocabulary set.

Grammar sub-topic.

Grammar pattern.

Content synchronization flow:

Application checks network availability.

Application requests the synchronization manifest for the selected level.

Backend returns entity keys, ETags and update timestamps.

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

Description: Track the user's learning progress across vocabulary, grammar and other supported learning items.

| ID | Functional Requirement | Priority |
| --- | --- | --- |
| FR-052 | Application records learning progress. | High |
| FR-053 | Application can submit multiple progress updates in one batch. | High |
| FR-054 | Application retrieves progress updates from the backend. | High |
| FR-055 | Application supports incremental progress synchronization. | High |
| FR-056 | Users can view learning progress summaries. | High |
| FR-057 | Users can view progress by JLPT level and topic. | High |
| FR-058 | Application preserves pending progress updates while offline. | High |

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

Description: Allow users to create and manage personal flashcard folders, sets and cards.

【英語】「WordHolic」を使って英単語を効率よく暗記してみた - 理系社会人から始める英語学習

StudyPod - Flashcards app | Create Flashcards online

Flashwise - Smarter Studying

10

| ID | Functional Requirement | Priority |
| --- | --- | --- |
| FR-059 | Users can create flashcard folders. | High |
| FR-060 | Users can view their folders. | High |
| FR-061 | Users can view folder details. | High |
| FR-062 | Users can update folders. | High |
| FR-063 | Users can delete folders. | High |
| FR-064 | Users can create flashcard sets and multiple cards. | High |
| FR-065 | Users can bulk create, update and delete cards. | High |
| FR-066 | Users can view flashcard sets by folder. | High |
| FR-067 | Users can view cards belonging to a set. | High |
| FR-068 | Users can delete a flashcard set. | High |
| FR-069 | Users can generate flashcards using AI. | Medium |
| FR-070 | Users can synchronize flashcard changes. | High |

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

Description: Generate and complete AI-powered Japanese exercises.

renshuu.org

Learn real Japanese — Kotrin

アークアカデミー新宿校 - JPSchool-navi

8

| ID | Functional Requirement | Priority |
| --- | --- | --- |
| FR-071 | Users can generate fill-in-the-blank exercises. | High |
| FR-072 | Users can generate multiple-choice exercises. | High |
| FR-073 | Users can browse generated practice sets. | High |
| FR-074 | Users can filter practice sets by topic, type and status. | Medium |
| FR-075 | Users can view practice set details. | High |
| FR-076 | Users can submit their answers. | High |
| FR-077 | Application displays the practice results returned by backend. | High |
| FR-078 | Application records learning activity after completing a practice set. | High |
| FR-079 | Application synchronizes practice data when necessary. | Medium |

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

Description: Encourage consistent learning through daily activity tracking and calendar visualization.

ポケット語学の使い方 | ポケット語学 【NHK出版】

Keep the chain. — Lingora Mandarin Language Learning App

브런치

6

| ID | Functional Requirement | Priority |
| --- | --- | --- |
| FR-080 | Users can view their current learning streak. | High |
| FR-081 | Users can view their longest learning streak if supported by backend. | Medium |
| FR-082 | Users can view learning minutes for the current day. | High |
| FR-083 | Application can record learning activities. | High |
| FR-084 | Application supports offline activity recording. | High |
| FR-085 | Application can synchronize pending learning activities. | High |
| FR-086 | Users can view their learning calendar by month. | High |
| FR-087 | Application displays daily learning activity on the calendar. | High |

Business rules:

Learning streaks must be calculated using the user's configured timezone.

Learning activity records must include duration and occurrence timestamp.

Activity synchronization must be idempotent.

Offline activities must preserve their original occurrence timestamps.

The daily learning goal is based on daily_goal_minutes.

Timezone changes must be handled consistently by backend streak calculation.

### 4.11. F11 – Device Registration & Push Notifications

Description: Register mobile devices and send learning reminders.

| ID | Functional Requirement | Priority |
| --- | --- | --- |
| FR-088 | Application requests notification permission when appropriate. | Medium |
| FR-089 | Application retrieves the FCM registration token. | High |
| FR-090 | Application registers or updates the device token. | High |
| FR-091 | Application removes the device token during logout. | High |
| FR-092 | Users can enable or disable push notifications. | Medium |
| FR-093 | Users can configure daily reminder time. | Medium |
| FR-094 | Backend sends reminders based on user timezone and settings. | High |
| FR-095 | Application handles notification payload and deeplink navigation. | Medium |

Reminder rules:

Notifications are sent only when reminders are enabled.

Notifications must respect the configured timezone.

Notifications should only be sent when the user has not reached the daily learning goal.

FCM payload must contain a notification type and deeplink.

Application should route the user to the corresponding screen when a supported notification is tapped.

### 4.12. F12 – AI Usage & Quota Management

Description: Provide transparency about AI feature usage and enforce usage limits.

| ID | Functional Requirement | Priority |
| --- | --- | --- |
| FR-096 | Users can view AI usage information. | High |
| FR-097 | Application displays flashcard generation quota. | High |
| FR-098 | Application displays practice generation quota. | High |
| FR-099 | Application handles quota exceeded responses. | High |
| FR-100 | Application displays the quota reset time. | Medium |

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

Description: Retrieve application configuration and handle system-level states.

| ID | Functional Requirement | Priority |
| --- | --- | --- |
| FR-101 | Application can check backend health. | Medium |
| FR-102 | Application can retrieve application configuration. | High |
| FR-103 | Application can detect maintenance mode. | High |
| FR-104 | Application can detect minimum supported application version. | High |
| FR-105 | Application handles server errors consistently. | High |

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

Main Learning Journey

Create Account / LoginAuthenticate and verify email

Select JLPT LevelChoose N5–N1 learning level

Explore TopicsBrowse vocabulary and grammar topics

Study Learning ContentRead vocabulary and grammar materials

Practice & ReviewUse personal flashcards and AI exercises

Track ProgressUpdate learning progress and daily activity

Maintain Learning HabitTrack streaks, goals and calendar

### 5.4. Offline Learning Journey

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

| Rule ID | Description |
| --- | --- |
| BR-001 | Only authenticated users can access learning content. |
| BR-002 | Unverified accounts cannot log in. |
| BR-003 | Authenticated APIs require a valid JWT. |
| BR-004 | Users can only access their own personal data. |

### 7.2. AI Usage Limits

| Feature | Daily Limit | Maximum per Request |
| --- | --- | --- |
| Flashcard Generation | 2 requests | 15 cards |
| Fill-in-the-Blank | 3 requests | 15 questions |
| Multiple Choice | Follow backend quota policy | Follow backend policy |

Quota must be enforced on the backend, not just in the Android application.

### 7.3. Learning Rules

| Rule ID | Description |
| --- | --- |
| BR-005 | Learning roadmap contains JLPT levels N5–N1. |
| BR-006 | Each topic belongs to a learning level. |
| BR-007 | Progress must be associated with the corresponding learning item. |
| BR-008 | Learning activities must include duration and occurrence timestamp. |
| BR-009 | Streak calculation must respect the user's timezone. |
| BR-010 | Daily goal completion is calculated using learning minutes. |

### 7.4. Offline Rules

| Rule ID | Description |
| --- | --- |
| BR-011 | Only previously downloaded content is available offline. |
| BR-012 | Offline changes must be stored locally until synchronized. |
| BR-013 | Content synchronization must use ETag validation where supported. |
| BR-014 | Progress synchronization must be idempotent. |
| BR-015 | Server-owned learning content must not be overwritten by local changes. |

### 7.5. Notification Rules

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

| ID | Requirement |
| --- | --- |
| NFR-022 | Application should use modular architecture. |
| NFR-023 | UI, business logic and data access must be separated. |
| NFR-024 | API communication must be centralized. |
| NFR-025 | Application should support dependency injection. |
| NFR-026 | Shared components and common business logic should be reusable. |

## 9. Technical Requirements

### 9.1. Android Platform

| Category | Requirement |
| --- | --- |
| Platform | Native Android |
| Language | Kotlin |
| UI | XML Views |
| Architecture | MVVM |
| Async processing | Kotlin Coroutines / Flow |
| Networking | Retrofit / OkHttp |
| Local database | Room |
| Dependency Injection | Hilt |
| Background tasks | WorkManager |
| Push notification | Firebase Cloud Messaging |
| Authentication | JWT |
| Serialization | Kotlin Serialization or Gson, consistent with selected networking stack |
| Image loading | Coil or equivalent |

Technology selection above is the proposed mobile implementation baseline. It does not imply that these libraries are already implemented in the project.

### 9.2. Architecture

The application should follow a layered MVVM architecture.

Presentation Layer

Activities · Fragments · XML Layouts · ViewModels

Domain Layer

Use Cases · Business Rules · Repository Interfaces

Data Layer

Repository Implementations · Remote Data Sources · Local Data Sources

Remote

Retrofit · REST API

Local

Room · DataStore

Architecture requirements:

Activities and Fragments should not directly access APIs or databases.

ViewModels handle UI state and user interactions.

Use Cases encapsulate business logic where appropriate.

Repository coordinates local and remote data sources.

Room serves as the primary local database for offline learning data.

WorkManager handles deferrable background synchronization.

Hilt manages dependency injection.

Coroutines and Flow handle asynchronous operations and observable data.

### 9.3. API Integration Standards

Base URL: /api

Response format:

{

"success": true,

"message": "Request successful",

"data": {},

"errors": null,

"statusCode": 200

}

Requirements:

Use a centralized Retrofit API client.

Use a common response handling mechanism.

Handle HTTP status codes consistently.

Attach JWT to authenticated requests.

Handle refresh token rotation.

Avoid duplicating API request logic across features.

Support ETag and If-None-Match for content synchronization.

Handle pagination and delta synchronization.

Map backend DTOs into domain models and UI models when appropriate.

### 9.4. Error Handling

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

Quy ước trạng thái trong quá trình triển khai mobile:

Ready Backend endpoint đã hoàn thiện, có thể tích hợp sau khi xác nhận request/response schema.

Pending Backend endpoint tồn tại nhưng cần sửa hoặc bổ sung.

New Backend endpoint cần phát triển mới.

Không được đánh dấu một chức năng là hoàn thiện chỉ vì endpoint đã tồn tại. Cần kiểm tra thêm authentication, authorization, validation, response schema và business logic.

## 11. Testing Requirements

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

| ID | Acceptance Criteria |
| --- | --- |
| AC-001 | User can register, verify email and log in successfully. |
| AC-002 | Unverified users cannot access authenticated features. |
| AC-003 | User can browse JLPT levels from N5 to N1. |
| AC-004 | User can access topics, vocabulary and grammar content. |
| AC-005 | User can access previously synchronized content offline. |
| AC-006 | Application can synchronize content using ETag. |
| AC-007 | Learning progress is preserved across application restarts. |
| AC-008 | Progress synchronization does not create duplicated records. |
| AC-009 | User can create and manage personal flashcard folders and sets. |
| AC-010 | AI can generate supported flashcards and exercises within quota. |
| AC-011 | User can complete exercises and view results. |
| AC-012 | Learning activities are recorded and synchronized correctly. |
| AC-013 | Streak and calendar reflect the user's learning activities and timezone. |
| AC-014 | User can configure daily learning goals and reminder preferences. |
| AC-015 | Push notifications are handled correctly. |
| AC-016 | Application handles expired authentication sessions and API errors. |
| AC-017 | Users cannot access or modify another user's personal data. |

## 12. Development Scope & Implementation Phases

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

Các tài liệu cần sử dụng làm nguồn tham chiếu trong quá trình phát triển:

| Reference | Purpose |
| --- | --- |
| backend_scope_final.md | Phạm vi backend và business requirements |
| mobile_srs.md | Mobile requirements hiện có |
| BACKEND_EXISTING_IMPLEMENTATION.md | Đối chiếu implementation thực tế |
| Swagger /api-docs | API endpoints, request/response schemas |
| API List – Nihonix | Feature mapping và backend implementation status |

## 15. Definition of Done

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

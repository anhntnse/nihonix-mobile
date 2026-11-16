# Home Dashboard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Trạng thái:** Planned — chỉ lập tài liệu ngày 2026-11-16; chưa triển khai hoặc chạy kiểm thử theo plan.
**Spec:** [SRS](../../SRS.md), [Sprint 09](../../sprints/sprint-09.md), [acceptance](../../sprints/sprint-09-acceptance.md).

**Goal:** Đáp ứng SRS §5.2 bằng màn tổng quan học tập dùng dữ liệu hiện có.
**Architecture:** Đề xuất feature home theo MVVM hiện tại, ghép repository account/progress/activity; từng section có loading/error/cache riêng. Không thêm backend endpoint trước khi chứng minh API hiện tại thiếu.
**Tech Stack:** Kotlin, Compose Material 3, StateFlow, Navigation Compose.
**Ưu tiên/phụ thuộc:** P2; thiết kế dưới đây là đề xuất cần review khi bắt đầu triển khai. F2/F3/F7/F9/F10; SRS §5.2, NFR-017–021.


## Ràng buộc chung

- Chỉ thực thi sau yêu cầu triển khai riêng; tài liệu này không tự cấp quyền sửa code hay gửi email/push/AI thật.
- Không deploy, chạy migration trên database dùng chung, build/chạy release, ký APK hoặc phát hành.
- Giữ Kotlin/Compose/Material 3, MVVM, ServiceLocator/AppViewModelFactory, Retrofit/Gson, Room/WorkManager hiện có.
- Giữ username login; quota flashcards 2/ngày, practice chung 3/ngày, tối đa 15, reset 00:00 Asia/Ho_Chi_Minh.
- Không thêm SM-2, Social Login, bookmark/ghi chú; không tự động đánh dấu đã học hoặc tự bắt đầu AI generation.
- Evidence cũ là baseline, không thay cho kết quả trên revision thực thi. Ghi commit, môi trường, lệnh/bước, expected/actual và giới hạn.

## Review Focus

- Một section lỗi không chặn toàn Home.
- Account switch/session loss không hiển thị dữ liệu người trước.
- Offline phân biệt tổng server đã nhận với pending local.
- Font lớn và màn hẹp không che action.
- Rotation/back navigation giữ trạng thái hợp lý.

## Phạm vi thiết kế đề xuất

Thứ tự: lời chào + JLPT → mục tiêu/tiến độ hôm nay + streak → shortcuts Vocabulary/Grammar/Practice/Flashcards/Progress/Calendar → Topics.
Recent/recommended topics chỉ hiển thị khi có nguồn dữ liệu đáng tin; không bịa recommendation.
Home là landing sau login và khi mở lại app có session. Notification tap vẫn theo destination allowlist.

## Task HOME-01: Chốt mapping dữ liệu và thiết kế

**Files:** Read `feature/auth/domain/model/User.kt`, `feature/progress/ProgressRepository.kt`, `feature/activity/ActivityRepository.kt`, `feature/activity/ActivityModels.kt`, `core/di/AppViewModelFactory.kt` dưới Android main package.
**Interfaces:** Giữ model/API hiện có; proposed UI `HomeScreen(viewModel: HomeViewModel, onNavigate: (HomeDestination) -> Unit)`; HomeDestination chỉ chứa destination đã có.

- [ ] Mapping mỗi widget đến field/repository thật, gồm account ID, cachedAt và trạng thái pending.
- [ ] Chốt layout, cách hiển thị section thiếu dữ liệu và back behavior bằng thiết kế reviewable.
- [ ] Nếu API thiếu, ghi contract proposal trước khi triển khai backend; không tính progress percentage thành minutes.
- [ ] Chốt nội dung empty/error/offline và source-of-truth trước Task HOME-02.

## Task HOME-02: Aggregate state an toàn account

**Files:** Create proposed `feature/home/HomeUiState.kt`, `HomeViewModel.kt`; Modify `core/di/AppViewModelFactory.kt`; test mới `HomeViewModelTest.kt` trong Android unit test package.
**Interfaces:** Proposed `HomeViewModel.uiState: StateFlow<HomeUiState>`; state chứa account ID và trạng thái riêng profile/activity/progress.

- [ ] Viết test fake repositories: thành công, một section lỗi, cache offline, A→B với response A trả chậm.
- [ ] Chạy test xác nhận RED trước implementation.
- [ ] Aggregate theo account; clear/cancel khi đổi account; không lấy signed-in từ token presence duy nhất.
- [ ] Test GREEN; không tự động generate AI hoặc tự đánh dấu completion.

## Task HOME-03: Screen và navigation

**Files:** Create `feature/home/HomeScreen.kt`; Modify `feature/auth/presentation/navigation/AuthNavGraph.kt`; test mới `app/src/androidTest/kotlin/com/nihonix/mobile/HomeScreenTest.kt`.
**Interfaces:** HOME-01 chốt HomeDestination; map vào routes hiện có, không ghép route bằng dữ liệu không encode.

- [ ] UI tests: mỗi shortcut mở đúng feature, Home login landing, back stack, session revoked, notification tap giữ đúng đích.
- [ ] Implement screen và routing; giữ Topics như destination riêng.
- [ ] Kiểm tra màn hẹp/font lớn/dark/rotation/loading/error/offline.
- [ ] Chạy unit, UI tests mục tiêu, lintDebug; record journey Home→study→Home.

## Điều kiện hoàn tất

- [ ] Widget SRS §5.2 có mapping và evidence, recommendation không có data được ghi rõ.
- [ ] UI không rò dữ liệu qua account switch; shortcuts/back/session behavior pass.
- [ ] Không thay quota, cách tính streak hoặc tiến độ bằng suy đoán.

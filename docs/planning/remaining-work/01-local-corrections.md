# Sửa lỗi local và trạng thái UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Trạng thái:** Verified locally — triển khai và kiểm chứng ngày 2026-11-16; chưa commit/push. [Execution evidence](evidence/local-corrections.md).

Kết quả: 137 unit tests, 74 instrumented tests pass; 5 phase-only tests chạy riêng (4 restart và backup). Lint/debug packaging pass. Backup cloud/default và transfer simulation thực hiện trên API37; pre-Android12/OEM chưa được chạy.
**Spec:** [SRS](../../SRS.md), [Sprint 09](../../sprints/sprint-09.md), [acceptance](../../sprints/sprint-09-acceptance.md).

**Goal:** Đóng D-09-06, sửa quota freshness và chuẩn hóa hiển thị streak.
**Architecture:** Sửa tại nơi sở hữu cache/trạng thái, giữ nguyên sync và backend quota enforcement. Không refactor toàn bộ feature.
**Tech Stack:** Android backup XML, SharedPreferences, Kotlin Flow, Compose.
**Ưu tiên/phụ thuộc:** P1; có thể bắt đầu độc lập. FR-102–105, F10/F12, NFR-012/016/019.


## Ràng buộc chung

- Chỉ thực thi sau yêu cầu triển khai riêng; tài liệu này không tự cấp quyền sửa code hay gửi email/push/AI thật.
- Không deploy, chạy migration trên database dùng chung, build/chạy release, ký APK hoặc phát hành.
- Giữ Kotlin/Compose/Material 3, MVVM, ServiceLocator/AppViewModelFactory, Retrofit/Gson, Room/WorkManager hiện có.
- Giữ username login; quota flashcards 2/ngày, practice chung 3/ngày, tối đa 15, reset 00:00 Asia/Ho_Chi_Minh.
- Không thêm SM-2, Social Login, bookmark/ghi chú; không tự động đánh dấu đã học hoặc tự bắt đầu AI generation.
- Evidence cũ là baseline, không thay cho kết quả trên revision thực thi. Ghi commit, môi trường, lệnh/bước, expected/actual và giới hạn.

## Review Focus

- Restore sang cài đặt mới không mang theo maintenance/version restriction cũ.
- Màn practice đứng yên quá 60 giây phải thể hiện allowance cũ.
- Quota đổi ngày hoặc app trở lại foreground phải kiểm tra freshness.
- Chuyển account không giữ quota hoặc ticker của account trước.
- Streak 0/1/n ngày có nội dung đúng.

## Task FIX-01: Backup cấu hình startup

**Files:** Modify `app/src/main/res/xml/backup_rules.xml`, `data_extraction_rules.xml` cùng thư mục; kiểm tra `core/di/ServiceLocator.kt` và `feature/system/StartupRepository.kt` dưới `app/src/main/kotlin/com/nihonix/mobile/`.
**Interfaces:** Cache hiện dùng SharedPreferences `app_config_<baseUrl.hashCode()>`; không đổi wire `app/config`.

- [x] Đọc lại đường dẫn thực tế và thiết kế backup hiện tại trước khi sửa.
- [x] Chọn lưu config trong vùng không backup, hoặc chính sách loại SharedPreferences có chủ đích; ghi rõ ảnh hưởng đến preferences khác. Không giả định XML hỗ trợ wildcard `app_config_*`.
- [x] Áp dụng cùng chính sách cho full backup, cloud backup và device transfer.
- [x] Kiểm tra backup/restore trên emulator bằng công cụ hỗ trợ; xác nhận token/user/database vẫn bị loại và config được tải mới.
- [x] Chạy lintDebug; lưu bằng chứng restore, không chỉ kiểm tra chuỗi XML.

## Task FIX-02: Quota freshness theo thời gian

**Files:** Modify `feature/practice/data/PracticeRepository.kt`, `presentation/PracticeViewModel.kt`; `presentation/QuotaPanel.kt` dùng state mới, không cần sửa; test `app/src/test/kotlin/com/nihonix/mobile/QuotaFreshnessTest.kt`.
**Interfaces:** Giữ `PracticeState.quotaFresh: Boolean`; hàm thuần `isQuotaFresh(savedAt: Long, now: Long, resetsAt: String? = null): Boolean` trong `feature/practice/data/QuotaFreshness.kt`. Fresh khi timestamp > 0, tuổi cache nằm trong `0 until 60000` và chưa qua server reset; thời gian tương lai coi là stale.

- [x] Viết test tuổi 59,999 ms là fresh, 60,000 ms là stale, timestamp tương lai là stale.
- [x] Reproduce màn đứng yên không có Room emission; test phải fail trước sửa.
- [x] Dùng clock có thể inject và cơ chế cập nhật freshness có lifecycle/cancellation; tái kiểm tra trước generate, không chỉ dựa boolean cũ.
- [x] Test mô phỏng clock jump khi resume, qua reset ngày, account switch và hủy collector; test Room chờ thực tế 61 giây không có DB write. Không gọi AI thật.
- [x] Chạy test mục tiêu và testDebugUnitTest; backend 429 vẫn là nguồn quyết định cuối.

## Task FIX-03: Copy streak

**Files:** Modify `feature/activity/ActivityScreen.kt`, `app/src/main/res/values/strings.xml`.
**Interfaces:** Không đổi `ActivitySummary` hoặc quy tắc 60 giây/ngày.

- [x] Dùng plural resources cho current/longest streak; kiểm tra 0, 1, 2 ngày.
- [x] Smoke UI màn Activity với font lớn; không thêm test chỉ sao chép chuỗi implementation.

## Điều kiện hoàn tất

- [x] Backup restore, freshness biên và account switch có bằng chứng.
- [x] Không đổi quota policy; không mất cache/pending data.
- [x] Review diff, ghi kết quả và liên kết từ Sprint 09; chỉ commit khi được yêu cầu.

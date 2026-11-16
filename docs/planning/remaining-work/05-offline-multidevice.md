# Offline, nhiều thiết bị và vòng đời dữ liệu Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Trạng thái:** Planned — chỉ lập tài liệu ngày 2026-11-16; chưa triển khai hoặc chạy kiểm thử theo plan.
**Spec:** [SRS](../../SRS.md), [Sprint 09](../../sprints/sprint-09.md), [acceptance](../../sprints/sprint-09-acceptance.md).

**Goal:** Kiểm chứng sync/recovery trên thiết bị thật và định nghĩa hướng quản lý tăng trưởng dữ liệu.
**Architecture:** Giữ Room outbox + WorkManager, stable UUID receipts, account isolation và flashcard preserve-local/manual conflict resolution. Test trên API/schema cô lập, không hứa sync tức thì dưới Doze.
**Tech Stack:** Room, WorkManager, Android lifecycle, NestJS/PostgreSQL.
**Ưu tiên/phụ thuộc:** P2; không phụ thuộc Home/provider thật. F6/F7/F8/F10, AC-005–009/012/013/017.


## Ràng buộc chung

- Chỉ thực thi sau yêu cầu triển khai riêng; tài liệu này không tự cấp quyền sửa code hay gửi email/push/AI thật.
- Không deploy, chạy migration trên database dùng chung, build/chạy release, ký APK hoặc phát hành.
- Giữ Kotlin/Compose/Material 3, MVVM, ServiceLocator/AppViewModelFactory, Retrofit/Gson, Room/WorkManager hiện có.
- Giữ username login; quota flashcards 2/ngày, practice chung 3/ngày, tối đa 15, reset 00:00 Asia/Ho_Chi_Minh.
- Không thêm SM-2, Social Login, bookmark/ghi chú; không tự động đánh dấu đã học hoặc tự bắt đầu AI generation.
- Evidence cũ là baseline, không thay cho kết quả trên revision thực thi. Ghi commit, môi trường, lệnh/bước, expected/actual và giới hạn.

## Review Focus

- Force-stop khác process recreation và reinstall; không dùng reinstall để chứng minh durability.
- Timeout sau commit phải retry cùng UUID/payload.
- Account switch trong khi request đang chạy không ghi nhầm account.
- Concurrent parent delete/child edit giữ đúng conflict policy.
- Compaction không phá receipt replay, cursor hoặc tombstone.

## Task OFF-01: Ma trận mạng và vòng đời

**Files:** Existing Android tests `OfflineProgressTest.kt`, `OfflineFlashcardsTest.kt`, `OfflinePracticeTest.kt`, `OfflineActivityTest.kt`, các `*ProcessRestartTest.kt`; main `core/offline/SyncWorker.kt`.
**Interfaces:** Reuse restartPhase seed/verify của fixture; cùng APK và database, PID khác.

- [ ] Chạy lại unit/instrumented fixture liên quan trên revision hiện tại.
- [ ] Trên thiết bị test: online→airplane→edit→background→online; Wi-Fi↔mobile, request timeout, Doze và background restriction.
- [ ] Ghi pending UUID/payload trước/sau force-stop và mở lại; verify sync không nhân đôi.
- [ ] Đổi account A→B khi response A đang bị delay; logout/deletion không hiển thị hoặc submit dữ liệu A dưới B.
- [ ] Ghi thời gian phục hồi đo được, trạng thái retry/discard và giới hạn OS; không đặt SLA khi chưa có số đo.

## Task OFF-02: Hai client sửa đồng thời

**Files:** Android flashcard/progress/activity repositories; backend Sprint05/06/08 integration fixtures; evidence mới `docs/planning/remaining-work/evidence/offline-multidevice.md`.
**Interfaces:** Progress timestamp/UUID LWW; flashcard CAS/dependency versions; activity stable UUID.

- [ ] Hai client cùng account: học/bỏ học cùng item, sửa cùng set, xóa folder khi client kia sửa card.
- [ ] Kiểm tra server-copy/save-copy bảo toàn bản local; không silent overwrite và không tạo bản sao do retry.
- [ ] Push binding account/session thay đổi được kiểm tra bằng fixtures; delivery thật thuộc INT-02.
- [ ] Sync cả hai lại, so server canonical và UI; kiểm tra deletion inventory và cursor advancement.

## Task OFF-03: Retention/compaction design và recovery rehearsal

**Files:** Modify `docs/architecture/offline-sync.md`; create proposal `docs/architecture/data-retention.md` nếu chưa tồn tại.
**Interfaces:** Không xóa receipts/tombstones hiện tại khi chưa có full-resync/versioned cursor contract.

- [ ] Đo kích thước cache/outbox/receipt/activity với dữ liệu đại diện; công bố scale giả định.
- [ ] Đề xuất retention cho từng loại, xử lý client lâu không online, invalid cursor và bootstrap lại.
- [ ] Thiết kế cache eviction không xóa pending edits, drafts hoặc canonical cần giải conflict.
- [ ] Rehearse backup/restore trên database/schema cô lập; kiểm tra ownership, receipts, pending và deletion sau restore.
- [ ] Ghi giới hạn down-migration Sprint06–08; không chạy downgrade metadata-destructive trên database dùng chung.
- [ ] Chỉ lập design/proposal cho compaction; implementation là task tiếp theo sau review contract.

## Điều kiện hoàn tất

- [ ] Ma trận ghi device/API/OS policy/network/expected/actual và lỗi còn lại.
- [ ] Không mất pending hoặc double write trong các case đã chạy.
- [ ] Retention/resync và recovery có proposal rõ ràng; chưa triển khai được ghi đúng trạng thái.

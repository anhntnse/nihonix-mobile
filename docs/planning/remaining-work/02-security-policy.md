# Chốt chính sách truy cập và bảo mật API Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Trạng thái:** Verified locally ngày 2026-11-16 — SEC-01/02/03 complete; D02 Accepted (authenticated catalog). [Execution report](evidence/security-review.md) · [Authorization matrix](evidence/authorization-matrix.md).

Backend build, 101 unit tests và Sprint03/04/05/09 PostgreSQL/JWT fixtures PASS. Android 146 unit tests, lint và debug build PASS. Catalog đã yêu cầu đăng nhập; không nâng cấp dependencies trong task audit. Chưa commit/push. [D02 ledger](evidence/catalog-authentication.md).
**Spec:** [SRS](../../SRS.md), [Sprint 09](../../sprints/sprint-09.md), [acceptance](../../sprints/sprint-09-acceptance.md).

**Goal:** Đóng D02 và D-09-07 bằng quyết định rõ ràng cùng negative tests. D-09-07 đã xử lý local; D02 đã Accepted theo lựa chọn của người dùng.
**Architecture:** Backend JWT bảo vệ mọi catalog read; Android gắn session và chỉ phục vụ cache khi còn local login. Backend chịu trách nhiệm authorization; Android không thay thế guard.
**Tech Stack:** NestJS guards/metadata, JWT, PostgreSQL fixtures, Retrofit.
**Ưu tiên/phụ thuộc:** P1; quyết định D02 là prerequisite cho thay đổi quyền catalog. BR-001, NFR-009/010/011, AC-002/017.


## Ràng buộc chung

- Chỉ thực thi sau yêu cầu triển khai riêng; tài liệu này không tự cấp quyền sửa code hay gửi email/push/AI thật.
- Không deploy, chạy migration trên database dùng chung, build/chạy release, ký APK hoặc phát hành.
- Giữ Kotlin/Compose/Material 3, MVVM, ServiceLocator/AppViewModelFactory, Retrofit/Gson, Room/WorkManager hiện có.
- Giữ username login; quota flashcards 2/ngày, practice chung 3/ngày, tối đa 15, reset 00:00 Asia/Ho_Chi_Minh.
- Không thêm SM-2, Social Login, bookmark/ghi chú; không tự động đánh dấu đã học hoặc tự bắt đầu AI generation.
- Evidence cũ là baseline, không thay cho kết quả trên revision thực thi. Ghi commit, môi trường, lệnh/bước, expected/actual và giới hạn.

## Review Focus

- Guest không truy cập dữ liệu cá nhân.
- User A không đọc/sửa/xóa dữ liệu B qua ID, parent ID hoặc cursor.
- Non-admin không được author/seed/generate catalog.
- Health công khai không tiết lộ key/provider diagnostics nhạy cảm.
- Route mới không vô tình public khi thiếu annotation.

## Task SEC-01: Authorization decision và ma trận

**Files:** Modify `docs/architecture/api-decisions.md`, `docs/SRS.md`; backend đọc `src/main.ts`, `src/app.module.ts`, controllers/guards trong sibling `../nihonix-backend`.
**Interfaces:** Ma trận route × guest/user/admin/owner; D02 Accepted hoặc còn Decision-required, không tự suy ra đã được duyệt.

- [x] Kiểm kê lại route và guards trên revision hiện tại.
- [x] Ghi hai lựa chọn: public catalog hoặc authenticated catalog, ảnh hưởng offline/cache/ETag và client No-Authentication.
- [x] Người dùng chốt D02 ngày 2026-11-16: bắt buộc đăng nhập để đọc catalog theo SRS/BR-001. Triển khai đồng bộ guards, Android headers/cache/sync và fixture; [ledger](evidence/catalog-authentication.md).
- [x] Liên kết quyết định đến BR-001 và các contract; không đánh dấu resolved nếu chỉ mới đề xuất.

## Task SEC-02: Health và bảo vệ route mặc định

**Files:** Backend `src/modules/ai/ai.controller.ts`, `src/modules/ai/services/ai.service.ts`; guard/module path cần xác định qua IDE trước sửa; test `test/sprint09.ownership.integration.ts`.
**Interfaces:** `GET ai/health` hiện public; `GET ai/quota` authenticated. Không đổi response quota khi chưa kiểm tra Android consumer.

- [x] Test response anonymous không lộ trạng thái key/config nhạy cảm; test quyền user/admin cho diagnostics.
- [x] Chọn health tối giản hoặc bảo vệ diagnostics; ghi quyết định.
- [x] So sánh global JWT guard + explicit public marker với kiểm kê route tự động. Đề xuất kiểm kê tự động trước để giảm thay đổi diện rộng; global guard là thay đổi riêng cần review đầy đủ.
- [x] Không chọn global guard trong increment này: giữ controller guards và exact-public inventory regression; có negative probes cho public controller mới, mất Admin guard và nới role sang User. Các auth/public exceptions được ghi rõ.
- [x] Chạy backend unit và `npm run test:sprint09` trên database/schema cô lập.

## Task SEC-03: Retention và dependency risk

**Files:** Modify `docs/architecture/api-decisions.md`; report mới đề xuất `docs/planning/remaining-work/evidence/security-review.md`.

- [x] Lập bảng dữ liệu user/media/session/device/outbox/receipt/log/backup: nơi lưu, khi xóa, thời hạn, ngoại lệ phục hồi backup.
- [x] Kiểm tra thực tế xóa tài khoản trên schema cô lập, kể cả avatar cleanup thất bại.
- [x] Chạy audit dependency read-only, triage findings có thể khai thác; không tự động major upgrade hoặc audit fix diện rộng.
- [x] Ghi rủi ro đã xác nhận riêng với rủi ro chưa chứng minh; tạo task remediation cụ thể nếu có.

## Điều kiện hoàn tất

- [x] Ma trận authorization có expected/actual; D02 có quyết định hoặc blocker rõ ràng.
- [x] Health không lộ diagnostics nhạy cảm; route coverage bảo vệ endpoint mới.
- [x] Retention/deletion và dependency triage có kết quả, không tuyên bố an toàn tuyệt đối.

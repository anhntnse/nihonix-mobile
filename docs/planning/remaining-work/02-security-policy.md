# Chốt chính sách truy cập và bảo mật API Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Trạng thái:** Planned — chỉ lập tài liệu ngày 2026-11-16; chưa triển khai hoặc chạy kiểm thử theo plan.
**Spec:** [SRS](../../SRS.md), [Sprint 09](../../sprints/sprint-09.md), [acceptance](../../sprints/sprint-09-acceptance.md).

**Goal:** Đóng D02 và D-09-07 bằng quyết định rõ ràng cùng negative tests.
**Architecture:** Backend chịu trách nhiệm authorization; Android không thay thế guard. Giữ public-read hiện tại đến khi có quyết định sản phẩm.
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

- [ ] Kiểm kê lại route và guards trên revision hiện tại.
- [ ] Ghi hai lựa chọn: public catalog hoặc authenticated catalog, ảnh hưởng offline/cache/ETag và client No-Authentication.
- [ ] Chốt lựa chọn với người phụ trách sản phẩm trước khi thay đổi quyền; trong lúc chờ vẫn hoàn tất ma trận và test hiện trạng.
- [ ] Liên kết quyết định đến BR-001 và các contract; không đánh dấu resolved nếu chỉ mới đề xuất.

## Task SEC-02: Health và bảo vệ route mặc định

**Files:** Backend `src/modules/ai/ai.controller.ts`, `src/modules/ai/services/ai.service.ts`; guard/module path cần xác định qua IDE trước sửa; test `test/sprint09.ownership.integration.ts`.
**Interfaces:** `GET ai/health` hiện public; `GET ai/quota` authenticated. Không đổi response quota khi chưa kiểm tra Android consumer.

- [ ] Test response anonymous không lộ trạng thái key/config nhạy cảm; test quyền user/admin cho diagnostics.
- [ ] Chọn health tối giản hoặc bảo vệ diagnostics; ghi quyết định.
- [ ] So sánh global JWT guard + explicit public marker với kiểm kê route tự động. Đề xuất kiểm kê tự động trước để giảm thay đổi diện rộng; global guard là thay đổi riêng cần review đầy đủ.
- [ ] Nếu chọn global guard: enumerate public routes rõ ràng, test register/login/recovery/config/health và catalog theo D02.
- [ ] Chạy backend unit và `npm run test:sprint09` trên database/schema cô lập.

## Task SEC-03: Retention và dependency risk

**Files:** Modify `docs/architecture/api-decisions.md`; report mới đề xuất `docs/planning/remaining-work/evidence/security-review.md`.

- [ ] Lập bảng dữ liệu user/media/session/device/outbox/receipt/log/backup: nơi lưu, khi xóa, thời hạn, ngoại lệ phục hồi backup.
- [ ] Kiểm tra thực tế xóa tài khoản trên schema cô lập, kể cả avatar cleanup thất bại.
- [ ] Chạy audit dependency read-only, triage findings có thể khai thác; không tự động major upgrade hoặc audit fix diện rộng.
- [ ] Ghi rủi ro đã xác nhận riêng với rủi ro chưa chứng minh; tạo task remediation cụ thể nếu có.

## Điều kiện hoàn tất

- [ ] Ma trận authorization có expected/actual; D02 có quyết định hoặc blocker rõ ràng.
- [ ] Health không lộ diagnostics nhạy cảm; route coverage bảo vệ endpoint mới.
- [ ] Retention/deletion và dependency triage có kết quả, không tuyên bố an toàn tuyệt đối.

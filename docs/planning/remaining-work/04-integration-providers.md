# Journey tích hợp và provider thật Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Trạng thái:** Planned — chỉ lập tài liệu ngày 2026-11-16; chưa triển khai hoặc chạy kiểm thử theo plan.
**Spec:** [SRS](../../SRS.md), [Sprint 09](../../sprints/sprint-09.md), [acceptance](../../sprints/sprint-09-acceptance.md).

**Goal:** Bổ sung bằng chứng xuyên feature và kiểm tra SMTP/Cloudinary/Gemini/FCM thật.
**Architecture:** Chạy journey với API local cô lập và deterministic providers trước; external provider acceptance là phần riêng, chỉ thực hiện khi có tài khoản test và authorization cụ thể.
**Tech Stack:** Android emulator/device, NestJS HTTP fixtures, PostgreSQL isolated schema, provider adapters.
**Ưu tiên/phụ thuộc:** P2; FIX-02 trước quota idle tests; HOME plan chỉ cần nếu test journey mới qua Home. AC-001–004/010–017, FR-105.


## Ràng buộc chung

- Chỉ thực thi sau yêu cầu triển khai riêng; tài liệu này không tự cấp quyền sửa code hay gửi email/push/AI thật.
- Không deploy, chạy migration trên database dùng chung, build/chạy release, ký APK hoặc phát hành.
- Giữ Kotlin/Compose/Material 3, MVVM, ServiceLocator/AppViewModelFactory, Retrofit/Gson, Room/WorkManager hiện có.
- Giữ username login; quota flashcards 2/ngày, practice chung 3/ngày, tối đa 15, reset 00:00 Asia/Ho_Chi_Minh.
- Không thêm SM-2, Social Login, bookmark/ghi chú; không tự động đánh dấu đã học hoặc tự bắt đầu AI generation.
- Evidence cũ là baseline, không thay cho kết quả trên revision thực thi. Ghi commit, môi trường, lệnh/bước, expected/actual và giới hạn.

## Review Focus

- Timeout sau server commit không tạo bản ghi/charge lặp.
- Provider malformed response không bị xem là generation thành công.
- SMTP/Cloudinary failure không để trạng thái tài khoản/media sai.
- Notification account cũ hoặc binding cũ bị bỏ qua.
- Session expiry giữa hành trình quay về đăng nhập và giữ dữ liệu đúng policy.

## Task INT-01: Journey local đủ feature

**Files:** Existing `docs/sprints/sprint-09-acceptance.md`; backend `test/sprint09.serve.ts`; new evidence `docs/planning/remaining-work/evidence/integration.md`.
**Interfaces:** Dùng các contract Sprint 02–08; không đổi wire chỉ để test dễ hơn.

- [ ] Seed schema cô lập đủ N5–N1, vocabulary sets/words, grammar groups/patterns và hai account test.
- [ ] Ghi counts/parent links mong đợi; phân biệt fixture đại diện với catalog thật đầy đủ.
- [ ] Chạy UI: register→verify→login→vocabulary/grammar→learned/unlearned→progress→flashcard CRUD/review→practice hai loại→submit/result→activity/calendar.
- [ ] Chạy AI cards preview→explicit save→recovery/replay; xác nhận chỉ có một set.
- [ ] Cross-check API totals, quota, completion IDs; so sánh sign-out/sign-in.
- [ ] Lặp với Asia/Ho_Chi_Minh, qua nửa đêm có clock fixture; session revoke, 401, 429, 5xx, malformed body, slow/timeout.
- [ ] Cleanup schema và khôi phục cấu hình debug; không sửa/xóa dữ liệu dùng chung.

## Task INT-02: Provider acceptance riêng biệt

**Files:** Read troubleshooting email/Firebase docs; backend adapter paths tìm lại trước thực thi; evidence như INT-01.
**Interfaces:** Chỉ dùng recipient/account/media test; không lưu secret, verification code hoặc device token trong report.

- [ ] Kiểm tra prerequisites và xin authorization cho đúng hành động external còn thiếu; không coi plan là quyền gửi thật.
- [ ] SMTP: nhận code, resend cooldown, code hết hạn/reuse, recovery; kiểm tra delivery thay vì chỉ key presence.
- [ ] Cloudinary: picker→upload→relaunch→replace→delete account; kiểm tra cleanup và failure.
- [ ] Gemini: hai dạng practice + flashcards, schema tiếng Nhật, max 15, charge đúng một lần, lỗi không consume quota; giới hạn chi phí/số lượt trước chạy.
- [ ] FCM: thiết bị test nhận data-only foreground/background, tap đúng đích, permission denied, stale binding/account; không bật scheduler cho người dùng khác.
- [ ] Ghi request ID đã làm sạch, expected/actual và cleanup. Thiếu provider nào ghi Blocked/Not run, không dùng fake PASS thay thế.

## Task INT-03: Replay/retry backend carryover

**Files:** Read `docs/sprints/sprint-07-report.md`; backend request/submission service paths phải được xác định qua IDE.
**Interfaces:** Stable UUID + immutable payload; receipt lookup phải giữ exact outcome, không double charge/completion.

- [ ] Reproduce alternate submission UUID nhận canonical result và retry readiness trước receipt lookup.
- [ ] Quyết định contract desired behavior; viết failing fixture cho lỗi đã xác nhận, rồi sửa tối thiểu.
- [ ] Chạy `npm run test:sprint07` và `npm run test:sprint09` trên schema cô lập.
- [ ] Ghi rõ follow-up nào không còn tồn tại trên current revision.

## Điều kiện hoàn tất

- [ ] Journey local phủ các màn thiếu trong Sprint 09 và FR-105 có ma trận lỗi theo feature.
- [ ] Provider mỗi loại có Pass/Fail/Blocked rõ ràng và evidence riêng.
- [ ] Không tự nâng tất cả AC thành accepted khi shared acceptance còn thiếu.

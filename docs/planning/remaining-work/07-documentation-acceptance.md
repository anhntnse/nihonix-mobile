# Đồng bộ tài liệu và nghiệm thu Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Trạng thái:** Planned — chỉ lập tài liệu ngày 2026-11-16; chưa triển khai hoặc chạy kiểm thử theo plan.
**Spec:** [SRS](../../SRS.md), [Sprint 09](../../sprints/sprint-09.md), [acceptance](../../sprints/sprint-09-acceptance.md).

**Goal:** Loại bỏ ghi chú lịch sử gây hiểu nhầm, liên kết yêu cầu với code và bằng chứng hiện tại.
**Architecture:** Giữ FR-001–105, BR-001–019, NFR-001–026, AC-001–017 và đường dẫn SRS; reports cũ giữ như lịch sử, thêm chú thích superseded thay vì viết lại kết quả.
**Tech Stack:** Markdown, IDE source search, sprint/contracts/evidence.
**Ưu tiên/phụ thuộc:** P1 cho cleanup baseline; P3 cho acceptance cuối, phụ thuộc kết quả các plan 01–06.


## Ràng buộc chung

- Chỉ thực thi sau yêu cầu triển khai riêng; tài liệu này không tự cấp quyền sửa code hay gửi email/push/AI thật.
- Không deploy, chạy migration trên database dùng chung, build/chạy release, ký APK hoặc phát hành.
- Giữ Kotlin/Compose/Material 3, MVVM, ServiceLocator/AppViewModelFactory, Retrofit/Gson, Room/WorkManager hiện có.
- Giữ username login; quota flashcards 2/ngày, practice chung 3/ngày, tối đa 15, reset 00:00 Asia/Ho_Chi_Minh.
- Không thêm SM-2, Social Login, bookmark/ghi chú; không tự động đánh dấu đã học hoặc tự bắt đầu AI generation.
- Evidence cũ là baseline, không thay cho kết quả trên revision thực thi. Ghi commit, môi trường, lệnh/bước, expected/actual và giới hạn.

## Review Focus

- Source presence không đồng nghĩa test pass hoặc acceptance.
- Ghi chú cũ không tạo lại backlog đã hoàn thành.
- Out-of-scope SM-2 không biến thành requirement mới.
- Blocked/Not run không bị tính vào Pass.
- Link evidence không chứa secret, account thật hoặc token.

## Task DOC-01: Reconcile SRS và decisions

**Files:** Modify `docs/SRS.md`, `docs/architecture/api-decisions.md`, `docs/architecture/offline-sync.md`, `docs/README.md`.
**Interfaces:** Requirement IDs giữ nguyên; trạng thái Planned/Implemented/Locally tested/Partial/Blocked phải có evidence.

- [ ] Sửa §1.2 để SM-2 thống nhất với Out of Scope §1.4; Home vẫn là gap §5.2 đến khi có implementation.
- [ ] Reconcile các đoạn nói chưa có Room/WorkManager, practice, streak, device/config services với source và reports Sprint05–09.
- [ ] Phân biệt Home dashboard chưa có với daily-goal/streak data đã có ở Activity.
- [ ] Cập nhật OQ-001–014 và D08/D12/D13 theo contracts đã có; D02/D16 chỉ đóng khi có quyết định.
- [ ] Giữ các API baseline như lịch sử có nhãn rõ ràng, hoặc thay bằng index contract hiện hành.
- [ ] Chọn policy Firebase client config trong version control và ghi hướng dẫn setup reproducible; không commit admin credentials.

## Task DOC-02: Ledger nghiệm thu và regression handoff

**Files:** Modify `docs/sprints/sprint-09.md`, `sprint-09-acceptance.md`, `docs/planning/delivery.md`; backend handoff doc phải tìm path hiện có trước sửa.
**Interfaces:** Mỗi evidence entry gồm requirement/task, revision, môi trường, steps/command, expected/actual, artifacts, giới hạn, ngày.

- [ ] Giữ baseline 15 Local pass/2 Partial như kết quả báo cáo hiện có cho đến khi rerun có evidence mới.
- [ ] Đánh dấu D-09-01/02 đã sửa test; không mở lại chỉ vì Sprint08 còn ghi failures.
- [ ] Kiểm tra regression handoff có `npm run test:sprint09`; script đã tồn tại trong backend package.json, không tạo script trùng.
- [ ] Gắn kết quả từng plan và case còn fail/blocked; provider thật và local fake có cột riêng.
- [ ] Giữ deploy/release/shared migrations trong mục ngoài phạm vi lần này; không xóa blocker lịch sử.
- [ ] Chỉ đóng từng AC/NFR khi đủ evidence theo delivery gates, không đổi định nghĩa Done để làm xanh checklist.

## Task DOC-03: Kiểm tra tài liệu

**Files:** Tất cả Markdown thay đổi trong nhóm này; không sửa application code.

- [ ] Kiểm đủ dải FR/BR/NFR/AC, không bỏ ID.
- [ ] Kiểm relative links, tables, task IDs và trạng thái không mâu thuẫn.
- [ ] Đọc lại các plan từ góc nhìn người thực thi; mỗi task có đầu vào, output và cách verify.
- [ ] Review diff chỉ docs; ghi danh sách file thay đổi và các quyết định còn pending.

## Điều kiện hoàn tất

- [ ] SRS mô tả đúng current code và gap; reports lịch sử không bị biến thành kết quả mới.
- [ ] Sprint09 có evidence index đầy đủ và remaining work cụ thể.
- [ ] Không tuyên bố acceptance của môi trường/thiết bị/provider chưa chạy.

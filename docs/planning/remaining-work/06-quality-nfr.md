# Chất lượng UI, accessibility và NFR Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Trạng thái:** Planned — chỉ lập tài liệu ngày 2026-11-16; chưa triển khai hoặc chạy kiểm thử theo plan.
**Spec:** [SRS](../../SRS.md), [Sprint 09](../../sprints/sprint-09.md), [acceptance](../../sprints/sprint-09-acceptance.md).

**Goal:** Có kết quả đo thay cho nhận xét chung về hiệu năng và usability.
**Architecture:** Đo debug build bằng cùng cấu hình/dataset để so sánh nội bộ; ghi rõ overhead và không suy ra hiệu năng release. Fix chỉ sau khi có reproduction và baseline.
**Tech Stack:** Compose semantics, Android accessibility tools, Android profiler, unit/instrumented tests.
**Ưu tiên/phụ thuộc:** P2; chạy lại phần bị ảnh hưởng sau Home/FIX. NFR-001–005/012–026.


## Ràng buộc chung

- Chỉ thực thi sau yêu cầu triển khai riêng; tài liệu này không tự cấp quyền sửa code hay gửi email/push/AI thật.
- Không deploy, chạy migration trên database dùng chung, build/chạy release, ký APK hoặc phát hành.
- Giữ Kotlin/Compose/Material 3, MVVM, ServiceLocator/AppViewModelFactory, Retrofit/Gson, Room/WorkManager hiện có.
- Giữ username login; quota flashcards 2/ngày, practice chung 3/ngày, tối đa 15, reset 00:00 Asia/Ho_Chi_Minh.
- Không thêm SM-2, Social Login, bookmark/ghi chú; không tự động đánh dấu đã học hoặc tự bắt đầu AI generation.
- Evidence cũ là baseline, không thay cho kết quả trên revision thực thi. Ghi commit, môi trường, lệnh/bước, expected/actual và giới hạn.

## Review Focus

- Japanese text dài/Unicode và list lớn không làm vỡ layout.
- Font lớn, TalkBack và focus order vẫn sử dụng được.
- Keyboard/IME không che save/submit ở màn hẹp.
- Rotation/process recreation giữ draft, filter và review position.
- AI loading/slow network không khóa navigation hoặc gây click lặp.

## Task QA-01: Thiết bị và tiêu chí đo

**Files:** Modify `docs/architecture/api-decisions.md` D16; create `docs/planning/remaining-work/evidence/nfr.md`.
**Interfaces:** Metric record gồm build revision, variant, device/API, dataset, network, cold/warm, số lần chạy, raw samples, median/p95.

- [ ] Chọn emulator màn hẹp, thiết bị thật đại diện và màn lớn; ghi chính xác model/API, không giả định đã có.
- [ ] Đo baseline startup, list first content, cached detail, scroll jank, sync recovery, memory sau vòng study dài.
- [ ] Đề xuất ngưỡng pass từ baseline và yêu cầu sản phẩm; chốt D16 trước tuyên bố đạt NFR. Không tự đặt target như yêu cầu đã được duyệt.
- [ ] Không so debug có profiler với run không profiler; không dùng một lần đo để kết luận p95.

## Task QA-02: UI và accessibility

**Files:** Màn auth/profile/topics/learning/flashcards/practice/progress/activity và Home nếu được triển khai; Android UI tests tương ứng.
**Interfaces:** Giữ navigation/actions hiện có; sửa semantics và layout tại component sở hữu.

- [ ] Ma trận light/dark, font 1.0/1.5/2.0, portrait/landscape, IME open, Japanese long text.
- [ ] TalkBack: tên action, role/state, heading, error announcement, traversal order và focus sau dialog.
- [ ] Kiểm tra touch target, contrast, icon descriptions và calendar không chỉ truyền ý nghĩa qua màu.
- [ ] Reproduce và ghi ảnh/steps cho lỗi; chỉ thêm regression test có giá trị cho lỗi hành vi/layout thật.
- [ ] Chạy screen tests liên quan và lintDebug sau sửa; không rewrite UI architecture ngoài phạm vi.

## Task QA-03: Error consistency và load

**Files:** `core/network/util/SafeApiCall.kt`, repositories từng feature, backend error envelope; test paths xác định theo case lỗi.
**Interfaces:** Error matrix feature × offline/401/403/404/409/429/5xx/malformed response; giữ request IDs, không log token/PII.

- [ ] Đánh giá thông báo, retry, giữ draft và busy state cho mỗi lỗi; liên kết FR-105.
- [ ] Test payload malformed/null/large và cancel coroutine; UI không crash hoặc treo loading.
- [ ] Chạy tải trên local backend cô lập cho catalog/sync/reminder giả lập, không gửi external notification/AI.
- [ ] Đo latency/error rate/dedup/queue growth theo concurrency đã ghi; kiểm tra DB locks và thời gian scheduler.
- [ ] Khi phát hiện bottleneck, dùng profiling skill phù hợp trước sửa; ghi before/after cùng điều kiện.

## Điều kiện hoàn tất

- [ ] D16 có target được chấp thuận hoặc trạng thái chưa chốt rõ ràng.
- [ ] Mỗi NFR áp dụng có metric/test/source evidence hoặc gap cụ thể.
- [ ] Accessibility/layout có ma trận kết quả và defect triage; không claim release performance.

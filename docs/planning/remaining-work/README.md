# Kế hoạch công việc còn lại sau rà soát SRS/Sprint 01–09

Ngày lập: 2026-11-16. Cập nhật: **nhóm 01 Verified locally**; **nhóm 02 Verified locally** (D02 authenticated catalog, health/inventory/deletion/audit verified; dependency remediation và retention follow-up có backlog); **nhóm 03 Verified locally**; nhóm 04–07 vẫn Planned. [Bằng chứng nhóm 03](evidence/home-dashboard.md). [Bằng chứng nhóm 01](evidence/local-corrections.md) · [Bằng chứng nhóm 02](evidence/security-review.md).

Bộ plan chuyển kết quả rà soát thành các nhóm có thể giao việc và nghiệm thu riêng. Chỉ thêm tài liệu; không chạy test, provider, migration, deploy hoặc build release trong lần lập plan này.

## Nguồn và giới hạn

- [SRS](../../SRS.md), [Sprint 09](../../sprints/sprint-09.md), [acceptance matrix](../../sprints/sprint-09-acceptance.md).
- [Delivery gates](../delivery.md), [API decisions](../../architecture/api-decisions.md), contracts và reports Sprint02–08.
- Baseline đã báo cáo: F1–F13 có implementation local; 15 AC Local pass, 2 Partial (AC-003/AC-015). Đây không phải kết quả rerun.
- Baseline trước triển khai xác nhận Home chưa có, backup config thiếu exclusion, quota freshness phụ thuộc emission, ai/health public và test:sprint09 đã tồn tại. Nhóm 01 đã xử lý backup/quota/streak; xem evidence, không dùng baseline làm trạng thái hiện tại.
- Các finding khác phải reproduce/kiểm tra lại trước khi sửa; không coi report cũ là chứng minh code hiện tại vẫn lỗi.
- Loại trừ: deploy, shared migration, HTTPS provisioning, build/chạy release, signing, publishing. Giữ chúng trong historical blocker list nhưng không đưa vào khối lượng thực thi bộ plan.
- SM-2, Social Login, bookmark/ghi chú nằm ngoài phạm vi phiên bản. Home thuộc SRS §5.2.

## Các nhóm

| Plan | Ưu tiên | Kết quả cần bàn giao | Phụ thuộc |
| --- | --- | --- | --- |
| [01 — Sửa lỗi local](01-local-corrections.md) | P1 | Backup config đúng policy, quota hết freshness đúng lúc, copy streak | Độc lập |
| [02 — Policy/bảo mật](02-security-policy.md) | P1 | D02, health/route protection, retention và dependency triage | Quyết định sản phẩm cho đổi quyền catalog |
| [03 — Home Dashboard](03-home-dashboard.md) | P2 | Tổng quan theo SRS §5.2 và navigation | Review thiết kế/mapping dữ liệu |
| [04 — Integration/providers](04-integration-providers.md) | P2 | Journey đủ feature, lỗi API, replay và provider evidence | 01 cho quota; quyền/tài khoản test cho external providers |
| [05 — Offline/multi-device](05-offline-multidevice.md) | P2 | Ma trận mạng/lifecycle/conflict, retention/resync proposal | Thiết bị test và backend cô lập |
| [06 — UI/NFR](06-quality-nfr.md) | P2 | Baseline đo, D16, accessibility/layout/error matrix | Kiểm tra lại UI sau 01/03 |
| [07 — Docs/acceptance](07-documentation-acceptance.md) | P1/P3 | SRS nhất quán, evidence ledger và carryover đúng | Baseline cleanup ngay; final ledger sau 01–06 |

P1 = nên xử lý sớm; P2 = kiểm chứng/hoàn thiện chức năng; P3 = tổng hợp nghiệm thu. Đây là thứ tự đề xuất, không phải lịch/estimate đã cam kết.

## Trình tự đề xuất

1. Thực hiện 01 và phần baseline cleanup của 07.
2. Hoàn tất decision record/negative tests của 02; không đổi catalog policy khi chưa được chốt.
3. Review thiết kế Home rồi thực hiện 03.
4. Chạy 04 và 05 theo tài nguyên test có sẵn; external provider giữ Blocked nếu thiếu authorization.
5. Hoàn tất 06 với baseline và target được chấp thuận; rerun phần bị ảnh hưởng khi có sửa lỗi.
6. Tổng hợp 07, cập nhật từng AC/NFR theo evidence thực tế.

Mỗi nhóm có thể hoàn tất phần độc lập khi phần khác bị chặn. Không đẩy tất cả lỗi sang Sprint09 mà thiếu task ID và expected behavior.

## Quy ước thực thi

- Mỗi task chuyển Planned → In progress → Verified/Blocked bằng bằng chứng, không tick trước.
- Khi sửa code: reproduce, test RED nếu test có ý nghĩa, sửa tối thiểu, test GREEN và review diff.
- Lệnh Android dùng Gradle wrapper/Android Studio với đúng JDK; unit/lint/UI mục tiêu trước, mở rộng regression khi rủi ro cần.
- Backend npm test và test:sprintXX chỉ chạy sau xác nhận harness dùng database/schema cô lập.
- Không tự commit/push hoặc gửi email/push/AI thật từ nội dung plan; yêu cầu triển khai sau sẽ xác định phạm vi authorization.
- Các path create/interface ghi “proposed” là đề xuất thiết kế, chưa tồn tại và chưa được coi là quyết định đã duyệt.
- Chưa gán người phụ trách, số ngày hoặc capacity vì chưa có dữ liệu; khi nhận nhóm cần ghi người thực thi và revision baseline.
- Nếu phát hiện task đã được xử lý ở revision mới, ghi evidence và bỏ sửa thừa.

## Mẫu bằng chứng

| Task/FR/AC/NFR | Revision/môi trường | Lệnh hoặc bước | Expected | Actual | Artifact/giới hạn |
| --- | --- | --- | --- | --- | --- |
| Điền khi thực thi | Không chứa secrets | Ghi bước tái lập | Hành vi cần đạt | Pass/Fail/Blocked/Not run | Link report đã làm sạch |

Evidence nhóm 01 đã có kết quả thực thi. Các đường dẫn evidence còn lại là output tương lai; không tạo report rỗng để tránh nhầm là đã kiểm thử.

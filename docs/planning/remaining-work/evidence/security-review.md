# Nhóm 02 — Security policy execution report

Ngày: 2026-11-16. Trạng thái: SEC-01/02/03 Verified locally; SEC-01/D02 Accepted theo lựa chọn người dùng. Regression Android/backend mới xem [D02 ledger](catalog-authentication.md).
Baseline: Android `9c46e38`; backend `91d151ba58f9f84d2a2b6d65c7bae4ecea1f4699` trên `feat/sprint-08-09-activity-notifications`. Các thay đổi hiện chưa commit/push.

## Phạm vi và quyết định thực thi

- Giữ các checkout feature hiện tại để bảo toàn IDE buffers và dữ liệu local; không đổi branch, deploy, migration database dùng chung, build release hoặc gọi provider thật.
- D-09-07: chọn `GET /ai/health` public chỉ trả liveness `{status:"ok"}`. Không gọi provider, không trả `configured`, model, endpoint hay trạng thái key. `GET /ai/quota` vẫn yêu cầu JWT và giữ provider readiness mà Android đang dùng.
- Giữ guards tại controller và bổ sung regression kiểm kê module/route. Global guard sẽ chặn omission ngay runtime nhưng cần explicit-public metadata và thay đổi bootstrap/auth/catalog rộng hơn; inventory ít thay đổi behavior nhưng chỉ phát hiện omission khi test chạy. Chọn inventory trong increment này; không gọi đây là runtime default-deny.
- D02 được người dùng chọn rõ ngày 2026-11-16: bắt buộc đăng nhập đọc catalog theo SRS/BR-001. [Ledger thay đổi và regression](catalog-authentication.md).
- Giữ xóa tài khoản fail-closed nếu Cloudinary cleanup thất bại: không trả 204, không xóa DB trước, người dùng có thể retry. Không tạo asynchronous cleanup queue khi chưa có contract/retention decision.

## SEC-01 — Authorization matrix và D02

[Ma trận đầy đủ 82 route / 20 controller](authorization-matrix.md): 13 public, 63 authenticated/owner hoặc catalog, 6 Admin-only.

Hai lựa chọn đã xem xét:

| Lựa chọn | Ảnh hưởng backend | Ảnh hưởng Android/offline | Trạng thái |
| --- | --- | --- | --- |
| Bắt buộc đăng nhập theo BR-001 | JWT trên levels/topics/catalog/vocabulary/grammar/entity-sync; anonymous và revoked sessions phải 401, kể cả conditional GET | Bỏ No-Authentication trên catalog; interceptor và worker phải gắn session/account; cache đã tải vẫn dùng offline trong session; không gửi token qua header cache hoặc phục vụ cache sau logout trái policy | Đã chọn — Accepted 2026-11-16 |
| Giữ public catalog | Giữ public read hiện tại; ghi ngoại lệ BR-001 có chủ đích; write vẫn Admin-only | Giữ public URL/ETag cache; UI vẫn không có guest entry; dữ liệu cá nhân luôn account-scoped | Không chọn |

D02 đổi contract đồng thời backend guards, Android header/cache/sync và per-sprint fixtures; guest, revoked và invalid sessions không nhận catalog hoặc 304. Offline dùng cache chỉ khi còn local login; remote revocation được phát hiện khi online. Lựa chọn public catalog trong bảng trên không được chọn.
OQ-015/D02 đã Accepted. D16, shared acceptance và các AC không tự được đóng bởi nhóm này.

## SEC-02 — Code và regression

Backend files:
- `src/modules/ai/ai.controller.ts`: public liveness tối giản, không đổi quota.
- `src/modules/ai/ai.controller.spec.ts`: public health không lộ readiness và không gọi provider; quota giữ response contract.
- `src/security/route-access.spec.ts`: duyệt imports của AppModule (cả dynamic modules), kiểm exact public exceptions và JWT/Admin metadata; không khởi tạo DB/provider.
- `test/sprint09.ownership.integration.ts`: public health, quota user/Admin, authenticated catalog, toàn bộ 69 guarded-route anonymous cases, ownership, token và deletion.

Unit inventory có negative probes cho controller mới vô tình public và route Admin bị mất RolesGuard. D02 authenticated-only đã được chấp thuận; inventory exact public exceptions còn 13 route.
Không có global JWT guard; test phải chạy trong regression. Swagger được đăng ký ngoài controller inventory, cần có exposure policy riêng nếu thay đổi môi trường.

## SEC-03 — Retention/deletion inventory

Bảng này ghi implementation hiện có, không tự ban hành thời hạn lưu mới.

| Dữ liệu | Nơi lưu | Khi logout/hết hạn | Khi xóa account | Thời hạn/việc còn cần chốt |
| --- | --- | --- | --- | --- |
| Profile, settings, avatar URL | users/PostgreSQL | Profile vẫn tồn tại; settings không xóa khi logout | Xóa user sau cleanup avatar thành công | Theo vòng đời account; chính sách backup/retention cần chủ sản phẩm/DB xác nhận |
| Password hash | user_auth | Không xóa khi logout | FK CASCADE | Không lưu plain password; cần retention backup |
| Refresh hash, IP, user agent | sessions | Logout/revocation vô hiệu session; expiry chặn sử dụng | FK CASCADE và device bindings | Expiry không đồng nghĩa physical purge; chưa có thời hạn dọn expired sessions được kiểm chứng |
| Email/reset challenge hash, attempt count | verification_tokens | Code expiring/used bị từ chối, không có purge job được chứng minh | FK CASCADE | Email verification 15 phút là validity, không phải physical retention |
| Progress + replay receipt | learning_progress/progress_receipts | Giữ dữ liệu; requests cần valid session | FK CASCADE | Receipt hiện theo vòng đời account; compaction phải có resync contract |
| Folders/sets/cards + heads/receipts | flashcard_* | Giữ bản canonical/tombstone/receipt | Cascade owner→folder→set→card; heads/receipts CASCADE | Không xóa receipts/tombstones trước full-resync design |
| AI ledger/results | ai_generation_requests | Reservations hết lease xử lý khi lock; quota theo ngày VN | FK CASCADE | Lease 2 phút không phải thời hạn xóa result/receipt |
| Practice/exercises/submissions/completions | practice_* | Giữ kết quả và replay state | Owner/session FK CASCADE | Theo vòng đời account; chưa định nghĩa compaction |
| Activity/streak/calendar | learning_activity_events, user_learning_* | Giữ lịch sử | FK CASCADE | Lịch sử/late events cần retention decision, không tự cắt |
| Device FCM token/binding | notification_devices | Session/logout/rebind/invalid token kiểm soát hiệu lực | User/session CASCADE | Cần phân biệt token mất hiệu lực và physical cleanup |
| Reminder dedup | reminder_attempts | Giữ key owner/local-day | FK CASCADE | Không purge trước khi thống nhất dedup window |
| Android token/user/push state | private SharedPreferences | Credentials clear/suppress theo logout/session loss | Clear credentials/push state | Toàn sharedpref domain loại khỏi backup từ nhóm 01 |
| Android catalog cache | Room shared catalog projection | Có thể giữ payload nhưng không phục vụ sau logout; current account/session gate | Không phải dữ liệu riêng user | Không lưu credential; eviction thuộc nhóm 05 |
| Android private drafts/outbox/cache | Room theo account/environment | Có thể vẫn lưu local để phục hồi; logout dừng worker/hiển thị theo account | Account purge qua AccountRepositoryImpl/ServiceLocator | Không xóa pending/draft bằng generic cache eviction; toàn database loại khỏi backup |
| Logs | stdout/Nest logger/provider logs | Ngoài DB cascade | Không tự xóa khi xóa user | Chưa có TTL/central-log policy trong code; xem SEC-LOG-01 |
| DB/media backup | Hạ tầng ngoài repo | Không thay đổi bởi token expiry | Không tự purge bản backup từ DELETE account | Chưa kiểm chứng retention/restore suppression; không tuyên bố đã xóa ở mọi backup |

### Deletion fixture

Local PostgreSQL schema ngẫu nhiên, kiểm local host, schema-only search_path, migrationsRun=false, cleanup DROP SCHEMA trong finally.
Không dùng dữ liệu public/shared. SMTP/Cloudinary/FCM/AI đều được thay bằng doubles cho các hành động fixture.

- Fake Cloudinary cleanup lỗi: DELETE account trả 400; user/session vẫn dùng được, card riêng vẫn còn, không trả success giả.
- Retry cleanup thành công: DELETE 204; token user bị từ chối.
- Metadata inventory tìm 17 FK trực tiếp đến users; kiểm không còn row owner sau deletion và card con biến mất.
- Có ít nhất 8 bảng owner đã được populate trước deletion; các bảng chưa populate được kiểm metadata/count, không tuyên bố đã test non-empty cascade riêng cho mọi bảng.
- Kiểm child flashcard trước/sau tránh kết luận dựa trên bảng trống.
- Đây là code/schema local evidence; chưa chứng minh retention ở backup, log hay provider thật.

## Dependency audit và triage

Audit read-only ngày 2026-11-16, không npm install/update/audit fix và không đổi lockfile.

| Lệnh | Kết quả |
| --- | --- |
| npm audit --json | 69 package findings: 3 critical, 24 high, 32 moderate, 10 low |
| npm audit --omit=dev --json | 60 package findings: 2 critical, 23 high, 31 moderate, 4 low |
| npm ls --omit=dev ... --all | Xác nhận installed versions và đường phụ thuộc bên dưới |

Exit 1 của audit là có findings, không phải lỗi test. Đây là affected-package aggregates, không phải 60 exploit đã được chứng minh.
[Snapshot chi tiết](dependency-audit.json) lưu metadata, range, advisory và proposed fix từ registry.
Production tree vẫn gồm build/type tools vì @nestjs/cli và @types/nodemailer đang nằm trong dependencies.

| Nhóm/package thực tế | Đường dùng và đánh giá | Task |
| --- | --- | --- |
| proxy-addr 2.0.7 — critical registry | Express 5.1.0; main.ts không cấu hình trust proxy. Advisory cần trust subnet sai dạng mapped IPv6; chưa chứng minh trigger với config hiện tại. Không được suy ra JWT bypass | DEP-02 |
| fast-xml-parser 5.2.5 — critical registry | Qua @types/nodemailer→AWS SDK. Không thấy runtime SES/XML route trong app; Firebase Storage có bản riêng 5.12.1 ngoài critical range này. Installed không đồng nghĩa input app đi vào parser dễ tổn thương | DEP-03 |
| body-parser 2.2.0 / Express 5.1.0 / Nest platform 11.1.7 | Runtime HTTP parser đứng trước controller auth, có untrusted request bodies; rủi ro DoS cần ưu tiên upgrade/test body/compression limits. Chưa chạy exploit phá process | DEP-01 |
| multer 2.0.2 | Avatar upload sau JWT, file-size 10MiB. Không có async fileFilter nên advisory async-filter riêng không đủ điều kiện; các multipart/cleanup advisories khác vẫn cần upgrade/test | DEP-01 |
| path-to-regexp 8.3.0 | Runtime router; app dùng route tĩnh và params đơn, không nhận route pattern từ user. Cần xem các advisory pattern cụ thể, không gọi tất cả path routing đã khai thác được | DEP-01 |
| jws 3.2.2 | Qua jsonwebtoken 9.0.2/@nestjs/jwt; advisory loại trừ jsonwebtoken dùng jws.verify. Không tìm direct createVerify trong app. Firebase path dùng jws 4.0.1 | DEP-02 |
| nodemailer 7.0.10 | SMTP runtime; recipient đến từ DTO, không expose raw/jsonTransport/SES options cho client. Các parser/SMTP advisories vẫn cần review và major migration riêng; không tự kết luận an toàn từ IsEmail | DEP-04 |
| validator 13.15.15 / class-validator 0.14.2 | Runtime DTO validation; không thấy IsUrl trong app; cần map từng advisory đến decorator thực dùng, test email/length/bad input | DEP-04 |
| axios 1.13.1 / form-data | Direct dependency nhưng không thấy app source import axios; Gemini dùng fetch. Kiểm runtime/transitive consumers trước remove/upgrade | DEP-05 |
| lodash, Nest core/config/swagger | Framework runtime/build dependency, không thấy app gọi template/unset/omit/pullAt bằng user input. Framework gadget applicability chưa chứng minh | DEP-05 |
| brace-expansion, minimatch, picomatch, browserslist, fast-uri, js-yaml, serialize-javascript, glob / Nest CLI | Có trong production install tree qua tooling; app không expose YAML/glob/template parsing API. Phân tách build-time với server runtime trước đánh giá và prune | DEP-03 |
| Các package medium/low/transitive còn lại trong snapshot | Registry evidence có, chưa reproduce riêng từng advisory; xử lý theo parent upgrade và rerun audit; không đóng giả là false positive | DEP-05 |

Nguồn advisory dùng để đối chiếu điều kiện:
- [proxy-addr GHSA-jqcg-44mw-7w3h](https://github.com/advisories/GHSA-jqcg-44mw-7w3h): trust-subnet precondition và patch 2.0.8.
- [fast-xml-parser GHSA-m7jm-9gc2-mpf2](https://github.com/advisories/GHSA-m7jm-9gc2-mpf2): affected 5.0.0–<5.3.5; cần untrusted XML đi vào parser/output.
- [multer GHSA-q7qp-rjg7-q26m](https://github.com/advisories/GHSA-q7qp-rjg7-q26m): async fileFilter precondition.
- [jws GHSA-869p-cjfg-cm3x](https://github.com/advisories/GHSA-869p-cjfg-cm3x): excludes jsonwebtoken verify path.

### Remediation backlog, chưa thực hiện trong nhóm audit

| ID | Ưu tiên | Việc cần làm | Điều kiện kiểm chứng |
| --- | --- | --- | --- |
| DEP-01 | P1 | Targeted compatible upgrade Nest/Express/body-parser/multer/path-to-regexp; đọc từng advisory hiện hành trước chọn version | Valid/oversize/malformed/compressed JSON, avatar aborted/multipart limits, auth-before-upload; backend build/unit + Sprint02/04/06/09 |
| DEP-02 | P1 | Patch proxy-addr/jws chain qua supported parent/lockfile; không dùng blanket major fix | JWT bad signature/replay, static secret, X-Forwarded-For và trust-proxy configs rõ ràng |
| DEP-03 | P2 | Chuyển CLI/types/tools sang đúng install scope và bỏ vulnerable XML copy khỏi runtime image nếu không cần | npm ci/build đúng workflow; npm ls --omit=dev xác nhận scope; SMTP/Firebase startup regressions |
| DEP-04 | P1 | Plan Nodemailer major migration và validator patch, giữ transport contract | Recipient parsing/CRLF/bounds, SMTP error/timeout và existing email tests; không gửi mail thật nếu chưa được phép |
| DEP-05 | P2 | Triage remaining advisory paths, remove unused axios nếu xác nhận, cập nhật parent dependencies | Source/consumer trace, focused tests, full audit before/after; không chỉ giảm count bằng suppression |
| SEC-LOG-01 | P1 | Redact reset-email success log đang có recipient email; thay raw Cloudinary error bằng metadata allowlist | Test capture log không chứa email/code/token/provider credential; không che failure signal |
| SEC-RET-01 | Decision-required | Chốt TTL logs/expired auth rows, backup retention và quy trình restore không hồi sinh account đã xóa | Document approved durations/owner, isolated recovery rehearsal; không tự purge receipts |
| SEC-RET-02 | P2 | Đánh giá retry khi provider đã xóa ảnh nhưng DB delete lỗi/đồng thời avatar replace | Transaction/provider fault fixture; decide deletion state machine trước sửa semantics |

SEC-LOG-01 là source finding có căn cứ (EmailService.sendPasswordResetCode và CloudinaryService.deleteImage), chưa sửa trong task audit. Không có secret/account thật trong report.

## Verification ledger

- Baseline backend build + Jest: 16 suites / 95 tests PASS.
- Public health regression RED: expected liveness-only, actual configured=false/status=unavailable. Sau sửa: health + quota tests GREEN.
- Route inventory self-check: negative controller/Admin probes reject missing guards. Một lần test setup dùng sai lower bound 21 controller thay vì 20; sửa count theo inventory thực, không sửa product để làm xanh test.
- Backend full Jest trước final review: 18 suites / 100 tests PASS. Sau bổ sung review regression: 18 suites / 101 tests PASS trên latest source.
- Backend build và expanded npm run test:sprint09: PASS, real local PostgreSQL/JWT, public health, quota compatibility, catalog current policy, 47 anonymous guarded-route cases, cross-user/Admin/replay/deletion failure+retry.
- Audit commands intentionally exit 1 with findings; dependency remediation chưa được thực hiện.
- Fresh independent read-only review: phát hiện inventory có thể chấp nhận roles admin+user. Thêm probe đã FAIL (RED), yêu cầu exact Admin-only set rồi chạy full build/Jest/integration GREEN. Doc status finding cũng đã cập nhật; không có deferred minor từ review.
- Trước quyết định D02: backend build + 101 unit tests + expanded Sprint09 fixture PASS, exit 0; diff checks PASS. Các kết quả này là baseline trước thay đổi Android catalog; kết quả mới xem [D02 ledger](catalog-authentication.md).
- D02 có thay đổi Android application code; package.json/package-lock.json không đổi.
- D02 đã chọn authenticated catalog; [ledger](catalog-authentication.md) theo dõi kiểm chứng mới.

# TN Launcher — Agent Registry và phối hợp dự án

Cập nhật: **18/10/2026**, revision **REGISTRY-2026-10-18.3**, trạng thái nội dung **IN_REVIEW**. Phiên OEM có baseline `main/0eebfc8f0b4f886673f191259dd87f32c06c6e81`; worktree/index sạch trước increment. Owner sau đó yêu cầu chạy UA, commit/push GitHub và tiếp tục trực tiếp thiết kế Figma mới. Đây là scope Codex được giao riêng, không thay toàn bộ role Notion hoặc phê duyệt handoff thiết kế cũ. Không mở increment code Android/tích hợp UI, cài TN, đổi Home, test xe mới hoặc đồng bộ GitLab. Commit local OEM2f8ceaa và kết quả publication tiếp theo đối chiếu Git; các kiểm tra remote17/10 bên dưới vẫn là lịch sử.

Registry này là nguồn chuẩn duy nhất trong repo về vai trò, ownership và bàn giao Agent. [STATUS](STATUS.md) giữ trạng thái phiên; [EXECPLAN](EXECPLAN.md) giữ kế hoạch sống; [parity matrix](../research/feature-parity-matrix.md) giữ trạng thái từng chức năng. Không sao chép các bảng đó thành backlog mới ở đây. Các sub-agent tạm của một phiên Codex là công cụ nội bộ, không phải Agent dự án mới và không thay người chịu trách nhiệm trong bảng dưới.

## Nguồn, thẩm quyền và revision

| ID | Nguồn đã đối chiếu | Giới hạn và cách dùng |
| --- | --- | --- |
| OWNER-17 | Master prompt “Multi-Agent Memory, Role Registry & Project Coordination”, Owner giao trực tiếp ngày 17/10/2026 | Nguồn quyết định vai trò hiện hành, Claude AI 2 PAUSED, QA Claude Opus 4.8, Notion AI mặc định và quyền thiết kế độc lập. Cho phép tạo registry sớm; không cấp quyền code hoặc publish. |
| OWNER-PUBLISH-17 | Yêu cầu tiếp theo ngày 17/10/2026: “Kiểm tra lại repo Github và đồng bộ lại pull hoặc commit push” | Cho phép kiểm tra, commit và push bộ tài liệu đã bàn giao lên GitHub hiện hành. Thay giới hạn publication của OWNER-17 cho hành động này; không cấp quyền đồng bộ GitLab, sửa cloud notes, tích hợp UI hoặc test xe. |
| OWNER-OEM-18 | Owner chọn cả camera/radar và CAN, sau đó yêu cầu IMPLEMENT kế hoạch ngày18/10/2026; xác nhận xe đứng yên, có người trực tiếp giám sát | Cho phép nghiên cứu tĩnh và quan sát/chẩn đoán OEM qua scrcpy/ADB riêng. Không cài TN, đổi Home, thử số lùi, IPC vendor, thay cấu hình nhà máy, quyền nâng cao hoặc commit/push. Thông tin kết nối và mã truy cập không ghi vào repo. |
| OWNER-ADB-18 | Owner yêu cầu tiếp theo thực hiện đúng HUONG-DAN-TN-FAST.md để kết nối scrcpy, và yêu cầu mở Terminal chạy tự động | Cho phép riêng hai lệnh bật ADB tạm/restart adbd trong guide; thay giới hạn không-setprop của kế hoạch ban đầu cho đúng mục đích kết nối này. Không root/SELinux/factory/Home/package changes. |
| OWNER-CAMBTN-18 | Owner yêu cầu tự động chạm nút nổi hình xe và cung cấp ảnh nhận dạng | Cho phép thao tác UI nút đó qua kết nối hiện có, không phát broadcast/CAN giả hoặc thử số lùi. Quan sát chưa thấy full-screen, không coi UI request là proof thành công. |
| OWNER-REVERSE-18 | Owner yêu cầu cùng kiểm tra camera360 tự chuyển khi lùi; xác nhận lại xe đỗ an toàn/có người kiểm soát/khu vực trống, rồi báo đã chọn R và đã đỗ/tự về Home | Thay giới hạn không thử số lùi của giai đoạn trước chỉ cho baseline OEM này. Người tại xe thao tác số; Codex chỉ đọc màn hình/activity/window. Không cài TN, đổi Home, điều khiển xe từ xa hoặc inject gear/CAN/IPC. |
| OWNER-DESIGN-PUBLISH-18 | Owner yêu cầu chạy Understand Anything, commit/push toàn bộ lên GitHub, tiếp tục thiết kế file4VroI0Z4ZyPNYu50tXDl6u trên Figma Desktop; chỉ định fonts/icon và Turn_Signal.wav | Cho phép publication có review và chỉnh trực tiếp đúng file Figma. Không tự giao Android UI/audio implementation, đồng bộ GitLab/cloud notes hoặc test xe mới. Yêu cầu đồng bộ âm thanh là contract triển khai sau, chưa là runtime verification. |
| GIT-17 | [GitHub commit42564cb](https://github.com/huuthitv95/TN-Launcher/commit/42564cbe10bcfaadfa1b8dce505acde2ed549b33), [GitLab cùng commit](https://gitlab.com/tn-fast/TN-Launcher/-/commit/42564cbe10bcfaadfa1b8dce505acde2ed549b33) | Đã đọc refs qua Git/API hai connector và local HEAD ngày 17/10. Remote-tracking `task0-gitlab/main` cục bộ có thể cũ; không dùng nó thay kiểm tra live. Không push, merge hoặc đổi mirror. |
| CODE-425 | [Kiến trúc](../architecture/architecture.md), [contract vendor](../architecture/wincavn-integration.md), [compatibility report](../testing/compatibility-report.md), [design system](../design/design-system.md), [icon mapping](../design/icon-mapping.md) tại 42564cb | Implementation và bằng chứng kiểm thử lịch sử; không phải test mới hoặc proof tương thích xe. |
| AI1-HISTORY | [Claude AI 1 — conversation lịch sử](https://claude.ai/share/c3960464-778b-4a0f-a7e2-a7a085f0dad3) | URL đã thử đọc nhưng công cụ không truy cập được. Nội dung vai trò/công việc dưới đây là Owner bàn giao, không tuyên bố đã đọc phiên Claude mới nhất. |
| AI2-HISTORY | [Claude AI 2 — conversation lịch sử](https://claude.ai/share/fa6e44b7-6c7a-4119-9b11-6796440f7c44) | Cùng giới hạn truy cập; 14 Work Items chưa được nhập từ báo cáo đầy đủ. PAUSED không đồng nghĩa thất bại/hủy Agent. |
| QA-OWNER | `GITLAB_QA_BASELINE_REPORT`, verdict `PASS_WITH_FINDINGS`, theo OWNER-17 | Chưa có bản báo cáo đầy đủ/link artifact được cung cấp. Tìm issue theo tên báo cáo không có kết quả; API MR/pipeline của project đều trả danh sách rỗng ngày 17/10. Không suy ra đã đọc nội dung hội thoại QA trên GitLab. |
| NOTION-HUB | [Knowledge Hub](https://app.notion.com/p/3fc8a3a2b4618123a32ef9740b3add0d), edited 2026-10-17T04:54:27.228Z | Vai trò, Owner approval, luồng Notion AI mặc định; đọc thực qua connector. |
| NOTION-V1 | [UI Design Production](https://app.notion.com/p/3fc8a3a2b461810396b0df93136d9459), edited 2026-10-17T05:18:36.270Z; [Owner Review 07](https://app.notion.com/p/2813402f7ce14f48b85e54763de73b08), edited 05:18:11.848Z | Có đầu ra v1 và tự ghi Owner chưa duyệt; không READY_FOR_ANDROID_INTEGRATION. Last-edited là revision nguồn, không là approval. |
| NOTION-TASK | [UI Master Prompt](https://app.notion.com/p/3fc8a3a2b461815d889ff59623af7781), edited 2026-10-17T04:54:09.765Z | Nhiệm vụ thiết kế, không phải bằng chứng đã hoàn tất frame/component hoặc nghiệm thu. |
| MEMORY-15 | Basic Memory Cloud project `TN Launcher`: [Agent Handoff Start Here](memory://my-notes-9c8e0e01/tn-launcher/00-start-here/tn-launcher-agent-handoff-start-here), revision checksum `67d93bbba20f2a3854fe7404de88a50e19024d7ec51b2b5c0f4872428d6bc6b0`; [Next Claude Agent Work Plan](memory://my-notes-9c8e0e01/tn-launcher/05-agent-context/tn-launcher-next-claude-agent-work-plan), checksum `50b2ab17768b26e211487aa45bce2d485faca2c755203ec4fd8fef3a7c2e8ede` | Đã đọc note đầy đủ; frontmatter as_of 2026-10-15/base 6a02097. Đây là lịch sử, không phải baseline hoặc lệnh triển khai hiện hành. Quyết định OWNER-17 cho thiết kế Notion độc lập thay giới hạn thiết kế cũ; Android integration vẫn cần duyệt riêng. Cloud không tự đồng bộ với repo; lượt này chỉ đọc. |

Không tự sửa Notion, Basic Memory hoặc ghi vào cuộc hội thoại Agent khác trong tác vụ này. Handoff qua artifact chính thức, có revision và Owner chuyển giao; việc connector có công cụ ghi không tự cấp quyền gửi thông điệp hoặc thay đổi nguồn ngoài.

## CURRENT STATE — danh sách và ownership hiện hành

Trạng thái dưới đây gắn nguồn; không suy trạng thái phiên live chỉ từ tên Agent. `ASSIGNED` ở AI1 biểu diễn vai trò điều phối Owner đã giao, không tự giao thêm backlog. Codex được Owner giao increment nghiên cứu OWNER-OEM-18; code Android/tích hợp UI vẫn chưa được giao trong scope này.

| Agent | Role | Responsibilities | Completed Work | Current Assignment | Current Status | Next Proposed Work | Dependencies | Input Sources | Output Artifacts | Reviewer | Approval Authority |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Claude AI 1 | Technical Lead & Multi-Agent Coordinator | Kiến trúc, dependency/backlog, tổng hợp parity, đối soát findings và kế hoạch | Review W1–W8, backlog T0–T11, baseline/core/provenance, phương án A/B/C — Owner báo cáo | Điều phối và đối soát các báo cáo hiện có | ASSIGNED; tiến độ phiên mới chưa xác minh; lịch sử WAITING_AGENT_REPORTS | Tiếp nhận bản đầy đủ AI2/QA, loại trùng, chốt findings còn mở rồi trình Owner | Báo cáo có revision từ AI2/QA, handoff Notion, quyết định Owner | OWNER-17, AI1-HISTORY, artifact repo/QA/Notion | Backlog đã đối soát, kế hoạch và báo cáo tổng hợp | AI2 phản biện OEM; QA phản biện chất lượng | Project Owner |
| Claude AI 2 | Independent OEM Reverse Engineering Reviewer | Phản biện độc lập camera/CAN/MCU/RemoteTools/media/BT/DSP/radio/nav; kiểm Evidence ID/provenance | Review W1–W8, Limo/type1507776/Activity6606, phản biện CAM-02, đề xuất14 Work Items — Owner báo cáo | Hoàn thiện báo cáo OEM đang dang dở khi tiếp tục phiên | PAUSED — hết quota theo OWNER-17; không coi là thất bại | Bàn giao đầy đủ14 Work Items và phần thiếu bằng chứng cho AI1; đối chiếu findings mới | Phiên được tiếp tục, artifact đúng hash/variant, AI1 cung cấp scope | OWNER-17, AI2-HISTORY, contract/parity | Independent OEM review, Evidence IDs, Work Items và giới hạn | AI1 đối soát; QA kiểm integrity | Project Owner |
| Codex Windows | Android Implementation & Reverse Engineering Engineer | Kotlin/Views/API29, APK research, architecture/parity, tests/emulator, UA, Figma và tích hợp UI khi được giao | Core/Settings/theme/wallpaper/folders/widget host/persistence, W1–W8, registry, OEM18-OEM-R01 — chỉ OEM baseline | OWNER-DESIGN-PUBLISH-18: chạy UA, bàn giao GitHub rồi tiếp tục thiết kế đúng Figma mới | OEM REPORTED; UA BLOCKED_STRICT_PARSER_API; Figma DESIGN_BLOCKED_CONNECTION, chưa xác nhận design mutation | Hoàn tất Git delivery theo authorization; sau khi kết nối Figma, inspect đúng file/tokens rồi tiếp tục thiết kế; không tự mở code/xe | Figma reauthentication/edit access, font khả dụng thực và inventory hiện có; review độc lập | OWNER-DESIGN-PUBLISH-18, design-system, registry/handoff và file Figma được Owner chọn | Canonical docs/graph, Git evidence; thiết kế Figma chỉ khi ghi/readback được; không APK mới | Owner review design; AI1 + QA cho repo; AI2 cho phần OEM | Project Owner |
| Claude Opus 4.8 — GitLab QA & CI Gatekeeper | Independent QA / CI / Code Review | Review source/MR, regression/security/safety, CI/test gates, integrity/parity | GITLAB_QA_BASELINE_REPORT: PASS_WITH_FINDINGS — Owner báo cáo | Báo cáo QA hiện có chờ đối soát; chưa có MR/pipeline để gate trong snapshot | REPORTED; CI NOT_CONFIGURED / NOT_RUN theo báo cáo, không có pipeline PASS | Chuyển bản đầy đủ cho AI1; review kế hoạch CI, tài liệu/code/MR khi được chọn | Artifact đúng SHA, AI1 đề xuất gate, Owner chọn workflow | OWNER-17, QA-OWNER, GitLab refs/MR/pipeline read-only | QA findings, review verdict, CI/test-gate proposals | AI1 đối soát; Codex cung cấp bằng chứng sửa | Project Owner |
| Notion AI mặc định | UI/UX Designer & Design Documentation Specialist | Visual system, screen/state/responsive Light/Dark, assets/fonts/icons, components và Android Views handoff | Bộ UI v1 có overview/specs,34 frame links và tài liệu component/token/asset/action — NOTION-V1; Figma chưa được kiểm trực tiếp trong lượt này | Thiết kế độc lập và chuyển v1 cho Owner Review; tiếp tục hoàn thiện phần còn thiếu | IN_REVIEW — UI_DESIGN_V1_OWNER_REVIEW; chưa DESIGN_COMPLETE, chưa READY_FOR_ANDROID_INTEGRATION | Nhận Owner feedback; hoàn thiện font/metrics, component anatomy/binding/prototype, states và inventory; bàn giao revision chốt | Owner chốt design, inventory app/provider và kích thước/insets được xác minh; không chờ OEM review để thiết kế | NOTION-HUB, NOTION-TASK, NOTION-V1, reference Figma và design/assets repo | 10 nhóm UI deliverables, Figma links + Android Views handoff có revision | Owner review thiết kế; Codex đánh giá khả năng tích hợp; AI1 điều phối | Project Owner |
| Project Owner — con người, không phải AI Agent | Final Decision Authority | Định hướng, giao scope, duyệt design/development/workflow/device/release | MASTER PROMPT chốt vai trò và thiết kế Notion độc lập | Review bộ nhớ này và UI v1; quyết định công việc tiếp theo | IN_REVIEW — các artifact đang chờ quyết định; không suy tiến độ cá nhân | Duyệt hoặc yêu cầu sửa registry/UI handoff; chọn increment và workflow | Kế hoạch/bằng chứng/review từ các Agent | Tất cả handoff có nguồn | Quyết định có phạm vi, ngày và revision | Các Agent tư vấn; không tự phê duyệt thay Owner | Project Owner |

QA hiện hành là **Claude Opus 4.8** theo cấu hình Owner bàn giao. Cấu hình Fable 5.1 cũ chỉ là lịch sử, không phải Agent QA hiện tại. Notion dùng AI mặc định, **không Custom Agent riêng**. Codex không tự nhận quyền Technical Lead hoặc quyền phê duyệt của Owner từ việc thực hiện tác vụ tài liệu này.

## HISTORICAL REPORTS — bằng chứng đã bàn giao, không phải test mới

- Android ở baseline 42564cb: Kotlin/Android Views, ViewModel/StateFlow, Preferences DataStore, API29; Home/Dock/Drawer/Search/Settings/theme/wallpaper/folders/widget host/persistence có trong source. Báo cáo lịch sử:25 unit PASS, lint 0 errors / 51 warnings,33 instrumentation và9 E2E mỗi profile 1280×720/160dpi,1920×1080/320dpi. Không chạy lại Android trong tác vụ registry.
- W1–W8 tại 42564cb:34 APK samples,88 evidence entries; graph baseline321 nodes/449 edges/68 indexed files. Đây là mức STATIC_VERIFIED cho vendor; không DEVICE_VERIFIED/AUTOMOTIVE_VERIFIED. Graph sau tác vụ registry phải kiểm lại theo file thật, không dùng các số baseline này như số hiện tại.
- Git recovery16/10: snapshot đầu GitHub/GitLab ở6a02097; local42564cb được giữ nguyên. Sau lệnh recovery Owner chạy, GitHub main và nhánh recovery ở 42564cb. Snapshot live17/10 xác nhận GitLab main cũng42564cb; không suy ai/automation nào đồng bộ nếu không có audit log. Không có dữ liệu source bị xóa bởi lượt kiểm tra.
- AI1 lịch sử WAITING_AGENT_REPORTS không tự là trạng thái hiện tại: QA đã được Owner báo REPORTED và Notion có UI v1; vẫn cần AI1 xác nhận đã nhận/đối soát báo cáo đầy đủ. Không chọn phương án A/B/C hoặc T0–T11 thay Owner.
- AI2 có14 Work Items theo Owner; chưa đọc bản đầy đủ nên không tạo14 task giả hoặc tự điền nội dung.
- QA F1: provenance42564cb và baseline; F2: 47 vs 49hàng parity; F3: CI chưa cấu hình; F4: thiếuMR/approval history; F5: khả năng đối chiếuGitHub của phiênQA; F6: bất thườngAPK inventory; F7: graph config/integrity. Đây là danh sách Owner chuyển giao, không tự đóng finding từ một snapshot mới.
- Đối chiếu bổ sung17/10 cho F1/F2/F3/F4/F7: commit42564cb có parent6a02097, đúng7 file W1–W8; đếm feature rows theo ID trong ba bảng parity được18 + 17 + 12 = 47. Chưa có nguồn/revision/phương pháp cho49 nên F2 vẫn chờ AI1/QA đối soát. Không thấy CI config trong repo; API pipelines/MR trả rỗng, không có pipeline PASS. Graph baseline schema/hashes là evidence trong repo; tác vụ hiện tại kiểm lại integrity riêng, không nâng verdict QA.
- Basic Memory notes ngày 15/10 còn nói baseline6a02097 và Figma NOT_READY. Giữ làm lịch sử; Owner17/10 đã cho phép **Notion UI design độc lập**, trong khi **Android UI integration chưa được giao/duyệt**. Không tự sửa cloud notes hoặc lấy chúng làm lệnh code mới.

## NEXT PROPOSED WORK — không phải nhiệm vụ đã giao

| Đề xuất | Agent chịu trách nhiệm nếu được chấp thuận | Điều kiện đầu vào / quyền duyệt |
| --- | --- | --- |
| Hợp nhất T0–T11,14 OEM Work Items và F1–F7; map trùng lặp và giữ Evidence IDs | AI1; AI2/QA cung cấp báo cáo | Cần bản đầy đủ, revision/baseline và Owner duyệt backlog trước khi Codex code |
| Hoàn thiện UI v1 và các câu hỏi typography/density/states/components | Notion AI mặc định | Theo nhiệm vụ thiết kế đang có; Owner chốt revision, không đợi OEM review để thiết kế |
| Đánh giá DESIGN_COMPONENT → ANDROID_VIEW và kế hoạch tái sử dụng | Codex, AI1/QA review | Chỉ sau khi nhận handoff thực đã được Owner chốt và giao nhiệm vụ đánh giá; chưa tự sửa Kotlin/XML |
| Thiết kế CI/test gates và MR/approval policy | AI1 đề xuất; QA review; Codex triển khai nếu được giao | Owner chọn GitHub/GitLab workflow, runner và phạm vi; không tự tạo pipeline/MR hoặc đồng bộ GitLab |
| Đồng bộ bộ nhớ cloud với registry repo sau khi bản này được duyệt | Agent được Owner giao | Dùng cùng revision và cập nhật note hiện có; không tạo bộ registry song song hoặc đồng bộ ngầm |

## Ma trận trách nhiệm và quyền quyết định

| Phạm vi | Chủ trì | Đối soát / review | Quyền quyết định cuối |
| --- | --- | --- | --- |
| Kiến trúc, dependency, backlog và kế hoạch tổng thể | Claude AI 1 | AI2 cho OEM, Codex về khả thi, QA về chất lượng | Owner |
| Phản biện OEM độc lập | Claude AI 2 | AI1 đối soát với bằng chứng Codex, QA kiểm integrity | Owner |
| Android/API29/source/tests/emulator/APK/UA | Codex Windows | AI1 + QA; AI2 phản biện phần OEM | Owner |
| CI, regression/security/safety review và MR verdict | GitLab QA | AI1 đối soát, Codex cung cấp test/artifact | Owner chọn workflow/merge; verdict QA không thay approval |
| UI/UX và design documentation | Notion AI mặc định | Owner review; Codex đánh giá Android khi được giao | Owner chốt thiết kế |
| Android UI integration | Codex khi được giao | Notion giải thích handoff; AI1/QA review | Owner chốt handoff + scope code |
| Device/vehicle test, đặc quyền, APK release, commit/push/merge | Agent được giao đúng hành động | Theo [AGENTS](../../AGENTS.md) §§13/16/20 và rollback | Owner cho phép riêng; không kế thừa từ quyền thiết kế/code |

AI1 điều phối với AI2/Codex/QA/Notion và trình Owner; quan hệ này không trao quyền phê duyệt thay Owner. UI design chạy song song OEM research và Android Core; UI integration là luồng khác có cổng handoff/phê duyệt riêng. Không Agent nào tự lặp task đã có owner hoặc thay đổi bằng chứng của reviewer độc lập.

## Handoff protocol

Mỗi lượt bàn giao ghi đủ 11 trường, ngay trong artifact canonical liên quan; không tạo file audit/backup mới chỉ để ghi hoạt động:

1. **Agent Name** — tên đúng registry; nếu dùng sub-agent nội bộ, Agent chính vẫn chịu trách nhiệm.
2. **Assigned Task** — scope Owner đã giao; tách NEXT PROPOSED WORK khỏi assignment.
3. **Project Baseline / Git Commit** — full SHA, branch, dirty/staged paths và revision nguồn ngoài.
4. **Files or Sources Inspected** — paths/URLs, revision/hash và giới hạn truy cập; không nhận đã đọc link chỉ vì được cung cấp.
5. **Changes Made** — file/artifact thực đã đổi; `NONE` nếu chỉ review.
6. **Findings** — Evidence IDs, source và confidence; ghi mâu thuẫn/chưa rõ.
7. **Validation Results** — lệnh/check thực + PASS/FAIL/NOT_RUN; giữ tách static confidence, implementation và verification.
8. **Open Issues** — blocker, lý do và bằng chứng còn thiếu; không biến mọi unknown thành hardware-only.
9. **Dependencies** — báo cáo/asset/quyết định cần nhận.
10. **Next Responsible Agent** — ai nhận tiếp, bằng artifact nào; không tự gửi tin cho Agent ngoài.
11. **Owner Approval Status** — chưa duyệt/đã duyệt, phạm vi và revision; một approval không mở rộng sang code/publish/xe.

Trạng thái chuẩn: `PLANNED`, `ASSIGNED`, `IN_PROGRESS`, `PAUSED`, `REPORTED`, `IN_REVIEW`, `COMPLETED`, `BLOCKED`, `DEFERRED`. Nhãn domain như `WAITING_APPROVED_TASK`, `WAITING_AGENT_REPORTS`, `UI_DESIGN_V1_OWNER_REVIEW` là mô tả bổ sung có nguồn, không thay trạng thái chuẩn. Không dùng `COMPLETED` cho phương án mới chỉ được viết, hoặc `DESIGN_COMPLETE` khi thiếu review/đầu ra.

Handoff đi từ Agent phụ trách → AI1 đối soát (QA/AI2 giữ tính độc lập) → Owner quyết định → Agent được giao thực hiện. Notion được tiếp tục nhiệm vụ thiết kế độc lập; thay đổi Android vẫn cần Owner giao scope mới. Agent nhận phải đọc artifact/revision thực, kiểm tra baseline và xác nhận phần đã nhận trong handoff kế tiếp. Không có cơ chế tự gửi/sync qua GitHub/GitLab/Notion/Basic Memory trong registry này.

## Notion Design → Codex: nhận đầu ra thực, chưa tích hợp

Đã đọc9 trang Notion (Hub,Production,MasterPrompt,5 trang output,OwnerReview 07) qua connector. Native verification của các trang là `unverified`; response không cung cấp `truncated`/`unknown_block_count`/`unknown_block_ids`, nên không ghi false/0 cho những trường này. Đã đọc từng trang trả về; đây không phải xác minh trực tiếp Figma. File output được Notion liên kết là [TN Launcher Figma](https://www.figma.com/design/I7IYyg8KMlVrVVCbGi84eI), khác [iPadOS reference](https://www.figma.com/design/z9h85ehDzhCsLpV5n7M0Kb/iOS-and-iPadOS-27?m=auto).

| Nhóm đầu ra cần nhận | Nguồn thực đã đọc | Trạng thái và giới hạn v1 |
| --- | --- | --- |
| 1. UI Design Overview | Production + OwnerReview 07 | Có v1 để review; Owner chưa duyệt |
| 2. Screen Index / Figma Frame Links | [Home/Dock](https://app.notion.com/p/3fc8a3a2b461815ea373de476e6ee4b1), [Drawer/Search](https://app.notion.com/p/3fc8a3a2b461815e9568eed50863658c), [Settings](https://app.notion.com/p/3fc8a3a2b461813f80e0edabf1b54e1a), [Folders/Widgets](https://app.notion.com/p/3fc8a3a2b4618155a9ffd2e2d948ad7d) | Có10+8+10+6=34 frame links/nodeIDs trong tài liệu; chưa xác minh Figma trực tiếp |
| 3. Design Tokens | [Visual System](https://app.notion.com/p/3fc8a3a2b46181319e62e59378890245) | Có baseline/semantic proposals, token sheet 2:593; không tự đưa vào Android |
| 4. Typography Specifications | Visual System và handoff | Figma dùng Inter fallback; chưa xác minh metrics/glyph SF Pro Text/Display và MRCH NewYork trong frame |
| 5. Component Library | Visual System + Folders/Widgets/Handoff | Tài liệu báo12 editable sets/168 swatches; anatomy/auto-layout/binding/publish chưa hoàn tất |
| 6. Light/Dark Variants | Các screen families | Có frame; chưa đủ mọi permutation/state |
| 7. Responsive Layouts | Home/Dock và screen specs | A1280×720dp/B960×540dp, có Home B font scale 1.5; nhiều font scale 1.5/state mới là spec, không phải số đo màn hình xe |
| 8. Assets Manifest | Visual System + handoff | Có provenance/logo/icon/font/wallpaper handling; thiếu inventory app/provider thực trên thiết bị |
| 9. Interaction & Navigation Matrix | Screen specs + handoff | Có state/action/Back/recovery matrix; prototype Figma chưa nối |
| 10. Android Views Integration Handoff | Folders/Widgets/Handoff | Có proposed resources/inputs/callbacks; chưa map callback vào source thực hoặc tích hợp/build/test |

Khoảng trống v1 cần giữ: Home loading/empty/error; Dock dragghost/overflowclip; Drawer font scale 1.5/loading/permission; Settings font scale 1.5/URIerror/persistence/ảnh thật; widget bind-denied/config/remove-confirm chưa đủ frame riêng. Drawer heading22sp mục tiêu nhưng frame28sp và dark focus/indicator `#80B6FF` còn chờ chốt; CTA `#0A64D9` không tự đổi. Slots “Chờ app/Favori” là wireframe được gắn nhãn, không phải installed apps hoặc dữ liệu được phép đưa vào APK.

Khi Owner chốt handoff và giao đánh giá, Codex đọc đủ overview/frame IDs/tokens/typography/components/variants/assets/action matrix và revision; đối chiếu source Android thật cho Home, Dock, App Drawer, Search, Settings, Theme, Wallpaper, Folders và Widgets. Ma trận `DESIGN_COMPONENT → ANDROID_VIEW` phải ghi component/frame/state, class/layout/resource hiện có, reuse/new/replace, token/asset, callback/state/Back/error, phụ thuộc OEM và kiểm thử API29 cần thiết. Tài nguyên cần kiểm: SF Pro Text, SF Pro Display, MRCH NewYork, TN brand, MacTahoe Settings, PackageManager icons và wallpaper người dùng, theo nguồn/quyền đã có trong repo.

Đánh giá khả năng tích hợp (UI_INTEGRATION_READINESS_REPORT) chỉ được lập khi được giao và có đủ đầu vào; không tạo report trùng chức năng trong lượt registry. Đánh giá không tự cấp quyền sửa Kotlin/XML; code cần scope Owner duyệt riêng, giữ các contract core/vendor, emulator-first và cổng thiết bị của AGENTS. Không dùng layout Figma làm proof camera/OEM hay giả control đang hoạt động.

## Handoff của tác vụ registry này

- Agent Name: Codex Windows; các helper nội bộ chỉ rà Notion/UA/integrity.
- Assigned Task: MASTER PROMPT quản lý bộ nhớ/roles/coordination và yêu cầu tiếp theo kiểm tra, bàn giao tài liệu qua GitHub; không Android development.
- Project Baseline: local/GitHub/GitLab main42564cbe10bcfaadfa1b8dce505acde2ed549b33; registry revision REGISTRY-2026-10-17.1.
- Sources Inspected: bảng nguồn ở trên, các tài liệu bắt buộc, Notion9 trang,2 Basic Memory notes, Git refs/API MR/pipeline và source metadata cần kiểm phạm vi.
- Changes Made: registry mới; AGENTS/STATUS/EXECPLAN và UA graph cập nhật theo phạm vi tài liệu. Không sửa source/Gradle/Manifest hoặc autoUpdate.
- Findings: current roles đã map; UI v1 IN_REVIEW; nguồn lịch sử Claude không truy cập được; QA report đầy đủ/AI2 Work Items chưa nhận; Basic Memory snapshot cũ; parity 47 theo phương pháp đếm nêu trên chưa tự đóng F2.
- Validation Results: PASS cho ma trận 6 hàng/12 cột (5 AI + Owner), 11 trường handoff, 15 liên kết tương đối/anchor, UTF-8, ownership/status/authority, diff/privacy; AGENTS cũ, lịch sử progress, source/assets/parity giữ nguyên. UA candidate qua schema chính thức và 69/69 LF hashes: 322 nodes/460 edges/69 files/8 layers/9 tour steps; chỉ thêm 1 document node + 11 Markdown edges, giữ source descriptors/IDs/449 cạnh cũ và provenance lịch sử. Stock prepare thực tế BLOCKED_DIRTY_WORKTREE; 32 file UA state/config và plugin giữ nguyên. Chỉ lưu candidate đã review sau final freeze rồi kiểm lại shared graph. Build/lint/unit/instrumentation/E2E NOT_RUN vì không đổi Android; không có test mới trên xe.
- Open Issues: publication của registry đã được cho phép; các nội dung cần đối soát và UI v1 vẫn chờ review. Thiếu bản đầy đủ AI1/AI2/QA, nguồn đếm 49 hàng cho F2, component/font/state/inventory gaps của UI v1.
- Dependencies: Owner feedback, AI1 đối soát reports và handoff UI revision chốt; không yêu cầu thử xe để hoàn tất registry.
- Next Responsible Agent: Project Owner review registry/UI; AI1 đối soát báo cáo; Notion AI tiếp tục nhiệm vụ design. Codex WAITING_APPROVED_TASK cho code.
- Owner Approval Status: đã cho phép bàn giao tài liệu bằng commit/push GitHub theo OWNER-PUBLISH-17; không mở development increment, Android integration, device testing hoặc đồng bộ GitLab. Các quyết định thiết kế/backlog đang IN_REVIEW vẫn giữ nguyên.

## Handoff nghiên cứu OEM18/10/2026

- **Agent Name:** Codex Windows; helper camera/CAN là nội bộ, đã chạm usage limit, root tiếp quản kết luận.
- **Assigned Task:** OWNER-OEM-18, bổ sung OWNER-ADB-18/OWNER-CAMBTN-18/OWNER-REVERSE-18; nghiên cứu tĩnh, fingerprint và quan sát UI/camera lùi OEM, không code TN.
- **Project Baseline / Git Commit:** main/0eebfc8f0b4f886673f191259dd87f32c06c6e81; worktree/index sạch trước tác vụ;8 tài liệu canonical và graph là phạm vi thay đổi local dự kiến, không commit/push.
- **Files or Sources Inspected:** AGENTS/README/registry/progress/parity/testing, MS/US/L8/FC đúng SHA trong contract/inventory; bảy APK active được hash tại chỗ; guide/scrcpy3.3.4/ADB36.0.0, targeted runtime properties/package/window data. Không đọc private app data/log/backups hoặc tải lại archive.
- **Changes Made:** contract/parity/inventory; STATUS/EXECPLAN/registry; compatibility report/device checklist và bounded graph refresh. Không source/test/Gradle/Manifest/asset/AGENTS hoặc UA config/plugin changes.
- **Findings:**11 IDs18-CAM-01–06/18-CAN-01–05; packet validity/conversion/unknown-gear/lifecycle/freshness và camera restore/button branches. Runtime API29/1280×800/160dpi, Home L8,7 APK SHA match, DC disabled. Ca riêng18-OEM-R01 quan sát một chu kỳ Home L8→camera phía sau toàn màn hình khi Owner chọn R→tự về Home khi Owner đỗ; radar và TN trên xe chưa kiểm chứng.
- **Validation Results:** ADB connect và runtime fingerprint PASS sau timeout ban đầu; scrcpy helper/renderer OBSERVED;18-OEM-R01 PASS một chu kỳ trong scope OEM. Static/document checks PASS:39 liên kết/UTF-8/privacy/scope,47 parity states/88 W8 rows,7 local hashes và symbolic arithmetic. Helper rà tài liệu độc lập và câu vendor-launch đã sửa, không thay formal review AI1/AI2/QA. Graph đã lưu/đọc lại PASS schema chính thức/69 LF hashes,322 nodes/460 edges/8 layers/9 tour steps; marker riêng giữ topology/source và32 file UA state/config không đổi. Android build/lint/unit/instrumentation/E2E NOT_RUN. CLI anonymous class DONT_GENERATE được ghi nhận và thay bằng raw DEX, không che lỗi.
- **Open Issues:** active decoder/physical units/freshness/access; fullscreen request delivery của nút nổi; radar/cảnh báo/precedence từ TN hoặc app khác, độ trễ/lặp và rollback. Không nâng các ô verification TN từ kết quả OEM một chu kỳ.
- **Dependencies:** AI1 đối soát findings; AI2/QA review độc lập khi có phiên; Owner quyết định scope tiếp. Không tự gửi thông điệp hoặc đồng bộ dịch vụ ngoài.
- **Next Responsible Agent:** AI1 nhận contract/parity và handoff này; AI2/QA phản biện bằng artifact thực; Owner chốt bước tiếp theo.
- **Owner Approval Status:** đã duyệt nghiên cứu/quan sát, bật ADB tạm, thao tác nút nổi và ca lùi OEM có giám sát theo các yêu cầu mới; không duyệt cài TN/đổi Home/thử từ TN/code UI/commit/push hoặc GitLab synchronization trong increment này.

## Handoff publication và Figma18/10/2026

- **Agent Name:** Codex Windows; helper UA trong TEMP và helper asset/constraint chỉ là công cụ nội bộ.
- **Assigned Task:** OWNER-DESIGN-PUBLISH-18; publication hiện có rồi thiết kế Figma, không code Android.
- **Project Baseline / Git Commit:** main0eebfc8 trước publication; commit OEM local2f8ceaa. SHA/publication cuối đối chiếu Git.
- **Files or Sources Inspected:** AGENTS/README/design/progress/parity; UA skill/helper/runtime; target/reference Figma connector và Desktop; fonts116file/icon/WAV nguồn cục bộ.
- **Changes Made:** tài liệu hiện có về scope/connection/audio; marker UA failure và doc hashes. Không Android source/Manifest/assets gốc, quyền hoặc cấu hình xe; chưa xác nhận node Figma được sửa.
- **Findings:** stock UA strict parser API mismatch sau một retry; graph hiện có hợp lệ. Figma yêu cầu reauthentication. Font/icon/WAV hợp lệ; NewYork thiếu combining marks và WAV có nhịp sẵn, không suy thành phase đèn OEM.
- **Validation Results:** current graph official schema/69 LF hashes; shared private UA state giữ nguyên khi thử isolated workflow. Các ca Android/xe/audio NOT_RUN trong scope mới. External APK rehash không chạy được vì thư mục archive cũ hiện không tồn tại; giữ provenance lịch sử.
- **Open Issues:** kết nối Figma trước khi inspect/sửa/readback; strict parser toolchain cần scope sửa riêng; event/phase/freshness và ngưỡng đo audio tương lai chưa chốt.
- **Dependencies:** Owner kết nối lại Figma; đúng file/edit access/font availability; không cần xe để hoàn tất thiết kế tĩnh.
- **Next Responsible Agent:** Codex tiếp tục design theo scope Owner sau kết nối; Owner review kết quả; AI1/QA nhận Git artifact qua quy trình hiện có.
- **Owner Approval Status:** đã cho phép commit/push GitHub và chỉnh Figma; không mở Android integration, GitLab sync hoặc test xe mới. Không tự gửi thông điệp sang Agent/cloud khác.

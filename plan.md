# plan.md — tas-android

> takeaseat(TAS) 안드로이드 클라이언트의 작업 소스오브트루스. 구조·현재 상태는 `README.md`, 규약은 `CLAUDE.md`.
> 백엔드·웹: `mjkang0987/tas`(로컬 `../hair_reservations`). iOS: `mjkang0987/tas-ios`(로컬 `../tas-ios`).
>
> _상태_ ⬜ 예정 · 🟡 진행 · ✅ 완료

---

## 🟡 진행 — 5단계: 예약 상세 취소·노쇼·예약전환 (`claude/android-reservation-status`, #4 위)

### 요구 (지시자 2026-10-19 — "안드로이드 계속")
- 상세 시트 하단 버튼 중 결제 없이 되는 것부터: 취소 · 노쇼 · 예약전환. 결제완료·변경·예약완료는 다음 단계.

### 웹에서 가져오는 것 (기준 tas v0.64.3)
- `ReservationDetailFooterActions` 보기 모드: 미결제 진행 → 취소·노쇼(빨강 테두리), 결제했으면 이번엔 버튼 없음, 취소·노쇼 → 예약전환(보라).
  확정대기는 확정·거절(온라인 예약 = 로그인 전용)이라 없음. 삭제는 웹도 게스트엔 안 보인다(owner·manager 만).
- 확인: 취소·노쇼는 같은 시트 본문이 확인으로 바뀐다(`ReservationStaticDiffSection` — 색 문구 14 600 + 회색 상자 서비스·날짜·시간·고객명),
  버튼 취소 / 예약 취소·노쇼 처리(빨강). 예약 전환은 가운데 확인창(`StyledConfirmModal` 폭 min(360, 90%), 12 둥글게).
- `cancelReservation`(게스트): 취소면 결제 비움 + 결제에 쓴 적립금 반환·준 적립 회수(`pointRefundOnCancel`, 적립금 이력), 노쇼는 상태만,
  이력 한 줄, 상세 닫힘. `restoreReservation`: 진행으로 + 이력, 상세는 열어 둠.

### 구현
- `domain/ReservationStatusChange.kt` 순수 함수(detailActions·changeReservationStatus·restoreReservation) + 단위 5.
- `ActionButton` 에 tone(Primary·Danger·DangerOutline), `DetailSheet` 에 centered(확인창)·버튼 줄 오른쪽 정렬을 시트로 옮김.

### 검증 (2026-10-19)
- 단위 43. 에뮬레이터: 취소 예약 → 예약전환 확인창 → 진행(버튼 취소·노쇼로 바뀜) → 노쇼 확인 → 노쇼(시트 닫힘, 빗금) → 다른 예약 취소
  (결제 비움 저장 확인), 이력 저장 확인. 코드리뷰 3회차: 버튼 줄 3벌 중복 → 시트로, 확인 문구 낭독(liveRegion), 적립금 이력 번호 웹 형식.
- 못 한 것: TalkBack 실제 낭독, 결제된 예약 취소의 적립금 반환은 화면으로 못 봄(결제 화면이 없어 데이터가 없다 — 단위 테스트로만).

### 남은 결정
- ~~대비: 노쇼 확인 문구·예약 전환 문구·4단계 가격 글자~~ → 웹과 함께 --warning-color #C5221F(5.8), --blue-color hsl(196,100%,32%)(5.0),
  `TextColorContrastTest` 가 지킨다(지시자 2026-10-19 "다 바꿔줘").
- 메모 태그·담당자 라벨 → 흰 글자 그대로, 흰 글자가 4.5 안 되는 밝은 색만 같은 색을 진하게(`backgroundForWhiteText`, 웹과 같은 값 — 테스트로 맞춤).
  처음엔 흰·검 글자 고르기로 했다가 지시자가 "메모 색 최악" — 검은 글자는 뺐다. 메모 태그는 높이 22·기준선 가운데(lineBox, 한 줄·말줄임),
  DarkGray2 43%·고객명 링크 #1967D2(웹과 함께, "같이 진행해줘"). 에뮬레이터: 메모 태그 8색·담당자 라벨 확인.
- 디버그 예시 데이터(app/src/debug)는 남긴다 — 빼면 새로 깐 앱이 서비스·담당자 없는 빈 매장이라 쓸 수 없다(초기 설정 화면 생길 때 뺀다).

---

## 🟡 진행 — 4단계: 예약 추가 (`claude/android-reservation-create`, #3 위)

### 요구 (지시자 2026-10-19 — "응 추가", 신규 컴포넌트 승인)
- 헤더 '+ 예약' → 웹 모바일 `ReservationCreate` 시트. 게스트 저장소에 예약(+ 새 고객) 저장.

### 웹에서 가져오는 것 (기준 tas v0.64.2)
- `useReservationCreateForm.ts`: 기본값(오늘·지금 30분 반올림, 끝 = 시작+30, 첫 재직 담당자, 전화예약), 고객 고르면 연락처 채움·
  새 이름 치면 신규로 전환(연락처 비움), 서비스 고르면 끝·가격 다시 계산(가격 손댔으면 가격은 그대로), 시작 바꾸면 끝 다시(끝 손댔으면 그대로),
  검사 순서·문구(validate), 이름+연락처가 같은 기존 고객은 다시 만들지 않음(findExactCustomer), 새 번호 = 최댓값+1(삭제 고객 포함).
- 계산: `buildCatalogMap`·`sumDurationMinutes`·`sumPrice`·`calcEndTime`·`formatDuration`·`findOverlap`·`getAssigneeAvailabilityState`·
  `splitAssigneesByStatus`/`sortAssignees`·`sortCustomersByName`·`roundToHalfHour`.
- 모양: `StyledForm`(간격 10, 라벨 12 600 #525252 → 4 → 입력 36·테두리 #e3e3e3·6 둥글게·13), `ServiceFields`(최대 200 스크롤 목록,
  카테고리 머리글, 선택 요약), `CustomerAutocomplete`(최대 160 추천), 하단 취소·저장(30, 7 둥글게), 헤더 '+ 예약' 알약.

### 신규 컴포넌트 (승인됨)
`ReservationCreateSheet`, `TasTextField`, `TasSelect`, `CustomerAutocomplete`, `ServiceFields`, `ActionButton`.

### 구현
- 폼 상태·바꾸기 규칙·검사·저장 반영은 순수 함수(`domain/ReservationCreate.kt`) — 단위 테스트. 화면은 그걸 부르기만.
- 날짜·시간은 시스템 날짜/시간 선택(웹 네이티브 date/time 자리), 예약경로·담당자는 드롭다운.
- 웹과 다르게 둔 것: 고객 추천 목록은 아래 칸 위에 겹치지 않고 밀어낸다(겹친 목록은 터치가 안 닿을 수 있다) · 23:45 이후 시작은 23:30
  (웹은 "24:00" 이 돼 시간 칸이 빈다) · '검색 결과 없음' 글자는 대비 위해 #737373(웹 #dbdbdb) · 오류가 화면 밖이면 그 자리로 스크롤(웹은 안 함).

### 검증 (2026-10-19)
- 단위 38(신규 10: 시작 반올림·시간 계산·기본값·서비스/시작/끝 연동·고객 고르기/다시 치기·검사 순서·겹침·저장 번호·담당자 정렬).
- 에뮬레이터: '+ 예약' → 시트, 빈 저장 → 고객명 오류, 기존 고객 고르기(연락처 채움) → 서비스 2개 → 겹침 오류(화면 밖 → 스크롤) →
  시간 선택 15:00(끝 자동 16:00) → 저장 → 타임라인·상세에 표시, 새 고객 저장(7번, 연락처 숫자만) · 가격 손대면 그대로 · 회전 2번 후 입력 유지.
- 코드리뷰 1회차: 입력 칸 이름이 낭독 노드와 따로 놀던 것(라벨을 입력 칸 장식 안으로), 같은 오류 재저장 때 스크롤, calcEndTime 이
  콜론 없는 값에 죽던 것(toMinutes 재사용), 날짜·시간 대화상자 코드 3벌 → `SystemDialogEffect`, 셰브론 아이콘 중복.
- 못 한 것: TalkBack 실제 낭독, 실제 화면 키보드(에뮬레이터가 하드웨어 키보드 모드 — 키보드 피하기는 코드만), 큰 글자, Android 8.

### 남은 결정
- ~~선택 서비스 가격 글자 대비~~ → 5단계에서 웹과 함께 바꿈.
- `FieldError`(웹 FieldError 이식)를 승인 목록 밖으로 하나 더 만들었다 — 오류 문구 한 줄이라 따로 둠.

---

## 🟡 진행 — 3단계: 예약 상세(보기) (`claude/android-reservation-detail`, #2 위)

### 요구 (지시자 2026-10-19 — "일단 쭉 진행", 신규 컴포넌트 승인)
- 일 타임라인의 예약 카드를 누르면 웹 모바일처럼 아래에서 올라오는 상세 레이어. 이번엔 **보기만** — 수정·결제·취소 버튼은 다음.

### 웹에서 가져오는 것 (기준 tas v0.64.1, 모바일 375px 실측)
- `ReservationViewSection.tsx` 행 차례·조건: 상태(진행 외) · 날짜 · 시간 · 서비스(칩) · 가격 · 결제(배지 위 한 줄 + 종류·금액 목록,
  결제완료인데 줄 없으면 '미입력', 네이버 예약금 줄) · 고객명(신규 배지) · 연락처(누르면 전화) · 고객 메모 태그 · 요청사항 · 적립금 ·
  담당자(색 알약) · 예약경로(채널 태그). 회원권·쿠폰은 로그인 전용이라 게스트에선 없다.
- `ModalStyles` 바텀시트: 배경 rgba(15,23,42,.4), 위만 12 둥글게, 테두리 rgba(148,163,184,.22), 최대 높이 90%, 0.26초 올라옴.
  헤더 padding 8/14 · 제목 16 굵게 · 닫기 34 정사각(테두리 #e3e3e3, 6 둥글게). 본문 padding 12/14/24, 표 60 + 나머지, 간격 8/12,
  항목명 13 500 #525252, 값 13.
- `ReservationStatusBadge`(RESERVATION_STATUS_BADGE_STYLES), `ColorTag`/`AssigneeLabel`, `LabelBadge` info soft(채널),
  `features/store-settings/labels.ts`(업종별 '서비스'·'담당자' 표시어).

### 신규 컴포넌트 (승인됨)
`DetailSheet`(웹 ModalStyles StyledOverlay+StyledDetail+Header — 이후 추가·결제 레이어도 쓴다), `ReservationDetailView`
(웹 ReservationViewSection), `StatusBadge`(웹 ReservationStatusBadge), `AssigneeLabel`(웹 동명/ColorTag).

### 검증 (2026-10-19)
- 단위 27(업종 라벨·결제 줄·가격 합·배지 색·CSS 8자리 색·네이버 예약금 줄 조건). 에뮬레이터: 카드 → 시트, 행 표시, 뒤로가기·
  바깥 탭·닫기로 닫힘, 시트 안 탭은 안 닫힘, 가로 화면. 코드리뷰 3회차 반영.
- 배지 상하 여백(지시자 지적): 화면 픽셀로 재서 위/아래 8:8 맞춤(전엔 11:6 — 아래 함정).
- 기능 확인(에뮬레이터, 데이터 넣어서): 완료·취소·노쇼·확정대기 상태, 결제완료/미결제·목록, 고객 메모·요청사항, 네이버 예약(예약금·번호·
  바로가기·안내), 연락처 → 전화 앱(번호만 채움), 바로가기 → 브라우저 → 뒤로가기로 복귀. 연락처·바로가기 실제 누르는 영역 약 43dp.
- 못 한 것: TalkBack 실제 낭독, 큰 글자(200%), Android 8.0/8.1 화면.

### 함정
- **한글 배지 글자가 아래로 처진다**: Noto CJK 글자 상자는 위 1.16em·아래 0.29em 라 상자를 가운데 두면 글자가 1~2dp 낮다.
  `lineBox(줄높이, 글자크기)`는 기준선으로 맞춘다(글자 몸통 가운데 = 기준선 위 0.38em). 한 줄 전용 — 줄바꿈되는 글(메모 태그)엔 쓰지 않는다.
- 시트 바깥 닫기는 `clickable` 이 아니라 손가락 탭만 받는다 — clickable 이면 시트 안 글자가 합쳐지거나(부모일 때) 보이지 않는 화면이
  키보드·낭독기 초점이 된다(형제일 때).
- 웹 1px 테두리는 패딩 바깥에 더해진다 — Modifier.border 는 안쪽에 그리므로 패딩 +1dp.

### 남은 결정 / 다음
- ~~상태 배지·예약경로·네이버 글자 대비 4.5 미만~~ → 웹·iOS 와 함께 바꿈(웹 v0.64.2, 대비 테스트).
- 고객명 → 고객 화면 링크(고객 화면 생길 때), 하단 버튼(수정·결제·취소)은 다음 단계.

---

## 🟡 진행 — 2단계: 게스트 데이터 + 캘린더 일 화면 (`claude/android-calendar-day`, #1 위)

### 요구 (지시자 2026-10-17 — "게스트 모드 + 캘린더")
- 로그인 없이 기기 안 데이터로 동작(웹 게스트 모드). 메인인 캘린더 **일 화면**부터.
- 이번엔 **보기만**: 날짜 이동 + 시간축 타임라인 + 예약 카드. 예약 추가·상세·드래그는 다음 PR.

### 범위
| 넣는다 | 다음으로 |
|---|---|
| 헤더 1줄: 오늘 · ‹ · › · 날짜(누르면 시스템 날짜 선택) | + 예약, 담당자 필터(전체보기), 고객 검색 |
| 일 타임라인: 시간축(영업시간), 정시선, 예약 카드, 겹침 좌우 나눔, 현재시각 바(오늘이면 그 위치로 스크롤), 휴무 배경 | 3일·주·월·년 탭, 카드 탭 → 상세, 빈 칸 탭 → 추가, 드래그 이동 |
| 게스트 저장소: 웹 `LocalDbSnapshot` 과 같은 모양의 JSON 파일 | 온보딩·약관, 로그인·이관 |

### 웹에서 가져오는 것 (하드코딩 금지 — 웹 정의를 옮긴다, 기준: tas `origin/main` v0.61.0)
- 모델: `features/reservations/model.ts`(Reservation·상태), `features/customers/model.ts`(Customer·`isNewCustomerVisit`),
  `features/assignees/model.ts`(Assignee·`getAssigneeColor`·`buildAssigneeColorMap`), `features/services/model.ts`
  (ServiceItem·`CATEGORY_BASE_COLOR_MAP`·`buildServiceColorMap`·`getServiceColor`·`parseServiceString`),
  `features/store-settings/model.ts`(StoreSettings·`DEFAULT_STORE_SETTINGS`·`normalizeStoreSettings`·`getStoreClosedKind`).
- 배치: `features/reservations/timeline-scale.ts`(단위·시간당 높이·카드 높이·내용 단계), `utils/timelineRange.ts`,
  `components/calendar/views/timelineEntries.ts`(겹침 칸 나누기 — 웹 모바일은 접지 않고 전부 나눈다), `inactiveHatch`.
- 화면: `Timeline.tsx`·`TimelineTitle.tsx`·`TimelineReservationCard.tsx`·`ui/Buttons.tsx` `StyledReserveButton`·
  `ui/ServiceChip.tsx`·`ui/NewCustomerBadge.tsx`·`CalendarDirection.tsx`·`CalendarHeading.tsx`. 수치는 웹 모바일(375px) 실측값.
- 저장 모양: `features/local-db/storage.ts` `LocalDbSnapshot` — 같은 키 이름. 나중 로그인 이관(`/api/migrate-local`)이 이 모양을 받는다.

### 구현
- `domain/` 순수 Kotlin(안드로이드 import 없음) + JVM 단위 테스트: 모델, 색, 배율·범위, 겹침 배치, 휴무 판정.
- `data/GuestStore`: 앱 내부 파일 `local-db.v1.json` 읽기/쓰기(kotlinx.serialization, 모르는 키 무시), `StateFlow<LocalDbSnapshot>`.
  디버그 빌드만 예시 데이터 채우기(예약 추가 화면이 없어 확인 수단이 없다 — 다음 PR에서 뺀다).
- `ui/calendar/`: `CalendarHeader`, `DayTimeline`(시간축 + 본문 한 스크롤), `ReservationCard`, `ServiceChip`, `NewCustomerBadge`.
- 의존성 추가: `kotlinx-serialization-json`(+ 플러그인). 표준 직렬화 — 손 파서보다 짧고 안전하다.

### 신규 컴포넌트 (웹에 있는 것을 옮기는 것뿐)
`CalendarHeader`(웹 CalendarDirection+CalendarHeading), `DayTimeline`(Timeline+TimelineTitle), `ReservationCard`(TimelineReservationCard),
`ServiceChip`(ServiceChipList), `NewCustomerBadge`(동명). 버튼 모양(흰 알약 32dp)은 `PillButton` 하나로 오늘·‹·›가 같이 쓴다.

### 검증 (2026-10-17)
- `./gradlew assembleDebug testDebugUnitTest` 통과(단위 20 — 배율·겹침·색·휴무·첫 방문·스냅샷 읽기/쓰기/복구).
  서비스 색 기대값은 웹 `buildServiceColorMap()` 을 실제로 돌려 받은 값.
- 에뮬레이터(`Small_Phone`, 예시 데이터)에서 웹 모바일(375px)과 나란히 비교: 30분 카드 세 줄·20분 한 줄·10분 이름만,
  겹침 2칸·3칸, 취소/노쇼 빗금, 확정대기 딤+배지, 현재시각 바 + 그 위치로 자동 스크롤(위 여백 72), ‹ › 이동, 날짜 선택(회전해도 유지),
  TalkBack 라벨(카드 한 문장 "16:00~17:00 신규 최유나 여성커트, 취소").
- 코드리뷰에서 고친 것: 기본값 키 누락(웹 모양 깨짐), 카드 이중 낭독, 아이콘 변환 중복, 회전 시 날짜 선택 창 누수, 회전마다 파일 재읽기.

### 함정 (다음 화면에도 해당)
- **한글 줄높이**: Compose 는 대체 글꼴 줄간격을 강제로 켜 `lineHeight` 를 줘도 한글(Noto CJK) 줄이 약 1.45em 아래로 안 준다.
  웹 1.2em 으로 맞춰야 하는 한 줄 글자는 `ui/components/lineBox` 를 쓴다(CSS line-height 처럼 상자만 고정).
- **웹 시간축 라벨 어긋남(웹 버그 의심)**: 웹 `TimelineTitle` 은 라벨 가운데를 `30 + 칸/2` 에 두어 30분 단위 매장에서만 정시선(55)과 맞는다
  (20분 단위면 8px, 60분이면 25px 어긋남). 앱은 단위와 무관하게 정시선에 맞췄다 — 웹 수정은 tas 쪽 별건.
- 32dp 버튼(오늘·‹ ›)은 웹과 같은 크기 — WCAG 2.2 AA(24) 충족, 안드로이드 권장 48 보다는 작다.

---

## 🟡 진행 — 1단계: 프로젝트 골격 + 하단 탭 (`claude/android-skeleton`)

### 요구
- Gradle·Jetpack Compose 프로젝트를 세운다. **디자인은 웹을 그대로 가져온다**(지시자 2026-10-16).
- 하단 탭 4개(캘린더·고객·매출·설정) — 웹 `client/components/layout/MobileTabBar.tsx` 이식. 각 탭은 빈 화면.
- CI: 빌드 + 단위 테스트.
- 기능 화면은 이후 화면 하나씩 별도 PR.

### 결정
| 항목 | 값 | 이유 |
|---|---|---|
| applicationId | `kr.co.takeaseat.app` | iOS 번들 ID와 같게 |
| minSdk / targetSdk / compileSdk | 26 / 36 / 37 | 지시자 선택(26). compileSdk 는 Compose BOM 2026.09(1.12)가 37 이상을 요구 |
| 툴체인 | Gradle 9.8.1 · AGP 9.4.1 · Kotlin 2.4.21 · Compose BOM 2026.09.00 | 2026-10-16 각 저장소 최신 정식 |
| 테마 | **라이트만** | 웹에 다크 모드가 없다(웹을 따른다) |
| 폰트 | 시스템 기본 | 웹도 웹폰트를 싣지 않고 OS 기본 한글 서체를 쓴다(`styles/fontStack.ts`) |

### 웹에서 가져오는 것 (하드코딩 금지 — 웹 정의를 옮긴다)
- 색·크기 토큰: `client/styles/globalStyle.ts` `:root` → `ui/theme/TasTokens.kt`
  (`--brand-color #6526d9`, `--brand-color-bg`, `--dark-gray-color2`, `--glass-bg`, `--white-color-60`, `--shadow-md`,
  `--chip-radius`, `--tiny-font 10px` 등 이번에 쓰는 것만).
- 하단 탭: `MobileTabBar.tsx` — 떠 있는 알약 바(좌우 12dp·아래 safe-area 또는 10dp 여백, 안쪽 2dp, 유리 배경 + 흰 60% 테두리 + 그림자),
  탭은 아이콘 16dp + 라벨 10sp 굵게, 위아래 7dp(터치 높이 44dp 이상), 선택 탭은 브랜드 연보라 알약 + 브랜드색.
- 아이콘: `AsideMenuIcon.tsx`의 SVG path(`calendarManage`·`customers`·`revenue`·`settings`)를 그대로 벡터로.
  선 두께 1.5, 선택 시 1.7(웹과 동일).

### 신규 컴포넌트 (웹에 있는 것을 옮기는 것뿐)
- `TasTheme` — 웹 `:root` 토큰. `MobileTabBar` — 웹 동명 컴포넌트. `TabIcon` — 웹 `AsideMenuIcon` 4종.
  빈 화면은 공용 `PlaceholderScreen` 하나로 4탭이 같이 쓴다.

### 검증
- `./gradlew assembleDebug testDebugUnitTest` · 에뮬레이터(`Small_Phone`)에서 실행해 탭 전환·선택 표시 확인 · TalkBack 라벨.

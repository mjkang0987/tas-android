# plan.md — tas-android

> takeaseat(TAS) 안드로이드 클라이언트의 작업 소스오브트루스. 구조·현재 상태는 `README.md`, 규약은 `CLAUDE.md`.
> 백엔드·웹: `mjkang0987/tas`(로컬 `../hair_reservations`). iOS: `mjkang0987/tas-ios`(로컬 `../tas-ios`).
>
> _상태_ ⬜ 예정 · 🟡 진행 · ✅ 완료

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

### 검증
- `./gradlew assembleDebug testDebugUnitTest` · 에뮬레이터(`Small_Phone`)에서 예시 데이터로 웹과 나란히 비교:
  카드 높이·내용 단계(full/compact/name)·겹침 2칸·취소/노쇼 빗금·확정대기 딤·현재시각 바·날짜 이동·날짜 선택 · TalkBack 라벨.

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

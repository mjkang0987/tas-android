# plan.md — tas-android

> takeaseat(TAS) 안드로이드 클라이언트의 작업 소스오브트루스. 구조·현재 상태는 `README.md`, 규약은 `CLAUDE.md`.
> 백엔드·웹: `mjkang0987/tas`(로컬 `../hair_reservations`). iOS: `mjkang0987/tas-ios`(로컬 `../tas-ios`).
>
> _상태_ ⬜ 예정 · 🟡 진행 · ✅ 완료

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

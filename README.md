# tas-android

takeaseat(TAS) 서비스의 안드로이드 클라이언트.

백엔드는 [`mjkang0987/tas`](https://github.com/mjkang0987/tas)이고, 모델·네트워킹은 그 저장소의
`/api/*` 도메인(`client/features/*/model.ts`)을 미러링한다. iOS 클라이언트는
[`mjkang0987/tas-ios`](https://github.com/mjkang0987/tas-ios).

## 상태

- 1단계 — 프로젝트 골격과 하단 탭 4개(캘린더·고객·매출·설정).
- 2단계 — 게스트 데이터(기기 안 JSON) + 캘린더 일 화면.
- 이후 — 예약 추가·상세·수정·결제, 고객, 매출, 설정(매장·서비스·담당자·적립금), 캘린더 일·3일·주·월·년 보기, 예약 알림(기기 알림). 진행·다음 작업은 `plan.md`.

## 실행

```bash
./gradlew assembleDebug testDebugUnitTest
```

Android Studio 내장 JDK(또는 JDK 17+)로 돈다. `compileSdk` 37 플랫폼이 SDK에 있어야 한다.

## 구조

```
app/src/main/java/kr/co/takeaseat/app/
  MainActivity.kt       진입점 · 탭 전환
  domain/               웹 features/*/model.ts 이식(순수 Kotlin) — 모델·색·타임라인 배율/겹침·휴무
  data/GuestStore       게스트 저장소 — 웹 LocalDbSnapshot 과 같은 JSON 파일(local-db.v1.json)
  notify/               예약 알림 — ReminderScheduler(알람 맞추기·알림), 울림·재부팅 리시버. 권한: 알림·정확한 알람·부팅 완료
  ui/theme/             TasTokens(웹 globalStyle.ts :root 이식) · TasTheme(라이트 전용, 웹 줄높이)
  ui/nav/               MobileTabBar(웹 MobileTabBar.tsx 이식) · TabIcon(웹 AsideMenuIcon SVG)
  ui/components/        신규 배지·서비스 칩·lineBox(한글 줄높이)·svgLineIcon
  ui/calendar/          CalendarScreen · CalendarHeader(보기 탭 포함) · CalendarTimeline(일·3일·주) · MonthView(월 격자) · YearView(열두 달) · ReservationListSection(고른 날·달 목록) · ReservationCard · NotificationCenter(알림 종·시트) · 예약 창들
app/src/debug/          예시 데이터(빈 매장일 때만, 예약 추가 화면 전까지)
app/src/main/res/      앱 아이콘(mipmap-anydpi-v26 적응형 — 앞면 웹 logo.svg 의자, 바탕 웹 아이콘 그라데이션) · 알림 아이콘
store/                 Play 스토어 등록용 512px 아이콘(png) + 원본 svg — 앱 아이콘을 바꾸면 svg 도 고쳐 다시 뽑는다
```

디자인은 웹을 그대로 옮긴다 — 색·크기는 `TasTokens`에 웹 변수 이름을 주석으로 달아 둔다.

## 세 클라이언트의 관계

| | 저장소 | 비고 |
|---|---|---|
| 웹(관리자·예약) | `tas` | 백엔드 + Next.js. API·도메인 모델의 소스오브트루스 |
| iOS | `tas-ios` | SwiftUI. 색·용어·비즈니스 로직은 웹과 일치, 조작 관용구는 네이티브 |
| Android | `tas-android` | 이 저장소 |

`tas-ios`가 자리잡은 규약(웹 정의 이식, 네이티브 골격 유지, 공통 영역 컴포넌트화)을 그대로 따른다.
자세한 것은 `tas-ios/CLAUDE.md` 참고.

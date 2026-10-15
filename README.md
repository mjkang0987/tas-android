# tas-android

takeaseat(TAS) 서비스의 안드로이드 클라이언트.

백엔드는 [`mjkang0987/tas`](https://github.com/mjkang0987/tas)이고, 모델·네트워킹은 그 저장소의
`/api/*` 도메인(`client/features/*/model.ts`)을 미러링한다. iOS 클라이언트는
[`mjkang0987/tas-ios`](https://github.com/mjkang0987/tas-ios).

## 상태

1단계 — 프로젝트 골격과 하단 탭 4개(캘린더·고객·매출·설정, 각 탭은 빈 화면). 진행·다음 작업은 `plan.md`.

## 실행

```bash
./gradlew assembleDebug testDebugUnitTest
```

Android Studio 내장 JDK(또는 JDK 17+)로 돈다. `compileSdk` 37 플랫폼이 SDK에 있어야 한다.

## 구조

```
app/src/main/java/kr/co/takeaseat/app/
  MainActivity.kt       진입점 · 탭 전환(지금은 탭마다 빈 화면)
  ui/theme/             TasTokens(웹 globalStyle.ts :root 이식) · TasTheme(라이트 전용)
  ui/nav/               MobileTabBar(웹 MobileTabBar.tsx 이식) · TabIcon(웹 AsideMenuIcon SVG)
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

# tas-android

takeaseat(TAS) 서비스의 안드로이드 클라이언트.

백엔드는 [`mjkang0987/tas`](https://github.com/mjkang0987/tas)이고, 모델·네트워킹은 그 저장소의
`/api/*` 도메인(`client/features/*/model.ts`)을 미러링한다. iOS 클라이언트는
[`mjkang0987/tas-ios`](https://github.com/mjkang0987/tas-ios).

## 상태

규약 문서(`CLAUDE.md`)만 있는 상태다. 프로젝트 골격(Gradle·Compose)과 CI는 아직 없다.

## 세 클라이언트의 관계

| | 저장소 | 비고 |
|---|---|---|
| 웹(관리자·예약) | `tas` | 백엔드 + Next.js. API·도메인 모델의 소스오브트루스 |
| iOS | `tas-ios` | SwiftUI. 색·용어·비즈니스 로직은 웹과 일치, 조작 관용구는 네이티브 |
| Android | `tas-android` | 이 저장소. Kotlin + Jetpack Compose. **화면 구성·디자인까지 웹을 따른다** |

웹 정의 이식·공통 영역 컴포넌트화는 `tas-ios`와 같지만, **iOS의 "네이티브 골격" 방침은 따르지 않는다.**
레이아웃·정보구조·간격·색은 웹 모바일 화면을 기준으로 옮긴다. 자세한 것은 `CLAUDE.md`의 디자인 방침 참고.

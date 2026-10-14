# plan.md — tas-android

> takeaseat(TAS) 안드로이드 클라이언트의 작업 단일 소스. 웹(`mjkang0987/tas`)과 **디자인·기능을 맞춘 네이티브 앱**(Kotlin + Jetpack Compose)을 만든다.
> 디자인 방침은 `CLAUDE.md`를 따른다. 웹이 기준이고 머티리얼 기본값은 웹 값으로 덮어쓴다.
>
> _상태_ ⬜ 예정 · 🟡 진행 · ✅ 완료 · 🔒 외부 준비 필요 · ❓ 결정 필요

---

## 0. 전제 · 결정 필요

### 현재 상태
- 이 저장소에는 `README.md`와 `CLAUDE.md`만 있다. Gradle 골격·CI·코드는 아직 없다.
- 이 컨테이너에는 Java와 Gradle만 있고 **Android SDK는 없다**. 그래서 로컬 빌드로 검증할 수 없고, iOS와 같이 **푸시한 뒤 CI가 그린인지로 검증**한다.

### ✅ 디자인 방침 확정 (2026-10-14)
- **iOS의 "네이티브 골격"을 따르지 않는다.** 웹 모바일 화면(배치·정보구조·간격·색)을 기준으로 옮기고, 머티리얼 기본값은 웹 값으로 덮어쓴다(`CLAUDE.md`).
- 예외는 시스템 동작뿐이다(뒤로가기·권한·알림·접근성). 픽셀 단위 일치는 목표로 하지 않는다. 스크롤 감각·키보드 같은 OS 처리 영역은 안드로이드 방식을 따른다.
- `README.md`에 남아 있던 "네이티브 골격 유지" 문구는 이 결정에 맞춰 고쳤다.

### ❓ 기술 스택 (제안, 확인 필요)
| 항목 | 제안 | 근거 |
|---|---|---|
| 언어/UI | Kotlin + Jetpack Compose | "네이티브" 요청. 웹 레이아웃을 옮기기에 View 시스템보다 유연하다 |
| minSdk / target | 26 / 최신 안정 | 26 미만은 점유율이 미미하고, `java.time`을 desugaring 없이 쓸 수 있다 |
| 네트워크 | OkHttp + Retrofit + kotlinx.serialization | 표준 조합이다. Bearer 토큰은 인터셉터 하나로 붙인다 |
| 로컬 저장(게스트) | JSON 파일 스냅샷 (iOS `GuestStore`·웹 `local-db/storage.ts`와 같은 구조) | Room은 과하다. 이관 API가 스냅샷을 통째로 받는다 |
| 토큰 저장 | EncryptedSharedPreferences(또는 DataStore + Keystore) | iOS Keychain에 대응한다 |
| DI | 초기에는 없음(수동 생성) | 화면 수가 늘어나서 필요해지면 그때 Hilt를 검토한다 |
| 구조 | MVVM (`ViewModel` + `StateFlow`) · 단일 Activity + Navigation Compose | |

### 범위 밖 (웹 전용)
랜딩(`/`의 익명 소개 화면) · 공개 예약 페이지(`/book/**`, 고객용) · SEO(`sitemap`/`robots`) · 점검 페이지(앱은 API 503 응답에 안내 문구만 띄운다) · 광고 배너(`AdBanner`, ❓ 앱에도 넣을지 결정 필요)

---

## 이식 원칙 (모든 이슈 공통)
- **색·라벨·포맷·판정은 웹 정의를 그대로 옮긴다.** 하드코딩하지 않는다. 웹의 단위 테스트 케이스도 함께 옮겨 Kotlin 테스트로 만든다.
- 이식할 순수 로직(웹 `client/features/**`): `reservations/{model,dateKey,timeline-scale,weekLanes}` · `customers/{chosung,search-highlight,merge-suggestion,similar}` · `services/{model,default-services}` · `store-settings/{model,labels,naver-access}` · `booking/availability` · `assignees/model` · 웹 `utils/revenue.ts` 등.
- 화면 하나를 옮길 때마다 **웹 모바일 화면(375px)과 나란히 놓고** 탭 구성·행 정보·빈 상태 문구까지 대조한다.
- 웹의 함정 이력(`tas/index.md` 횡단 규칙)이 앱에도 해당되면 같은 방식으로 막는다. 날짜는 KST 기준 `dateKey`, 타임라인 배율은 한 곳에서만 얻는다.
- 공용 컴포넌트는 **만들기 전에 브리핑하고 승인을 받는다**(CLAUDE.md). S3가 그 브리핑 자리다.

---

## 작업 분할 (이슈 단위 · 위에서부터 순서대로)

> 각 항목 = 이슈 1개 = 브랜치 1개 = PR 1개(base `main`). 의존 관계는 `←`로 표시한다.
> **게스트(오프라인) 모드를 먼저** 완성한다. 로그인은 OAuth 키와 백엔드 변경을 기다려야 해서(🔒) 뒤로 미루고, 게스트 모드만으로 핵심 기능을 전부 쓸 수 있게 한다(웹 `shouldUseLocalDb()`·iOS와 동일).

### S — 기반
| # | 이슈 | 내용 | 의존 |
|---|---|---|---|
| S0 | 문서 정리 | ~~README 불일치 해소~~(✅ 완료), 버전 규약(`versionName` 0.0.0 · `versionCode` 1부터) 명시 | — |
| S1 | 프로젝트 골격 + CI | Gradle KTS(version catalog), `app` 모듈, Compose, 빈 화면 1개. GitHub Actions에 `assembleDebug`와 `testDebugUnitTest` 추가 | S0 |
| S2 | 디자인 토큰 | 웹 `styles/globalStyle.ts`의 색(`--brand-color #6526d9` 등)·폰트 스케일(10~18 + display 24/32/64)·반경·그림자를 Compose `Theme`로 옮긴다. 머티리얼 기본값을 덮어쓴다. 폰트 패밀리는 `fontStack.ts` 기준 | S1 |
| S3 | 공용 컴포넌트 브리핑 → 구현 | 웹 `components/ui/`에 대응하는 컴포넌트 목록과 만들어야 하는 이유를 **먼저 승인받는다**: `ServiceChip` · `ReservationStatusBadge`(`RESERVATION_STATUS_BADGE_STYLES`) · `NewCustomerBadge` · `Dot`/`ColorTag` · `ReservationInfoCard` · `ToggleSwitch` · `ConfirmDialog` · Toast · `LoadingOverlay` · `HighlightMatch` · `GuestNotice` | S2 |
| S4 | 도메인 모델 + 순수 로직 | `features/*/model.ts` 미러링(kotlinx.serialization)과 위 순수 로직 이식. 웹 테스트 케이스를 옮긴다 | S1 |
| S5 | 데이터 계층 | `ApiClient`(Bearer·401 시 토큰 폐기) · 게스트 로컬 스냅샷 · `Repository`가 게스트/로그인을 자동으로 분기 · `Loadable` 상태 | S4 |
| S6 | 앱 셸 | 하단 탭 4개(**캘린더·고객·매출·설정**, 웹 `MobileTabBar` 알약 스타일) + 헤더 + 네비게이션 | S2, S5 |

### A — 진입
| # | 이슈 | 내용 | 의존 |
|---|---|---|---|
| A1 | 로그인 화면 + 게스트 시작 | 웹 `/login` 레이아웃. 소셜 버튼 3개는 A4 전까지 비활성 | S6 |
| A2 | 약관 동의 게이트 | 웹 `/consent`(이용약관·개인정보·DPA). 약관 본문은 `/policies/:slug`를 앱 안에서 연다 | A1 |
| A3 | 온보딩 | 웹 `OnboardingStep1~5` 업종 선택 + 기본 서비스 시드(`default-services.ts`). 사람 진료 업종은 제외(동물병원은 유지) | A2, S5 |
| A4 | 🔒 로그인 (웹 위임) | Custom Tabs로 웹 `/login` → 앱 콜백 → `/api/mobile-auth/exchange`. **백엔드가 바뀌어야 한다**: `server/api/mobile-auth/complete.ts`의 콜백이 `tasios://auth/callback`으로 **고정돼 있어** 안드로이드로 돌아올 수 없다. 플랫폼 파라미터를 받아 허용 목록에서 고르게 하는 별도 tas 이슈로 다룬다. OAuth 키는 사용자가 준비해야 한다 | A1, tas 이슈 |
| A5 | 🔒 게스트 → 로그인 이관 | `/api/migrate-local`. 409 응답이면 확인 후 진행 | A4 |

### C — 캘린더 (웹 `/`, 기본 = 월 뷰)
| # | 이슈 | 내용 | 의존 |
|---|---|---|---|
| C1 | 월 뷰 + 하단 예약 리스트 | `Month`·`ReservationListSection`, 휴무(임시·정기) 배경 표시 | S6, S3 |
| C2 | 일 타임라인 | `Timeline`·`TimelineCluster`. 배율은 `timeline-scale` 한 곳에서만 얻는다. 2건 겹침은 나눠서, 3건 이상은 묶음으로 보여준다(웹 규칙) | C1 |
| C3 | 3일·주·년 뷰 | 주 뷰는 웹 모바일 기준 칼럼 64dp + 가로 스크롤 + 시간축 고정, 고객명만 표시. 열면 오늘이 가운데 오게 한다 | C2 |
| C4 | 예약 생성 | `ReservationCreate` + 고객 자동완성(초성) + 담당자 겹침 경고 + 예약 시점 결제. 연속 탭으로 고객이 중복 생성되지 않게 막는다 | C1 |
| C5 | 예약 상세·수정 | 상태(취소·노쇼·확정·거절) · 결제 레이어(적립금 사용·적립) · 변경 이력 · 삭제 | C4 |
| C6 | 헤더 검색 레이어 | 웹 `HeaderSearchLayer`(고객·예약 검색) | C5, D1 |
| C7 | 🔒 예약 요청함 벨 | `/api/book-requests`. 온라인 예약이 켜져 있고 로그인한 경우에만 노출 | C5, A4 |

> 드래그로 예약 이동·생성: 웹은 모바일(≤640px)에서 꺼 두었다. 웹 모바일을 따르므로 **앱에서도 만들지 않는다**.

### D — 고객 (웹 `/address`)
| # | 이슈 | 내용 | 의존 |
|---|---|---|---|
| D1 | 고객 명단 | 초성 검색 · 매치 하이라이트 · 메모 검색과 메모 노출 · 이름 정렬(`Intl.Collator ko numeric`). 행에 최근 시술 칩·적립금·4버킷 건수 배지 표시 | S6, S3 |
| D2 | 고객 상세·추가·수정 | `AddressCustomerSummary`·`Reservations`·`Tags`, `CustomerAddModal`. 예약 이력을 누르면 예약 상세로 간다 | D1, C5 |
| D3 | 적립금 충전 | `AddressCustomerRecharge`. 매장 설정에서 선불 충전이 켜졌을 때만 노출 | D2 |
| D4 | 고객 병합 + 병합 제안 | `customers-merge` · `CustomerMergeSuggestionModal`(`merge-suggestion.ts`·`similar.ts`). 후보에 예약 건수·적립금을 근거로 보여준다 | D2 |

### R — 매출 (웹 `/settings/revenue`, 하단 탭)
| # | 이슈 | 내용 | 의존 |
|---|---|---|---|
| R1 | 매출 | 기간(월·년) · KPI 5종 · 추세 차트 · 일자별 목록 · 담당자별 · 드릴다운. 금액은 `sumPrice(parseServiceString)`로 합산한다. iOS는 여기서 조합 시술을 0원으로 계산하는 버그가 있었다 | S6, C5 |

### F — 설정 (웹 `/menu` 허브 + `/settings/[tab]`)
| # | 이슈 | 내용 | 의존 |
|---|---|---|---|
| F1 | 설정 허브 | 웹 `menu.tsx`. 권한·기능 토글 게이팅은 `settingsMenu.ts`를 옮겨 쓴다. 고객센터·사용안내·로그아웃·약관 포함 | S6 |
| F2 | 매장 관리 | `StoreManageSection` 매장 정보·업종·기능 토글·영업시간·휴무 | F1 |
| F3 | 서비스 관리 | `ServiceManageSection`, 카테고리 색·농도(`buildServiceColorMap`) | F1 |
| F4 | 담당자 관리 | `AssigneeManageSection` 근무시간 요약(`summarizeSchedule`) · 병합 | F1 |
| F5 | 적립금 관리 | `PointSettingsTab`·`PointAdjustTab`·`PointHistoryTab` | F1, D2 |
| F6 | 공지사항 관리 | `NoticeManageSection` | F1 |
| F7 | 🔒 쿠폰·회원권 | 상품 CRUD + 발급·차감. 화면 전체가 로그인 전용이다(웹 `isLocal` 게이트) | F1, A4 |
| F8 | 🔒 고객 예약 설정 | `BookingManageSection` 슬러그 중복 확인 · 연락처 필수 · 안내문구 · 노출 서비스 | F1, A4 |
| F9 | 계정·고객센터 | `/mypage`(닉네임·탈퇴) · `/inquiry` 문의와 이력. 게스트는 로그아웃(데이터 삭제 경고)만 가능 | F1 |
| F10 | 🔒 SNS 연동 · 멤버/초대 · 매장 전환 | `SNSLinkingSection` · `MemberSection` · `StoreSwitcher` | A4 |
| F11 | 🔒 네이버예약 연동 | `naverBookingEnabled`가 true인 매장에만 노출한다. 확인 없이 붙이면 연동 대상이 아닌 매장에 보이고, 연결을 누르면 403이 난다 | A4 |

### P — 마감 · 출시
| # | 이슈 | 내용 |
|---|---|---|
| P1 | 접근성 점검 | TalkBack 라벨 · 48dp 터치 타깃 · 명도 대비 · 글자 크기 확대. 웹은 라이트 전용이므로 다크모드 지원 여부는 ❓ 결정 필요 |
| P2 | 스크린샷 CI | 에뮬레이터에 게스트 시드를 심고 캘린더를 캡처한다(iOS `ios-screenshot.yml`과 같은 방식). 시드 디코딩 테스트를 함께 둔다 |
| P3 | 🔒 출시 | Play Console 등록 · 앱 서명 키 · 내부 테스트 트랙 업로드 CI · 개인정보 처리방침 URL · 데이터 보안 섹션 |

---

## 진행 순서 요약
1. **S0~S6** 기반 → 2. **A1~A3** 게스트로 진입 → 3. **C1·C2·C4·C5** 예약 핵심 → 4. **D1~D2·R1** → 5. **F1~F6** → 6. 나머지 C·D 항목 → 7. 🔒 로그인 묶음(A4·A5·C7·F7·F8·F10·F11, tas 백엔드 이슈 선행) → 8. P

## 다음 단계
- 위 ❓ 항목(기술 스택 · 광고 배너 · 다크모드)을 확정한다.
- 확정되면 GitHub 이슈(에픽 + 서브이슈)를 만들고 S0부터 착수한다.

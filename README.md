# J.Park Lab

박정훈(제이팍)이 만드는 앱 모음.

## 바로스캔 (`www/`)
- 문서 스캔: 여러 장 촬영 → 자동 크롭·스캔 보정 → PDF 한 파일 / JPG
- 명함 스캔: 한 장 또는 여러 장 연속 촬영, 앞·뒷면 → 글자 인식 → 연락처에 바로 저장

### 안드로이드 앱 설치
최신 APK: https://github.com/pleasuroma/symmetrical-spork/releases/latest/download/baroscan.apk

`main`에 반영되면 GitHub Actions가 APK를 만들어 Releases에 올립니다 (`.github/workflows/android.yml`).
같은 서명키(`android-res/debug.keystore`)를 쓰므로 새 버전은 기존 앱 위에 덮어 설치됩니다.

웹 버전: https://pleasuroma.github.io/personal-homepage/scanner/

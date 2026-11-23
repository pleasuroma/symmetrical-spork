#!/usr/bin/env bash
# `npx cap add android` 직후 실행: 권한·아이콘·서명키를 넣는다.
set -euo pipefail
M=android/app/src/main/AndroidManifest.xml
PERMS='    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.READ_CONTACTS" />
    <uses-permission android:name="android.permission.WRITE_CONTACTS" />
    <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="32" />'
python3 - "$M" "$PERMS" <<'PY'
import sys
p,perms=sys.argv[1],sys.argv[2]
s=open(p).read()
s=s.replace('</manifest>',perms+'\n</manifest>',1)
s=s.replace('<application','<application\n        android:requestLegacyExternalStorage="true"',1)
open(p,'w').write(s)
PY
# 런처 아이콘 (적응형 아이콘 XML은 지워서 PNG를 쓰게 함)
RES=android/app/src/main/res
rm -rf "$RES/mipmap-anydpi-v26"
for d in mdpi hdpi xhdpi xxhdpi xxxhdpi; do cp android-res/mipmap-$d/*.png "$RES/mipmap-$d/"; done
# 매번 같은 서명키 → 새 버전을 지우지 않고 덮어 설치 가능
mkdir -p ~/.android && cp android-res/debug.keystore ~/.android/debug.keystore
grep -c "uses-permission" "$M"
# ML Kit 문서 스캐너 + 한글 글자 인식 (자체 플러그인)
J=android/app/src/main/java/com/jparklab/baroscan
cp android-src/*.java "$J/"
cat >> android/app/build.gradle <<'G'

dependencies {
    implementation 'com.google.mlkit:text-recognition-korean:16.0.1'
    implementation 'com.google.android.gms:play-services-mlkit-document-scanner:16.0.0-beta1'
}
G

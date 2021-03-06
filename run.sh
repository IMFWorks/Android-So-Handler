if [ $1 = "-" ]; then
  echo ---------------------------------------------------------更新load-assets-7z---------------------------------------------------------
  ./gradlew -P userPlugin=false :load-assets-7z:upload
fi
echo ---------------------------------------------------------开始构建APK---------------------------------------------------------
./gradlew :app:clean :app:assembleDebug
echo ---------------------------------------------------------  安装APK  ---------------------------------------------------------
adb install -r -t -d ./app/build/outputs/apk/debug/app-debug.apk
echo ---------------------------------------------------------  启动APK  ---------------------------------------------------------
adb shell am start  com.imf.test/.MainActivity
if [ $1 = "-rM" ]; then
  echo ---------------------------------------------------------开始恢复maven状态---------------------------------------------------------
  rm -rf ./maven/com
  git checkout HEAD maven
fi
echo ---------------------------------------------------------开始上传maven---------------------------------------------------------
./gradlew -P userPlugin=false clean
./gradlew -P userPlugin=false :android-un7z:upload
./gradlew -P userPlugin=false :file-plugin:upload
./gradlew -P userPlugin=false :load-hook:upload
./gradlew -P userPlugin=false :load-hook-plugin:upload
./gradlew -P userPlugin=false :load-assets-7z:upload
echo ---------------------------------------------------------上传maven完成---------------------------------------------------------
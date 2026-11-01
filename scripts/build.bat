@echo off
rem Builds the APK (release = R8 + shrunk resources, signed with the debug key).
rem Output: app\build\outputs\apk\release\app-release.apk  (or ...\debug\app-debug.apk)
rem Usage: scripts\build.bat             (release, pauses at the end)
rem        scripts\build.bat auto        (release, no pause)
rem        scripts\build.bat auto debug  (debug build, no pause)
setlocal EnableExtensions
cd /d "%~dp0.."
if not exist logs mkdir logs
> logs\build.log echo [%date% %time%] build starting
set "VARIANT=release"
if /i "%~1"=="debug" set "VARIANT=debug"
if /i "%~2"=="debug" set "VARIANT=debug"
set "TASK=assembleRelease"
if /i "%VARIANT%"=="debug" set "TASK=assembleDebug"

rem --- JDK: use JAVA_HOME, else Android Studio's bundled JBR
if not defined JAVA_HOME if exist "%ProgramFiles%\Android\Android Studio\jbr\bin\java.exe" set "JAVA_HOME=%ProgramFiles%\Android\Android Studio\jbr"
if not defined JAVA_HOME if exist "%LOCALAPPDATA%\Programs\Android Studio\jbr\bin\java.exe" set "JAVA_HOME=%LOCALAPPDATA%\Programs\Android Studio\jbr"

rem --- Android SDK
if not defined ANDROID_HOME if defined ANDROID_SDK_ROOT set "ANDROID_HOME=%ANDROID_SDK_ROOT%"
if not defined ANDROID_HOME if exist "%LOCALAPPDATA%\Android\Sdk" set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
if not defined ANDROID_HOME goto nosdk

if exist local.properties goto havelocal
set "SDKP=%ANDROID_HOME:\=/%"
> local.properties echo sdk.dir=%SDKP%
:havelocal

>> logs\build.log echo ANDROID_HOME=%ANDROID_HOME%
>> logs\build.log echo JAVA_HOME=%JAVA_HOME%
call gradlew.bat %TASK% --console=plain >> logs\build.log 2>&1
set "RC=%ERRORLEVEL%"
> logs\build.status echo %RC%
if "%RC%"=="0" goto ok
echo BUILD FAILED - see logs\build.log
goto end
:ok
echo BUILD OK: app\build\outputs\apk\%VARIANT%\app-%VARIANT%.apk
goto end

:nosdk
> logs\build.status echo 2
>> logs\build.log echo ANDROID_HOME not found. Is Android Studio installed?
echo ANDROID_HOME not found.

:end
if /i "%~1"=="auto" exit /b %RC%
pause
exit /b %RC%

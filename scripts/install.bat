@echo off
rem Installs the APK on the TV and makes Simple Keyboard TV the active input method.
rem The TV address (ip:port) is in scripts\tv.txt.
rem Usage: scripts\install.bat   |   scripts\install.bat auto   |   scripts\install.bat auto debug
rem Installs the release APK unless "debug" is given (build it first with scripts\build.bat).
setlocal EnableExtensions
cd /d "%~dp0.."
if not exist logs mkdir logs
set "LOG=logs\install.log"
set "VARIANT=release"
if /i "%~1"=="debug" set "VARIANT=debug"
if /i "%~2"=="debug" set "VARIANT=debug"
set "APK=app\build\outputs\apk\%VARIANT%\app-%VARIANT%.apk"
> "%LOG%" echo [%date% %time%] install starting

if not exist "%APK%" goto noapk
if not exist scripts\tv.txt goto notv
set /p TV=<scripts\tv.txt

set "ADB="
where adb >nul 2>&1 && set "ADB=adb"
if not defined ADB if defined ANDROID_HOME if exist "%ANDROID_HOME%\platform-tools\adb.exe" set "ADB=%ANDROID_HOME%\platform-tools\adb.exe"
if not defined ADB if exist "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" set "ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
if not defined ADB goto noadb

>> "%LOG%" echo == adb connect %TV%
"%ADB%" connect %TV% >> "%LOG%" 2>&1
"%ADB%" -s %TV% get-state >> "%LOG%" 2>&1
if errorlevel 1 goto notconnected

>> "%LOG%" echo == install
"%ADB%" -s %TV% install -r "%APK%" >> "%LOG%" 2>&1
if errorlevel 1 goto installfail

>> "%LOG%" echo == tv-setup.sh
"%ADB%" -s %TV% push scripts\tv-setup.sh /data/local/tmp/kb-setup.sh >> "%LOG%" 2>&1
"%ADB%" -s %TV% shell sh /data/local/tmp/kb-setup.sh >> "%LOG%" 2>&1

> logs\install.status echo 0
echo INSTALL OK. Details: %LOG%
goto end

:noapk
> logs\install.status echo 3
>> "%LOG%" echo No APK. Run scripts\build.bat first.
echo No APK. Run scripts\build.bat first.
goto end
:notv
> logs\install.status echo 4
>> "%LOG%" echo scripts\tv.txt missing. Put the TV address in it (example: 192.168.1.50:5555).
echo scripts\tv.txt missing.
goto end
:noadb
> logs\install.status echo 5
>> "%LOG%" echo adb not found. Is the Android SDK platform-tools folder installed?
echo adb not found.
goto end
:notconnected
> logs\install.status echo 6
echo Cannot reach the TV. Is network debugging on? Details: %LOG%
goto end
:installfail
> logs\install.status echo 7
echo Install failed. Details: %LOG%

:end
if /i "%~1"=="auto" exit /b 0
pause

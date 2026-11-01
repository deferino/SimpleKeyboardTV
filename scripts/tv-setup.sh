#!/system/bin/sh
# Runs ON the TV (adb shell). Enables and selects the keyboard, then compiles it ahead of time.
PKG=com.simplekeyboardtv.ime
IME=$PKG/rkr.simplekeyboard.inputmethod.latin.LatinIME

echo "enable + select keyboard..."
ime enable $IME 2>&1
ime set $IME 2>&1

echo "ahead-of-time compile (faster cold start)..."
cmd package compile -m speed -f $PKG 2>&1

echo "--- result ---"
echo "default_input_method=$(settings get secure default_input_method)"
echo "enabled_input_methods=$(settings get secure enabled_input_methods)"
echo "--- package ---"
pm list packages | grep -F "$PKG"

/*
 * Copyright (C) 2014 The Android Open Source Project
 * Copyright (C) 2025 Raimondas Rimkus
 * Copyright (C) 2024 wittmane
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rkr.simplekeyboard.inputmethod.latin.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.SwitchPreference;
import java.util.Map;

import rkr.simplekeyboard.inputmethod.R;
import rkr.simplekeyboard.inputmethod.keyboard.KeyboardTheme;

/**
 * "Appearance" settings sub screen.
 */
public final class AppearanceSettingsFragment extends SubScreenFragment {
    @Override
    public void onCreate(final Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.prefs_screen_appearance);

        setupGboardTvPreset();
        setupKeyboardWidthSettings();
        setupKeyboardHeightSettings();
        setupBottomOffsetPortraitSettings();
        setupKeyboardColorSettings();
    }

    @Override
    public void onResume() {
        super.onResume();

        updateGboardTvSwitch();
        ThemeSettingsFragment.updateKeyboardThemeSummary(findPreference(Settings.SCREEN_THEME));

        final Preference colorPreference = findPreference(Settings.PREF_KEYBOARD_COLOR);
        if (colorPreference.isEnabled()) {
            final SharedPreferences prefs = getSharedPreferences();
            final KeyboardTheme theme = KeyboardTheme.getKeyboardTheme(prefs);
            colorPreference.setEnabled(theme.mCustomColorSupport);
        }
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences prefs, final String key) {
        if (!mApplyingGboardTvStyle && isGboardTvKey(key)) {
            updateGboardTvSwitch();
        }
        if (KeyboardTheme.KEYBOARD_THEME_KEY.equals(key)) {
            ThemeSettingsFragment.updateKeyboardThemeSummary(findPreference(Settings.SCREEN_THEME));

            final KeyboardTheme theme = KeyboardTheme.getKeyboardTheme(prefs);
            setPreferenceEnabled(Settings.PREF_KEYBOARD_COLOR, theme.mCustomColorSupport);
        }
    }

    // The settings that make up the "Gboard TV style".
    private static final String[] GBOARD_TV_KEYS = {
        Settings.PREF_CENTERED_KEYBOARD,
        Settings.PREF_CENTERED_FLOATING,
        Settings.PREF_KEYBOARD_WIDTH,
        Settings.PREF_KEYBOARD_HEIGHT,
        Settings.PREF_BOTTOM_OFFSET_PORTRAIT,
        Settings.PREF_SHOW_NUMBER_ROW,
        Settings.PREF_SHOW_SPECIAL_CHARS,
    };
    // Where the settings from before the style was switched on are kept, so that switching it
    // off brings them back.
    private static final String PREF_GBOARD_TV_BACKUP = "pref_gboard_tv_backup";

    private boolean mApplyingGboardTvStyle;

    /**
     * "Gboard TV style" switch: centered compact keyboard, 80% wide, 70% high, number row on,
     * special characters off. The chosen theme and custom colour are left alone. The switch
     * always reflects the real settings: it turns itself off when one of them is changed by hand.
     * Switching it off restores the settings from before, or the standard full-width keyboard
     * when there are none saved.
     */
    private void setupGboardTvPreset() {
        final SwitchPreference pref = (SwitchPreference)findPreference(
                Settings.PREF_GBOARD_TV_PRESET);
        if (pref == null) {
            return;
        }
        pref.setChecked(isGboardTvStyleActive());
        pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(final Preference preference, final Object newValue) {
                mApplyingGboardTvStyle = true;
                try {
                    if ((Boolean)newValue) {
                        applyGboardTvStyle();
                    } else {
                        leaveGboardTvStyle();
                    }
                } finally {
                    mApplyingGboardTvStyle = false;
                }
                refreshAfterGboardTvStyleChange();
                return true;
            }
        });
    }

    private static boolean isGboardTvKey(final String key) {
        for (final String gboardKey : GBOARD_TV_KEYS) {
            if (gboardKey.equals(key)) {
                return true;
            }
        }
        return false;
    }

    private boolean isGboardTvStyleActive() {
        final SharedPreferences prefs = getSharedPreferences();
        return Settings.readCenteredKeyboardEnabled(prefs)
                && !Settings.readCenteredFloatingEnabled(prefs)
                && isSameScale(Settings.readKeyboardWidth(prefs, SettingsValues.DEFAULT_WIDTH_SCALE),
                        SettingsValues.DEFAULT_WIDTH_SCALE)
                && isSameScale(Settings.readKeyboardHeight(prefs, SettingsValues.DEFAULT_HEIGHT_SCALE),
                        SettingsValues.DEFAULT_HEIGHT_SCALE)
                && Settings.readBottomOffsetPortrait(prefs) == Settings.DEFAULT_BOTTOM_OFFSET
                && Settings.readShowNumberRow(prefs)
                && !Settings.readShowSpecialChars(prefs);
    }

    private static boolean isSameScale(final float a, final float b) {
        return Math.abs(a - b) < 0.001f;
    }

    private void applyGboardTvStyle() {
        final SharedPreferences prefs = getSharedPreferences();
        final SharedPreferences.Editor editor = prefs.edit();
        if (!isGboardTvStyleActive()) {
            // Remember what the person had, so that switching the style off can bring it back.
            final Map<String, ?> all = prefs.getAll();
            final StringBuilder backup = new StringBuilder();
            for (final String key : GBOARD_TV_KEYS) {
                final Object value = all.get(key);
                if (value != null) {
                    backup.append(key).append('=').append(value).append(';');
                }
            }
            editor.putString(PREF_GBOARD_TV_BACKUP, backup.toString());
        }
        // Width, height and bottom offset go back to their defaults (80%, 70%, 0 dp).
        editor.remove(Settings.PREF_KEYBOARD_WIDTH)
                .remove(Settings.PREF_KEYBOARD_HEIGHT)
                .remove(Settings.PREF_BOTTOM_OFFSET_PORTRAIT)
                .putBoolean(Settings.PREF_CENTERED_KEYBOARD, true)
                .putBoolean(Settings.PREF_CENTERED_FLOATING, false)
                .putBoolean(Settings.PREF_SHOW_NUMBER_ROW, true)
                .putBoolean(Settings.PREF_SHOW_SPECIAL_CHARS, false)
                .apply();
    }

    private void leaveGboardTvStyle() {
        final SharedPreferences prefs = getSharedPreferences();
        final String backup = prefs.getString(PREF_GBOARD_TV_BACKUP, null);
        final SharedPreferences.Editor editor = prefs.edit();
        for (final String key : GBOARD_TV_KEYS) {
            editor.remove(key);
        }
        if (backup != null) {
            for (final String entry : backup.split(";")) {
                final int separator = entry.indexOf('=');
                if (separator <= 0) {
                    continue;
                }
                final String key = entry.substring(0, separator);
                final String value = entry.substring(separator + 1);
                try {
                    if (Settings.PREF_KEYBOARD_WIDTH.equals(key)
                            || Settings.PREF_KEYBOARD_HEIGHT.equals(key)) {
                        editor.putFloat(key, Float.parseFloat(value));
                    } else if (Settings.PREF_BOTTOM_OFFSET_PORTRAIT.equals(key)) {
                        editor.putInt(key, Integer.parseInt(value));
                    } else {
                        editor.putBoolean(key, Boolean.parseBoolean(value));
                    }
                } catch (final NumberFormatException ignored) {
                    // Skip a damaged entry; the setting falls back to its default.
                }
            }
            editor.remove(PREF_GBOARD_TV_BACKUP);
        } else {
            // Nothing saved: the standard Simple Keyboard look (full width and height).
            editor.putBoolean(Settings.PREF_CENTERED_KEYBOARD, false)
                    .putFloat(Settings.PREF_KEYBOARD_WIDTH, 1.0f)
                    .putFloat(Settings.PREF_KEYBOARD_HEIGHT, 1.0f)
                    .putBoolean(Settings.PREF_SHOW_NUMBER_ROW, false)
                    .putBoolean(Settings.PREF_SHOW_SPECIAL_CHARS, true);
        }
        editor.apply();
    }

    /** Updates the other preferences on this screen after the style was switched. */
    private void refreshAfterGboardTvStyleChange() {
        final Preference centered = findPreference(Settings.PREF_CENTERED_KEYBOARD);
        final Preference floating = findPreference(Settings.PREF_CENTERED_FLOATING);
        final SharedPreferences prefs = getSharedPreferences();
        if (centered instanceof SwitchPreference) {
            ((SwitchPreference)centered).setChecked(Settings.readCenteredKeyboardEnabled(prefs));
        }
        if (floating instanceof SwitchPreference) {
            ((SwitchPreference)floating).setChecked(Settings.readCenteredFloatingEnabled(prefs));
        }
        // Refresh the summaries of the seek bar preferences.
        setupKeyboardWidthSettings();
        setupKeyboardHeightSettings();
        setupBottomOffsetPortraitSettings();
    }

    private void updateGboardTvSwitch() {
        final Preference pref = findPreference(Settings.PREF_GBOARD_TV_PRESET);
        if (pref instanceof SwitchPreference) {
            ((SwitchPreference)pref).setChecked(isGboardTvStyleActive());
        }
    }

    private void setupKeyboardWidthSettings() {
        final SeekBarDialogPreference pref = (SeekBarDialogPreference)findPreference(
                Settings.PREF_KEYBOARD_WIDTH);
        if (pref == null) {
            return;
        }
        final SharedPreferences prefs = getSharedPreferences();
        final Resources res = getResources();
        pref.setInterface(new SeekBarDialogPreference.ValueProxy() {
            private static final float PERCENTAGE_FLOAT = 100.0f;

            private float getValueFromPercentage(final int percentage) {
                return percentage / PERCENTAGE_FLOAT;
            }

            private int getPercentageFromValue(final float floatValue) {
                return Math.round(floatValue * PERCENTAGE_FLOAT);
            }

            @Override
            public void writeValue(final int value, final String key) {
                prefs.edit().putFloat(key, getValueFromPercentage(value)).apply();
            }

            @Override
            public void writeDefaultValue(final String key) {
                prefs.edit().remove(key).apply();
            }

            @Override
            public int readValue(final String key) {
                return getPercentageFromValue(Settings.readKeyboardWidth(
                        prefs, SettingsValues.DEFAULT_WIDTH_SCALE));
            }

            @Override
            public int readDefaultValue(final String key) {
                return getPercentageFromValue(SettingsValues.DEFAULT_WIDTH_SCALE);
            }

            @Override
            public String getValueText(final int value) {
                if (value < 0) {
                    return res.getString(R.string.settings_system_default);
                }
                return res.getString(R.string.abbreviation_unit_percent, value);
            }

            @Override
            public void feedbackValue(final int value) {}
        });
    }

    private void setupKeyboardHeightSettings() {
        final SeekBarDialogPreference pref = (SeekBarDialogPreference)findPreference(
                Settings.PREF_KEYBOARD_HEIGHT);
        if (pref == null) {
            return;
        }
        final SharedPreferences prefs = getSharedPreferences();
        final Resources res = getResources();
        pref.setInterface(new SeekBarDialogPreference.ValueProxy() {
            private static final float PERCENTAGE_FLOAT = 100.0f;

            private float getValueFromPercentage(final int percentage) {
                return percentage / PERCENTAGE_FLOAT;
            }

            private int getPercentageFromValue(final float floatValue) {
                return Math.round(floatValue * PERCENTAGE_FLOAT);
            }

            @Override
            public void writeValue(final int value, final String key) {
                prefs.edit().putFloat(key, getValueFromPercentage(value)).apply();
            }

            @Override
            public void writeDefaultValue(final String key) {
                prefs.edit().remove(key).apply();
            }

            @Override
            public int readValue(final String key) {
                return getPercentageFromValue(Settings.readKeyboardHeight(
                        prefs, SettingsValues.DEFAULT_HEIGHT_SCALE));
            }

            @Override
            public int readDefaultValue(final String key) {
                return getPercentageFromValue(SettingsValues.DEFAULT_HEIGHT_SCALE);
            }

            @Override
            public String getValueText(final int value) {
                if (value < 0) {
                    return res.getString(R.string.settings_system_default);
                }
                return res.getString(R.string.abbreviation_unit_percent, value);
            }

            @Override
            public void feedbackValue(final int value) {}
        });
    }

    private void setupBottomOffsetPortraitSettings() {
        final SeekBarDialogPreference pref = (SeekBarDialogPreference)findPreference(
                Settings.PREF_BOTTOM_OFFSET_PORTRAIT);
        if (pref == null) {
            return;
        }
        final SharedPreferences prefs = getSharedPreferences();
        final Resources res = getResources();
        pref.setInterface(new SeekBarDialogPreference.ValueProxy() {
            @Override
            public void writeValue(final int value, final String key) {
                prefs.edit().putInt(key, value).apply();
            }

            @Override
            public void writeDefaultValue(final String key) {
                prefs.edit().remove(key).apply();
            }

            @Override
            public int readValue(final String key) {
                return Settings.readBottomOffsetPortrait(prefs);
            }

            @Override
            public int readDefaultValue(final String key) {
                return Settings.DEFAULT_BOTTOM_OFFSET;
            }

            @Override
            public String getValueText(final int value) {
                if (value < 0) {
                    return res.getString(R.string.settings_system_default);
                }
                return res.getString(R.string.abbreviation_unit_dp, value);
            }

            @Override
            public void feedbackValue(final int value) {}
        });
    }

    private void setupKeyboardColorSettings() {
        final ColorDialogPreference pref = (ColorDialogPreference)findPreference(
                Settings.PREF_KEYBOARD_COLOR);
        if (pref == null) {
            return;
        }
        final SharedPreferences prefs = getSharedPreferences();
        final Context context = this.getActivity().getApplicationContext();
        pref.setInterface(new ColorDialogPreference.ValueProxy() {
            @Override
            public void writeValue(final int value, final String key) {
                prefs.edit().putInt(key, value).apply();
            }

            @Override
            public int readValue(final String key) {
                return Settings.readKeyboardColor(prefs, context);
            }

            @Override
            public void writeDefaultValue(final String key) {
                Settings.removeKeyboardColor(prefs);
            }
        });
    }
}

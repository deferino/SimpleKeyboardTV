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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.SwitchPreference;
import android.widget.Toast;

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
        if (KeyboardTheme.KEYBOARD_THEME_KEY.equals(key)) {
            ThemeSettingsFragment.updateKeyboardThemeSummary(findPreference(Settings.SCREEN_THEME));

            final KeyboardTheme theme = KeyboardTheme.getKeyboardTheme(prefs);
            setPreferenceEnabled(Settings.PREF_KEYBOARD_COLOR, theme.mCustomColorSupport);
        }
    }

    /**
     * One-press "Gboard TV style": centered compact keyboard, 80% wide, 70% high, number row on,
     * special characters off. The chosen theme and custom colour are left alone.
     */
    private void setupGboardTvPreset() {
        final Preference pref = findPreference(Settings.PREF_GBOARD_TV_PRESET);
        if (pref == null) {
            return;
        }
        pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.gboard_tv_style)
                        .setMessage(R.string.gboard_tv_style_confirm)
                        .setPositiveButton(android.R.string.ok,
                                new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialog, final int which) {
                                applyGboardTvPreset();
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
                return true;
            }
        });
    }

    private void applyGboardTvPreset() {
        // Width, height and bottom offset are reset to their defaults (80%, 70%, 0 dp).
        getSharedPreferences().edit()
                .remove(Settings.PREF_KEYBOARD_WIDTH)
                .remove(Settings.PREF_KEYBOARD_HEIGHT)
                .remove(Settings.PREF_BOTTOM_OFFSET_PORTRAIT)
                .putBoolean(Settings.PREF_SHOW_NUMBER_ROW, true)
                .putBoolean(Settings.PREF_SHOW_SPECIAL_CHARS, false)
                .apply();
        final Preference centered = findPreference(Settings.PREF_CENTERED_KEYBOARD);
        if (centered instanceof SwitchPreference) {
            ((SwitchPreference)centered).setChecked(true);
        }
        final Preference floating = findPreference(Settings.PREF_CENTERED_FLOATING);
        if (floating instanceof SwitchPreference) {
            ((SwitchPreference)floating).setChecked(false);
        }
        // Refresh the summaries of the seek bar preferences.
        setupKeyboardWidthSettings();
        setupKeyboardHeightSettings();
        setupBottomOffsetPortraitSettings();
        Toast.makeText(getActivity(), R.string.gboard_tv_style_applied, Toast.LENGTH_SHORT).show();
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

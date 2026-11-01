/*
 * Copyright (C) 2014 The Android Open Source Project
 * Copyright (C) 2021 wittmane
 * Copyright (C) 2021 Raimondas Rimkus
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
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;

import java.util.Comparator;
import java.util.Locale;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import rkr.simplekeyboard.inputmethod.R;
import rkr.simplekeyboard.inputmethod.compat.MenuItemIconColorCompat;
import rkr.simplekeyboard.inputmethod.latin.Subtype;
import rkr.simplekeyboard.inputmethod.latin.RichInputMethodManager;
import rkr.simplekeyboard.inputmethod.latin.common.LocaleUtils;
import rkr.simplekeyboard.inputmethod.latin.utils.LocaleResourceUtils;
import rkr.simplekeyboard.inputmethod.latin.utils.SubtypeLocaleUtils;

import static rkr.simplekeyboard.inputmethod.latin.settings.SingleLanguageSettingsFragment.LOCALE_BUNDLE_KEY;

/**
 * "Languages" settings sub screen.
 */
public final class LanguagesSettingsFragment extends PreferenceFragment {
    private static final String TAG = LanguagesSettingsFragment.class.getSimpleName();

    private static final boolean DEBUG_SUBTYPE_ID = false;

    private RichInputMethodManager mRichImm;
    private CharSequence[] mUsedLocaleNames;
    private String[] mUsedLocaleValues;
    private CharSequence[] mUnusedLocaleNames;
    private String[] mUnusedLocaleValues;
    private AlertDialog mAlertDialog;
    private View mView;

    @Override
    public void onCreate(final Bundle icicle) {
        super.onCreate(icicle);
        RichInputMethodManager.init(getActivity());
        mRichImm = RichInputMethodManager.getInstance();

        addPreferencesFromResource(R.xml.empty_settings);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        mView = super.onCreateView(inflater, container, savedInstanceState);
        return mView;
    }

    @Override
    public void onStart() {
        super.onStart();
        buildContent();
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        inflater.inflate(R.menu.remove_language, menu);
        inflater.inflate(R.menu.add_language, menu);

        MenuItem addLanguageMenuItem = menu.findItem(R.id.action_add_language);
        MenuItemIconColorCompat.matchMenuIconColor(mView, addLanguageMenuItem,
                getActivity().getActionBar());
        MenuItem removeLanguageMenuItem = menu.findItem(R.id.action_remove_language);
        MenuItemIconColorCompat.matchMenuIconColor(mView, removeLanguageMenuItem,
                getActivity().getActionBar());
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == R.id.action_add_language) {
            showAddLanguagePopup();
        } else if (itemId == R.id.action_remove_language) {
            showRemoveLanguagePopup();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPrepareOptionsMenu (Menu menu) {
        if (mUsedLocaleNames != null) {
            menu.findItem(R.id.action_remove_language).setVisible(mUsedLocaleNames.length > 1);
        }
    }

    /**
     * Build the preferences and them to this settings screen.
     */
    private void buildContent() {
        final Context context = getActivity();
        final PreferenceGroup group = getPreferenceScreen();
        group.removeAll();

        final PreferenceCategory languageCategory = new PreferenceCategory(context);
        languageCategory.setTitle(R.string.user_languages);
        group.addPreference(languageCategory);

        final Comparator<Locale> comparator = new LocaleUtils.LocaleComparator();
        final Set<Subtype> enabledSubtypes = mRichImm.getEnabledSubtypes(false);
        final SortedSet<Locale> usedLocales = getUsedLocales(enabledSubtypes, comparator);
        final SortedSet<Locale> unusedLocales = getUnusedLocales(usedLocales, comparator);

        buildLanguagePreferences(usedLocales, group, context);
        setLocaleEntries(usedLocales, unusedLocales);
        buildActionPreferences(group, context, !unusedLocales.isEmpty(), usedLocales.size() > 1);
    }

    /**
     * Add "Add language" / "Remove language" as regular list items. The action bar buttons are
     * not shown on many TVs and cannot be reached with a remote, so these work with the D-pad.
     */
    private void buildActionPreferences(final PreferenceGroup group, final Context context,
                                        final boolean canAdd, final boolean canRemove) {
        if (canAdd) {
            final Preference addPreference = new Preference(context);
            addPreference.setTitle(R.string.add_language);
            addPreference.setOnPreferenceClickListener(
                    new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(final Preference preference) {
                            showAddLanguagePopup();
                            return true;
                        }
                    });
            group.addPreference(addPreference);
        }
        if (canRemove) {
            final Preference removePreference = new Preference(context);
            removePreference.setTitle(R.string.remove_language);
            removePreference.setOnPreferenceClickListener(
                    new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(final Preference preference) {
                            showRemoveLanguagePopup();
                            return true;
                        }
                    });
            group.addPreference(removePreference);
        }
    }

    /**
     * Get all of the unique languages from the subtypes that have been enabled.
     * @param subtypes the list of subtypes for this IME that have been enabled.
     * @param comparator the comparator to sort the languages.
     * @return a set of locales for the used languages sorted using the specified comparator.
     */
    private SortedSet<Locale> getUsedLocales(final Set<Subtype> subtypes,
                                             final Comparator<Locale> comparator) {
        final SortedSet<Locale> locales = new TreeSet<>(comparator);

        for (final Subtype subtype : subtypes) {
            if (DEBUG_SUBTYPE_ID) {
                Log.d(TAG, String.format("Enabled subtype: %-6s 0x%08x %11d %s",
                        subtype.getLocale(), subtype.hashCode(), subtype.hashCode(),
                        subtype.getName()));
            }
            locales.add(subtype.getLocaleObject());
        }

        return locales;
    }

    /**
     * Get the list of languages supported by this IME that aren't included in
     * {@link #getUsedLocales}.
     * @param usedLocales the used locales.
     * @param comparator the comparator to sort the languages.
     * @return a set of locales for the unused languages sorted using the specified comparator.
     */
    private SortedSet<Locale> getUnusedLocales(final Set<Locale> usedLocales,
                                               final Comparator<Locale> comparator) {
        final SortedSet<Locale> locales = new TreeSet<>(comparator);
        for (String localeString : SubtypeLocaleUtils.getSupportedLocales()) {
            final Locale locale = LocaleUtils.constructLocaleFromString(localeString);
            if (usedLocales.contains(locale)) {
                continue;
            }
            locales.add(locale);
        }
        return locales;
    }

    /**
     * Create a language preference for each of the specified locales in the preference group. These
     * preferences will be added to the group in the order of the locales that are passed in.
     * @param locales the locales to add preferences for.
     * @param group the preference group to add preferences to.
     * @param context the context for this application.
     */
    private void buildLanguagePreferences(final SortedSet<Locale> locales,
                                          final PreferenceGroup group, final Context context) {
        for (final Locale locale : locales) {
            final String localeString = LocaleUtils.getLocaleString(locale);
            final SingleLanguagePreference pref =
                    new SingleLanguagePreference(context, localeString);
            group.addPreference(pref);
        }
    }

    /**
     * Set the lists of used languages that can be removed and unused languages that can be added.
     * @param usedLocales the enabled locales for this IME.
     * @param unusedLocales the unused locales that are supported in this IME.
     */
    private void setLocaleEntries(final SortedSet<Locale> usedLocales,
                                  final SortedSet<Locale> unusedLocales) {
        mUsedLocaleNames = new CharSequence[usedLocales.size()];
        mUsedLocaleValues = new String[usedLocales.size()];
        int i = 0;
        for (Locale locale : usedLocales) {
            final String localeString = LocaleUtils.getLocaleString(locale);
            mUsedLocaleValues[i] = localeString;
            mUsedLocaleNames[i] =
                    LocaleResourceUtils.getLocaleDisplayNameInSystemLocale(localeString);
            i++;
        }

        mUnusedLocaleNames = new CharSequence[unusedLocales.size()];
        mUnusedLocaleValues = new String[unusedLocales.size()];
        i = 0;
        for (Locale locale : unusedLocales) {
            final String localeString = LocaleUtils.getLocaleString(locale);
            mUnusedLocaleValues[i] = localeString;
            mUnusedLocaleNames[i] =
                    LocaleResourceUtils.getLocaleDisplayNameInSystemLocale(localeString);
            i++;
        }
    }

    /**
     * Show the popup to add a new language.
     */
    private void showAddLanguagePopup() {
        showMultiChoiceDialog(mUnusedLocaleNames, R.string.add_language, R.string.add, true,
                new OnMultiChoiceDialogAcceptListener() {
                    @Override
                    public void onClick(boolean[] checkedItems) {
                        // enable the default layout for all of the checked languages
                        for (int i = 0; i < checkedItems.length; i++) {
                            if (!checkedItems[i]) {
                                continue;
                            }
                            final Subtype subtype = SubtypeLocaleUtils.getDefaultSubtype(
                                    mUnusedLocaleValues[i],
                                    LanguagesSettingsFragment.this.getResources());
                            mRichImm.addSubtype(subtype);
                        }

                        // refresh the list of enabled languages
                        getActivity().invalidateOptionsMenu();
                        buildContent();
                    }
                });
    }

    /**
     * Show the popup to remove an existing language.
     */
    private void showRemoveLanguagePopup() {
        showMultiChoiceDialog(mUsedLocaleNames, R.string.remove_language, R.string.remove, false,
                new OnMultiChoiceDialogAcceptListener() {
                    @Override
                    public void onClick(boolean[] checkedItems) {
                        // disable the layouts for all of the checked languages
                        for (int i = 0; i < checkedItems.length; i++) {
                            if (!checkedItems[i]) {
                                continue;
                            }
                            final Set<Subtype> subtypes =
                                    mRichImm.getEnabledSubtypesForLocale(mUsedLocaleValues[i]);
                            for (final Subtype subtype : subtypes) {
                                mRichImm.removeSubtype(subtype);
                            }
                        }

                        // refresh the list of enabled languages
                        getActivity().invalidateOptionsMenu();
                        buildContent();
                    }
                });
    }

    /**
     * Show a multi-select popup. The buttons sit in a column beside the list instead of below it:
     * the list can have about a hundred entries and on a TV remote the buttons below it could only
     * be reached after scrolling to the very end. Pressing right in the list moves to the positive
     * button.
     * @param names the list of the choice display names.
     * @param titleRes the title of the dialog.
     * @param positiveButtonRes the text for the positive button.
     * @param allowAllChecked whether the positive button should be enabled when all items are
     *                       checked.
     * @param listener the listener for when the user clicks the positive button.
     */
    private void showMultiChoiceDialog(final CharSequence[] names, final int titleRes,
                                       final int positiveButtonRes, final boolean allowAllChecked,
                                       final OnMultiChoiceDialogAcceptListener listener) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // the builder's context carries the dialog theme, so the buttons look like dialog buttons
        final Context context = builder.getContext();
        final DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        final int gap = (int) (8 * metrics.density);

        final ListView listView = new ListView(context);
        listView.setId(View.generateViewId());
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        listView.setAdapter(new ArrayAdapter<CharSequence>(context,
                android.R.layout.simple_list_item_multiple_choice, names));

        final Button positiveButton = createDialogButton(context, positiveButtonRes);
        positiveButton.setId(View.generateViewId());
        // disabled since nothing is checked by default
        positiveButton.setEnabled(false);
        final Button negativeButton = createDialogButton(context, android.R.string.cancel);

        listView.setNextFocusRightId(positiveButton.getId());
        positiveButton.setNextFocusLeftId(listView.getId());
        negativeButton.setNextFocusLeftId(listView.getId());

        final LinearLayout buttons = new LinearLayout(context);
        buttons.setOrientation(LinearLayout.VERTICAL);
        buttons.setGravity(Gravity.CENTER_VERTICAL);
        buttons.addView(positiveButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        buttons.addView(negativeButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.HORIZONTAL);
        content.setPadding(gap, 0, gap, gap);
        final int listHeight = (int) (metrics.heightPixels * 0.6f);
        content.addView(listView, new LinearLayout.LayoutParams(
                (int) (metrics.widthPixels * 0.4f), listHeight));
        content.addView(buttons, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, listHeight));

        final AlertDialog dialog = builder.setTitle(titleRes).setView(content).create();
        mAlertDialog = dialog;

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> parent, final View view,
                                    final int position, final long id) {
                // make sure the positive button is only enabled when at least one item is checked
                // and when not all of the items are checked (unless allowAllChecked is true)
                final SparseBooleanArray checked = listView.getCheckedItemPositions();
                boolean hasCheckedItem = false;
                boolean hasUncheckedItem = false;
                for (int i = 0; i < names.length; i++) {
                    if (checked.get(i)) {
                        hasCheckedItem = true;
                    } else {
                        hasUncheckedItem = true;
                    }
                }
                positiveButton.setEnabled(
                        hasCheckedItem && (hasUncheckedItem || allowAllChecked));
            }
        });
        positiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final SparseBooleanArray checked = listView.getCheckedItemPositions();
                final boolean[] checkedItems = new boolean[names.length];
                for (int i = 0; i < checkedItems.length; i++) {
                    checkedItems[i] = checked.get(i);
                }
                dialog.dismiss();
                listener.onClick(checkedItems);
            }
        });
        negativeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    private static Button createDialogButton(final Context context, final int textRes) {
        final Button button = new Button(context, null, android.R.attr.buttonBarButtonStyle);
        button.setText(textRes);
        button.setMinWidth((int) (112 * context.getResources().getDisplayMetrics().density));
        return button;
    }

    /**
     * Interface used to add some code to run when the positive button on a multi-select dialog is
     * clicked.
     */
    private interface OnMultiChoiceDialogAcceptListener {
        /**
         * Handler triggered when the positive button of the dialog is clicked.
         * @param checkedItems a list of whether each item in the dialog was checked.
         */
        void onClick(final boolean[] checkedItems);
    }

    /**
     * Preference to link to a language specific settings screen.
     */
    private static class SingleLanguagePreference extends Preference {
        private final String mLocale;
        private Bundle mExtras;

        /**
         * Create a new preference for a language.
         * @param context the context for this application.
         * @param localeString a string specification of a locale, in a format of "ll_cc_variant",
         *                     where "ll" is a language code, "cc" is a country code.
         */
        public SingleLanguagePreference(final Context context, final String localeString) {
            super(context);
            mLocale = localeString;

            setTitle(LocaleResourceUtils.getLocaleDisplayNameInSystemLocale(localeString));
            setFragment(SingleLanguageSettingsFragment.class.getName());
        }

        @Override
        public Bundle getExtras() {
            if (mExtras == null) {
                mExtras = new Bundle();
                mExtras.putString(LOCALE_BUNDLE_KEY, mLocale);
            }
            return mExtras;
        }

        @Override
        public Bundle peekExtras() {
            return mExtras;
        }
    }
}

/*
 * Project: Timeriffic
 * Copyright (C) 2008 ralfoide gmail com,
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.rdrrlabs.timeriffic.ui;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceChangeListener;
import android.view.WindowManager;

import com.alfray.timeriffic.R;
import com.rdrrlabs.timeriffic.core.profiles1.Columns;
import com.rdrrlabs.timeriffic.core.settings.SettingFactory;

/**
 * Displays preferences
 */
public class PrefsUI extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Have the system blur any windows behind this one.
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND,
                WindowManager.LayoutParams.FLAG_BLUR_BEHIND);

        setTitle(R.string.prefs_title);
        addPreferencesFromResource(R.xml.prefs);

        Preference useDataTogglePref = findPreference("use_data_toggle");
        if (useDataTogglePref != null) {
            useDataTogglePref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    SettingFactory.getInstance().forgetSetting(Columns.ACTION_DATA);
                    return true;
                }
            });
        }
    }
}

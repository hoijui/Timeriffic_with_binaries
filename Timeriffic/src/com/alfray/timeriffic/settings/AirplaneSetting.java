/*
 * Project: Timeriffic
 * Copyright (C) 2010 ralfoide gmail com,
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

package com.alfray.timeriffic.settings;

import com.alfray.timeriffic.R;
import com.alfray.timeriffic.actions.PrefToggle;
import com.alfray.timeriffic.profiles.Columns;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;

//-----------------------------------------------

public class AirplaneSetting implements ISetting {

    private static final boolean DEBUG = true;
    private static final String TAG = "AirplaneSetting";

    @Override
    public boolean isSupported(Context context) {
        return true;
    }

    @Override
    public Object createUi(Activity activity, String[] currentActions) {
        PrefToggle p = new PrefToggle(activity,
                        R.id.airplaneButton,
                        currentActions,
                        Columns.ACTION_AIRPLANE,
                        activity.getString(R.string.editaction_airplane));
        p.setEnabled(isSupported(activity), activity.getString(R.string.setting_not_supported));
        return p;
    }

    @Override
    public void collectUiResults(Object settingUi, StringBuilder outActions) {
        if (settingUi instanceof PrefToggle) {
            ((PrefToggle) settingUi).collectResult(outActions);
        }
    }

    @Override
    public String getActionLabel(Context context, String action) {
        try {
            int value = Integer.parseInt(action.substring(1));
            return context.getString(value > 0 ? R.string.timedaction_airplane_on :
                                                 R.string.timedaction_airplane_off);
        } catch (NumberFormatException e) {
            // ignore
        }
        return null;
    }

    @Override
    public void performAction(Context context, String action) {
        try {
            int value = Integer.parseInt(action.substring(1));
            change(context, value > 0);
        } catch (NumberFormatException e) {
            if (DEBUG) Log.d(TAG, "Perform action failed for " + action);
        }
    }

    /** Changes the airplane mode */
    private void change(Context context, boolean turnOn) {
        // Reference: settings source is in the cupcake gitweb tree at
        //   packages/apps/Settings/src/com/android/settings/AirplaneModeEnabler.java
        // http://android.git.kernel.org/?p=platform/packages/apps/Settings.git;a=blob;f=src/com/android/settings/AirplaneModeEnabler.java;h=f105712260fd7b2d7804460dd180d1d6cea01afa;hb=HEAD

        if (DEBUG) Log.d(TAG, "changeAirplaneMode: " + (turnOn ? "on" : "off"));

        try {
            // Change the system setting
            Settings.System.putInt(
                            context.getContentResolver(),
                            Settings.System.AIRPLANE_MODE_ON,
                            turnOn ? 1 : 0);

            // Post the intent
            Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
            intent.putExtra("state", turnOn);
            context.sendBroadcast(intent);
        } catch (Exception e) {
            if (DEBUG) Log.e(TAG, "Change failed", e);
        }

    }

}



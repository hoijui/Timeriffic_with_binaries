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

package com.rdrrlabs.timeriffic.core.settings;

import java.lang.reflect.Method;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.ServiceManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.alfray.timeriffic.R;
import com.android.internal.telephony.ITelephony;
import com.rdrrlabs.timeriffic.core.actions.PrefToggle;
import com.rdrrlabs.timeriffic.core.prefs.PrefsValues;
import com.rdrrlabs.timeriffic.core.profiles1.Columns;

//-----------------------------------------------

/**
 * An attempt to toggle data off by directly accessing ITelephony.enableDataConnectivity().
 * Requires permission android.permission.MODIFY_PHONE_STATE which is
 * unfortunately not granted (probably a signatureOrSystem permission).
 */
public class DataSetting implements ISetting {

    private static final boolean DEBUG = true;
    public static final String TAG = DataSetting.class.getSimpleName();

    private boolean mCheckSupported = true;
    private boolean mIsSupported = false;
    private boolean mIsEnabled = false;

    public boolean isSupported(Context context) {
        if (mCheckSupported) {
            try {
                if (!mIsEnabled) {
                    // check prefs to see if we should enable this.
                    PrefsValues pv = new PrefsValues(context);
                    mIsEnabled = pv.getUseDataToggle();
                }
                if (!mIsEnabled) {
                    mIsSupported = checkMinApiLevel(7);

                    if (mIsSupported) {
                        // Requires permission android.permission.MODIFY_PHONE_STATE which is
                        // usually not granted (a signatureOrSystem permission.)
                        mIsSupported = context.getPackageManager().checkPermission(
                                Manifest.permission.MODIFY_PHONE_STATE,
                                context.getPackageName()) == PackageManager.PERMISSION_GRANTED;
                    }
                    if (mIsSupported) {
                        ITelephony it = getITelephony(context);

                        // just check we can call one of the method. we don't need the info
                        it.isDataConnectivityPossible();

                        // check we have the methods we want to call
                        mIsSupported =
                            (it.getClass().getDeclaredMethod("disableDataConnectivity", (Class[]) null) != null) &&
                            (it.getClass().getDeclaredMethod("enableDataConnectivity",  (Class[]) null) != null);
                    }
                }
            } catch (Throwable e) {
                Log.d(TAG, "Missing Data toggle API");
                mIsSupported = false;
            } finally {
                mCheckSupported = false;
            }
        }
        return mIsSupported || mIsEnabled;
    }

    public Object createUi(Activity activity, String[] currentActions) {

        boolean supported = isSupported(activity);
        if (!mIsEnabled) {
            // New in version 1.9.14: if not enabled in the prefs, the UI
            // is not shown at all.
            // It's OK to return null here as EditActionUI just stores the
            // value as-is and collectUiResults() checks using instanceof
            // so it's null-safe.
            return null;
        }

        PrefToggle p = new PrefToggle(activity,
                        -1 /*button id*/,
                        currentActions,
                        Columns.ACTION_DATA,
                        activity.getString(R.string.editaction_data));
        p.setEnabled(supported,
                mIsEnabled ? activity.getString(R.string.setting_not_supported)
                           : activity.getString(R.string.setting_not_enabled));
        return p;
    }

    public void collectUiResults(Object settingUi, StringBuilder outActions) {
        if (settingUi instanceof PrefToggle) {
            ((PrefToggle) settingUi).collectResult(outActions);
        }
    }

    public String getActionLabel(Context context, String action) {
        try {
            int value = Integer.parseInt(action.substring(1));
            return context.getString(value > 0 ? R.string.timedaction_data_on :
                                                 R.string.timedaction_data_off);

        } catch (NumberFormatException e) {
            // ignore
        }
        return null;
    }

    public boolean performAction(Context context, String action) {
        try {
            int value = Integer.parseInt(action.substring(1));
            change(context, value > 0);
        } catch (Throwable e) {
            if (DEBUG) Log.e(TAG, "Perform action failed for " + action, e);
        }

        return true;
    }

    // ----

    private ITelephony getITelephony(Context context) {
        try {
            // Get the internal ITelephony proxy directly.
            ITelephony it = ITelephony.Stub.asInterface(ServiceManager.getService(Context.TELEPHONY_SERVICE));
            if (it != null) return it;
        } catch (Throwable t) {
            // Ignore any error, we'll retry differently below.
        }

        try {
            // Let's try harder, although this is unlikely to work if the previous one failed.
            TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (manager == null) return null;

            Class<? extends TelephonyManager> c = manager.getClass();

            Method getIT = c.getDeclaredMethod("getITelephony", (Class[]) null);
            getIT.setAccessible(true);
            Object t = getIT.invoke(manager, (Object[]) null);
            return (ITelephony) t;

        } catch (Throwable t) {
            Log.d(TAG, "Missing Data toggle API");
        }

        return null;
    }

    private boolean checkMinApiLevel(int minApiLevel) {
        // Build.SDK_INT is only in API 4 and we're still compatible with API 3
        try {
            int n = Integer.parseInt(Build.VERSION.SDK);
            return n >= minApiLevel;
        } catch (Exception e) {
            Log.d(TAG, "Failed to parse Build.VERSION.SDK=" + Build.VERSION.SDK, e);
        }
        return false;
    }

    private void change(Context context, boolean enabled) {
        // This requires permission android.permission.MODIFY_PHONE_STATE

        try {
            ITelephony it = getITelephony(context);
            if (it != null) {
                if (enabled) {
                    it.enableApnType("default");
                    it.enableDataConnectivity();
                } else {
                    it.disableDataConnectivity();
                    it.disableApnType("default");
                }
            }
        } catch (Throwable e) {
            // We're not supposed to get here since isSupported() should return false.
            if (DEBUG) Log.d(TAG, "Missing Data toggle API", e);
        }
    }
}



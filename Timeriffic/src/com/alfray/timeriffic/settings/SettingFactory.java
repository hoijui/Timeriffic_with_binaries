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

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.Context;

import com.alfray.timeriffic.profiles.Columns;

//-----------------------------------------------

public class SettingFactory {

    private static final SettingFactory sInstance = new SettingFactory();
    private final Map<Character, ISetting> mSettings = new HashMap<Character, ISetting>();

    public static SettingFactory getInstance() {
        return sInstance;
    }

    private SettingFactory() {
    }

    /**
     * Returns an {@link ISetting}. Never returns null.
     */
    public ISetting getSetting(char code) {
        ISetting s = mSettings.get(code);
        if (s != null) return s;

        switch(code) {
            case Columns.ACTION_RINGER:
                s = new RingerSetting();
                break;
            case Columns.ACTION_VIBRATE:
                s = new VibrateSetting();
                break;
            case Columns.ACTION_RING_VOLUME:
                s = new VolumeSetting();
                break;
            case Columns.ACTION_NOTIF_VOLUME:
                break;
            case Columns.ACTION_MEDIA_VOLUME:
                break;
            case Columns.ACTION_ALARM_VOLUME:
                break;
            case Columns.ACTION_BRIGHTNESS:
                s = new BrightnessSetting();
                break;
            case Columns.ACTION_WIFI:
                s = new WifiSetting();
                break;
            case Columns.ACTION_AIRPLANE:
                s = new AirplaneSetting();
                break;
            case Columns.ACTION_BLUETOOTH:
                s = new BluetoothSetting();
                break;
            case Columns.ACTION_APN_DROID:
                s = new ApnDroidSetting();
                break;
        }

        if (s == null) {
            s = new NullSetting();
        }

        if (s != null) mSettings.put(code, s);
        return s;
    }

    private static class NullSetting implements ISetting {
        @Override
        public boolean isSupported(Context context) {
            return false;
        }

        @Override
        public Object createUi(Activity activity, String[] currentActions) {
            return null;
        }

        @Override
        public void collectUiResults(Object settingUi, StringBuilder outActions) {
            // pass
        }


        @Override
        public String getActionLabel(Context context, String action) {
            return null;
        }

        @Override
        public void performAction(Context context, String action) {
            // pass
        }

    }
}


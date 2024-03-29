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

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnDismissListener;
import android.database.Cursor;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.SparseArray;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.CheckBox;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TimePicker;

import com.alfray.timeriffic.R;
import com.rdrrlabs.timeriffic.core.actions.PrefBase;
import com.rdrrlabs.timeriffic.core.actions.PrefEnum;
import com.rdrrlabs.timeriffic.core.actions.PrefPercent;
import com.rdrrlabs.timeriffic.core.actions.PrefPercentDialog;
import com.rdrrlabs.timeriffic.core.actions.TimedActionUtils;
import com.rdrrlabs.timeriffic.core.profiles1.Columns;
import com.rdrrlabs.timeriffic.core.profiles1.ProfilesDB;
import com.rdrrlabs.timeriffic.core.settings.SettingFactory;
import com.rdrrlabs.timeriffic.core.utils.AgentWrapper;
import com.rdrrlabs.timeriffic.core.utils.SettingsHelper;

public class EditActionUI extends ExceptionHandlerUI {

    private static boolean DEBUG = true;
    public static final String TAG = EditActionUI.class.getSimpleName();

    /** Extra long with the action prof_id (not index) to edit. */
    public static final String EXTRA_ACTION_ID = "action_id";

    /*package*/ static final int DIALOG_EDIT_PERCENT = 100;
    /** Maps dialog ids to their {@link PrefPercent} instance. */
    private final SparseArray<PrefPercent> mPercentDialogMap = new SparseArray<PrefPercent>();
    private long mActionId;

    private TimePicker mTimePicker;
    private SettingsHelper mSettingsHelper;
    private AgentWrapper mAgentWrapper;

    private PrefEnum mPrefRingerMode;
    private PrefEnum mPrefRingerVibrate;
    private PrefPercent mPrefRingerVolume;
    private PrefPercent mPrefNotifVolume;
    private PrefPercent mPrefMediaVolume;
    private PrefPercent mPrefAlarmVolume;
    private PrefPercent mPrefSystemVolume;
    private PrefPercent mPrefVoiceVolume;
    private PrefPercent mPrefBrightness;
    private Object mPrefAirplane;
    private Object mPrefWifi;
    private Object mPrefBluetooth;
    private Object mPrefApnDroid;
    private Object mPrefData;

    /**
     * Day checkboxes, in the same index order than {@link Columns#MONDAY_BIT_INDEX}
     * to {@link Columns#SUNDAY_BIT_INDEX}.
     */
    private CheckBox[] mCheckDays;

    private View mCurrentContextMenuView;
    private int mRestoreHourMinValue = -1;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.edit_action);
        setTitle(R.string.editaction_title);

        Intent intent = getIntent();
        mActionId = intent.getExtras().getLong(EXTRA_ACTION_ID);

        if (DEBUG) Log.d(TAG, String.format("edit prof_id: %08x", mActionId));

        if (mActionId == 0) {
            Log.e(TAG, "action id not found in intent.");
            finish();
            return;
        }

        mSettingsHelper = new SettingsHelper(this);
        SettingFactory factory = SettingFactory.getInstance();

        // get profiles db helper
        ProfilesDB profilesDb = new ProfilesDB();
        profilesDb.onCreate(this);

        // get cursor
        String prof_id_select = String.format("%s=%d", Columns.PROFILE_ID, mActionId);
        Cursor c = profilesDb.query(
                -1, // id
                // projection, a.k.a. the list of columns to retrieve from the db
                new String[] {
                        Columns.PROFILE_ID,
                        Columns.HOUR_MIN,
                        Columns.DAYS,
                        Columns.ACTIONS
                },
                prof_id_select, // selection
                null, // selectionArgs
                null // sortOrder
                );
        try {
            if (!c.moveToFirst()) {
                Log.w(TAG, "cursor is empty: " + prof_id_select);
                finish();
                return;
            }

            // get column indexes
            int hourMinColIndex = c.getColumnIndexOrThrow(Columns.HOUR_MIN);
            int daysColIndex = c.getColumnIndexOrThrow(Columns.DAYS);
            int actionsColIndex = c.getColumnIndexOrThrow(Columns.ACTIONS);


            String actions_str = c.getString(actionsColIndex);
            if (DEBUG) Log.d(TAG, String.format("Edit Action=%s", actions_str));

            String[] actions = actions_str != null ? actions_str.split(",") : null;

            // get UI widgets
            mTimePicker = (TimePicker) findViewById(R.id.timePicker);

            mPrefRingerMode = new PrefEnum(this,
                    R.id.ringerModeButton,
                    SettingsHelper.RingerMode.values(),
                    actions,
                    Columns.ACTION_RINGER,
                    getString(R.string.editaction_ringer));
            mPrefRingerMode.setEnabled(mSettingsHelper.canControlAudio(),
                    getString(R.string.setting_not_supported));

            mPrefRingerVibrate = new PrefEnum(this,
                    R.id.ringerVibButton,
                    SettingsHelper.VibrateRingerMode.values(),
                    actions,
                    Columns.ACTION_VIBRATE,
                    getString(R.string.editaction_vibrate));
            mPrefRingerVibrate.setEnabled(mSettingsHelper.canControlAudio(),
                    getString(R.string.setting_not_supported));

            mPrefRingerVolume = (PrefPercent) factory.getSetting(Columns.ACTION_RING_VOLUME).createUi(this, actions);
            int dialogId = DIALOG_EDIT_PERCENT;
            mPercentDialogMap.put(mPrefRingerVolume.setDialogId(++dialogId), mPrefRingerVolume);

            mPrefNotifVolume = (PrefPercent) factory.getSetting(Columns.ACTION_NOTIF_VOLUME).createUi(this, actions);
            mPercentDialogMap.put(mPrefNotifVolume.setDialogId(++dialogId), mPrefNotifVolume);

            mPrefMediaVolume = (PrefPercent) factory.getSetting(Columns.ACTION_MEDIA_VOLUME).createUi(this, actions);
            mPercentDialogMap.put(mPrefMediaVolume.setDialogId(++dialogId), mPrefMediaVolume);

            mPrefAlarmVolume = (PrefPercent) factory.getSetting(Columns.ACTION_ALARM_VOLUME).createUi(this, actions);
            mPercentDialogMap.put(mPrefAlarmVolume.setDialogId(++dialogId), mPrefAlarmVolume);

            mPrefSystemVolume = (PrefPercent) factory.getSetting(Columns.ACTION_SYSTEM_VOLUME).createUi(this, actions);
            mPercentDialogMap.put(mPrefSystemVolume.setDialogId(++dialogId), mPrefSystemVolume);

            mPrefVoiceVolume = (PrefPercent) factory.getSetting(Columns.ACTION_VOICE_CALL_VOLUME).createUi(this, actions);
            mPercentDialogMap.put(mPrefVoiceVolume.setDialogId(++dialogId), mPrefVoiceVolume);

            mPrefBrightness = (PrefPercent) factory.getSetting(Columns.ACTION_BRIGHTNESS).createUi(this, actions);
            mPercentDialogMap.put(mPrefBrightness.setDialogId(++dialogId), mPrefBrightness);

            mPrefBluetooth = factory.getSetting(Columns.ACTION_BLUETOOTH).createUi(this, actions);
            mPrefApnDroid  = factory.getSetting(Columns.ACTION_APN_DROID).createUi(this, actions);
            mPrefData      = factory.getSetting(Columns.ACTION_DATA).createUi(this, actions);
            mPrefWifi      = factory.getSetting(Columns.ACTION_WIFI).createUi(this, actions);
            mPrefAirplane  = factory.getSetting(Columns.ACTION_AIRPLANE).createUi(this, actions);

            mCheckDays = new CheckBox[] {
                    (CheckBox) findViewById(R.id.dayMon),
                    (CheckBox) findViewById(R.id.dayTue),
                    (CheckBox) findViewById(R.id.dayWed),
                    (CheckBox) findViewById(R.id.dayThu),
                    (CheckBox) findViewById(R.id.dayFri),
                    (CheckBox) findViewById(R.id.daySat),
                    (CheckBox) findViewById(R.id.daySun)
            };

            TextView[] labelDays = new TextView[] {
                    (TextView) findViewById(R.id.labelDayMon),
                    (TextView) findViewById(R.id.labelDayTue),
                    (TextView) findViewById(R.id.labelDayWed),
                    (TextView) findViewById(R.id.labelDayThu),
                    (TextView) findViewById(R.id.labelDayFri),
                    (TextView) findViewById(R.id.labelDaySat),
                    (TextView) findViewById(R.id.labelDaySun)
            };

            // fill in UI from cursor data

            // Update the time picker.
            // BUG WORKAROUND: when updating the timePicker here in onCreate, the timePicker
            // might override some values when it redisplays in onRestoreInstanceState so
            // we'll update there instead.
            mRestoreHourMinValue = c.getInt(hourMinColIndex);
            setTimePickerValue(mTimePicker, mRestoreHourMinValue);

            // Update days checked
            int days = c.getInt(daysColIndex);
            for (int i = Columns.MONDAY_BIT_INDEX; i <= Columns.SUNDAY_BIT_INDEX; i++) {
                mCheckDays[i].setChecked((days & (1<<i)) != 0);
            }

            String[] dayNames = TimedActionUtils.getDaysNames();
            for (int i = 0; i < dayNames.length; i++) {
                labelDays[i].setText(dayNames[i]);
            }

            mPrefRingerMode.requestFocus();
            ScrollView sv = (ScrollView) findViewById(R.id.scroller);
            sv.scrollTo(0, 0);

        } finally {
            c.close();
            profilesDb.onDestroy();
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        // Bug workaround. See mRestoreHourMinValue in onCreate.
        if (mRestoreHourMinValue >= 0) {
            setTimePickerValue(mTimePicker, mRestoreHourMinValue);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view,
                    ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);

        mCurrentContextMenuView = null;

        Object tag = view.getTag();
        if (tag instanceof PrefBase) {
            ((PrefBase) tag).onCreateContextMenu(menu);
            mCurrentContextMenuView = view;
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        if (mCurrentContextMenuView instanceof View) {
            Object tag = mCurrentContextMenuView.getTag();
            if (tag instanceof PrefBase) {
                ((PrefBase) tag).onContextItemSelected(item);
            }
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onContextMenuClosed(Menu menu) {
        super.onContextMenuClosed(menu);
        mCurrentContextMenuView = null;
    }

    @Override
    protected Dialog onCreateDialog(final int id) {

        PrefPercent pp = mPercentDialogMap.get(id);
        if (DEBUG) Log.d(TAG,
                String.format("Create dialog id=%d, pp=%s",
                        id,
                        pp == null ? "null" : pp.getDialogTitle()));
        if (pp != null) {
            PrefPercentDialog d = new PrefPercentDialog(this, pp);

            // We need to make sure to remove the dialog once it gets dismissed
            // otherwise the next use of the same dialog might reuse the previous
            // dialog from another setting!
            d.setOnDismissListener(new OnDismissListener() {
                public void onDismiss(DialogInterface dialog) {
                    removeDialog(id);
                }
            });
            return d;
        }

        return super.onCreateDialog(id);
    }

    // -----------


    @Override
    protected void onResume() {
        super.onResume();

        mAgentWrapper = new AgentWrapper();
        mAgentWrapper.start(this);
        mAgentWrapper.event(AgentWrapper.Event.OpenTimeActionUI);
    }


    @Override
    protected void onPause() {
        super.onPause();

        ProfilesDB profilesDb = new ProfilesDB();
        try {
            profilesDb.onCreate(this);

            int hourMin = getTimePickerHourMin(mTimePicker);

            int days = 0;

            for (int i = Columns.MONDAY_BIT_INDEX; i <= Columns.SUNDAY_BIT_INDEX; i++) {
                if (mCheckDays[i].isChecked()) {
                    days |= 1<<i;
                }
            }

            SettingFactory factory = SettingFactory.getInstance();
            StringBuilder actions = new StringBuilder();

            mPrefRingerMode.collectResult(actions);
            mPrefRingerVibrate.collectResult(actions);
            mPrefRingerVolume.collectResult(actions);
            mPrefNotifVolume.collectResult(actions);
            mPrefMediaVolume.collectResult(actions);
            mPrefAlarmVolume.collectResult(actions);
            mPrefSystemVolume.collectResult(actions);
            mPrefVoiceVolume.collectResult(actions);

            factory.getSetting(Columns.ACTION_BRIGHTNESS).collectUiResults(mPrefBrightness, actions);
            factory.getSetting(Columns.ACTION_BLUETOOTH).collectUiResults(mPrefBluetooth, actions);
            factory.getSetting(Columns.ACTION_APN_DROID).collectUiResults(mPrefApnDroid, actions);
            factory.getSetting(Columns.ACTION_DATA).collectUiResults(mPrefData, actions);
            factory.getSetting(Columns.ACTION_AIRPLANE).collectUiResults(mPrefAirplane, actions);
            factory.getSetting(Columns.ACTION_WIFI).collectUiResults(mPrefWifi, actions);

            if (DEBUG) Log.d(TAG, "new actions: " + actions.toString());

            String description = TimedActionUtils.computeDescription(
                    this, hourMin, days, actions.toString());

            int count = profilesDb.updateTimedAction(mActionId,
                    hourMin,
                    days,
                    actions.toString(),
                    description);

            if (DEBUG) Log.d(TAG, "written rows: " + Integer.toString(count));

        } finally {
            profilesDb.onDestroy();
        }

        mAgentWrapper.stop(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    // -----------


    private int getTimePickerHourMin(TimePicker timePicker) {

        // If the user was manually editing one of the time picker fields,
        // the internal time picker values might not have been properly
        // updated yet. Requesting a focus on the time picker forces it
        // to update by side-effect.
        timePicker.requestFocus();

        int hours = timePicker.getCurrentHour();
        int minutes = timePicker.getCurrentMinute();

        return hours*60 + minutes;
    }

    private void setTimePickerValue(TimePicker timePicker, int hourMin) {
        if (hourMin < 0) hourMin = 0;
        if (hourMin >= 24*60) hourMin = 24*60-1;
        int hours = hourMin / 60;
        int minutes = hourMin % 60;

        timePicker.setCurrentHour(hours);
        timePicker.setCurrentMinute(minutes);

        timePicker.setIs24HourView(DateFormat.is24HourFormat(this));
    }
}

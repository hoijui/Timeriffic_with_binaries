/*
 * Project: Timeriffic
 * Copyright (C) 2009 ralfoide gmail com,
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

package com.alfray.timeriffic.core.actions;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.text.SpannableStringBuilder;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.alfray.timeriffic.R;
import com.alfray.timeriffic.core.actions.PrefPercentDialog.Accessor;

//-----------------------------------------------

public class PrefPercent extends PrefBase implements View.OnClickListener {

    private char mActionPrefix;
    private Button mButton;

    /** Indicates the preference is set to "unchanged" */
    public final static int VALUE_UNCHANGED = -1;
    /** Indicates the preference is set to the custom choice */
    public final static int VALUE_CUSTOM_CHOICE = -2;

    /** One of {@link #VALUE_UNCHANGED}, {@link #VALUE_CUSTOM_CHOICE}, or 0..100 */
    private int mCurrentValue;

    private final String mDialogTitle;
    private final int mIconResId;

    private int mDialogId;
    private final Accessor mAccessor;
    private String mDisabledMessage;

    public PrefPercent(Activity activity,
                    int buttonResId,
                    String[] actions,
                    char actionPrefix,
                    String dialogTitle,
                    int iconResId,
                    PrefPercentDialog.Accessor accessor) {
        super(activity);
        mActionPrefix = actionPrefix;
        mDialogTitle = dialogTitle;
        mIconResId = iconResId;
        mAccessor = accessor;

        mButton = findButtonById(buttonResId);
        mButton.setOnClickListener(this);
        mButton.setTag(this);

        mCurrentValue = VALUE_UNCHANGED;
        initValue(actions, actionPrefix);
        updateButtonText();
    }

    @Override
    public void setEnabled(boolean enable, String disabledMessage) {
        mDisabledMessage = disabledMessage;
        mButton.setEnabled(enable);
        updateButtonText();
    }

    @Override
    public boolean isEnabled() {
        return mButton.isEnabled();
    }

    @Override
    public void requestFocus() {
        mButton.requestFocus();
    }

    public String getDialogTitle() {
        return mDialogTitle;
    }

    public int getIconResId() {
        return mIconResId;
    }

    public Accessor getAccessor() {
        return mAccessor;
    }

    /** Returns one of {@link #VALUE_UNCHANGED}, {@link #VALUE_CUSTOM_CHOICE}, or 0..100 */
    public int getCurrentValue() {
        return mCurrentValue;
    }

    /** Sets to one of {@link #VALUE_UNCHANGED}, {@link #VALUE_CUSTOM_CHOICE}, or 0..100 */
    public void setValue(int percent) {
        mCurrentValue = percent;
        updateButtonText();
    }

    private void initValue(String[] actions, char prefix) {

        String currentValue = getActionValue(actions, prefix);

        char customChoiceValue = mAccessor.getCustomChoiceValue();
        if (currentValue != null &&
                        currentValue.length() == 1 &&
                        currentValue.charAt(0) == customChoiceValue) {
            mCurrentValue = VALUE_CUSTOM_CHOICE;
        } else {
            try {
                mCurrentValue = Integer.parseInt(currentValue);
            } catch (Exception e) {
                mCurrentValue = VALUE_UNCHANGED;
            }
        }
    }

    private void updateButtonText() {
        Resources r = getActivity().getResources();

        String label = r.getString(R.string.percent_button_unchanged);
        int customStrId = mAccessor.getCustomChoiceButtonLabel();
        if (customStrId > 0 && mCurrentValue == VALUE_CUSTOM_CHOICE) {
            label = r.getString(customStrId);
        } else if (mCurrentValue >= 0) {
            label = String.format("%d%%", mCurrentValue);
        }

        CharSequence t = r.getText(R.string.editaction_button_label);

        SpannableStringBuilder sb = new SpannableStringBuilder(t);

        for (int i = 0; i < sb.length(); i++) {
            char c = sb.charAt(i);
            if (c == '@') {
                sb.replace(i, i + 1, mDialogTitle);
            } else if (c == '$') {
                if (!isEnabled() && mDisabledMessage != null) {
                    sb.replace(i, i + 1, mDisabledMessage);
                } else {
                    sb.replace(i, i + 1, label);
                }
            }
        }

        mButton.setText(sb);

        Drawable d = r.getDrawable(
                        mCurrentValue == VALUE_CUSTOM_CHOICE ? ID_DOT_EXTRA :
                            mCurrentValue < 0 ? ID_DOT_UNCHANGED : ID_DOT_PERCENT);
        mButton.setCompoundDrawablesWithIntrinsicBounds(
                d,    // left
                null, // top
                null, // right
                null  // bottom
                );
    }

    public void collectResult(StringBuilder actions) {
        if (isEnabled()) {
            char customChoiceValue = mAccessor.getCustomChoiceValue();
            if (customChoiceValue > 0 && mCurrentValue == VALUE_CUSTOM_CHOICE) {
                appendAction(actions, mActionPrefix, Character.toString(customChoiceValue));
            } else if (mCurrentValue >= 0) {
                appendAction(actions, mActionPrefix, Integer.toString(mCurrentValue));
            }
        }
    }

    @Override
    public void onContextItemSelected(MenuItem item) {
        // from PrefBase, not used here
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu) {
        // from PrefBase, not used here
    }

    @Override
    public void onClick(View v) {
        getActivity().showDialog(mDialogId);
    }

    public int setDialogId(int dialogId) {
        mDialogId = dialogId;
        return mDialogId;
    }
}

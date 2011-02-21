/*
 * (c) ralfoide gmail com, 2009
 * Project: Timeriffic
 * License TBD
 */

/**
 *
 */
package com.alfray.timeriffic.core.profiles1;

import android.view.View;
import android.widget.AbsListView.RecyclerListener;

/**
 * This {@link RecyclerListener} is attached to the profile list to
 * call {@link BaseHolder#clearCursor()} of the tags of the reclaimed
 * views. This should ensure that not dangling cursor reference exists.
 */
public class ProfileRecyclerListener implements RecyclerListener {
    @Override
    public void onMovedToScrapHeap(View view) {
        @SuppressWarnings("unused")
        Object tag = view.getTag();
        // pass... not doing anything anymore since BaseHolder doesn't
        // hold a cursor anymore. TODO remove later.
    }
}

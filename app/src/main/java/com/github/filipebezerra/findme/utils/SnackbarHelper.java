package com.github.filipebezerra.findme.utils;

import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.view.View;

/**
 * .
 *
 * @author Filipe Bezerra
 * @version #, 30/07/2015
 * @since #
 */
public final class SnackbarHelper {
    public static void show(@NonNull final View view, @NonNull final CharSequence message,
            final int duration) {
        Snackbar.make(view, message, duration).show();
    }
}

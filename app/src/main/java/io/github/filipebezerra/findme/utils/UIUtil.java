package io.github.filipebezerra.findme.utils;

import android.content.Context;
import android.support.annotation.NonNull;
import com.afollestad.materialdialogs.MaterialDialog;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;

/**
 * User interface helper methods.
 *
 * @author Filipe Bezerra
 * @version 1.0, 09/04/2015
 * @since #
 */
public class UIUtil {
    public static MaterialDialog showDialog(@NonNull final Context context, final String title,
            @NonNull final String message) {
        return showDialog(context, title, message, false);
    }

    public static MaterialDialog showProgress(@NonNull final Context context, final String title,
            @NonNull final String message) {
        return showDialog(context, title, message, true);
    }

    private static MaterialDialog showDialog(@NonNull final Context context, final String title,
            @NonNull final String message, final boolean progress) {
        MaterialDialog.Builder dialogBuilder = new MaterialDialog.Builder(context).content(message);

        if (title != null) {
            dialogBuilder.title(title);
        }

        if (progress) {
            dialogBuilder.progress(true, 0);
        }

        return dialogBuilder.show();
    }

    public static void showMessage(@NonNull final Context context, @NonNull final String message) {
        SnackbarManager.show(
                Snackbar.with(context).text(message)
        );
    }
}
package io.github.filipebezerra.findme;

import android.app.Application;
import timber.log.Timber;

/**
 * .
 *
 * @author Filipe Bezerra
 * @version #, 06/04/2015
 * @since #
 */
public class LastLocationApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Timber.plant(new Timber.DebugTree());
    }
}

package io.github.filipebezerra.findme.tasks;

import android.app.IntentService;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.text.TextUtils;
import io.github.filipebezerra.findme.R;
import io.github.filipebezerra.findme.utils.Constants;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import timber.log.Timber;

/**
 * .
 *
 * @author Filipe Bezerra
 * @version #, 06/04/2015
 * @since #
 */
public class FetchAddressIntentService extends IntentService {
    private static final String TAG = FetchAddressIntentService.class.getSimpleName();

    /**
     * The receiver where results are forwarded from this service.
     */
    private ResultReceiver mReceiver;

    public FetchAddressIntentService(){
        super(TAG);
        Timber.tag(TAG);
    }

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public FetchAddressIntentService(String name) {
        super(name);
        Timber.tag(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        mReceiver = intent.getParcelableExtra(Constants.RECEIVER);

        // Check if receiver was properly registered.
        if (mReceiver == null) {
            Timber.d(TAG, "No receiver received. There is nowhere to send the results.");
            return;
        }

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        String errorMessage = "";

        Location location = intent.getParcelableExtra(Constants.LOCATION_DATA_EXTRA);

        List<Address> addresses = null;

        try {
            addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
        } catch (IOException e) {
            // Catch network or other I/O problems.
            errorMessage = getString(R.string.service_not_available);
            Timber.e(e, "Getting reverse geocoding from location %s", location.toString());
        } catch (IllegalArgumentException  e) {
            // Catch invalid latitude or longitude values
            errorMessage = getString(R.string.invalid_lat_long_used);
            Timber.e(e, "Invalid latitude and longitude from location %s", location.toString());
        }

        if (addresses == null || addresses.size() == 0) {
            if (errorMessage.isEmpty()) {
                errorMessage = getString(R.string.no_address_found);
                Timber.e("None address found from location %s", location.toString());
            }

            deliverResultToReceiver(Constants.FAILURE_RESULT, errorMessage);
        } else {
            Address address = addresses.get(0);
            List<String> addressFragments = new ArrayList<>();

            for (int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                addressFragments.add(address.getAddressLine(i));
            }

            Timber.d("Address found from location %s", location.toString());
            deliverResultToReceiver(Constants.SUCCESS_RESULT,
                    TextUtils.join(System.getProperty("line.separator"), addressFragments));
        }
    }

    private void deliverResultToReceiver(int resultCode, String message) {
        Bundle bundle = new Bundle();
        bundle.putString(Constants.RESULT_DATA_KEY, message);

        mReceiver.send(resultCode, bundle);
    }
}

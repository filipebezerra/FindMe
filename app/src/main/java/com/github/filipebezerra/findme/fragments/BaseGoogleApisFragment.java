package com.github.filipebezerra.findme.fragments;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.View;
import com.github.filipebezerra.findme.tasks.FetchAddressIntentService;
import com.github.filipebezerra.findme.utils.Constants;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import java.text.DateFormat;
import java.util.Date;
import timber.log.Timber;

import static com.github.filipebezerra.findme.utils.ConnectionResultError.getConnectionResultErrorMessage;
import static com.github.filipebezerra.findme.utils.ConnectionResultError.getConnectionSuspendedCauseMessage;
import static com.google.android.gms.common.GooglePlayServicesUtil.getErrorDialog;
import static com.google.android.gms.common.GooglePlayServicesUtil.isGooglePlayServicesAvailable;
import static com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import static com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;

/**
 * Base fragment class containing helper methods from {@link com.google.android.gms.common.api.GoogleApiClient}.
 *
 * @author Filipe Bezerra
 * @version 1.0, 08/04/2015
 * @since #
 */
public abstract class BaseGoogleApisFragment extends Fragment
    implements ConnectionCallbacks, OnConnectionFailedListener, LocationListener {

    private static final String TAG = BaseGoogleApisFragment.class.getSimpleName();

    protected static final String STATE_REQUESTING_LOCATION_UPDATES_KEY = "STATE_REQUESTING_LOCATION_UPDATES_KEY";
    protected static final String STATE_LOCATION_KEY = "STATE_LOCATION_KEY";
    protected static final String STATE_LAST_UPDATED_TIME_STRING_KEY = "STATE_LAST_UPDATED_TIME_STRING_KEY";
    protected static final String STATE_ADDRESS_REQUESTED_KEY = "STATE_ADDRESS_REQUESTED_KEY";
    protected static final String STATE_LOCATION_ADDRESS_KEY = "STATE_LOCATION_ADDRESS_KEY";

    /**
     * Request code to call {@link com.google.android.gms.common.GooglePlayServicesUtil#getErrorDialog}
     */
    protected static final int REQUEST_CODE_GET_ERROR_DIALOG = 1001;

    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;

    /**
     * The fastest rate for active location updates. Exact. Updates will never be more frequent
     * than this value.
     */
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    /**
     * Provides the entry point to Google Play services.
     */
    protected GoogleApiClient mGoogleApiClient;

    /**
     * Stores parameters for requests to the FusedLocationProviderApi.
     */
    protected LocationRequest mLocationRequest;

    /**
     * Represents a geographical location.
     */
    protected Location mLastLocation;

    /**
     * Time when the location was updated represented as a String.
     */
    protected String mLastUpdateTime = "";

    /**
     * Tracks the status of the location updates request.
     */
    protected boolean mRequestingLocationUpdates = false;

    /**
     * Tracks whether the user has requested an address. Becomes true when the user requests an
     * address and false when the address (or an error message) is delivered.
     * The user requests an address by pressing the Fetch Address button. This may happen
     * before GoogleApiClient connects. This activity uses this boolean to keep track of the
     * user's intent. If the value is true, the activity tries to fetch the address as soon as
     * GoogleApiClient connects.
     */
    protected boolean mAddressRequested = false;

    /**
     * The formatted location address.
     */
    protected String mAddressOutput = "";

    /**
     * Receiver registered with this activity to get the response from FetchAddressIntentService.
     */
    private AddressResultReceiver mResultReceiver;


    protected int mGoogleApiAvailability = -1;

    protected abstract void updateGeographicalLocationUI();

    protected abstract void displayAddressOutput();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Timber.tag(TAG);
        buildGoogleApiClient();
        mResultReceiver = new AddressResultReceiver(new Handler());
        updateValuesFromBundle(savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        connectGoogleApiClient();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (! showErrorDialogIfGooglePlayNotAvailable()) {
            if (mRequestingLocationUpdates) {
                startLocationUpdates();
            }

            if (mAddressRequested) {
                fetchAddressHandler();
            }
        }
    }

    public boolean showErrorDialogIfGooglePlayNotAvailable() {
        if (mGoogleApiAvailability == -1) {
            mGoogleApiAvailability = isGooglePlayServicesAvailable(getActivity());
        }

        if (mGoogleApiAvailability != ConnectionResult.SUCCESS) {
            Timber.d("Google Play Services it's not mGoogleApiAvailability with error message %s",
                    getConnectionResultErrorMessage(mGoogleApiAvailability));
            getErrorDialog(mGoogleApiAvailability, getActivity(), REQUEST_CODE_GET_ERROR_DIALOG)
                    .show();
            return true;
        }

        return false;
    }

    protected void fetchAddressHandler() {
        if (! showErrorDialogIfGooglePlayNotAvailable()) {
            if (isGoogleApiClientConnected() && mLastLocation != null) {
                startIntentService();
            }

            mAddressRequested = true;
            // TODO: showMessage
            //showMessage(getActivity(), "Getting your current address...");
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    @Override
    public void onStop() {
        super.onStop();
        disconnectGoogleApiClient();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Timber.d("Saving requesting location updates state as %s", mRequestingLocationUpdates);

        // Save whether the location updates has been requested.
        outState.putBoolean(STATE_REQUESTING_LOCATION_UPDATES_KEY, mRequestingLocationUpdates);

        // Save the last location.
        outState.putParcelable(STATE_LOCATION_KEY, mLastLocation);

        // Save the last location update time string.
        outState.putString(STATE_LAST_UPDATED_TIME_STRING_KEY, mLastUpdateTime);

        // Save whether the address has been requested.
        outState.putBoolean(STATE_ADDRESS_REQUESTED_KEY, mAddressRequested);

        // Save the address string.
        outState.putString(STATE_LOCATION_ADDRESS_KEY, mAddressOutput);
        super.onSaveInstanceState(outState);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Timber.d("onActivityResult for request %d with result %d", requestCode, resultCode);
        switch (requestCode) {
            case REQUEST_CODE_GET_ERROR_DIALOG:
                if (resultCode == Activity.RESULT_OK) {
                    Timber.d("onActivityResult for GooglePlayServices error dialog result ok");
                    mGoogleApiClient.connect();
                } else {
                    Timber.d("onActivityResult for GooglePlayServices error dialog result "
                            + "cancelled or failed with code %d", resultCode);
                    // TODO: showMessage
                    //showMessage(getActivity(), "Google Play must be installed and updated");
                }
                break;
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Timber.d("onConnected, Google Play Services...");

        if (mLastLocation == null) {
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

            if (mLastLocation != null) {
                mLastUpdateTime = DateFormat.getDateTimeInstance().format(new Date());
                updateGeographicalLocationUI();
            }
        }

        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }

        if (mAddressRequested) {
            startIntentService();
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Timber.d("onConnectionSuspended, Google Play Services with error message %s",
                getConnectionResultErrorMessage(result.getErrorCode()));
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Timber.d("onConnectionSuspended, Google Play Services with error message %s",
                getConnectionSuspendedCauseMessage(cause));
        mGoogleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {
        Timber.d("onLocationChanged, the current location is %s", location.toString());
        mLastLocation = location;
        mLastUpdateTime = DateFormat.getDateTimeInstance().format(new Date());

        if (mLastLocation != null) {
            mLastUpdateTime = DateFormat.getDateTimeInstance().format(new Date());
            updateGeographicalLocationUI();
        }
    }

    /**
     * Builds a GoogleApiClient. Uses the {@code #addApi} method to request the
     * LocationServices API.
     */
    protected synchronized void buildGoogleApiClient() {
        Timber.d("Building new GoogleApiClient instance...");
        mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        createLocationRequest();
    }

    /**
     * Sets up the location request. Android has two location request settings:
     * {@code ACCESS_COARSE_LOCATION} and {@code ACCESS_FINE_LOCATION}. These settings control
     * the accuracy of the current location. This sample uses ACCESS_FINE_LOCATION, as defined in
     * the AndroidManifest.xml.
     * <p/>
     * When the ACCESS_FINE_LOCATION setting is specified, combined with a fast update
     * interval (5 seconds), the Fused Location Provider API returns location updates that are
     * accurate to within a few feet.
     * <p/>
     * These settings are appropriate for mapping applications that show real-time location
     * updates.
     */
    protected void createLocationRequest() {
        Timber.d("Creating location request");
        mLocationRequest = new LocationRequest();

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are mGoogleApiAvailability, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    protected void connectGoogleApiClient() {
        if (mGoogleApiClient != null) {
            Timber.d("Connecting with Google Play Services...");
            mGoogleApiClient.connect();
        }
    }

    protected void disconnectGoogleApiClient() {
        if (isGoogleApiClientConnected()) {
            Timber.d("Disconnecting from Google Play Services...");
            mGoogleApiClient.disconnect();
        }
    }

    public boolean isGoogleApiClientConnected() {
        return mGoogleApiClient != null && mGoogleApiClient.isConnected();
    }

    /**
     * Requests location updates from the FusedLocationApi.
     */
    protected void startLocationUpdates() {
        if (isGoogleApiClientConnected()) {
            Timber.d("Staring location updates");
            // The final argument to {@code requestLocationUpdates()} is a LocationListener
            // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);
        }
    }

    /**
     * Removes location updates from the FusedLocationApi.
     */
    protected void stopLocationUpdates() {
        if (isGoogleApiClientConnected()) {
            Timber.d("Stopping location updates");
            // It is a good practice to remove location requests when the activity is in a paused or
            // stopped state. Doing so helps battery performance and is especially
            // recommended in applications that request frequent location updates.

            // The final argument to {@code requestLocationUpdates()} is a LocationListener
            // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
    }

    /**
     * Creates an intent, adds location data to it as an extra, and starts the intent service for
     * fetching an address.
     */
    protected void startIntentService() {
        // Create an intent for passing to the intent service responsible for fetching the address.
        Intent intent = new Intent(getActivity(), FetchAddressIntentService.class);

        // Pass the result receiver as an extra to the service.
        intent.putExtra(Constants.RECEIVER, mResultReceiver);

        // Pass the location data as an extra to the service.
        intent.putExtra(Constants.LOCATION_DATA_EXTRA, mLastLocation);

        // Start the service. If the service isn't already running, it is instantiated and started
        // (creating a process for it if needed); if it is running then it remains running. The
        // service kills itself automatically once all intents are processed.
        getActivity().startService(intent);
    }

    /**
     * Updates fields based on data stored in the bundle.
     *
     * @param savedInstanceState The activity state saved in the Bundle.
     */
    private void updateValuesFromBundle(Bundle savedInstanceState) {
        Timber.i(TAG, "Updating values from bundle");
        if (savedInstanceState != null) {
            // Update the value of mRequestingLocationUpdates from the Bundle, and make sure that
            // the Start Updates and Stop Updates buttons are correctly enabled or disabled.
            if (savedInstanceState.keySet().contains(STATE_REQUESTING_LOCATION_UPDATES_KEY)) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean(
                        STATE_REQUESTING_LOCATION_UPDATES_KEY);
            }

            // Update the value of mCurrentLocation from the Bundle and update the UI to show the
            // correct latitude and longitude.
            if (savedInstanceState.keySet().contains(STATE_LOCATION_KEY)) {
                // Since LOCATION_KEY was found in the Bundle, we can be sure that mCurrentLocation
                // is not null.
                mLastLocation = savedInstanceState.getParcelable(STATE_LOCATION_KEY);
            }

            // Update the value of mLastUpdateTime from the Bundle and update the UI.
            if (savedInstanceState.keySet().contains(STATE_LAST_UPDATED_TIME_STRING_KEY)) {
                mLastUpdateTime = savedInstanceState.getString(STATE_LAST_UPDATED_TIME_STRING_KEY);
            }

            // Check savedInstanceState to see if the address was previously requested.
            if (savedInstanceState.keySet().contains(STATE_ADDRESS_REQUESTED_KEY)) {
                mAddressRequested = savedInstanceState.getBoolean(STATE_ADDRESS_REQUESTED_KEY);
            }

            // Check savedInstanceState to see if the location address string was previously found
            // and stored in the Bundle. If it was found, display the address string in the UI.
            if (savedInstanceState.keySet().contains(STATE_LOCATION_ADDRESS_KEY)) {
                mAddressOutput = savedInstanceState.getString(STATE_LOCATION_ADDRESS_KEY);
                displayAddressOutput();
            }

            if (mLastLocation != null) {
                updateGeographicalLocationUI();
            }
        }
    }

    protected class AddressResultReceiver extends ResultReceiver {

        public AddressResultReceiver(Handler handler) {
            super(handler);
        }
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            Timber.d("onReceiveResult address with result %d", resultCode);
            mAddressOutput = resultData.getString(Constants.RESULT_DATA_KEY);

            if (resultCode == Constants.FAILURE_RESULT) {
                // TODO: showMessage
                //showMessage(getActivity(), mAddressOutput);
            } else {
                displayAddressOutput();
            }

            mAddressRequested = false;
        }
    }
}
package io.github.filipebezerra.findme.activities;

import android.content.Intent;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.util.Linkify;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import io.github.filipebezerra.findme.R;
import io.github.filipebezerra.findme.tasks.FetchAddressIntentService;
import io.github.filipebezerra.findme.utils.Constants;
import java.text.DateFormat;
import java.util.Date;
import timber.log.Timber;

import static com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import static io.github.filipebezerra.findme.utils.ConnectionResultError.getConnectionResultErrorMessage;
import static io.github.filipebezerra.findme.utils.ConnectionResultError.getConnectionSuspendedCauseMessage;

public class MainActivity extends ActionBarActivity implements GoogleApiClient.ConnectionCallbacks,
        OnConnectionFailedListener, LocationListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    /**
     * Request code to call {@link GooglePlayServicesUtil#getErrorDialog}
     */
    private static final int REQUEST_CODE_GET_ERROR_DIALOG = 1001;

    /**
     * Saved application state for saving location update requests
     */
    private static final String STATE_REQUESTING_LOCATION_UPDATES_KEY = "STATE_REQUESTING_LOCATION_UPDATES_KEY";
    private static final String STATE_LOCATION_KEY = "STATE_LOCATION_KEY";
    private static final String STATE_LAST_UPDATE_TIME_STRING_KEY = "STATE_LAST_UPDATE_TIME_STRING_KEY";
    private static final String STATE_ADDRESS_REQUESTED_KEY = "STATE_ADDRESS_REQUESTED_KEY";
    private static final String STATE_LOCATION_ADDRESS_KEY = "STATE_LOCATION_ADDRESS_KEY";

    /**
     * Entry point for Google Play services
     */
    private GoogleApiClient mGoogleClientApi;

    /**
     * Last known location retrieved from {@link com.google.android.gms.location.LocationServices#FusedLocationApi}
     */
    private Location mCurrentLocation;

    /**
     * Receive location updates
     */
    private LocationRequest mLocationRequest;

    private String mLastUpdateTime;

    private boolean mRequestingLocationUpdates = true;

    private boolean mAddressRequested;

    /**
     * Receiver registered with this activity to get the response from FetchAddressIntentService.
     */
    private AddressResultReceiver mResultReceiver;

    /**
     * The formatted location address.
     */
    protected String mAddressOutput;

    @InjectView(R.id.last_location_text) TextView mLastLocationView;
    @InjectView(R.id.last_update_time_text) TextView mLastUpdateTimeView;
    @InjectView(R.id.last_address_text) TextView mLastAddressView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Timber.tag(TAG);
        ButterKnife.inject(this);

        setupActionBar();

        setupViews();

        buildGoogleClientApi();

        mResultReceiver = new AddressResultReceiver(new Handler());

        updateValuesFromBundle(savedInstanceState);
    }

    private void setupViews() {
        Linkify.addLinks(mLastAddressView, Linkify.ALL);
    }

    private void setupActionBar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.title_activity_main_after_showing));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.mipmap.ic_menu);
    }

    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            // Update the value of mRequestingLocationUpdates from the Bundle, and
            // make sure that the Start Updates and Stop Updates buttons are
            // correctly enabled or disabled.
            if (savedInstanceState.keySet().contains(STATE_REQUESTING_LOCATION_UPDATES_KEY)) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean(
                        STATE_REQUESTING_LOCATION_UPDATES_KEY);
            }

            // Update the value of mCurrentLocation from the Bundle and update the
            // UI to show the correct latitude and longitude.
            if (savedInstanceState.keySet().contains(STATE_LOCATION_KEY)) {
                // Since LOCATION_KEY was found in the Bundle, we can be sure that
                // mCurrentLocationis not null.
                mCurrentLocation = savedInstanceState.getParcelable(STATE_LOCATION_KEY);
            }

            // Update the value of mLastUpdateTime from the Bundle and update the UI.
            if (savedInstanceState.keySet().contains(STATE_LAST_UPDATE_TIME_STRING_KEY)) {
                mLastUpdateTime = savedInstanceState.getString(
                        STATE_LAST_UPDATE_TIME_STRING_KEY);
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

            updateUI();
        }
    }

    protected synchronized void buildGoogleClientApi() {
        Timber.d("Building new GoogleApiClient instance...");
        mGoogleClientApi = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest
                .setInterval(10000) // 10 seconds
                .setFastestInterval(5000) // 5 seconds
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY); // most precise, use the GPS

        // The priority of PRIORITY_HIGH_ACCURACY, combined with the ACCESS_FINE_LOCATION permission
        // setting that you've defined in the app manifest, and a fast update interval of 5000
        // milliseconds (5 seconds), causes the fused location provider to return location updates
        // that are accurate to within a few feet

        // Performance hint:
        // If your app accesses the network or does other long-running work after receiving a
        // location update, adjust the fastest interval to a slower value. This adjustment prevents
        // your app from receiving updates it can't use. Once the long-running work is done,
        // set the fastest interval back to a fast value.
    }

    @Override
    protected void onStart() {
        super.onStart();
        Timber.d("OnStart. Connecting with Google Play Services...");
        mGoogleClientApi.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();


        if (mGoogleClientApi.isConnected()) {
            Timber.d("OnStop. Disconnecting from Google Play Services...");
            stopLocationUpdates();

            mGoogleClientApi.disconnect();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        int available = GooglePlayServicesUtil.isGooglePlayServicesAvailable(
                this);

        if (available != ConnectionResult.SUCCESS) {
            Timber.d("OnResume. Google Play Services it's not available with error message %s",
                    getConnectionResultErrorMessage(available));
            GooglePlayServicesUtil.getErrorDialog(available, this, REQUEST_CODE_GET_ERROR_DIALOG)
                    .show();
        } else {
            if (mRequestingLocationUpdates) {
                startLocationUpdates();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Timber.d("Saving requesting location updates state as %s", mRequestingLocationUpdates);

        // Save whether the location updates has been requested.
        outState.putBoolean(STATE_REQUESTING_LOCATION_UPDATES_KEY, mRequestingLocationUpdates);

        // Save the last location.
        outState.putParcelable(STATE_LOCATION_KEY, mCurrentLocation);

        // Save the last location update time string.
        outState.putString(STATE_LAST_UPDATE_TIME_STRING_KEY, mLastUpdateTime);

        // Save whether the address has been requested.
        outState.putBoolean(STATE_ADDRESS_REQUESTED_KEY, mAddressRequested);

        // Save the address string.
        outState.putString(STATE_LOCATION_ADDRESS_KEY, mAddressOutput);
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_location_updates:
                mRequestingLocationUpdates = ! mRequestingLocationUpdates;

                if (mRequestingLocationUpdates) {
                    startLocationUpdates();
                } else {
                    stopLocationUpdates();
                }

                updateMenuItemLocationUpdates(item);
                return true;
            case R.id.action_fetch_address:
                startIntentService();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        updateMenuItemLocationUpdates(menu);
        return true;
    }

    private void updateMenuItemLocationUpdates(final Menu menu) {
        MenuItem actionLocationUpdates = menu.findItem(R.id.action_location_updates);
        updateMenuItemLocationUpdates(actionLocationUpdates);
    }

    private void updateMenuItemLocationUpdates(final MenuItem item) {
        if (mRequestingLocationUpdates) {
            item.setIcon(R.mipmap.ic_location_updates_off);
            item.setTitle(R.string.action_location_updates_stop);
        } else {
            item.setIcon(R.mipmap.ic_location_updates_tracking);
            item.setTitle(R.string.action_location_updates);
        }
    }

    private void startLocationUpdates() {
        if (mGoogleClientApi.isConnected()) {
            Timber.d("Starting location updates...");
            createLocationRequest();

            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleClientApi,
                    mLocationRequest,
                    this);
        }
    }

    private void stopLocationUpdates() {
        if (mGoogleClientApi.isConnected()) {
            Timber.d("Stopping location updates");
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleClientApi, this);
        }
    }

    private void startIntentService() {
        if (mGoogleClientApi.isConnected()) {
            Intent intent = new Intent(this, FetchAddressIntentService.class);
            intent.putExtra(Constants.RECEIVER, mResultReceiver);
            intent.putExtra(Constants.LOCATION_DATA_EXTRA, mCurrentLocation);
            startService(intent);
        }

        mAddressRequested = true;
    }

    private void showSnackbar(final String text) {
        SnackbarManager.show(
                Snackbar.with(this)
                        .text(text));
    }

    @Override
    public void onConnected(Bundle bundle) {
        Timber.d("Google Play Services onConnected...");

        mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleClientApi);

        if (mCurrentLocation != null) {
            mLastUpdateTime = DateFormat.getDateTimeInstance().format(new Date());
            updateUI();
        }

        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }

        if (mCurrentLocation != null) {
            if (!Geocoder.isPresent()) {
                showSnackbar(getString(R.string.no_geocoder_available));
            }

            if (mAddressRequested) {
                startIntentService();
            }
        }
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Timber.d("Google Play Services onConnectionSuspended with error message %s",
                getConnectionSuspendedCauseMessage(cause));
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Timber.d("Google Play Services onConnectionSuspended with error message %s",
                getConnectionResultErrorMessage(result.getErrorCode()));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Timber.d("onActivityResult for request %d with result %d", requestCode, resultCode);
        switch (requestCode) {
            case REQUEST_CODE_GET_ERROR_DIALOG:
                if (resultCode == RESULT_OK) {
                    Timber.d("onActivityResult for GooglePlayServices error dialog result ok");
                    mGoogleClientApi.connect();
                } else {
                    Timber.d("onActivityResult for GooglePlayServices error dialog result "
                            + "cancelled or failed with code %d", resultCode);
                    finish();
                }
                break;
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Timber.d("onLocationChanged in location %s, %s", location.getLatitude(),
                location.getLongitude());
        mCurrentLocation = location;
        mLastUpdateTime = DateFormat.getDateTimeInstance().format(new Date());
        updateUI();
    }

    private void updateUI() {
        final String lastLocation = String.valueOf(mCurrentLocation.getLatitude()).concat(", ")
                .concat(String.valueOf(mCurrentLocation.getLongitude()));
        mLastLocationView.setText(lastLocation);
        mLastUpdateTimeView.setText(mLastUpdateTime);
    }

    class AddressResultReceiver extends ResultReceiver {
        public AddressResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            Timber.d("onReceiveResult address");
            mAddressOutput = resultData.getString(Constants.RESULT_DATA_KEY);
            displayAddressOutput();
            mAddressRequested = false;
        }
    }

    private void displayAddressOutput() {
        mLastAddressView.setText(mAddressOutput);
    }
}

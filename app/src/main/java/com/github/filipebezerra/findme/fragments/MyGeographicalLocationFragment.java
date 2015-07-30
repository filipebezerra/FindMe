package com.github.filipebezerra.findme.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import butterknife.Bind;
import butterknife.ButterKnife;
import com.github.filipebezerra.findme.R;

/**
 * .
 *
 * @author Filipe Bezerra
 * @version 1.1, 09/04/2015
 * @since #
 */
public class MyGeographicalLocationFragment extends BaseGoogleApisFragment {
    /**
     * Injected UI widgets
     */
    @Bind(R.id.last_location_text) protected TextView mLastLocationView;
    @Bind(R.id.last_update_time_text) protected TextView mLastUpdateTimeView;
    @Bind(R.id.last_address_text) protected TextView mLastAddressView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View fragmentView = inflater.inflate(R.layout.fragment_my_geographical_location, container,
                false);
        ButterKnife.bind(this, fragmentView);
        return fragmentView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_my_geographical_location, menu);
    }

    private void updateMenuItemLocationUpdates(final Menu menu) {
        MenuItem actionLocationUpdates = menu.findItem(R.id.action_location_updates);
        updateMenuItemLocationUpdates(actionLocationUpdates);
    }

    private void updateMenuItemLocationUpdates(final MenuItem item) {
        if (item != null) {
            if (mRequestingLocationUpdates) {
                item.setIcon(R.mipmap.ic_location_updates_off);
                item.setTitle(R.string.action_location_updates_stop);
            } else {
                item.setIcon(R.mipmap.ic_location_updates_tracking);
                item.setTitle(R.string.action_location_updates);
            }
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        updateMenuItemLocationUpdates(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_location_updates:
                switchRequestingLocationUpdatesHandler(item);
                return true;
            case R.id.action_fetch_address:
                fetchAddressHandler();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void switchRequestingLocationUpdatesHandler(MenuItem item) {
        if (! showErrorDialogIfGooglePlayNotAvailable()) {
            if (mRequestingLocationUpdates) {
                stopLocationUpdates();
                mRequestingLocationUpdates = false;
                // TODO: showMessage
                //showMessage(getActivity(), "Tracking your location turned off");
            } else {
                startLocationUpdates();
                mRequestingLocationUpdates = true;
                // TODO: showMessage
                //showMessage(getActivity(), "Tracking your location turned on");
            }
            updateMenuItemLocationUpdates(item);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }

    @Override
    protected void updateGeographicalLocationUI() {
        final String lastLocation = String.valueOf(mLastLocation.getLatitude()).concat(", ")
                .concat(String.valueOf(mLastLocation.getLongitude()));
        mLastLocationView.setText(lastLocation);
        mLastUpdateTimeView.setText(mLastUpdateTime);
    }

    @Override
    protected void displayAddressOutput() {
        mLastAddressView.setText(mAddressOutput);
    }
}
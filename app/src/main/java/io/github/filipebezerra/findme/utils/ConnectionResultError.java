package io.github.filipebezerra.findme.utils;

import static com.google.android.gms.common.ConnectionResult.API_UNAVAILABLE;
import static com.google.android.gms.common.ConnectionResult.CANCELED;
import static com.google.android.gms.common.ConnectionResult.DEVELOPER_ERROR;
import static com.google.android.gms.common.ConnectionResult.INTERNAL_ERROR;
import static com.google.android.gms.common.ConnectionResult.INTERRUPTED;
import static com.google.android.gms.common.ConnectionResult.INVALID_ACCOUNT;
import static com.google.android.gms.common.ConnectionResult.LICENSE_CHECK_FAILED;
import static com.google.android.gms.common.ConnectionResult.NETWORK_ERROR;
import static com.google.android.gms.common.ConnectionResult.RESOLUTION_REQUIRED;
import static com.google.android.gms.common.ConnectionResult.SERVICE_DISABLED;
import static com.google.android.gms.common.ConnectionResult.SERVICE_INVALID;
import static com.google.android.gms.common.ConnectionResult.SERVICE_MISSING;
import static com.google.android.gms.common.ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED;
import static com.google.android.gms.common.ConnectionResult.SIGN_IN_FAILED;
import static com.google.android.gms.common.ConnectionResult.SIGN_IN_REQUIRED;
import static com.google.android.gms.common.ConnectionResult.SUCCESS;
import static com.google.android.gms.common.ConnectionResult.TIMEOUT;

import static com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED;
import static com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks.CAUSE_NETWORK_LOST;

/**
 * .
 *
 * @author Filipe Bezerra
 * @version #, 06/04/2015
 * @since #
 */
public final class ConnectionResultError {

    public static String getConnectionResultErrorMessage(final int errorCode) {
        switch (errorCode) {
            case API_UNAVAILABLE:
                return "One of the API components you attempted to connect to is not available.";
            case CANCELED:
                return "The client canceled the connection by calling disconnect()";
            case DEVELOPER_ERROR:
                return "The application is misconfigured.";
            case INTERNAL_ERROR:
                return "An internal error occurred.";
            case INTERRUPTED:
                return "An interrupt occurred while waiting for the connection complete.";
            case INVALID_ACCOUNT:
                return "The client attempted to connect to the service with an invalid account name specified.";
            case LICENSE_CHECK_FAILED:
                return "The application is not licensed to the user.";
            case NETWORK_ERROR:
                return "A network error occurred.";
            case RESOLUTION_REQUIRED:
                return "Completing the connection requires some form of resolution.";
            case SERVICE_DISABLED:
                return "The installed version of Google Play services has been disabled on this device.";
            case SERVICE_INVALID:
                return "The version of the Google Play services installed on this device is not authentic.";
            case SERVICE_MISSING:
                return "Google Play services is missing on this device.";
            case SERVICE_VERSION_UPDATE_REQUIRED:
                return "The installed version of Google Play services is out of date.";
            case SIGN_IN_FAILED:
                return "The client attempted to connect to the service but the user is not signed in.";
            case SIGN_IN_REQUIRED:
                return "The client attempted to connect to the service but the user is not signed in.";
            case TIMEOUT:
                return "The timeout was exceeded while waiting for the connection to complete.";
            case SUCCESS:
                return "The connection was successful.";
            default:
                return "unknown error code";
        }
    }

    public static String getConnectionSuspendedCauseMessage(final int cause) {
        switch (cause) {
            case CAUSE_NETWORK_LOST:
                return "peer device connection was lost";
            case CAUSE_SERVICE_DISCONNECTED:
                return "service has been killed";
            default:
                return "unknown cause";
        }
    }
}

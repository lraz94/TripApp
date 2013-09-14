package com.source.tripwithme.managers;


import android.app.Dialog;
import android.content.IntentSender.SendIntentException;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationRequest;
import com.source.tripwithme.TripWithMeMain;

public class LocationManager implements ConnectionCallbacks, OnConnectionFailedListener {

    public static final String LOCATION_HANDELING_TAG = "LocationHandeling";

    private static final long ONE_MINUTE_MILLIS = 60000L;

    private static final long DEFAULT_LOCATION_UPDATES_INTERVAL_MILLIS = 15 * ONE_MINUTE_MILLIS;

    private static LocationManager instance;
    private final TripWithMeMain caller;

    // A request to connect to Location Services
    private LocationRequest mLocationRequest;

    // Stores the current instantiation of the location client in this object
    private LocationClient mLocationClient;

    private long millisIntervalBetweenRefresh;

    private LocationManager(TripWithMeMain caller) {
        this.caller = caller;
        millisIntervalBetweenRefresh = DEFAULT_LOCATION_UPDATES_INTERVAL_MILLIS;

        // Create a new global location parameters object
        mLocationRequest = LocationRequest.create();
        // Set the update interval
        mLocationRequest.setInterval(millisIntervalBetweenRefresh);
        // Use high accuracy
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        // Set the interval ceiling to one minute
        mLocationRequest.setFastestInterval(ONE_MINUTE_MILLIS);

        // Create a new location client, using the enclosing class to handle callbacks.
        mLocationClient = new LocationClient(caller, this, this);

    }

    public static LocationManager instance(TripWithMeMain caller) {
        if (instance == null) {
            instance = new LocationManager(caller);
        }
        return instance;
    }

    public Location getCurrentLocation() {
        Location location = null;
        if (mLocationClient.isConnected()) {
            location = mLocationClient.getLastLocation();
        }
        return location;
    }

    public void setMillisIntervalBetweenRefresh(long millis) {
        millisIntervalBetweenRefresh = millis;
        mLocationRequest.setInterval(millis);
    }

    public long getMillisIntervalBetweenRefresh() {
        return millisIntervalBetweenRefresh;
    }

    // location handeling
    @Override
    public void onConnected(Bundle bundle) {
        Log.d(LOCATION_HANDELING_TAG, "Connected to client");
        startPeriodicUpdates();
    }

    @Override
    public void onDisconnected() {
        Log.d(LOCATION_HANDELING_TAG, "Disconnected from client");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(LOCATION_HANDELING_TAG, "connection failed to client!");
        /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(caller, TripWithMeMain.CONNECTION_FAILURE_RESOLUTION_REQUEST);
                /*
                * Thrown if Google Play services canceled the original
                * PendingIntent
                */
            } catch (SendIntentException e) {
                // Log the error
                Log.e("SendIntentException", "Thrown", e);
            }
        } else {
            // If no resolution is available, display a dialog to the user with the error.
            showErrorDialog(connectionResult.getErrorCode());
        }
    }


    /**
     * Show a dialog returned by Google Play services for the
     * connection error code
     *
     * @param errorCode An error code returned from onConnectionFailed
     */
    private void showErrorDialog(int errorCode) {
        // Get the error dialog from Google Play services
        Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(errorCode, caller,
                                                                   TripWithMeMain.CONNECTION_FAILURE_RESOLUTION_REQUEST);
        // If Google Play services can provide an error dialog
        if (errorDialog != null) {
            errorDialog.getWindow().setGravity(Gravity.CENTER);
            errorDialog.show();
        }
    }

    public void connect() {
        mLocationClient.connect();
    }

    public void stopAndDisconnect() {
        stopPeriodicUpdates();
        // After disconnect() is called, the client is considered "dead".
        mLocationClient.disconnect();
    }

    // periodic updates are started each on start and might not be used if user doesn't want them
    private void startPeriodicUpdates() {
        if (mLocationClient.isConnected() && caller.servicesConnected()) {
            mLocationClient.requestLocationUpdates(mLocationRequest, caller);
        }
    }

    private void stopPeriodicUpdates() {
        if (mLocationClient.isConnected() && caller.servicesConnected()) {
            mLocationClient.removeLocationUpdates(caller);
        }
    }
}

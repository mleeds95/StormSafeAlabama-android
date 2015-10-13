package mwleeds.stormsafealabama;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.geojson.GeoJsonLayer;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class MapsActivity extends FragmentActivity
        implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private GoogleMap mMap;
    private GoogleApiClient mLocationClient;
    private LocationListener mLocationListener;
    private LocationRequest mLocationRequest;
    private boolean mLocationSettingsGood = false;

    // constants
    public static final long GPS_LOCATION_INTERVAL = 5000;
    public static final long GPS_FASTEST_INTERVAL = 1000;
    public static final int MAP_ZOOM_LEVEL = 15;
    public static final int REQUEST_CHECK_SETTINGS = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mLocationClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mLocationClient.connect();

        mMap.setMyLocationEnabled(true);

        addRefugeLocationsToMap();
    }

    private void addRefugeLocationsToMap() {
        try {
            GeoJsonLayer layer = new GeoJsonLayer(mMap, R.raw.ua_bara_2014_08_18, getApplicationContext());
            layer.addLayerToMap();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {

        // implement the LocationListener interface to update the map for each change
        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                LatLng latlng = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latlng, MAP_ZOOM_LEVEL));
            }
        };

        // set the time interval for location updates
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(GPS_LOCATION_INTERVAL);
        mLocationRequest.setFastestInterval(GPS_FASTEST_INTERVAL);

        // Check if the user's location services is enabled
        LocationSettingsRequest.Builder locationSettingsBuilder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);
        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi
                .checkLocationSettings(mLocationClient, locationSettingsBuilder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult locationSettingsResult) {
                final Status status = locationSettingsResult.getStatus();
                switch(status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        mLocationSettingsGood = true;
                        // request regular location updates
                        LocationServices.FusedLocationApi.requestLocationUpdates(mLocationClient, mLocationRequest, mLocationListener);
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // open a dialog asking the user to fix their location settings
                        try {
                            status.startResolutionForResult(MapsActivity.this, REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {
                            Toast.makeText(MapsActivity.this, R.string.error_fix_location_settings, Toast.LENGTH_LONG).show();
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // inform the user they have no means of finding their location
                        Toast.makeText(MapsActivity.this, R.string.bad_location_settings, Toast.LENGTH_SHORT).show();
                        mLocationSettingsGood = false;
                        break;
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        mLocationSettingsGood = true;
                        // request regular location updates
                        LocationServices.FusedLocationApi.requestLocationUpdates(mLocationClient, mLocationRequest, mLocationListener);
                        break;
                    case Activity.RESULT_CANCELED:
                        // inform the user they won't be able to get their location
                        Toast.makeText(MapsActivity.this, R.string.location_settings_unchanged, Toast.LENGTH_SHORT).show();
                        mLocationSettingsGood = false;
                        break;
                    default:
                        break;
                }
                break;
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(this, R.string.get_location_suspended, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(this, R.string.get_location_failed, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // stop requesting location updates when in the background
        LocationServices.FusedLocationApi.removeLocationUpdates(mLocationClient, mLocationListener);
    }
}

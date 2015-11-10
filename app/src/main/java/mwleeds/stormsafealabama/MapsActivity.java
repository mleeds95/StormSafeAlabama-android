package mwleeds.stormsafealabama;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
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
import com.google.android.gms.maps.model.Marker;
import com.google.maps.android.geojson.GeoJsonFeature;
import com.google.maps.android.geojson.GeoJsonLayer;
import com.google.maps.android.geojson.GeoJsonPointStyle;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.IOException;

public class MapsActivity extends FragmentActivity
        implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private GoogleMap mMap;
    private GoogleApiClient mLocationClient;
    private LocationListener mLocationListener;
    private LocationRequest mLocationRequest;
    private Context mContext;

    // constants
    // how often to request location updates (low numbers hurt battery life)
    public static final long GPS_LOCATION_INTERVAL = 5000;
    public static final long GPS_FASTEST_INTERVAL = 1000;
    // zoom level after a location change
    public static final int MAP_ZOOM_LEVEL = 15;
    // constant used in the callback when the user changes location settings
    public static final int REQUEST_CHECK_SETTINGS = 100;
    // coordinates of the map's initial position
    public static final double ALABAMA_LAT = 32.8;
    public static final double ALABAMA_LONG = -86.7;
    // initial zoom level of the map
    public static final float INITIAL_ZOOM = 6;
    // number of meters of location change to trigger the map to move
    public static final float LOCATION_CHANGE_DISTANCE = 20;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mContext = getApplicationContext();
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

        // move camera to Alabama
        LatLng initialLatLng = new LatLng(ALABAMA_LAT, ALABAMA_LONG);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(initialLatLng, INITIAL_ZOOM));

        mLocationClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        // connect to Google Location Services
        mLocationClient.connect();

        // add a blue dot at the user's location
        mMap.setMyLocationEnabled(true);

        // change the look of info windows to accommodate text that spans multiple lines
        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                LinearLayout info = new LinearLayout(mContext);
                info.setOrientation(LinearLayout.VERTICAL);

                TextView title = new TextView(mContext);
                title.setTextColor(Color.BLACK);
                title.setGravity(Gravity.CENTER);
                title.setTypeface(null, Typeface.BOLD);
                title.setText(marker.getTitle());

                TextView snippet = new TextView(mContext);
                snippet.setTextColor(Color.GRAY);
                snippet.setText(marker.getSnippet());

                info.addView(title);
                info.addView(snippet);

                return info;
            }
        });

        // add all the refuge location coordinates to the map
        addRefugeLocationsToMap();
    }

    private void addRefugeLocationsToMap() {
        try {
            // load the refuge location data from the file system
            FileInputStream inputStream = openFileInput(getString(R.string.geojson_filename));
            StringBuilder builder = new StringBuilder();
            int ch;
            while((ch = inputStream.read()) != -1){
                builder.append((char)ch);
            }
            JSONObject geoJSON = new JSONObject(builder.toString());

            // add the refuge area locations to the map
            GeoJsonLayer layer = new GeoJsonLayer(mMap, geoJSON);

            // add title and snippet properties for the markers asynchronously
            AddMarkerProperties addMarkerProperties = new AddMarkerProperties();
            addMarkerProperties.execute(layer);

        } catch (IOException | JSONException e) {
                e.printStackTrace();
                Toast.makeText(this, R.string.load_geojson_failed, Toast.LENGTH_LONG).show();
        }
    }

    private class AddMarkerProperties extends AsyncTask<GeoJsonLayer, Void, GeoJsonLayer> {

        @Override
        protected GeoJsonLayer doInBackground(GeoJsonLayer... params) {
            GeoJsonLayer layer = params[0];
            // add the building name and refuge area description from the GeoJSON properties
            // to the map marker's title and snippet attributes so the user can see them
            for (GeoJsonFeature feature : layer.getFeatures()) {
                GeoJsonPointStyle pointStyle = new GeoJsonPointStyle();
                pointStyle.setTitle(feature.getProperty("Building"));
                String snippet = "";
                if (feature.hasProperty("Floor")) {
                    String floor = feature.getProperty("Floor");
                    if (floor.length() != 0) {
                        if (floor.equals("ALL")) {
                            snippet += "All floors. ";
                        } else {
                            snippet += "Floor " + floor + ". ";
                        }
                    }
                }
                snippet += feature.getProperty("Best Available Refuge Area");
                pointStyle.setSnippet(snippet);
                feature.setPointStyle(pointStyle);
            }
            return layer;
        }

        @Override
        protected void onPostExecute(GeoJsonLayer layer) {
            layer.addLayerToMap();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {

        // implement the LocationListener interface to update the map for each change
        mLocationListener = new LocationListener() {
            private Location lastLocation = null;

            @Override
            public void onLocationChanged(Location location) {

                /*Float deltaDistance  = (lastLocation == null) ? 0 : location.distanceTo(lastLocation);
                String msg = String.format("New location: %f, %f. Accuracy: %f. Delta Distance: %f",
                        location.getLatitude(), location.getLongitude(), location.getAccuracy(), deltaDistance);
                Toast.makeText(mContext, msg, Toast.LENGTH_LONG).show();*/

                // only move the map location if the user has moved significantly
                if (lastLocation == null ||
                        ((location.getAccuracy() <= lastLocation.getAccuracy()) &&
                         (location.distanceTo(lastLocation) > LOCATION_CHANGE_DISTANCE))) {

                    LatLng latlng = new LatLng(location.getLatitude(), location.getLongitude());

                    // update the map view, only zooming if the user hasn't zoomed in or out
                    if (mMap.getCameraPosition().zoom == INITIAL_ZOOM) {
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latlng, MAP_ZOOM_LEVEL));
                    } else {
                        mMap.animateCamera(CameraUpdateFactory.newLatLng(latlng));
                    }

                    lastLocation = location;
                }
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
                        // request regular location updates
                        LocationServices.FusedLocationApi.requestLocationUpdates(mLocationClient, mLocationRequest, mLocationListener);
                        break;
                    case Activity.RESULT_CANCELED:
                        // inform the user they won't be able to get their location
                        Toast.makeText(MapsActivity.this, R.string.location_settings_unchanged, Toast.LENGTH_SHORT).show();
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

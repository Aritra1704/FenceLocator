package com.arpaul.fencelocator.activity;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.arpaul.fencelocator.R;
import com.arpaul.fencelocator.common.AppConstant;
import com.arpaul.fencelocator.common.ApplicationInstance;
import com.arpaul.fencelocator.common.GeofenceErrorMessages;
import com.arpaul.fencelocator.dataAccess.FLCPConstants;
import com.arpaul.fencelocator.dataObject.PrefLocationDO;
import com.arpaul.fencelocator.service.GeofenceTransitionsIntentService;
import com.arpaul.utilitieslib.ColorUtils;
import com.arpaul.utilitieslib.LogUtils;
import com.arpaul.utilitieslib.PermissionUtils;
import com.arpaul.utilitieslib.StringUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;

/**
 * Created by ARPaul on 11-09-2016.
 */
public class GeoFenceActivity extends BaseActivity implements
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        ResultCallback<Status>,
        LoaderManager.LoaderCallbacks {

    private View llGeoFencActivity;
    protected ArrayList<Geofence> mGeofenceList;

    private final String LOG_TAG ="FenceLocator";

    private GoogleApiClient mGoogleApiClient;
    private GoogleMap mMap;
    private SupportMapFragment mapFragment;
    private LatLng currentLatLng = null;
    private boolean isGpsEnabled;
    private boolean ispermissionGranted = false;
    private ArrayList<PrefLocationDO> arrPrefLocationDO = new ArrayList<>();
    private LocationRequest mLocationRequest;

    @Override
    public void initialize() {
        llGeoFencActivity = baseInflater.inflate(R.layout.activity_geofence,null);
        llBody.addView(llGeoFencActivity, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);

        initialiseControls();

        bindControls();
    }

    private void bindControls(){
        mGeofenceList = new ArrayList<Geofence>();

        buildGoogleApiClient();

        if(new PermissionUtils().checkPermission(this, new String[]{
                android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION}) != 0){
            new PermissionUtils().verifyLocation(this,new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
        } else {
            buildGoogleApiClient();
        }
    }

    /**
     * Builds a GoogleApiClient. Uses the {@code #addApi} method to request the LocationServices API.
     */
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    public void addGeofencesButtonHandler() {
        if (!mGoogleApiClient.isConnected()) {
            Toast.makeText(this, getString(R.string.not_connected), Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            LocationServices.GeofencingApi.addGeofences(
                    mGoogleApiClient,
                    // The GeofenceRequest object.
                    getGeofencingRequest(),
                    // A pending intent that that is reused when calling removeGeofences(). This
                    // pending intent is used to generate an intent when a matched geofence
                    // transition is observed.
                    getGeofencePendingIntent()
            ).setResultCallback(this); // Result processed in onResult().

            showLocations();
        } catch (SecurityException securityException) {
            // Catch exception generated if the app does not use ACCESS_FINE_LOCATION permission.
        }
    }

    private PendingIntent getGeofencePendingIntent() {
        Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling addgeoFences()
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public void populateGeofenceList() {
        for(PrefLocationDO objPrefLocationDO : arrPrefLocationDO) {
            mGeofenceList.add(new Geofence.Builder()
                    // Set the request ID of the geofence. This is a string to identify this geofence.
                    .setRequestId(objPrefLocationDO.LocationId + "")

                    // Set the circular region of this geofence.
                    .setCircularRegion(
                            objPrefLocationDO.Latitude,
                            objPrefLocationDO.Longitude,
                            AppConstant.GEOFENCE_RADIUS_IN_METERS
                    )

                    // Set the expiration duration of the geofence. This geofence gets automatically
                    // removed after this period of time.
                    .setExpirationDuration(AppConstant.GEOFENCE_EXPIRATION_IN_MILLISECONDS)

                    // Set the transition types of interest. Alerts are only generated for these
                    // transition. We track entry and exit transitions in this sample.
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)

                    // Create the geofence.
                    .build());
        }

        addGeofencesButtonHandler();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mGoogleApiClient != null && (!mGoogleApiClient.isConnecting() || !mGoogleApiClient.isConnected())) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnecting() || mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        LogUtils.infoLog(LOG_TAG, "Connected to GoogleApiClient");

        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if(location != null){
            currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

            if(getSupportLoaderManager().getLoader(ApplicationInstance.LOADER_FETCH_ALL_LOCATION) != null)
                getSupportLoaderManager().restartLoader(ApplicationInstance.LOADER_FETCH_ALL_LOCATION, null, this);
            else
                getSupportLoaderManager().initLoader(ApplicationInstance.LOADER_FETCH_ALL_LOCATION, null, this);

        } //else {
            mLocationRequest = LocationRequest.create();
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            mLocationRequest.setInterval(AppConstant.LOCATION_UPDATES_IN_SECONDS * 1000); // Update location every second

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ){
                    LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
                }
            } else
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        //}
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        LogUtils.infoLog(LOG_TAG, "GoogleApiClient connection has failed");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionSuspended(int i) {
        LogUtils.infoLog(LOG_TAG, "GoogleApiClient connection has been suspend");
    }

    @Override
    public void onLocationChanged(Location location) {
        LogUtils.infoLog(LOG_TAG, location.toString());
        //txtOutput.setText(location.toString());

        currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

        Toast.makeText(GeoFenceActivity.this, "Lat: "+currentLatLng.latitude+" Lon: "+currentLatLng.longitude, Toast.LENGTH_SHORT).show();
        showLocations();
    }

    @Override
    public void onResult(@NonNull Status status) {
        if (status.isSuccess()) {
            Toast.makeText(this,"Geofences Added",Toast.LENGTH_SHORT).show();
        } else {
            // Get the status code for the error and log it using a user-friendly message.
            String errorMessage = GeofenceErrorMessages.getErrorString(this, status.getStatusCode());
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if(isGpsEnabled) {
            new Handler().postDelayed(new Runnable()
            {
                @Override
                public void run()
                {
                    showCurrentLocation();
                }
            }, 1000);
        }
        else if(ispermissionGranted) {
            showSettingsAlert();
        }
    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        switch (id){
            case ApplicationInstance.LOADER_FETCH_ALL_LOCATION :
                return new CursorLoader(this, FLCPConstants.CONTENT_URI_PREF_LOC,
                        new String[]{PrefLocationDO.LOCATIONID, PrefLocationDO.LOCATIONNAME, PrefLocationDO.ADDRESS,
                                PrefLocationDO.LATITUDE, PrefLocationDO.LONGITUDE},
                        null,
                        null,
                        null);
            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader loader, Object data) {
        switch (loader.getId()){
            case ApplicationInstance.LOADER_FETCH_ALL_LOCATION :
                if(data instanceof Cursor) {
                    Cursor cursor = (Cursor) data;
                    if(cursor != null && cursor.moveToFirst()){
                        PrefLocationDO objPrefLocationDO = null;
                        arrPrefLocationDO.clear();
                        do {
                            objPrefLocationDO = new PrefLocationDO();
                            objPrefLocationDO.LocationId = StringUtils.getInt(cursor.getString(cursor.getColumnIndex(PrefLocationDO.LOCATIONID)));
                            objPrefLocationDO.LocationName = cursor.getString(cursor.getColumnIndex(PrefLocationDO.LOCATIONNAME));
                            objPrefLocationDO.Address = cursor.getString(cursor.getColumnIndex(PrefLocationDO.ADDRESS));
                            objPrefLocationDO.Latitude = StringUtils.getDouble(cursor.getString(cursor.getColumnIndex(PrefLocationDO.LATITUDE)));
                            objPrefLocationDO.Longitude = StringUtils.getDouble(cursor.getString(cursor.getColumnIndex(PrefLocationDO.LONGITUDE)));

                            arrPrefLocationDO.add(objPrefLocationDO);
                        } while (cursor.moveToNext());

                        // Get the geofences used. Geofence data is hard coded in this sample.
                        populateGeofenceList();
                    } else {
                        showCurrentLocation();
                    }
                }
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader loader) {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1) {
            buildGoogleApiClient();

            if(mGoogleApiClient != null)
                mGoogleApiClient.connect();
        }
    }

    private void showCurrentLocation(){
        if(currentLatLng != null) {
            if(mMap!=null) {
//                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng,16.0f));
                MarkerOptions markerOptions = new MarkerOptions().position(currentLatLng);
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
                markerOptions.title("Your Location");
                mMap.addMarker(markerOptions);
                mMap.addMarker(markerOptions).showInfoWindow();
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng,15.0f));
            }
        } else {
            Toast.makeText(this, "Unable to fetch your current location please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showLocations(){
        if(arrPrefLocationDO != null && arrPrefLocationDO.size() > 0)
        {
            if(mMap!=null)
            {
                mMap.clear();
                for(PrefLocationDO objPrefLocationDO : arrPrefLocationDO){
                    LatLng latLngFarm = new LatLng(objPrefLocationDO.Latitude,objPrefLocationDO.Longitude);
                    MarkerOptions markerOptions = new MarkerOptions().position(latLngFarm);
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
                    markerOptions.title(objPrefLocationDO.LocationName);
                    mMap.addMarker(markerOptions);
//                    mMap.addMarker(markerOptions).showInfoWindow();
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLngFarm,15.0f));

                    //Instantiates a new CircleOptions object +  center/radius
                    CircleOptions circleOptions = new CircleOptions()
                            .center(latLngFarm)
                            .radius(AppConstant.GEOFENCE_RADIUS_IN_METERS)
                            .fillColor(ColorUtils.getColor(GeoFenceActivity.this, R.color.color_Light_Pink))
                            .strokeColor(ColorUtils.getColor(GeoFenceActivity.this, R.color.color_SkyBlue))
                            .strokeWidth(2);

                        // Get back the mutable Circle
                    mMap.addCircle(circleOptions);
                }
            }
        }

        showCurrentLocation();
    }

    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(mGeofenceList);
        return builder.build();
    }

    private void initialiseControls(){
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }
}

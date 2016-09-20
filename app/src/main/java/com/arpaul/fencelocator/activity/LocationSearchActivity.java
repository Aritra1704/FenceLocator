package com.arpaul.fencelocator.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.arpaul.customalertlibrary.popups.statingDialog.CustomPopupType;
import com.arpaul.fencelocator.R;
import com.arpaul.fencelocator.common.AppPreference;
import com.arpaul.fencelocator.common.ApplicationInstance;
import com.arpaul.fencelocator.dataAccess.FLCPConstants;
import com.arpaul.fencelocator.dataAccess.InsertDataType;
import com.arpaul.fencelocator.dataAccess.InsertLoader;
import com.arpaul.fencelocator.dataObject.PrefLocationDO;
import com.arpaul.gpslibrary.fetchAddressGeoCode.AddressConstants;
import com.arpaul.gpslibrary.fetchAddressGeoCode.AddressDO;
import com.arpaul.gpslibrary.fetchAddressGeoCode.FetchAddressLoader;
import com.arpaul.gpslibrary.fetchAddressGeoCode.FetchGeoCodeLoader;
import com.arpaul.gpslibrary.fetchLocation.GPSCallback;
import com.arpaul.gpslibrary.fetchLocation.GPSErrorCode;
import com.arpaul.gpslibrary.fetchLocation.GPSUtills;
import com.arpaul.utilitieslib.LogUtils;
import com.arpaul.utilitieslib.PermissionUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

/**
 * Created by Aritra on 19-09-2016.
 */
public class LocationSearchActivity extends BaseActivity implements GPSCallback,
        OnMapReadyCallback,
        GoogleMap.OnCameraChangeListener,
        LoaderManager.LoaderCallbacks {

    private View llLocSearchActivity;
    private final String LOG_TAG ="FenceLocator";

    private ImageView ivLocation, ivCross;
    private GoogleMap mMap;
    private EditText edtAddress;
    private Button btnSave;
    private SupportMapFragment mapFragment;
    private LatLng currentLatLng = null;
    private GPSUtills gpsUtills;
    private boolean isGpsEnabled;
    private boolean ispermissionGranted = false;
    private MaterialDialog mdFilter;
    private static int HANDLER_TIME_OUT = 2500;
    private int locationId = 1;

    @Override
    public void initialize() {
        llLocSearchActivity = baseInflater.inflate(R.layout.activity_location_search,null);
        llBody.addView(llLocSearchActivity, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);

        initialiseControls();

        bindControls();
    }

    private void bindControls(){
        gpsUtills = GPSUtills.getInstance(LocationSearchActivity.this);
        gpsUtills.setLogEnable(true);
        gpsUtills.setPackegeName(getPackageName());
        gpsUtills.setListner(LocationSearchActivity.this);

        if(new PermissionUtils().checkPermission(this, new String[]{
                android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION}) != 0){
            new PermissionUtils().verifyLocation(this,new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
        }
        else{
            createGPSUtils();
        }

        ivCross.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                edtAddress.setText("");
            }
        });

        edtAddress.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    performLocationSearch();

                    hideKeyBoard();
                    return true;
                }
                return false;
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(!TextUtils.isEmpty(edtAddress.getText().toString()))
                    saveLocation();
                else
                    showCustomDialog(getString(R.string.alert),getString(R.string.location_address_empty),getString(R.string.ok),null,getString(R.string.location_address_empty), CustomPopupType.DIALOG_ALERT,false);
            }
        });
    }

    private void createGPSUtils(){
        gpsUtills.isGpsProviderEnabled();
    }

    private void saveLocation(){

        boolean wrapInScrollView = true;
        MaterialDialog.Builder builder = new MaterialDialog.Builder(this)
                .title(R.string.location)
                .cancelable(false)
                .customView(R.layout.dialog_savelocation, wrapInScrollView);

        if(mdFilter == null)
            mdFilter =  builder.build();

        View view = mdFilter.getCustomView();

        final EditText edtLocationName        = (EditText) view.findViewById(R.id.edtLocationName);
        TextView tvAddress                    = (TextView) view.findViewById(R.id.tvAddress);

        final String address = edtAddress.getText().toString();
        tvAddress.setText(address);

        edtLocationName.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    hideKeyBoard();
                    handled = true;
                }
                return handled;
            }
        });

        builder.positiveText(R.string.accept)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(final @NonNull MaterialDialog dialog, @NonNull DialogAction which) {

                        if(!TextUtils.isEmpty(edtLocationName.getText().toString())){
                            String locationName = edtLocationName.getText().toString();

                            Uri CONTENT_URI = Uri.parse(FLCPConstants.CONTENT + FLCPConstants.CONTENT_AUTHORITY + FLCPConstants.DELIMITER +
                                    FLCPConstants.PREFERRED_LOCATION_TABLE_NAME + FLCPConstants.DELIMITER);
                            Cursor cursor = getContentResolver().query(CONTENT_URI,
                                    new String[]{"MAX(" + PrefLocationDO.LOCATIONID + ") AS " + PrefLocationDO.MAXLOCATIONID},
                                    null,
                                    null,
                                    null);

                            if (cursor != null && cursor.moveToFirst() && cursor.getCount() > 0)
                                locationId = cursor.getInt(cursor.getColumnIndex(PrefLocationDO.MAXLOCATIONID)) + 1;

                            PrefLocationDO objPrefLocationDO = new PrefLocationDO();
                            objPrefLocationDO.LocationId = locationId;
                            objPrefLocationDO.LocationName = locationName;
                            objPrefLocationDO.Address = address;
                            objPrefLocationDO.Latitude = currentLatLng.latitude;
                            objPrefLocationDO.Longitude = currentLatLng.longitude;

                            Bundle bundle = new Bundle();
                            bundle.putSerializable(InsertLoader.BUNDLE_INSERTLOADER,objPrefLocationDO);

                            getSupportLoaderManager().initLoader(ApplicationInstance.LOADER_SAVE_LOCATION, bundle, LocationSearchActivity.this).forceLoad();

                            /*if(uri != null){
                                showCustomDialog(getString(R.string.success),getString(R.string.location_successfuly_added),null,null,getString(R.string.location_successfuly_added),false);
                                new Handler().postDelayed(new Runnable() {

                                    @Override
                                    public void run() {
                                        hideCustomDialog();
                                        dialog.dismiss();

                                        preference.saveStringInPreference(AppPreference.PREF_LOC, ""+locationId);
                                        Intent resultIntent = new Intent();
                                        resultIntent.putExtra("address",address);
                                        setResult(1001);

                                        finish();
                                    }
                                }, HANDLER_TIME_OUT);
                            }*/
                        }
                    }
                });

        builder.negativeText(R.string.discard)
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();
                    }
                });
        try{
            if (mdFilter == null || !mdFilter.isShowing()){
                mdFilter = builder.build();
                mdFilter.show();
            }
        }catch(Exception e){
            e.printStackTrace();
        }

    }

    private void performLocationSearch(){
        String address = edtAddress.getText().toString();
        if(TextUtils.isEmpty(address))
            showCustomDialog(getString(R.string.alert),getString(R.string.please_enter_proper_address),getString(R.string.ok),null,getString(R.string.please_enter_proper_address), CustomPopupType.DIALOG_ALERT,false);
        else {
            if(getSupportLoaderManager().getLoader(ApplicationInstance.LOADER_FETCH_LOCATION) != null )
                getSupportLoaderManager().restartLoader(ApplicationInstance.LOADER_FETCH_LOCATION, null, this).forceLoad();
            else
                getSupportLoaderManager().initLoader(ApplicationInstance.LOADER_FETCH_LOCATION, null, this).forceLoad();
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if(gpsUtills != null && ispermissionGranted && !isGpsEnabled){
            gpsUtills.isGpsProviderEnabled();
        }
        if(isGpsEnabled) {
            new Handler().postDelayed(new Runnable()
            {
                @Override
                public void run()
                {
                    gpsUtills.getCurrentLatLng();
                    showCurrentLocation();
                    mMap.setOnCameraChangeListener(LocationSearchActivity.this);
                }
            }, 1000);
        }
        else if(ispermissionGranted) {
            showSettingsAlert();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1) {
            ispermissionGranted = true;
            gpsUtills.connectGoogleApiClient();
            createGPSUtils();

            getCurrentLocation();
        }
    }

    @Override
    public void gotGpsValidationResponse(Object response, GPSErrorCode code)
    {
        if(code == GPSErrorCode.EC_GPS_PROVIDER_NOT_ENABLED) {
            isGpsEnabled = false;
            showCustomDialog(getString(R.string.gpssettings),getString(R.string.gps_not_enabled),getString(R.string.settings),getString(R.string.cancel),getString(R.string.settings), CustomPopupType.DIALOG_ALERT,false);
        }
        else if(code == GPSErrorCode.EC_GPS_PROVIDER_ENABLED) {
            isGpsEnabled = true;
            gpsUtills.getCurrentLatLng();
        }
        else if(code == GPSErrorCode.EC_UNABLE_TO_FIND_LOCATION) {
            currentLatLng = (LatLng) response;
        }
        else if(code == GPSErrorCode.EC_LOCATION_FOUND) {
            currentLatLng = (LatLng) response;
            LogUtils.debugLog("GPSTrack", "Currrent latLng :"+currentLatLng.latitude+" \n"+currentLatLng.longitude);

            //loader.hideLoader();
            showCurrentLocation();
            gpsUtills.stopLocationUpdates();
        }
        else if(code == GPSErrorCode.EC_CUSTOMER_LOCATION_IS_VALID) {
        }
        else if(code == GPSErrorCode.EC_CUSTOMER_lOCATION_IS_INVAILD) {
        }
        else if(code == GPSErrorCode.EC_DEVICE_CONFIGURED_PROPERLY) {
            startIntentService();
        }
    }

    protected void startIntentService() {
        getSupportLoaderManager().restartLoader(ApplicationInstance.LOADER_FETCH_ADDRESS, null, this).forceLoad();
    }

    private void getCurrentLocation(){
        new Handler().postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                gpsUtills.getCurrentLatLng();
                showCurrentLocation();
            }
        }, 2 * 1000);
    }

    private void showCurrentLocation(){
        if(currentLatLng != null)
        {
            if(mMap!=null)
            {
                mMap.clear();
                /*final MarkerOptions markerOptions = new MarkerOptions().position(currentLatLng);
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
                markerOptions.title("Your Location");
                mMap.addMarker(markerOptions);*/
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng,16.0f));

                getSupportLoaderManager().initLoader(ApplicationInstance.LOADER_FETCH_ADDRESS, null, this).forceLoad();
            }
        }
        else {
            Toast.makeText(this, "Unable to fetch your current location please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    public void setFailureAddress(String message){
        showCustomDialog(getString(R.string.failure),message,getString(R.string.ok),null,getString(R.string.failure), CustomPopupType.DIALOG_FAILURE,false);
        edtAddress.setText("");
    }

    private void setAddress(String message){
        if(message.contains("\n"))
            message = message.replace("\n", " ");
        edtAddress.setText(message);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(gpsUtills != null && ispermissionGranted){
            gpsUtills.connectGoogleApiClient();

            getCurrentLocation();
        }
    }

    @Override
    protected void onStop() {
        gpsUtills.stopLocationUpdates();
        super.onStop();
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {

        currentLatLng = cameraPosition.target;
    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        switch(id){
            case ApplicationInstance.LOADER_FETCH_ADDRESS:
                return new FetchAddressLoader(this, currentLatLng);

            case ApplicationInstance.LOADER_FETCH_LOCATION:
                return new FetchGeoCodeLoader(this, edtAddress.getText().toString());

            case ApplicationInstance.LOADER_SAVE_LOCATION:
                return new InsertLoader(this, InsertDataType.INSERT_PREF_LOC, args);

            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader loader, final Object data) {
        switch(loader.getId()){
            case ApplicationInstance.LOADER_FETCH_ADDRESS:
                if(data instanceof AddressDO){
                    AddressDO objAddressDO = (AddressDO) data;
                    if(objAddressDO.code == AddressConstants.SUCCESS_RESULT)
                        setAddress(objAddressDO.message);
                    else if(objAddressDO.code == AddressConstants.FAILURE_RESULT)
                        setFailureAddress(objAddressDO.message);
                }
                break;

            case ApplicationInstance.LOADER_FETCH_LOCATION:
                if(data instanceof AddressDO) {
                    AddressDO objAddressDO = (AddressDO) data;
                    if(objAddressDO.code == AddressConstants.SUCCESS_RESULT){
                        LatLng latlng = new LatLng(objAddressDO.location.getLatitude(),objAddressDO.location.getLongitude());

                        currentLatLng = latlng;
                        showCurrentLocation();
                    } else if(objAddressDO.code == AddressConstants.FAILURE_RESULT)
                        setFailureAddress(objAddressDO.message);
                }
                break;

            case ApplicationInstance.LOADER_SAVE_LOCATION:
                if(data instanceof String){
                    if(data != null && !TextUtils.isEmpty((String) data)){
                        showCustomDialog(getString(R.string.success),getString(R.string.location_successfuly_added),null,null,getString(R.string.location_successfuly_added), CustomPopupType.DIALOG_SUCCESS,false);
                        new Handler().postDelayed(new Runnable() {

                            @Override
                            public void run() {
                                hideCustomDialog();
//                            dialog.dismiss();//Need to think

                                preference.saveStringInPreference(AppPreference.PREF_LOC, ""+locationId);
                                Intent resultIntent = new Intent();
                                resultIntent.putExtra("address",(String) data);
                                setResult(1001);

                                finish();
                            }
                        }, HANDLER_TIME_OUT);
                    }
                }
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader loader) {

    }

    private void initialiseControls(){
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        ivLocation          = (ImageView) llLocSearchActivity.findViewById(R.id.ivLocation);
        ivCross             = (ImageView) llLocSearchActivity.findViewById(R.id.ivCross);
        edtAddress          = (EditText) llLocSearchActivity.findViewById(R.id.edtAddress);
        btnSave             = (Button) llLocSearchActivity.findViewById(R.id.btnSave);
    }
}

package com.arpaul.fencelocator.activity;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.arpaul.fencelocator.R;
import com.arpaul.fencelocator.adapter.GeoLocationsAdapter;
import com.arpaul.fencelocator.common.ApplicationInstance;
import com.arpaul.fencelocator.dataAccess.FLCPConstants;
import com.arpaul.fencelocator.dataObject.PrefLocationDO;
import com.arpaul.utilitieslib.StringUtils;

import java.util.ArrayList;

public class FenceActivity extends BaseActivity implements LoaderManager.LoaderCallbacks {

    private View llFenceActivity;
    private Toolbar toolbar;
    private FloatingActionButton fabAddLocation, fabGeoFence;
    private TextView tvNoLocations;
    private RecyclerView rvGeoLocations;
    private ArrayList<PrefLocationDO> arrPrefLocationDO = new ArrayList<>();
    private GeoLocationsAdapter adapter;

    @Override
    public void initialize() {
        llFenceActivity = baseInflater.inflate(R.layout.activity_fence,null);
        llBody.addView(llFenceActivity, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);

        initialiseControls();

        bindControls();
    }

    private void bindControls(){
        fabAddLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(FenceActivity.this, LocationSearchActivity.class));
            }
        });

        fabGeoFence.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(FenceActivity.this, GeoFenceActivity.class));
            }
        });

        getSupportLoaderManager().initLoader(ApplicationInstance.LOADER_FETCH_ALL_LOCATION, null, this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        getSupportLoaderManager().restartLoader(ApplicationInstance.LOADER_FETCH_ALL_LOCATION, null, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_fence, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
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

                        if(arrPrefLocationDO != null && arrPrefLocationDO.size() > 0){
                            tvNoLocations.setVisibility(View.GONE);
                            rvGeoLocations.setVisibility(View.VISIBLE);

                            adapter.refresh(arrPrefLocationDO);
                        } else {
                            tvNoLocations.setVisibility(View.VISIBLE);
                            rvGeoLocations.setVisibility(View.GONE);
                        }

                    }
                }
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader loader) {

    }

    private void initialiseControls(){
        toolbar = (Toolbar) llFenceActivity.findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        fabAddLocation = (FloatingActionButton) llFenceActivity.findViewById(R.id.fabAddLocation);
        fabGeoFence = (FloatingActionButton) llFenceActivity.findViewById(R.id.fabGeoFence);

        tvNoLocations = (TextView) llFenceActivity.findViewById(R.id.tvNoLocations);
        rvGeoLocations = (RecyclerView) llFenceActivity.findViewById(R.id.rvGeoLocations);

        adapter = new GeoLocationsAdapter(FenceActivity.this, new ArrayList<PrefLocationDO>());
        rvGeoLocations.setAdapter(adapter);
    }
}

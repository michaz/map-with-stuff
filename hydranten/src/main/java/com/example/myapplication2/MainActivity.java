package com.example.myapplication2;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.UrlTileProvider;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity implements GooglePlayServicesClient.ConnectionCallbacks, GooglePlayServicesClient.OnConnectionFailedListener, GoogleMap.OnMapClickListener, GoogleMap.OnMarkerClickListener {

    private GoogleMap mMap;
    private BitmapDescriptor mFireStation;
    private BitmapDescriptor mHydrantMarker;
    private BitmapDescriptor mAboveGround;
    private BitmapDescriptor mBelowGround;
    private GoogleApiClient mGoogleApiClient;
    private HydrantDetailsFragment mHydrantDetails;
    private Map<Marker, Overpass.Hydrant> mHydrantMarkers = new HashMap<Marker, Overpass.Hydrant>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setUpMapIfNeeded();
        mHydrantDetails = (HydrantDetailsFragment) getFragmentManager().findFragmentById(R.id.hydrant_details);
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.hide(mHydrantDetails);
        ft.commit();
        mFireStation = BitmapDescriptorFactory.fromResource(R.drawable.firestation);
        mHydrantMarker = BitmapDescriptorFactory.fromResource(R.drawable.hydrant);
        mAboveGround = BitmapDescriptorFactory.fromResource(R.drawable.above_ground);
        mBelowGround = BitmapDescriptorFactory.fromResource(R.drawable.below_ground);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private void setUpMapIfNeeded() {
        if (mMap == null) {
            mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
            if (mMap != null) {
                mMap.setMyLocationEnabled(true);
                mMap.addTileOverlay(new TileOverlayOptions().tileProvider(new UrlTileProvider(256, 256) {
                    @Override
                    public URL getTileUrl(int x, int y, int zoom) {
                        try {
                            return new URL("http://www.openfiremap.org/hytiles/" + zoom + "/" + x + "/" + y + ".png");
                        } catch (MalformedURLException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }));
                mMap.setOnMarkerClickListener(this);
                mMap.setOnMapClickListener(this);
                new DownloadFireStationsTask().execute();
                new DownloadHydrantsTask().execute();
            }
        }
    }

    @Override
    public void onConnected(Bundle bundle) {

    }

    @Override
    public void onDisconnected() {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_create:
                printLocation();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void printLocation() {
        Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        Toast toast = Toast.makeText(getApplicationContext(), Double.toString(lastLocation.getAccuracy()), Toast.LENGTH_SHORT);
        toast.show();
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        Overpass.Hydrant hydrant = mHydrantMarkers.get(marker);
        if (hydrant != null) {
            mHydrantDetails.setHydrant(hydrant);
            if (mHydrantDetails.isHidden()) {
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.show(mHydrantDetails);
                ft.commit();
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onMapClick(LatLng latLng) {
        if (!mHydrantDetails.isHidden()) {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.hide(mHydrantDetails);
            ft.commit();
        }
    }

    private class DownloadHydrantsTask extends AsyncTask<Void, Void, List<Overpass.Hydrant>> {

        @Override
        protected List<Overpass.Hydrant> doInBackground(Void... v) {
            List<Overpass.Hydrant> hydrants = new ArrayList<Overpass.Hydrant>();
            try {
                hydrants = Overpass.getHydrants();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            }
            return hydrants;
        }

        @Override
        protected void onPostExecute(List<Overpass.Hydrant> hydrants) {
            for (Overpass.Hydrant hydrant : hydrants) {
                BitmapDescriptor icon;
                if ("underground".equals(hydrant.type)) {
                    icon = mBelowGround;
                } else if ("pillar".equals(hydrant.type)) {
                    icon = mAboveGround;
                } else {
                    icon = mHydrantMarker;
                }
                Marker marker = mMap.addMarker(new MarkerOptions()
                        .position(hydrant.latLng)
                        .icon(icon));
                mHydrantMarkers.put(marker, hydrant);
            }
        }
    }

    private class DownloadFireStationsTask extends AsyncTask<Void, Void, List<Overpass.FireStation>> {

        @Override
        protected List<Overpass.FireStation> doInBackground(Void... v) {
            List<Overpass.FireStation> hydrants = new ArrayList<Overpass.FireStation>();
            try {
                hydrants = Overpass.getFireStations();
                hydrants.addAll(Overpass.getFireStationsWays());
            } catch (IOException e) {
                e.printStackTrace();
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            }
            return hydrants;
        }

        @Override
        protected void onPostExecute(List<Overpass.FireStation> fireStations) {
            for (Overpass.FireStation fireStation : fireStations) {
                mMap.addMarker(new MarkerOptions()
                        .position(fireStation.latLng)
                        .icon(mFireStation));
            }
        }
    }


}

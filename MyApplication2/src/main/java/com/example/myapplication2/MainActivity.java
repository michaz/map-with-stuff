package com.example.myapplication2;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MainActivity extends Activity {

    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setUpMapIfNeeded();
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
                for (int i=0;i<1520;i++) {
                    double lon = 13.3125 + Math.random() * (13.4595-13.3125);
                    double lat = 52.4842 + Math.random() * (52.5542-52.4842);
                    mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(lat, lon))
                    .title("Hydrant Nr. "+ i));
                }

            }
        }
    }


}

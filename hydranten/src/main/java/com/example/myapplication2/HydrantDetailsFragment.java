package com.example.myapplication2;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by michaelzilske on 22/12/13.
 */
public class HydrantDetailsFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.hydrant_details, container);
    }

    public void setHydrant(Overpass.Hydrant hydrant) {
        TextView type = (TextView) getView().findViewById(R.id.hydrant_type);
        if ("underground".equals(hydrant.type)) {
            type.setText("Unterflurhydrant");
        } else if ("pillar".equals(hydrant.type)) {
            type.setText("Überflurhydrant");
        } else {
            type.setText("<unbekannter Typ>");
        }
    }
}

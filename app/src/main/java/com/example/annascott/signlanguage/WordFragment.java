package com.example.annascott.signlanguage;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class WordFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.word, container, false);
        Button searchButton = (Button) rootView.findViewById(R.id.search_button);
        final View peaceView = (View) rootView.findViewById(R.id.peace_view);
        searchButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // your handler code here
                peaceView.setVisibility(View.VISIBLE);
            }
        });
    return rootView;} }

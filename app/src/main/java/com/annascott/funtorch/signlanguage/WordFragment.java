package com.annascott.funtorch.signlanguage;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

public class WordFragment extends Fragment {
    String word;
    Button searchButton1;
    View peaceView;
    EditText word_to_lookup;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.word, container, false);
        searchButton1 = (Button) rootView.findViewById(R.id.search_button_1);
        peaceView = (View) rootView.findViewById(R.id.peace_view);
        word_to_lookup = (EditText) rootView.findViewById(R.id.word);
        searchButton1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                word = word_to_lookup.getText().toString().trim();
                if (word.equalsIgnoreCase("zero")){
                    peaceView.setBackgroundResource(R.drawable.zero);
                }
                else if (word.equalsIgnoreCase("one")){
                    peaceView.setBackgroundResource(R.drawable.one);
                }
                else if (word.equalsIgnoreCase("two")){
                    peaceView.setBackgroundResource(R.drawable.two);
                }
                else if (word.equalsIgnoreCase("three")){
                    peaceView.setBackgroundResource(R.drawable.three);
                }
                else if (word.equalsIgnoreCase("four")){
                    peaceView.setBackgroundResource(R.drawable.four);
                }
                else if (word.equalsIgnoreCase("five")){
                    peaceView.setBackgroundResource(R.drawable.five);
                }
                else if (word.equalsIgnoreCase("six")){
                    peaceView.setBackgroundResource(R.drawable.six);
                }
                else if (word.equalsIgnoreCase("nine")){
                    peaceView.setBackgroundResource(R.drawable.nine);
                }
                else if (word.equalsIgnoreCase("seven")){
                    peaceView.setBackgroundResource(R.drawable.seven);
                }
                else if (word.equalsIgnoreCase("eight")){
                    peaceView.setBackgroundResource(R.drawable.eight);
                }
                else {
                    peaceView.setBackgroundResource(R.drawable.spelling_check);
                }
            }
        });


    return rootView;} }

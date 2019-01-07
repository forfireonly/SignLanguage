package com.annascott.funtorch.signlanguage;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;


//import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class SignFragment extends Fragment {
    private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 1888;
    Button button, searchButton;
    ImageView  peaceView;
    Bitmap capturedBmp;

    static {
        System.loadLibrary("tensorflow_inference");
    }

    //PATH TO OUR MODEL FILE AND NAMES OF THE INPUT AND OUTPUT NODES
    private String MODEL_PATH = "file:///android_asset/sign_lang_net.pb";
    private String INPUT_NAME = "0";
    private String OUTPUT_NAME = "add_13:0";

    //    private TensorFlowInferenceInterface tf;
    private ImageClassifier classifier;

    Bitmap bitmap;

    //ARRAY TO HOLD THE PREDICTIONS AND FLOAT VALUES TO HOLD THE IMAGE DATA
    float[] PREDICTIONS = new float[1000];
    private float[] floatValues;
    private int[] INPUT_SIZE = {64,64,1};

    ImageView imageView;
    TextView resultView;
    TextView resultProbaView;
    LinearLayout resultLayout;
    Snackbar progressBar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.sign,
                container, false);

        // tf = new TensorFlowInferenceInterface(getContext().getAssets(),MODEL_PATH);
        try {
            classifier = new ImageClassifier(this.getActivity());
            classifier.useCPU();
        } catch (IOException e) {
            Log.e("Sign Language","FAILED TO INIT MODEL " + e.getMessage());
        }

        // progressBar = Snackbar.make(imageView,"PROCESSING IMAGE",Snackbar.LENGTH_INDEFINITE);


        button = (Button) rootView.findViewById(R.id.camera_button);
        imageView = (ImageView) rootView.findViewById(R.id.camera_image);
        searchButton = (Button) rootView.findViewById(R.id.search_button);
        resultLayout = (LinearLayout) rootView.findViewById(R.id.result_layout);
        resultView = (TextView) rootView.findViewById(R.id.class_name);
        resultProbaView = (TextView) rootView.findViewById(R.id.class_proba);

        //peaceView = (ImageView) rootView.findViewById(R.id.peace_image);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent,
                        CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);

            }
        });

        searchButton.setEnabled(false);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               // imageView.setImageResource ( R.drawable.number_four);

                try{
                    predict(capturedBmp);
                }
                catch (Exception e){

                }


            }
        });
        return rootView;

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {

                Bitmap bmp = (Bitmap) data.getExtras().get("data");
                ByteArrayOutputStream stream = new ByteArrayOutputStream();

                bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
                byte[] byteArray = stream.toByteArray();

                // convert byte array to Bitmap

                bitmap = BitmapFactory.decodeByteArray(byteArray, 0,
                        byteArray.length);

                capturedBmp = bitmap;
                imageView.setImageBitmap(bitmap);

                // Update Result Layout
                resultLayout.setVisibility(View.GONE);

                // Update Search Button State
                searchButton.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                searchButton.setEnabled(true);
            } else {
                // Update Search Button State
                searchButton.setBackgroundColor(getResources().getColor(R.color.colorDisabled));
                searchButton.setEnabled(false);
            }
        }
    }

    //FUNCTION TO COMPUTE THE MAXIMUM PREDICTION AND ITS CONFIDENCE
    public Object[] argmax(float[] array){


        int best = -1;
        float best_confidence = 0.0f;

        for(int i = 0;i < array.length;i++){

            float value = array[i];

            if (value > best_confidence){

                best_confidence = value;
                best = i;
            }
        }

        return new Object[]{best,best_confidence};


    }

    @SuppressLint("StaticFieldLeak")
    public void predict(final Bitmap bitmap){

        //Runs inference in background thread
        new AsyncTask<Integer,Integer,Integer>(){

            @Override
            protected Integer doInBackground(Integer ...params){
                Bitmap resized_image = ImageUtils.processBitmap(bitmap,64);
                classifier.classifyFrame(resized_image);

                Log.e("Sign Language", "PREDICTED LABEL : " + classifier.prediction);
                return 0;
            }

            @Override
            protected void onPostExecute(Integer result) {
                // Update result View
                resultLayout.setVisibility(View.VISIBLE);
                resultProbaView.setText(String.format("%4.2f", classifier.proba ) + "%");
                resultView.setText(classifier.prediction);

                // Update Search Button State
                searchButton.setBackgroundColor(getResources().getColor(R.color.colorDisabled));
                searchButton.setEnabled(false);
            }


        }.execute(0);

    }

}

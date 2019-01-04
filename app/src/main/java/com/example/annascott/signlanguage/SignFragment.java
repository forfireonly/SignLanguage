package com.example.annascott.signlanguage;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.List;

import static android.content.ContentValues.TAG;

public class SignFragment extends Fragment {
    private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 1888;
    Button button, searchButton;
    ImageView  peaceView;

    static {
        System.loadLibrary("tensorflow_inference");
    }

    //PATH TO OUR MODEL FILE AND NAMES OF THE INPUT AND OUTPUT NODES
    private String MODEL_PATH = "file:///android_asset/sign_lang_net.pb";
    private String INPUT_NAME = "0:0";
    private String OUTPUT_NAME = "add_16:0";
    private TensorFlowInferenceInterface tf;

    Bitmap bitmap;

    //ARRAY TO HOLD THE PREDICTIONS AND FLOAT VALUES TO HOLD THE IMAGE DATA
    float[] PREDICTIONS = new float[1000];
    private float[] floatValues;
    private int[] INPUT_SIZE = {224,224,3};

    ImageView imageView;
    TextView resultView;
    Snackbar progressBar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.sign,
                container, false);



        resultView = (TextView) rootView.findViewById(R.id.class_name);

        tf = new TensorFlowInferenceInterface(getContext().getAssets(),MODEL_PATH);

       // progressBar = Snackbar.make(imageView,"PROCESSING IMAGE",Snackbar.LENGTH_INDEFINITE);


        button = (Button) rootView.findViewById(R.id.camera_button);
        imageView = (ImageView) rootView.findViewById(R.id.camera_image);
        searchButton = (Button) rootView.findViewById(R.id.search_button);
        //peaceView = (ImageView) rootView.findViewById(R.id.peace_image);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent,
                        CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);

            }
        });

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               // imageView.setImageResource ( R.drawable.number_four);


                try{

                    //READ THE IMAGE FROM ASSETS FOLDER
                   InputStream imageStream = getContext().getAssets().open("eight.png");

                    Bitmap bitmap = BitmapFactory.decodeStream(imageStream);

                    imageView.setImageBitmap(bitmap);

                   // progressBar.show();

                    predict(bitmap);
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

                imageView.setImageBitmap(bitmap);

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

                //Resize the image into 224 x 224
                Bitmap resized_image = ImageUtils.processBitmap(bitmap,224);

                //Normalize the pixels
                floatValues = ImageUtils.normalizeBitmap(resized_image,224,127.5f,1.0f);

                //Pass input into the tensorflow
                tf.feed(INPUT_NAME,floatValues,1,3,224,224);

               // Log.w("myApp", "input");
                //compute predictions
                tf.run(new String[]{OUTPUT_NAME});

                //Log.w("myApp", "output");

                //copy the output into the PREDICTIONS array
                tf.fetch(OUTPUT_NAME,PREDICTIONS);

               // Log.w("myApp", "predictions");
                //Obtained highest prediction
                Object[] results = argmax(PREDICTIONS);


                int class_index = (Integer) results[0];
                float confidence = (Float) results[1];


                try{

                    final String conf = String.valueOf(confidence * 100).substring(0,5);



                    //Convert predicted class index into actual label name
                    final String label = ImageUtils.getLabel(getContext().getAssets().open("labels.json"),class_index);



                    //Display result on UI
                    getActivity().runOnUiThread(new Runnable() {
                       @Override
                        public void run() {

                            progressBar.dismiss();
                            resultView.setText(label + " : " + conf + "%");

                        }
                    });

                }

                catch (Exception e){


               }


                return 0;
            }



        }.execute(0);

    }

}

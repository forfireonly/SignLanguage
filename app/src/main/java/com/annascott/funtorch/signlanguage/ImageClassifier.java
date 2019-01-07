/* Copyright 2017 The TensorFlow Authors. All Rights Reserved.
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package com.annascott.funtorch.signlanguage;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.util.Log;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import org.tensorflow.lite.Interpreter;

/**
 * Classifies images with Tensorflow Lite.
 */
public class ImageClassifier {
    private float[][] labelProbArray = null;

    // Display preferences
    private static final float GOOD_PROB_THRESHOLD = 0.3f;
    private static final int SMALL_COLOR = 0xffddaa88;

    /**
     * The net requires additional normalization of the used input.
     */
    private static final float IMAGE_MEAN = 127.5f;
    private static final float IMAGE_STD = 127.5f;

    /** Tag for the {@link Log}. */
    private static final String TAG = "Sign Language";

    /** Number of results to show in the UI. */
    private static final int RESULTS_TO_SHOW = 10;

    /** Dimensions of inputs. */
    private static final int DIM_BATCH_SIZE = 1;

    private static final int DIM_PIXEL_SIZE = 1;

    /** Preallocated buffers for storing image data in. */
    private int[] intValues = new int[getImageSizeX() * getImageSizeY()];

    /** Options for configuring the Interpreter. */
    private final Interpreter.Options tfliteOptions = new Interpreter.Options();

    /** The loaded TensorFlow Lite model. */
    private MappedByteBuffer tfliteModel;

    /** An instance of the driver class to run model inference with Tensorflow Lite. */
    protected Interpreter tflite;

    /** Labels corresponding to the output of the vision model. */
    private List<String> labelList;

    /** A ByteBuffer to hold image data, to be feed into Tensorflow Lite as inputs. */
    protected ByteBuffer imgData = null;

    private static final int FILTER_STAGES = 3;
    private static final float FILTER_FACTOR = 0.4f;

    private PriorityQueue<Map.Entry<String, Float>> sortedLabels =
            new PriorityQueue<>(
                    RESULTS_TO_SHOW,
                    new Comparator<Map.Entry<String, Float>>() {
                        @Override
                        public int compare(Map.Entry<String, Float> o1, Map.Entry<String, Float> o2) {
                            return (o1.getValue()).compareTo(o2.getValue());
                        }
                    });

    public String prediction = null;
    public double proba = 0;

    /** holds a gpu delegate */
//    Delegate gpuDelegate = null;

    /** Initializes an {@code ImageClassifier}. */
    public ImageClassifier(Activity activity) throws IOException {
        tfliteModel = loadModelFile(activity);
        tflite = new Interpreter(tfliteModel, tfliteOptions);
        labelList = loadLabelList(activity);
        labelProbArray = new float[1][getNumLabels()];
        imgData = ByteBuffer.allocateDirect(
                        DIM_BATCH_SIZE
                                * getImageSizeX()
                                * getImageSizeY()
                                * DIM_PIXEL_SIZE
                                * getNumBytesPerChannel());
        imgData.order(ByteOrder.nativeOrder());

        Log.e(TAG, "Created a Tensorflow Lite Image Classifier.");
    }

    /** Classifies a frame from the preview stream. */
    public void classifyFrame(Bitmap bitmap) {
        if (tflite == null) {
            Log.e(TAG, "Image classifier has not been initialized; Skipped.");
        }

        convertBitmapToByteBuffer(bitmap);
        // Here's where the magic happens!!!

        long startTime = SystemClock.uptimeMillis();
        runInference();
        long endTime = SystemClock.uptimeMillis();
        Log.e(TAG, "Timecost to run model inference: " + Long.toString(endTime - startTime));

        // get the results.
        getTopKLabels();
        long duration = endTime - startTime;
    }

    private void recreateInterpreter() {
        if (tflite != null) {
            tflite.close();
            // TODO(b/120679982)
            // gpuDelegate.close();
            tflite = new Interpreter(tfliteModel, tfliteOptions);
        }
    }

    public void useGpu() {
//        if (gpuDelegate == null && GpuDelegateHelper.isGpuDelegateAvailable()) {
//            gpuDelegate = GpuDelegateHelper.createGpuDelegate();
//            tfliteOptions.addDelegate(gpuDelegate);
//            recreateInterpreter();
//        }
    }

    public void useCPU() {
        tfliteOptions.setUseNNAPI(false);
        recreateInterpreter();
    }

    public void useNNAPI() {
        tfliteOptions.setUseNNAPI(true);
        recreateInterpreter();
    }

    public void setNumThreads(int numThreads) {
        tfliteOptions.setNumThreads(numThreads);
        recreateInterpreter();
    }

    /** Closes tflite to release resources. */
    public void close() {
        tflite.close();
        tflite = null;
        tfliteModel = null;
    }

    /** Reads label list from Assets. */
    private List<String> loadLabelList(Activity activity) throws IOException {
        List<String> labels = new ArrayList<String>();

        labels.add("NINE");
        labels.add("ZERO");
        labels.add("SEVEN");
        labels.add("SIX");
        labels.add("ONE");
        labels.add("EIGHT");
        labels.add("FOUR");
        labels.add("THREE");
        labels.add("TWO");
        labels.add("FIVE");

        return labels;
    }

    /** Memory-map the model file in Assets. */
    private MappedByteBuffer loadModelFile(Activity activity) throws IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(getModelPath());
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    /** Writes Image data into a {@code ByteBuffer}. */
    private void convertBitmapToByteBuffer(Bitmap bitmap) {
        if (imgData == null) {
            return;
        }
        imgData.rewind();

        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        // Convert the image to floating point.
        int pixel = 0;
        long startTime = SystemClock.uptimeMillis();
        for (int i = 0; i < getImageSizeX(); ++i) {
            for (int j = 0; j < getImageSizeY(); ++j) {
                final int val = intValues[pixel++];
                addPixelValue(val);
            }
        }
        long endTime = SystemClock.uptimeMillis();
        Log.e(TAG, "Timecost to put values into ByteBuffer: " + Long.toString(endTime - startTime));
    }

    /** Get top-K labels */
    private void getTopKLabels() {
        sortedLabels.clear();
        for (int i = 0; i < getNumLabels(); i++) {
            sortedLabels.add(
                    new AbstractMap.SimpleEntry<>(labelList.get(i), getNormalizedProbability(i)));
            if (sortedLabels.size() > RESULTS_TO_SHOW) {
                sortedLabels.poll();
            }
        }

        Log.e(TAG, "RetrieveTopK");
        final int size = sortedLabels.size();
        double cumProba = 0;
//        Double minProba = null;
        for (int i = 0; i < size; i++) {
            Map.Entry<String, Float> label = sortedLabels.poll();
            String key = label.getKey();
            float val = label.getValue();
            Log.e(TAG, String.format("%s: %4.5f\n", key, Math.exp(val * 20)));

            prediction = key;
            cumProba += Math.exp(val * 20);
            proba = Math.exp(val * 20);
        }
        Log.e(TAG,proba + " " + cumProba);
        proba = (proba / cumProba) * 100;
        // proba = (1 / (1 + proba)) * 100;
    }

    /**
     * Get the name of the model file stored in Assets.
     *
     * @return
     */
    protected String getModelPath() {
        // you can download this file from
        // https://storage.googleapis.com/download.tensorflow.org/models/tflite/inception_v3_slim_2016_android_2017_11_10.zip
        return "sign_lang_net.tflite";
    }

    /**
     * Get the name of the label file stored in Assets.
     *
     * @return
     */
    protected String getLabelPath() {
        return "labels.json";
    }
    /**
     * Get the image size along the x axis.
     *
     * @return
     */
    protected int getImageSizeX() {
        return 64;
    }

    /**
     * Get the image size along the y axis.
     *
     * @return
     */
    protected int getImageSizeY() {
        return 64;
    }

    /**
     * Get the number of bytes that is used to store a single color channel value.
     *
     * @return
     */
    protected int getNumBytesPerChannel() {
        // a 32bit float value requires 4 bytes
        return 4;
    }

    /**
     * Add pixelValue to byteBuffer.
     *
     * @param pixelValue
     */
    protected void addPixelValue(int pixelValue){
        imgData.putFloat(
                (float) (
                    (0.21 * ((((pixelValue >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD)) +
                    (0.72 * ((((pixelValue >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD)) +
                    (0.07 * (((pixelValue & 0xFF) - IMAGE_MEAN) / IMAGE_STD))
                )
        );
    }

    /**
     * Read the probability value for the specified label This is either the original value as it was
     * read from the net's output or the updated value after the filter was applied.
     *
     * @param labelIndex
     * @return
     */
    protected float getProbability(int labelIndex){
        return labelProbArray[0][labelIndex];
    }

    /**
     * Set the probability value for the specified label.
     *
     * @param labelIndex
     * @param value
     */
    protected void setProbability(int labelIndex, Number value){
        labelProbArray[0][labelIndex] = value.floatValue();
    }

    /**
     * Get the normalized probability value for the specified label. This is the final value as it
     * will be shown to the user.
     *
     * @return
     */
    protected float getNormalizedProbability(int labelIndex) {
        return getProbability(labelIndex);
    }

    /**
     * Run inference using the prepared input in {@link #imgData}. Afterwards, the result will be
     * provided by getProbability
     * ().
     *
     * <p>This additional method is necessary, because we don't have a common base for different
     * primitive data types.
     */
    protected void runInference(){
        tflite.run(imgData, labelProbArray);
    }

    /**
     * Get the total number of labels.
     *
     * @return
     */
    protected int getNumLabels() {
        return labelList.size();
    }
}
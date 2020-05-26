package com.cactus.mycactusdisease;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import android.content.res.AssetFileDescriptor;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.Media;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Collections;

import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

import org.tensorflow.lite.Interpreter;

public class MainActivity extends AppCompatActivity {
    static final int REQUEST_IMAGE_CAPTURE = 2;
    public static final int REQUEST_GALLERY = 1;
    private ImageView imageDisplay;
    private TextView guildImage, btnCamera, btnGalery;
    private LinearLayout diagnoseDisease;
    private Bitmap imagePreBmp;

    // model tensorflow lite
    private Interpreter tflite;
    private static final int MAX_RESULTS = 3;
    private static final int BATCH_SIZE = 1;
    private static final int PIXEL_SIZE = 3;

    // presets for rgb conversion
    private static final int IMAGE_MEAN = 128;
    private static final float IMAGE_STD = 128.0f;

    // input image dimensions for the Inception Model
    private int DIM_IMG_SIZE_X = 180;
    private int DIM_IMG_SIZE_Y = 180;
    private int DIM_PIXEL_SIZE = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageDisplay = (ImageView) findViewById(R.id.imageDisplay);

        initialVariables();
        setAction();

        // First we must create the tflite object, loaded from the model file
        try {
            tflite = new Interpreter(loadModelFile());
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    private void initialVariables() {
        // Image display selected image camera/gallery
        imageDisplay = (ImageView) findViewById(R.id.imageDisplay);
        guildImage = (TextView) findViewById(R.id.guildImage);
        btnCamera = (TextView) this.findViewById(R.id.btnCamera);
        btnGalery = (TextView) findViewById(R.id.btnGallery);
        diagnoseDisease = (LinearLayout) findViewById(R.id.diagnoseDisease);
    }

    private void setAction() {
        btnCamera.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);

            }

        });

        btnGalery.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK, Media.EXTERNAL_CONTENT_URI);
                intent.setType("image/*");
                startActivityForResult(intent.createChooser(intent
                        , "Select Picture"),REQUEST_GALLERY);
            }
        });

        diagnoseDisease.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (imagePreBmp != null) {
                    int prediction = predictionCactus(imagePreBmp);
                    if (prediction != -1) {
                        Intent intent = new Intent(MainActivity.this, ResultActivity.class);
                        intent.putExtra("Max", prediction);

                        // Convert bitmap to byte array
                        ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
                        imagePreBmp.compress(Bitmap.CompressFormat.PNG, 100, byteArray);
                        intent.putExtra("Image", byteArray.toByteArray());
                        startActivity(intent);
                    }
                }
            }
        });
    }

    // Load model tensorflow lite
    private MappedByteBuffer loadModelFile() throws IOException {
        AssetFileDescriptor fileDescriptor = this.getAssets().openFd("model_cactus.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Set guild line to gone
        guildImage.setVisibility(View.GONE);
        // Set image display to visible
        imageDisplay.setVisibility(View.VISIBLE);

        if (requestCode == REQUEST_GALLERY && resultCode == RESULT_OK) {
            try {
                final Uri selectedImageUri = data.getData();
                final InputStream imageStream = getContentResolver().openInputStream(selectedImageUri);
                imagePreBmp = BitmapFactory.decodeStream(imageStream);
                imageDisplay.setImageBitmap(imagePreBmp);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            imagePreBmp = (Bitmap) extras.get("data");
            imageDisplay.setImageBitmap(imagePreBmp);
        } else {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }
    }

    // Prediction cactus
    public int predictionCactus(Bitmap bmp) {
        float[][] output = new float[1][3];
        // resize the bitmap to the required input size to the CNN
        Bitmap bitmap = getResizedBitmap(bmp, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y);
        // Convert bitmap to bytebuffer size 200*200
        ByteBuffer byteBuffer = convertBitmapToByteBuffer(bitmap);
        // Find prediction by model cactus
        tflite.run(byteBuffer, output);
        int indexOfMax = getPredicOfMaxValue(output[0]);
        return indexOfMax;
    }

    // Find max in array output
    public static int getPredicOfMaxValue(float[] numbers){
        float maxValue = numbers[0];
        int index = 0;
        for(int i=1;i < numbers.length;i++){
            if(numbers[i] > maxValue){
                maxValue = numbers[i];
                index = i;
            }
        }
        return index;
    }

    // resize bitmap to given dimensions
    public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);
        return resizedBitmap;
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * BATCH_SIZE * DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y * PIXEL_SIZE);
        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues = new int[DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        int pixel = 0;
        for (int i = 0; i < DIM_IMG_SIZE_X; ++i) {
            for (int j = 0; j < DIM_IMG_SIZE_Y; ++j) {
                final int val = intValues[pixel++];
                byteBuffer.putFloat((((val >> 16) & 0xFF)-IMAGE_MEAN)/IMAGE_STD);
                byteBuffer.putFloat((((val >> 8) & 0xFF)-IMAGE_MEAN)/IMAGE_STD);
                byteBuffer.putFloat((((val) & 0xFF)-IMAGE_MEAN)/IMAGE_STD);
            }
        }
        return byteBuffer;
    }
}

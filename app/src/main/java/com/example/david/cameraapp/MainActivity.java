package com.example.david.cameraapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    Button mTakePictureButton;
    ImageView mCameraPicture;

    private static final String TAG = "Camera Main Activity";

    private static final int REQUEST_SAVE_IMAGE_PERMISSION_REQUEST_CODE = 1001;

    private static int TAKE_PICTURE_REQUEST_CODE = 0;

    private String mImagePath;

    private Bitmap mImage;

    private static final String IMAGE_FILEPATH_KEY = "image filepath key";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null){
            mImagePath = savedInstanceState.getString(IMAGE_FILEPATH_KEY);
        }

        mCameraPicture = (ImageView) findViewById(R.id.camera_picture);
        mTakePictureButton = (Button) findViewById(R.id.take_picture_button);

        mTakePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takePhoto();
            }
        });
    }

    private void takePhoto(){
        Intent pictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (pictureIntent.resolveActivity(getPackageManager()) == null){
            Toast.makeText(MainActivity.this,"Your device does not have a camera",Toast.LENGTH_LONG).show();
        }
        else {
            String imageFileName = "camera_app_" + new Date().getTime();

            File storageDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File imageFile = null;
            Uri imageFileUri = null;
            try{
                imageFile = File.createTempFile(imageFileName, ".jpg",storageDirectory);
                mImagePath = imageFile.getAbsolutePath();
                imageFileUri = FileProvider.getUriForFile(MainActivity.this,"com.example.david.cameraapp",imageFile);
                Log.i(TAG,"image file URI " + imageFileUri);


            }
            catch (IOException ioe){
                Log.e(TAG,"Error creating file for photo storage", ioe);
                return;
            }

            pictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,imageFileUri);

            startActivityForResult(pictureIntent,TAKE_PICTURE_REQUEST_CODE);

        }

    }

    public void onActivityResult(int requestCode, int resultCode,Intent data){
        Log.d(TAG,"On activity result");

        if(resultCode == RESULT_OK && requestCode == TAKE_PICTURE_REQUEST_CODE){
            saveImageToMediaStore();
        }
    }

    private void saveImageToMediaStore() {

        //Add image to device's MediaStore - this makes the image accessible to the
        //gallery app, and any other apps that can read from the MediaStore

        //Do we have permission to write to storage?

        // Marshmallow and before, we just need to request permission in AndroidManifest,
        // and this check will return true if we've done so. The user will be notified that this app uses the file system when they first install it.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            MediaStore.Images.Media.insertImage(getContentResolver(), mImage, "SimpleCameraApp", "Photo taken by SimpleCameraApp");
        }

        // WRITE_EXTERNAL_STORAGE is a dangerous permission. So for Nougat and above, need to request permission in the mainfest
        // AND we will need to ask the user for permission when the app runs.
        else {
            //This request opens a dialog box for the user to accept the permission request.
            // When the user clicks ok or cancel, the onRequestPermission method (below) is called with the results
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_SAVE_IMAGE_PERMISSION_REQUEST_CODE);
        }
    }

    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == REQUEST_SAVE_IMAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //Now should be able to save
                MediaStore.Images.Media.insertImage(getContentResolver(), mImage, "SimpleCameraApp", "Photo taken by SimpleCameraApp");
            } else {
                Log.w(TAG, "Permission to WRITE_EXERNAL_STORAGE was NOT granted.");
                Toast.makeText(this, "The images taken will NOT saved to the gallery", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus){
        super.onWindowFocusChanged(hasFocus);
        Log.d(TAG,"onWindowsFocusChanged");

        if (hasFocus && mImagePath != null){
            scaleBitmap();
            mCameraPicture.setImageBitmap(mImage);
        }
    }


    private void scaleBitmap() {

        //Step 1: what size is the ImageView?
        int imageViewHeight = mCameraPicture.getHeight();
        int imageViewWidth = mCameraPicture.getWidth();

        // If height or width are zero, there's no point doing this. Return.
        if (imageViewHeight == 0 || imageViewWidth == 0) {
            Log.w(TAG, "The image view size is zero. Unable to scale.");
            return;
        }

        //Step 2: decode file to find out how large the image is.

        // BitmapFactory is used to create bitmaps from pixels in a file.
        // Many options and settings, so use a BitmapFactory.Options object to store our desired settings.
        // Set the inJustDecodeBounds flag to true,
        // which means just the *information about* the picture is decoded and stored in bOptions
        // Not all of the pixels have to be read and stored in this process.
        // When we've done this, we can query bOptions to find out the original picture's height and width.

        BitmapFactory.Options bOptions = new BitmapFactory.Options();
        bOptions.inJustDecodeBounds = true;

        BitmapFactory.decodeFile(mImagePath, bOptions);

        int pictureHeight = bOptions.outHeight;
        int pictureWidth = bOptions.outWidth;

        //Step 3. Can use the original size and target size to calculate scale factor
        int scaleFactor = Math.min(pictureHeight / imageViewHeight, pictureWidth / imageViewWidth);

        //Step 4. Decode the image file into a new Bitmap, scaled to fit the ImageView
        bOptions.inJustDecodeBounds = false;   //setting this to false will actually decode the file to a Bitmap
        bOptions.inSampleSize = scaleFactor;

        Bitmap bitmap = BitmapFactory.decodeFile(mImagePath, bOptions);
        mImage = bitmap;

    }

    public void onSaveInstaceState(Bundle outBundle){
        outBundle.putString(IMAGE_FILEPATH_KEY,mImagePath);
    }

}

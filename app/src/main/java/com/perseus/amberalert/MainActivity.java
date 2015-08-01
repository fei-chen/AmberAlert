package com.perseus.amberalert;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONObject;

import com.facepp.error.FaceppParseException;
import com.facepp.http.HttpRequests;
import com.facepp.http.PostParameters;

public class MainActivity extends Activity {

    final private static String TAG = "MainActivity";
    final private int PICTURE_CHOOSE = 1;

    private ImageView imageView = null;
    private Bitmap img = null;
    private TextView textView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button buttonNew = (Button)this.findViewById(R.id.buttonNewReport);  //create a new report
        buttonNew.setOnClickListener(new View.OnClickListener() {

            public void onClick(View arg0) {
            //new report form
            Intent reportIntent = new Intent(MainActivity.this, NewReportActivity.class);
            startActivity(reportIntent);
            }
        });

        Button buttonPhoto = (Button)this.findViewById(R.id.buttonAddPhoto);  //save a photo
        buttonPhoto.setOnClickListener(new View.OnClickListener() {

            public void onClick(View arg0) {
            //get a picture from the phone
            Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
            photoPickerIntent.setType("image/*");
            startActivityForResult(photoPickerIntent, PICTURE_CHOOSE);
            }
        });

        Button buttonExit = (Button)this.findViewById(R.id.buttonExit);  //Exit
        buttonExit.setOnClickListener(new View.OnClickListener() {

            public void onClick(View arg0) {
                //exit the application
                finish();
                System.exit(0);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        //the image picker callback
        if (requestCode == PICTURE_CHOOSE) {
            if (intent != null) {
                //The Android API
                //Log.d(TAG, "idButSelPic Photopicker: " + intent.getDataString());
                Cursor cursor = getContentResolver().query(intent.getData(), null, null, null, null);
                cursor.moveToFirst();
                int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                String fileSrc = cursor.getString(idx);
                //Log.d(TAG, "Picture:" + fileSrc);

                //just read size
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                img = BitmapFactory.decodeFile(fileSrc, options);

                //scale size to read
                options.inSampleSize = Math.max(1, (int)Math.ceil(
                                       Math.max((double)options.outWidth / 1024f,
                                               (double)options.outHeight / 1024f)));
                options.inJustDecodeBounds = false;
                img = BitmapFactory.decodeFile(fileSrc, options);

                //call /person/add_face, TODO: what is the value for faceId?
                String personId = "fakeid";
                String faceId = "fakeId";
                new PhotoTask().execute(personId, faceId);

            } else {
                Log.d(TAG, "Photopicker canceled");
            }
        }
    }

    private class PhotoTask extends AsyncTask<String, Void, JSONObject> {
        @Override
        protected JSONObject doInBackground(String... params) {
            HttpRequests httpRequests = new HttpRequests(Constants.API_KEY, Constants.API_SECRET,
                    true, false);
            PostParameters postParams = new PostParameters();
            postParams.setPersonId(params[0]);
            postParams.setFaceId(params[1]);
            JSONObject result = null;
            try {
                result = httpRequests.personAddFace();
            } catch (FaceppParseException e) {
                e.printStackTrace();
            }
            return result;
        }

        @Override
        protected void onPostExecute(JSONObject result) {
            TextView txt = (TextView) findViewById(R.id.person_name_title);
            txt.setText("Saved");
            //httpRequests.personDelete();
        }
    }
/*
    private class FaceppDetect {
        DetectCallback callback = null;

        public void setDetectCallback(DetectCallback detectCallback) {
            callback = detectCallback;
        }

        public void detect(final Bitmap image) {

            new Thread(new Runnable() {

                public void run() {
                    HttpRequests httpRequests = new HttpRequests(API_KEY, API_SECRET, true, false);
                    //Log.v(TAG, "image size : " + img.getWidth() + " " + img.getHeight());

                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    float scale = Math.min(1, Math.min(600f / img.getWidth(), 600f / img.getHeight()));
                    Matrix matrix = new Matrix();
                    matrix.postScale(scale, scale);

                    Bitmap imgSmall = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, false);
                    //Log.v(TAG, "imgSmall size : " + imgSmall.getWidth() + " " + imgSmall.getHeight());

                    imgSmall.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                    byte[] array = stream.toByteArray();

                    try {
                        //detect
                        JSONObject result = httpRequests.detectionDetect(new PostParameters().setImg(array));
                        //finished , then call the callback function
                        if (callback != null) {
                            callback.detectResult(result);
                        }
                    } catch (FaceppParseException e) {
                        e.printStackTrace();
                        MainActivity.this.runOnUiThread(new Runnable() {
                            public void run() {
                                textView.setText("Network error.");
                            }
                        });
                    }

                }
            }).start();
        }
    }
*/

    interface DetectCallback {
        void detectResult(JSONObject rst);
    }
}

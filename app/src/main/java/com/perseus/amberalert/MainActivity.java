package com.perseus.amberalert;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.provider.MediaStore;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import com.facepp.error.FaceppParseException;
import com.facepp.http.HttpRequests;
import com.facepp.http.PostParameters;

import java.io.ByteArrayOutputStream;

public class MainActivity extends Activity {

    final private static String TAG = "MainActivity";
    final private int PICTURE_CHOOSE = 1;

    private ImageView imageView = null;
    private Bitmap img = null;
    private Button buttonDetect = null;
    private TextView textView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button buttonNew = (Button)this.findViewById(R.id.button);  //create a new report
        buttonNew.setOnClickListener(new View.OnClickListener() {

            public void onClick(View arg0) {
                //new report form
                Intent reportIntent = new Intent(MainActivity.this, NewReportActivity.class);
                startActivity(reportIntent);
            }
        });

        Button buttonPhoto = (Button)this.findViewById(R.id.button3);  //Save a photo
        buttonPhoto.setOnClickListener(new View.OnClickListener() {

            public void onClick(View arg0) {
                //get a picture from the phone
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, PICTURE_CHOOSE);
            }
        });

        textView = (TextView)this.findViewById(R.id.textView1);

        buttonDetect = (Button)this.findViewById(R.id.button2);
        buttonDetect.setVisibility(View.INVISIBLE);
        buttonDetect.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {

                textView.setText("Waiting ...");

                FaceppDetect faceppDetect = new FaceppDetect();
                faceppDetect.setDetectCallback(new DetectCallback() {

                    public void detectResult(JSONObject rst) {
                        //Log.v(TAG, rst.toString());

                        //use the red paint
                        Paint paint = new Paint();
                        paint.setColor(Color.RED);
                        paint.setStrokeWidth(Math.max(img.getWidth(), img.getHeight()) / 100f);

                        //create a new canvas
                        Bitmap bitmap = Bitmap.createBitmap(img.getWidth(), img.getHeight(), img.getConfig());
                        Canvas canvas = new Canvas(bitmap);
                        canvas.drawBitmap(img, new Matrix(), null);


                        try {
                            //find out all faces
                            final int count = rst.getJSONArray("face").length();
                            for (int i = 0; i < count; ++i) {
                                float x, y, w, h;
                                //get the center point
                                x = (float) rst.getJSONArray("face").getJSONObject(i)
                                        .getJSONObject("position").getJSONObject("center").getDouble("x");
                                y = (float) rst.getJSONArray("face").getJSONObject(i)
                                        .getJSONObject("position").getJSONObject("center").getDouble("y");

                                //get face size
                                w = (float) rst.getJSONArray("face").getJSONObject(i)
                                        .getJSONObject("position").getDouble("width");
                                h = (float) rst.getJSONArray("face").getJSONObject(i)
                                        .getJSONObject("position").getDouble("height");

                                //change percent value to the real size
                                x = x / 100 * img.getWidth();
                                w = w / 100 * img.getWidth() * 0.7f;
                                y = y / 100 * img.getHeight();
                                h = h / 100 * img.getHeight() * 0.7f;

                                //draw the box to mark it out
                                canvas.drawLine(x - w, y - h, x - w, y + h, paint);
                                canvas.drawLine(x - w, y - h, x + w, y - h, paint);
                                canvas.drawLine(x + w, y + h, x - w, y + h, paint);
                                canvas.drawLine(x + w, y + h, x + w, y - h, paint);
                            }

                            //save new image
                            img = bitmap;

                            MainActivity.this.runOnUiThread(new Runnable() {

                                public void run() {
                                    //show the image
                                    imageView.setImageBitmap(img);
                                    textView.setText("Finished, " + count + " faces.");
                                }
                            });

                        } catch (JSONException e) {
                            e.printStackTrace();
                            MainActivity.this.runOnUiThread(new Runnable() {
                                public void run() {
                                    textView.setText("Error.");
                                }
                            });
                        }

                    }
                });
                faceppDetect.detect(img);
            }
        });

        imageView = (ImageView)this.findViewById(R.id.imageView1);
        imageView.setImageBitmap(img);
        Button buttonExit = (Button)this.findViewById(R.id.button5);  //Exit
        buttonExit.setOnClickListener(new View.OnClickListener() {

            public void onClick(View arg0) {
                //exit the application
                finish();
                System.exit(0);
            }
        });
    }

    /*
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detect_person);

        Button button = (Button)this.findViewById(R.id.button1);
        button.setOnClickListener(new View.OnClickListener() {

            public void onClick(View arg0) {
                //get a picture from the phone
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, PICTURE_CHOOSE);
            }
        });

        textView = (TextView)this.findViewById(R.id.textView1);

        buttonDetect = (Button)this.findViewById(R.id.button2);
        buttonDetect.setVisibility(View.INVISIBLE);
        buttonDetect.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {

                textView.setText("Waiting ...");

                FaceppDetect faceppDetect = new FaceppDetect();
                faceppDetect.setDetectCallback(new DetectCallback() {

                    public void detectResult(JSONObject rst) {
                        //Log.v(TAG, rst.toString());

                        //use the red paint
                        Paint paint = new Paint();
                        paint.setColor(Color.RED);
                        paint.setStrokeWidth(Math.max(img.getWidth(), img.getHeight()) / 100f);

                        //create a new canvas
                        Bitmap bitmap = Bitmap.createBitmap(img.getWidth(), img.getHeight(), img.getConfig());
                        Canvas canvas = new Canvas(bitmap);
                        canvas.drawBitmap(img, new Matrix(), null);


                        try {
                            //find out all faces
                            final int count = rst.getJSONArray("face").length();
                            for (int i = 0; i < count; ++i) {
                                float x, y, w, h;
                                //get the center point
                                x = (float) rst.getJSONArray("face").getJSONObject(i)
                                        .getJSONObject("position").getJSONObject("center").getDouble("x");
                                y = (float) rst.getJSONArray("face").getJSONObject(i)
                                        .getJSONObject("position").getJSONObject("center").getDouble("y");

                                //get face size
                                w = (float) rst.getJSONArray("face").getJSONObject(i)
                                        .getJSONObject("position").getDouble("width");
                                h = (float) rst.getJSONArray("face").getJSONObject(i)
                                        .getJSONObject("position").getDouble("height");

                                //change percent value to the real size
                                x = x / 100 * img.getWidth();
                                w = w / 100 * img.getWidth() * 0.7f;
                                y = y / 100 * img.getHeight();
                                h = h / 100 * img.getHeight() * 0.7f;

                                //draw the box to mark it out
                                canvas.drawLine(x - w, y - h, x - w, y + h, paint);
                                canvas.drawLine(x - w, y - h, x + w, y - h, paint);
                                canvas.drawLine(x + w, y + h, x - w, y + h, paint);
                                canvas.drawLine(x + w, y + h, x + w, y - h, paint);
                            }

                            //save new image
                            img = bitmap;

                            MainActivity.this.runOnUiThread(new Runnable() {

                                public void run() {
                                    //show the image
                                    imageView.setImageBitmap(img);
                                    textView.setText("Finished, " + count + " faces.");
                                }
                            });

                        } catch (JSONException e) {
                            e.printStackTrace();
                            MainActivity.this.runOnUiThread(new Runnable() {
                                public void run() {
                                    textView.setText("Error.");
                                }
                            });
                        }

                    }
                });
                faceppDetect.detect(img);
            }
        });

        imageView = (ImageView)this.findViewById(R.id.imageView1);
        imageView.setImageBitmap(img);

    }
    */

    //TODO: need to add this dialog into a menu
    private void fireNewPersonDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.activity_new_report);

        TextView personNameView = (TextView) dialog.findViewById(R.id.person_name_title);
        final EditText editName = (EditText) dialog.findViewById(R.id.person_name_val);
        TextView personAgeView = (TextView) dialog.findViewById(R.id.person_age_title);
        final EditText editAge = (EditText) dialog.findViewById(R.id.person_age_val);
        TextView personLostDayView = (TextView) dialog.findViewById(R.id.person_lost_time_title);
        final EditText editLostDay = (EditText) dialog.findViewById(R.id.person_lost_time_val);
        TextView personLostLocationView = (TextView) dialog.findViewById(R.id.person_loc_title);
        final EditText editLostLocation = (EditText) dialog.findViewById(R.id.person_loc_val);
        TextView personPhoneView = (TextView) dialog.findViewById(R.id.person_phone1_title);
        final EditText editPhone1 = (EditText) dialog.findViewById(R.id.person_phone1_val);
        Button saveButton = (Button) dialog.findViewById(R.id.person_button_save);
        LinearLayout rootLayout = (LinearLayout) dialog.findViewById(R.id.person_root_layout);
        String tag;

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = editName.getText().toString();
                String age = editAge.getText().toString();
                String lostDay = editLostDay.getText().toString();
                String lostLoc = editLostLocation.getText().toString();
                String phone1 = editPhone1.getText().toString();

                //tag = name + "," + age + "," + lostDay + "," +
                //             lostLoc + "," + phone1;
                dialog.dismiss();
                //TODO: call /person/create API


            }
        });

        Button cancelButton = (Button) dialog.findViewById(R.id.person_button_cancel);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
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
        switch(item.getItemId()) {
            case R.id.action_new:
                fireNewPersonDialog();
                return true;
            case R.id.action_exit:
                finish();
                return true;
            default:
                return false;
        }
        /* old code
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
        */
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        //the image picker callback
        if (requestCode == PICTURE_CHOOSE) {
            if (intent != null) {
                //The Android api ~~~
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
                textView.setText("Click Detect. ==>");


                imageView.setImageBitmap(img);
                buttonDetect.setVisibility(View.VISIBLE);
            }
            else {
                Log.d(TAG, "idButSelPic Photopicker canceled");
            }
        }
    }

    private class FaceppDetect {
        DetectCallback callback = null;

        public void setDetectCallback(DetectCallback detectCallback) {
            callback = detectCallback;
        }

        public void detect(final Bitmap image) {

            new Thread(new Runnable() {

                public void run() {
                    //HttpRequests httpRequests = new HttpRequests("4480afa9b8b364e30ba03819f3e9eff5", "Pz9VFT8AP3g_Pz8_dz84cRY_bz8_Pz8M", true, false);
                    HttpRequests httpRequests = new HttpRequests("7a2d5545b8619d26cdb0d8931ecf9cc8", "EttYzuqfmknONu-x7pD7Ur3yJq47k9ql", true, false);
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

    interface DetectCallback {
        void detectResult(JSONObject rst);
    }
}

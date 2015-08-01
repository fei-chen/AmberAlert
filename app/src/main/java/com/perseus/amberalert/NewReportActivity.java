package com.perseus.amberalert;

import android.app.Dialog;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;


public class NewReportActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_report);

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
        getMenuInflater().inflate(R.menu.menu_new_report, menu);
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
}

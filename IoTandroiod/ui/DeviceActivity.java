package com.example.android_resapi.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.example.android_resapi.R;
import com.example.android_resapi.ui.apicall.GetThingShadow;
import com.example.android_resapi.ui.apicall.UpdateShadow;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Timer;
import java.util.TimerTask;

public class DeviceActivity extends AppCompatActivity {
    String urlStr;
    final static String TAG = "AndroidAPITest";
    Timer timer;
    Button startGetBtn;
    Button stopGetBtn;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);
        Intent intent = getIntent();
        urlStr = intent.getStringExtra("thingShadowURL");

        startGetBtn = findViewById(R.id.startGetBtn);
        startGetBtn.setEnabled(true);
        startGetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        new GetThingShadow(DeviceActivity.this, urlStr).execute();
                    }
                },
                        0,2000);

                startGetBtn.setEnabled(false);
                stopGetBtn.setEnabled(true);
            }
        });

        stopGetBtn = findViewById(R.id.stopGetBtn);
        stopGetBtn.setEnabled(false);
        stopGetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (timer != null)
                    timer.cancel();
                clearTextView();
                startGetBtn.setEnabled(true);
                stopGetBtn.setEnabled(false);
            }
        });

        Button updateBtn = findViewById(R.id.updateBtn);
        updateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText edit_mod = findViewById(R.id.edit_mod);

                JSONObject payload = new JSONObject();

                try {
                    JSONArray jsonArray = new JSONArray();
                    String mod_input = edit_mod.getText().toString();
                    if (mod_input != null && !mod_input.equals("")) {
                        JSONObject tag1 = new JSONObject();
                        tag1.put("tagName", "mod");//"tagName":"tagValue"
                        tag1.put("tagValue", mod_input);//"mod":"mod_input" mod_input= user or admin

                        jsonArray.put(tag1);
                    }


                    if (jsonArray.length() > 0)
                        payload.put("tags", jsonArray);
                } catch (JSONException e) {
                    Log.e(TAG, "JSONEXception");
                }
                Log.i(TAG,"payload="+payload);
                if (payload.length() >0 )
                    new UpdateShadow(DeviceActivity.this,urlStr).execute(payload);
                else
                    Toast.makeText(DeviceActivity.this,"변경할 상태 정보 입력이 필요합니다", Toast.LENGTH_SHORT).show();
            }
        });


    }

    private void clearTextView() {
        TextView reported_mod = findViewById(R.id.reported_mod);
        TextView reported_top = findViewById(R.id.reported_top);
        TextView reported_trash_cm = findViewById(R.id.reported_trash_cm);

        reported_mod.setText("");
        reported_top.setText("");
        reported_trash_cm.setText("");


    }

}



package com.example.android_resapi.ui.apicall;

import android.app.Activity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONException;
import org.json.JSONObject;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import com.example.android_resapi.R;
import com.example.android_resapi.httpconnection.GetRequest;

public class GetThingShadow extends GetRequest {
    final static String TAG = "AndroidAPITest";
    String urlStr;
    public GetThingShadow(Activity activity, String urlStr) {
        super(activity);
        this.urlStr = urlStr;
    }

    @Override
    protected void onPreExecute() {
        try {
            Log.e(TAG, urlStr);
            url = new URL(urlStr);

        } catch (MalformedURLException e) {
            Toast.makeText(activity,"URL is invalid:"+urlStr, Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            activity.finish();
        }
    }

    @Override
    protected void onPostExecute(String jsonString) {
        if (jsonString == null)
            return;
        Map<String, String> state = getStateFromJSONString(jsonString);
        TextView reported_mod = activity.findViewById(R.id.reported_mod);
        TextView reported_top = activity.findViewById(R.id.reported_top);
        TextView reported_trash_cm = activity.findViewById(R.id.reported_trash_cm);
        reported_mod.setText(state.get("reported_mod"));
        reported_top.setText(state.get("reported_top"));
        reported_trash_cm.setText(state.get("reported_trash_cm"));


    }

    protected Map<String, String> getStateFromJSONString(String jsonString) {
        Map<String, String> output = new HashMap<>();
        try {
            // 처음 double-quote와 마지막 double-quote 제거
            jsonString = jsonString.substring(1,jsonString.length()-1);
            // \\\" 를 \"로 치환
            jsonString = jsonString.replace("\\\"","\"");
            Log.i(TAG, "jsonString="+jsonString);
            JSONObject root = new JSONObject(jsonString);
            JSONObject state = root.getJSONObject("state");
            JSONObject reported = state.getJSONObject("reported");
            String mod = reported.getString("mod");
            String top = reported.getString("top");
            String trash_cm = reported.getString("trash_cm");

            output.put("reported_mod", mod);
            output.put("reported_top",top);
            output.put("reported_trash_cm",trash_cm);


            JSONObject desired = state.getJSONObject("desired");
            String desired_mod = desired.getString("mod");
            String desired_top = desired.getString("top");
            String desired_trash_cm = desired.getString("trash_cm");
            output.put("desired_mod", desired_mod);
            output.put("desired_top",desired_top);
            output.put("desired_trash_cm",desired_trash_cm);

        } catch (JSONException e) {
            Log.e(TAG, "Exception in processing JSONString.", e);
            e.printStackTrace();
        }
        return output;
    }
}

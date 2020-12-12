package com.example.android_resapi.httpconnection;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;
//AsyncTask는 ui thread와는 별ㄷ록 수행 되도록 만든 class
//android app은 ui code + network operation 으로 나누어 집니다.
//ui에서 버튼을 누르면 network operation을 호출해서 aws와 http통신을 하고 내용을 서버에서 가져옵니다.
abstract public class GetRequest extends AsyncTask<String, Void, String> {
    final static String TAG = "AndroidAPITest";
    protected Activity activity;
    protected URL url;

    public GetRequest(Activity activity) {
        this.activity = activity;
    }

    //ui threa->onPreExecute->doInBackGround->onPostExecute
    @Override
    protected String doInBackground(String... strings) { //network operation을 해주는 코드
        StringBuffer output = new StringBuffer();

        try {
            if (url == null) {
                Log.e(TAG, "Error: URL is null ");
                return null;
            }
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            if (conn == null) {
                Log.e(TAG, "HttpsURLConnection Error");
                return null;
            }
            conn.setConnectTimeout(10000);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.setDoOutput(false);

            int resCode = conn.getResponseCode();

            if (resCode != HttpsURLConnection.HTTP_OK) {
                Log.e(TAG, "HttpsURLConnection ResponseCode: " + resCode);
                conn.disconnect();
                return null;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line = null;
            while (true) {
                line = reader.readLine();
                if (line == null) {
                    break;
                }
                output.append(line);
            }

            reader.close();
            conn.disconnect();

        } catch (IOException ex) {
            Log.e(TAG, "Exception in processing response.", ex);
            ex.printStackTrace();
        }

        return output.toString();
    }

}

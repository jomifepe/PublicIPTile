package com.jomifepe.publiciptile;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.support.annotation.StringRes;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class QSTileService extends TileService {
    private static final String SERVICE_STATUS_FLAG = "serviceStatus";
    private static final String PREFERENCES_KEY = "com.jomifepe.myipquicksettingstile";
    private static final String IPIFY_API_URL = "https://api.ipify.org";
    private static final String MYIP_API_URL = "https://api.myip.com";
    public static String currentIp;

    @Override
    public void onTileAdded() {
        disableTile();
    }

    @Override
    public void onStartListening() {
        if (isLocked()) {
            disableTile();
        }
    }

    @Override
    public void onClick() {
        if (isLocked()) {
            unlockAndRun(new Runnable() {
                @Override
                public void run() {
                    updateTile();
                }
            });
        } else {
            updateTile();
        }
    }

    private void updateTile() {
        boolean isActive = getServiceStatus();
        if (isActive) {
            disableTile();
        } else {
            if (isNetworkAvailable()) {
                enableTile();
            } else {
                Toast.makeText(this, R.string.error_message_no_connection,
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void disableTile() {
        changeTile(R.string.label_disable, Tile.STATE_INACTIVE);
    }

    OnFailureListener myIpFailureListener = new OnFailureListener() {
        @Override
        public void onFailure(Exception e) {
            Log.d(this.getClass().getSimpleName(), e.getMessage());
            Toast.makeText(QSTileService.this,
                    R.string.error_message_request_failed, Toast.LENGTH_LONG).show();
        }
    };

    OnFailureListener ipifyFailureListener = new OnFailureListener() {
        @Override
        public void onFailure(Exception e) {
            Log.d(this.getClass().getSimpleName(), e.getMessage());
            getIPFromAPI(IPIFY_API_URL, new OnSuccessListener() {
                @Override
                public void onSuccess(String response) {
                    changeTile(currentIp = response, Tile.STATE_ACTIVE);
                }
            }, myIpFailureListener);
        }
    };

    private void enableTile() {
        getIPFromAPI(MYIP_API_URL, new OnSuccessListener() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    String label = String.format("%s (%s)",
                            currentIp = jsonResponse.getString("ip"),
                            jsonResponse.getString("cc"));
                    changeTile(label, Tile.STATE_ACTIVE);
                } catch (JSONException e) {
                    ipifyFailureListener.onFailure(e);
                }
            }
        }, ipifyFailureListener);
    }

    private void changeTile(String label, int state) {
        final Tile tile = this.getQsTile();
        tile.setLabel(label);
        tile.setState(state);
        setServiceStatus(state == Tile.STATE_ACTIVE);
        tile.updateTile();
    }

    private void changeTile(@StringRes int label, int state) {
        changeTile(getString(label), state);
    }

    private void getIPFromAPI(String apiUrl, final OnSuccessListener successListener,
                              final OnFailureListener failureListener) {
        HTTPGETRequest request = new HTTPGETRequest(apiUrl);
        request.addOnSuccessListener(successListener);
        request.addOnFailureListener(failureListener);
        request.execute();
    }

    public boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    private boolean getServiceStatus() {
        SharedPreferences prefs = getApplicationContext()
                .getSharedPreferences(PREFERENCES_KEY, MODE_PRIVATE);
        return prefs.getBoolean(SERVICE_STATUS_FLAG, false);
    }

    public void setServiceStatus(boolean active) {
        SharedPreferences prefs = getApplicationContext()
                .getSharedPreferences(PREFERENCES_KEY, MODE_PRIVATE);
        prefs.edit().putBoolean(SERVICE_STATUS_FLAG, active).apply();
    }

    public interface OnSuccessListener {
        void onSuccess(String response);
    }
    public interface OnFailureListener {
        void onFailure(Exception e);
    }
    public static class HTTPException extends RuntimeException {
        HTTPException(int statusCode) {
            super(String.valueOf(statusCode));
        }
    }

    public static class HTTPGETRequest extends AsyncTask<Void, Void, String> {
        private OnSuccessListener onSuccessListener;
        private OnFailureListener onFailureListener;
        private String url;

        public HTTPGETRequest(String url) {
            this.url = url;
        }

        public HTTPGETRequest addOnSuccessListener(OnSuccessListener listener) {
            this.onSuccessListener = listener;
            return this;
        }

        public HTTPGETRequest addOnFailureListener(OnFailureListener listener) {
            this.onFailureListener = listener;
            return this;
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                URL target = new URL(url);
                HttpURLConnection connection = (HttpURLConnection)target.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setConnectTimeout(4000);
                connection.connect();

                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    throw new HTTPException(connection.getResponseCode());
                }

                InputStream stream = new BufferedInputStream(connection.getInputStream());
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stream));
                StringBuilder sb = new StringBuilder();

                String inputString;
                while ((inputString = bufferedReader.readLine()) != null) {
                    sb.append(inputString);
                }

                connection.disconnect();
                return sb.toString();
            } catch (IOException e) {
                if (onFailureListener != null) {
                    onFailureListener.onFailure(e);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String response) {
            if (response != null && onSuccessListener != null) {
                onSuccessListener.onSuccess(response);
            }
        }
    }
}

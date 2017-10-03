package com.akexorcist.googlelocationapi;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Tunyaporn on 10/2/2017.
 */

public class AutoStartup extends Service implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {
    private GoogleApiClient googleApiClient;
    private TextView textView;
    private MyRepeat thread;
    private String url = "http://service.eternity.co.th/AndroidTest/app/CenterService/updateRecNow.php";
    private Context context;

    private class SynSendGPS extends AsyncTask<Void, Void, String> {
        Context context;
        String latString, longString, timeString;

        public SynSendGPS(Context context, String latString, String longString, String timeString) {
            this.context = context;
            this.latString = latString;
            this.longString = longString;
            this.timeString = timeString;
        }

        @Override
        protected String doInBackground(Void... voids) {
            try{
                OkHttpClient okHttpClient = new OkHttpClient();
                RequestBody requestBody = new FormEncodingBuilder()
                        .add("isAdd", "true")
                        .add("gps_lat", latString)
                        .add("gps_lon", longString)
                        .add("gps_timeStamp",timeString)
                        .build();
                Request.Builder builder = new Request.Builder();
                Request request = builder.post(requestBody).url(url).build();
                Response response = okHttpClient.newCall(request).execute();
                return response.body().string();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            Log.d("Tag", "s " + s);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onConnected(Bundle bundle) {
        LocationAvailability locationAvailability = LocationServices.FusedLocationApi.getLocationAvailability(googleApiClient);
        if (locationAvailability.isLocationAvailable()) {
            // Call Location Services
            LocationRequest locationRequest = new LocationRequest()
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setInterval(30000);
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
        } else {
            // Do something when Location Provider not available
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (googleApiClient != null && googleApiClient.isConnected()) {
            // Disconnect Google API Client if available and connected
            googleApiClient.disconnect();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("ServiceTag", "Open Device");
        thread = new MyRepeat();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!thread.isAlive()) {
            thread.start();
            googleApiClient.connect();
        }
        return START_STICKY;
    }



    private class MyRepeat extends Thread {
        @Override
        public void run() {
            textView = (TextView) context.findViewById(R.id.text_view);
            googleApiClient = new GoogleApiClient.Builder(context)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(new AutoStartup())
                    .addOnConnectionFailedListener(new AutoStartup())
                    .build();
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS", Locale.getDefault());
        Date date = new Date();
        String lat = String.valueOf(location.getLatitude());
        String lng = String.valueOf(location.getLongitude());
        String time = dateFormat.format(date);
        textView.setText("Latitude : " + lat + "\n" +
                "Longitude : " + lng + "\n" + "Time : " + time);

        SynSendGPS synSendGPS = new SynSendGPS(getBaseContext(), lat, lng, time);
        synSendGPS.execute();
    }
}

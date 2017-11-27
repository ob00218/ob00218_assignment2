package com1032.cw2.ob00218.ob00218_assignment2;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.support.v7.app.NotificationCompat;

public class UserLocationService extends Service {

    private LocationListener locationListener;
    private LocationManager locationManager;
    private Handler mHandler;
    private HandlerThread handlerThread;

    public UserLocationService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        //Initialize a new HandlerThread
        handlerThread = new HandlerThread("locationHandlerThread");
        handlerThread.start();

        //Inintialize a new Handler
        mHandler = new Handler(handlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {

            }
        };

        //Start the new thread
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                locationListener = new LocationListener() {
                    //If location changed, broadcase new latitude and longitude
                    @Override
                    public void onLocationChanged(Location location) {
                        Intent intent = new Intent("onLocationChanged");
                        intent.putExtra("lat", location.getLatitude());
                        intent.putExtra("lng", location.getLongitude());
                        sendBroadcast(intent);
                    }

                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {

                    }

                    @Override
                    public void onProviderEnabled(String provider) {

                    }

                    //If GPS is disabled open Location settings to allow the user to enable GPS
                    @Override
                    public void onProviderDisabled(String provider) {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                };

                locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

                //Don't need to inspect permission as already asked for in MainActivity
                //noinspection MissingPermission
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationListener);

            }
        });
    }

    /**
     * When service is stopped, stop updates from the locationListener and stop the thread safely
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        if(locationManager != null) {
            locationManager.removeUpdates(locationListener);
        }
        handlerThread.quitSafely();
    }
}

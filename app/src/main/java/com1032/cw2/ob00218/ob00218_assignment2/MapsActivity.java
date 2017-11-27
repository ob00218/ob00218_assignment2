package com1032.cw2.ob00218.ob00218_assignment2;

import android.app.NotificationManager;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.PersistableBundle;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.maps.android.SphericalUtil;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private PolylineOptions polylineOptions;
    private Polyline polyline;
    private Marker mCurrLocationMarker;
    private ArrayList<LatLng> mLatLngList = new ArrayList<LatLng>();
    private ArrayList<String> mLatLngStringList = new ArrayList<String>();
    private double distance;
    private ImageButton mStopButton;
    private TextView timerTextView;
    private long startTime = 0;
    private String grabTime;
    private TextView distanceTextView;
    private TextView paceTextView;
    private double avgPace;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private BroadcastReceiver broadcastReciever;
    private BitmapDrawable bitmapDrawable;
    private Bitmap mapMarker;
    private long millis;
    private NotificationManager mNotificationManager;


    /**
     * On a new thread create a stopwatch which counts up in seconds
     */
    Handler handler = new Handler();
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            millis = System.currentTimeMillis() - startTime;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;

            int totalSeconds = (int) (millis/1000);

            timerTextView.setText(String.format("%02d:%02d", minutes, seconds));
            grabTime = timerTextView.getText().toString();
            calculateDistance();
            //Set relevant TextViews with their relevant formats
            NumberFormat numberFormat = new DecimalFormat("#0.00");
            distanceTextView.setText(numberFormat.format(distance/1000));
            NumberFormat numberFormat2 = new DecimalFormat("#0.00");
            avgPace = (distance/totalSeconds) * (18/5);
            paceTextView.setText(numberFormat2.format(avgPace));

            handler.postDelayed(this, 500);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        mDatabase = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();


        polylineOptions = new PolylineOptions();

        mStopButton = (ImageButton) findViewById(R.id.stopButton);

        timerTextView = (TextView) findViewById(R.id.time);

        distanceTextView = (TextView) findViewById(R.id.distance);

        paceTextView = (TextView) findViewById(R.id.pace);

        setUpTimer();

        //Resize image in drawable and save it as bitmap
        bitmapDrawable = (BitmapDrawable) getResources().getDrawable(R.drawable.map_icon);
        Bitmap b = bitmapDrawable.getBitmap();
        mapMarker = Bitmap.createScaledBitmap(b, 80, 80, false);

        //Setup notification to be shown that service is running
        showNotification();
    }

    @Override
    public void onResume() {
        super.onResume();
        //Remove the notification if acvitiy is resumed
        mNotificationManager.cancel(1);

        //Setup the broadcast reciever to recieve from location service
        if(broadcastReciever == null) {
            broadcastReciever = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    //Run handleLocationUpdate() method with values retrieved from location service
                    handleLocationUpdate(intent.getExtras().getDouble("lat"), intent.getExtras().getDouble("lng"));
                    Log.d("MapsActivity", "Location info recieved from service");
                }
            };
        }
        //Dynamically register broadcastReciever
        registerReceiver(broadcastReciever, new IntentFilter("onLocationChanged"));
    }

    @Override
    protected void onPause() {
        super.onPause();
        //Show notification if put in background
        showNotification();
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        //Initializes the map fragment
        mMap = googleMap;


    }

    /**
     * Method to handle the location updates recieved from the broadcast reciever
     * @param lat
     * @param lng
     */
    public void handleLocationUpdate(double lat,double lng) {

        //If there is alrady a marker, remove it
        if(mCurrLocationMarker != null) {
            mCurrLocationMarker.remove();
        }

        //Setup the markerOptions and then draw it on the map after a location update
        LatLng latLng = new LatLng(lat, lng);
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.flat(true);
        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(mapMarker));
        mCurrLocationMarker = mMap.addMarker(markerOptions);

        //Move camera to user location
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 19));

        //Setup a polyline to be drawn on the map to trace the users route
        polylineOptions.add(new LatLng(lat, lng));
        polyline = mMap.addPolyline(polylineOptions.color(Color.RED).width(11).startCap(new RoundCap()).endCap(new RoundCap()));

        //Save a list of latitude and longitude in LatLng object
        mLatLngList.add(latLng);
        //Also save a list of latitude and longitude as String for saving to Firebase database
        mLatLngStringList.add(latLng.toString());

        Log.d("MapsActivity", "handleLocationUpdate being called from broadcast reciever");

    }

    /**
     * Method to calculate the distance covered by the user
     */
    public void calculateDistance() {
        //Returns the length of the path in meters
        distance = SphericalUtil.computeLength(mLatLngList);
    }

    /**
     * If mStopButton is clicked:
     * Calculate the distance
     * Reset the startTime for the stopwatch and stop the stopwatch
     * Remove the notification that the service is running
     * Save the new run data to the Firebase database
     * Stop the UserLocationService
     * Start the MainActivity
     * @param view
     */
    public void stopOnClick(View view){
        calculateDistance();

        startTime = System.currentTimeMillis();
        handler.removeCallbacks(runnable);
        Log.d("MapsActivity", grabTime);

        mNotificationManager.cancel(1);

        writeNewRun(distance, grabTime, avgPace, mLatLngStringList);

        Intent intent = new Intent(getApplicationContext(), UserLocationService.class);
        stopService(intent);

        startActivity(new Intent(MapsActivity.this, MainActivity.class));
        finish();
    }

    /**
     * Setup the timer by setting the startTime to the current time and start the stopwatch
     */
    private void setUpTimer() {
        startTime = System.currentTimeMillis();
        handler.postDelayed(runnable, 0);
    }

    /**
     * Method to retrieve current time and date the run was saved
     * @return formatted time and date String
     */
    public String creationDate() {
        SimpleDateFormat format = new SimpleDateFormat("HH:mm dd-MM-yyyy");
        Date date = new Date();
        return format.format(date);

    }

    /**
     * Method to save the run data to the Firebase data
     * @param distance
     * @param time
     * @param avgPace
     * @param latLngList
     */
    private void writeNewRun(double distance, String time, double avgPace, ArrayList<String> latLngList) {
        //Database reference to where to save the run information, new CompletionListener
        mDatabase.child("users").child(mAuth.getCurrentUser().getUid()).child("runs").push().setValue(new Run(distance, time, avgPace, creationDate(), latLngList), new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                //If error saving to Firebase database, display error Toast
                if(databaseError != null) {
                    Toast.makeText(MapsActivity.this, "Error saving data", Toast.LENGTH_SHORT).show();
                //Else display save successful Toast
                } else {
                    Toast.makeText(MapsActivity.this, "Run data saved!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //When activity is destroyed unregister the broadcast reciever if it is not null
        if(broadcastReciever != null) {
            unregisterReceiver(broadcastReciever);
        }
        //Removes notifcation that service is running
        mNotificationManager.cancel(1);
    }

    /**
     * Save the value of millis in case of state change
     * @param outState
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong("millis", this.millis);
    }

    /**
     * Restore the value of millis after state change
     * @param savedInstanceState
     */
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        this.millis = savedInstanceState.getLong("millis");
    }

    /**
     * Disable back button
     */
    @Override
    public void onBackPressed() {
    }

    /**
     * Display a persistent notification when called
     */
    private void showNotification() {
        android.support.v4.app.NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.runningrabbit)
                        .setContentTitle("ob00218_assignment2")
                        .setContentText("You have a run in progress!")
                        .setOngoing(true);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MapsActivity.class);

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        //id 1 allows reference to this notification
        mNotificationManager.notify(1, mBuilder.build());

    }
}

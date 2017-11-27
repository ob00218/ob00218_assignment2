package com1032.cw2.ob00218.ob00218_assignment2;

import android.*;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener{

    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private FloatingActionButton fab;
    private GoogleApiClient mGoogleApiClient;

    private RecyclerView mRunRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;

    private FirebaseRecyclerAdapter<Run, RunViewHolder> mFirebaseAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Call checkLogin() to check if user is already logged in
        checkLogin();

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Configure the google sign in
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("606729802498-a3i523vftsoo89vuf7ld3o5sa28kep61.apps.googleusercontent.com")
                .requestEmail()
                .build();

        //Build a new GoogleApiClient object
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        //Initialize Firebase database reference
        mDatabase = FirebaseDatabase.getInstance().getReference();
        //Initialize Firebase auth
        mAuth = FirebaseAuth.getInstance();

        mRunRecyclerView = (RecyclerView) findViewById(R.id.runRecyclerView);
        mLinearLayoutManager = new LinearLayoutManager(this);
        mLinearLayoutManager.setStackFromEnd(false);

        //Initialize the FirebaseRecyclerAdapter to retrieve stored Run objects from the Firebase database
        //Display each element in the R.layout.run layout
        mFirebaseAdapter = new FirebaseRecyclerAdapter<Run, RunViewHolder>(
                Run.class,
                R.layout.run,
                RunViewHolder.class,
                mDatabase.child("users").child(mAuth.getCurrentUser().getUid()).child("runs")) {
            //Populate the viewHolder by retrieving values from the Run model and setting the appropriate TextView
            @Override
            protected void populateViewHolder(RunViewHolder viewHolder, Run model, final int position) {
                viewHolder.time.setText(model.getTime());
                NumberFormat numberFormat = new DecimalFormat("#0.00");
                viewHolder.distance.setText(numberFormat.format(model.getDistance()));
                viewHolder.pace.setText(numberFormat.format(model.getAvgPace()));
                viewHolder.date.setText(model.getDate());
            }
        };

        //If new Run is inserted add it to the end of the list. Scroll to newly added run.
        mFirebaseAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                int runCount = mFirebaseAdapter.getItemCount();
                int lastVisiblePosition = mLinearLayoutManager.findLastCompletelyVisibleItemPosition();
                if(lastVisiblePosition == -1 || (positionStart >= (runCount -1) && lastVisiblePosition == (positionStart -1))) {
                    mRunRecyclerView.scrollToPosition(positionStart);
                }
            }
        });

        mRunRecyclerView.setLayoutManager(mLinearLayoutManager);
        mRunRecyclerView.setAdapter(mFirebaseAdapter);


        fab = (FloatingActionButton) findViewById(R.id.fab);

        //Using the ItemTouchHelper class, setup recyclerView to allow swiping of each card.
        //Upon swipe left or right, remove it from Firebase database
        //As a result it is removed from the recyclerView because of the Firebase Adapter
        ItemTouchHelper swipeToDismissTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction)
            {

                mFirebaseAdapter.getRef(viewHolder.getAdapterPosition()).removeValue();


            }

        });
        swipeToDismissTouchHelper.attachToRecyclerView(mRunRecyclerView);

        //Check if location ACCESS_FINE_LOCATION permission is granted
        checkPermissions();

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), UserLocationService.class);
                //Start the location service
                startService(intent);
                startActivity(new Intent(MainActivity.this, MapsActivity.class));
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

        //If Sign out button pressed, sign the user out by calling signOut()
        if (id == R.id.action_signOut) {
            signOut();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));

        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Check the sharedPreferences "login" to see if boolean is true or false. If user previously logged in boolean will be true
     * and therefore no need to go to the LoginActivity. Otherwise go to LoginActivity.
     */
    public void checkLogin() {
        SharedPreferences preferences = getSharedPreferences("login", Context.MODE_PRIVATE);
        boolean isLoggedIn = preferences.getBoolean("loggedIn", false);
        if(isLoggedIn == false){
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
        }
    }

    /**
     * Check if permission to ACCESS_FINE_LOCATION has been granted. If it has not, request the permission from the user, else don't request.
     * @return true if permission not granted
     *         false if permission already granted
     */
    private boolean checkPermissions() {
        if(Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 101);

            return true;

        }
        return false;
    }

    /**
     * Process the requestPermissions() method sent in the checkPermissions() method.
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == 101) {
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //Do nothing
            } else {
                checkPermissions();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Intent intent = new Intent(getApplicationContext(), UserLocationService.class);
        //Stops location service
        stopService(intent);

    }

    private void signOut() {
        //Firebase sign out
        mAuth.signOut();

        //Google sign out
        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
            new ResultCallback<Status>() {
                @Override
                public void onResult(@NonNull Status status) {
                }
            });

        saveLoginStatus();
    }

    /**
     * If error connecting to Google Play services display a toast
     * @param connectionResult
     */
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d("MainActivity", "onConnectionFailed:" + connectionResult);
        Toast.makeText(this, "Google Play Services error.", Toast.LENGTH_SHORT).show();
    }

    /**
     * Update login status in sharedPreferences if user logs out
     */
    public void saveLoginStatus() {
        SharedPreferences sharedPref = getSharedPreferences("login", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean("loggedIn", false);
        editor.commit();
    }
}

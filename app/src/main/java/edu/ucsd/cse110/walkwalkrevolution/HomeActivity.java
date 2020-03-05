package edu.ucsd.cse110.walkwalkrevolution;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Properties;

import edu.ucsd.cse110.walkwalkrevolution.firebase.FirebaseGoogleSignInService;
import edu.ucsd.cse110.walkwalkrevolution.fitness.FitnessService;
import edu.ucsd.cse110.walkwalkrevolution.fitness.FitnessServiceFactory;
import edu.ucsd.cse110.walkwalkrevolution.fitness.GoogleFitAdapter;


public class HomeActivity extends AppCompatActivity {

    public static final String FITNESS_SERVICE_KEY = "FITNESS_SERVICE_KEY";
    public static final String FITNESS_SERVICE_PROPERTIES = "fitness.properties";

    private static final String TAG = "HomeActivity";

    private int mockStepCount = 0;
    private boolean receiversRegistered;

    private TextView textSteps, textDistance;
    private FitnessService fitnessService;

    private BroadcastReceiver updateBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            fitnessService.updateStepCount();
        }
    };

    private BroadcastReceiver mockCountBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mockStepCount = intent.getIntExtra("Mock Step Count", 0);
        }
    };

    private FirebaseGoogleSignInService firebaseSignInService;
    private boolean isBound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Log.d(TAG, "Creating home activity");

        textSteps = findViewById(R.id.box_dailySteps);
        textDistance = findViewById(R.id.box_dailyDistance);

        // Check if user pressed start new run button
        Button bt_newRun = (Button) findViewById(R.id.bt_startNewWalk);
        bt_newRun.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gotoNewRun();
            }
        });

        // Check if user pressed routes button
        Button bt_routes = (Button) findViewById(R.id.bt_routes);
        bt_routes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gotoRoutes();
            }
        });


        // Support mocking
        Button bt_mock = (Button) findViewById(R.id.bt_mock);
        bt_mock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gotoMock();
            }
        });

        showHeightDialog();
        displayLastWalk();
        registerReceivers();
        launchFitnessActivity();
        launchUpdateService();
        launchFirebaseSignInService();
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            FirebaseGoogleSignInService.LocalBinder localBinder = (FirebaseGoogleSignInService.LocalBinder)service;
            firebaseSignInService = localBinder.getService();
            isBound = true;
            saveUserLogin();
        }
        @Override
        public void onServiceDisconnected(ComponentName name) { isBound = false; }
    };

    public void registerReceivers() {
        if (!receiversRegistered) {
            // Register broadcast receiver from step count updater
            registerReceiver(updateBroadcastReceiver, new IntentFilter(StepCountUpdateService.BROADCAST_ACTION));
            registerReceiver(mockCountBroadcastReceiver, new IntentFilter(MockActivity.BROADCAST_ACTION));
            receiversRegistered = true;
        }
    }

    @Override
    protected void onRestart() {
        Log.d(TAG, "Restarting home activity");
        registerReceivers();
        super.onRestart();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "Destroying home activity");
        if (receiversRegistered) {
            unregisterReceiver(updateBroadcastReceiver);
            unregisterReceiver(mockCountBroadcastReceiver);
            receiversRegistered = false;
        }
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // If authentication was required during google fit setup, this will be called after the user authenticates
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == fitnessService.getRequestCode()) {
                fitnessService.updateStepCount();
            }
        }
        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        else if (requestCode == FirebaseGoogleSignInService.RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseSignInService.firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e);
            }
        } else {
            Log.e(TAG, "ERROR, home activity result code: " + resultCode);
        }

    }

    public void launchFitnessActivity() {
        // Read the fitness key from the fitness properties file
        PropertyReader propertyReader = new PropertyReader(this, FITNESS_SERVICE_PROPERTIES);
        Properties properties = propertyReader.get();
        String fitnessServiceKey = properties.getProperty(FITNESS_SERVICE_KEY);

        // Create the fitness service using the factory
        FitnessServiceFactory.put(fitnessServiceKey, new FitnessServiceFactory.BluePrint() {
            @Override
            public FitnessService create(HomeActivity HomeActivity) {
                return new GoogleFitAdapter(HomeActivity);
            }
        });

        // If a fitness service key is passed in, it has higher priority (for testing)
        Intent intent = getIntent();
        if (intent.hasExtra(FITNESS_SERVICE_KEY)) {
            fitnessServiceKey = intent.getStringExtra(FITNESS_SERVICE_KEY);
        }

        fitnessService = FitnessServiceFactory.create(fitnessServiceKey, this);
        fitnessService.setup();
    }

    public void launchUpdateService() {
        Intent intent = new Intent(this, StepCountUpdateService.class);
        intent.putExtra("interval", 500);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startService(intent);
    }

    public void launchFirebaseSignInService() {
        Intent intent = new Intent(this, FirebaseGoogleSignInService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    public void gotoNewRun() {
        // Reset mock step count because the mock doesn't apply to a new walk
        Log.d(TAG, "Resetting mock count");
        mockStepCount = 0;
        updateStepCount();

        Log.d(TAG, String.valueOf(WalkWalkRevolutionApplication.stepCount.get()));
        Intent intent = new Intent(this, CurrentWalkActivity.class);
        SharedPreferences savedHeightPref = getSharedPreferences("saved_height", MODE_PRIVATE);
        float savedHeight = savedHeightPref.getFloat("user_height", -1);
        intent.putExtra("savedHeight", savedHeight);
        intent.putExtra("isSavedRoute", false);
        startActivity(intent);
    }

    public void gotoMock() {
        Intent intent = new Intent(this, MockActivity.class);
        startActivity(intent);
    }

    public void gotoRoutes() {
        Intent intent = new Intent(this, RoutesActivity.class);
        startActivity(intent);
    }

    public void showHeightDialog() {
        // Create an instance of the dialog fragment and show it
        HeightPromptDialog heightPrompt = new HeightPromptDialog();

        SharedPreferences savedHeightPref = getSharedPreferences("saved_height", MODE_PRIVATE);
        float savedHeight = savedHeightPref.getFloat("user_height", -1);
        boolean useHeight = savedHeightPref.getBoolean("use_height", true);

        if (savedHeight <= 0 && useHeight) {
            heightPrompt.show(getSupportFragmentManager(), "Height");
        }
    }

    public void setStepCount(int stepCount) {
        // Add mocked steps, if any
        stepCount += mockStepCount;

        WalkWalkRevolutionApplication.stepCount.set(stepCount);
        textSteps.setText(String.valueOf(stepCount));

        SharedPreferences savedHeightPref = getSharedPreferences("saved_height", MODE_PRIVATE);
        float savedHeight = savedHeightPref.getFloat("user_height", -1);
        float distance = MeasurementConverter.stepToMiles(stepCount, savedHeight);
        textDistance.setText(String.format("%.1f", distance));
    }

    public void updateStepCount() {
        fitnessService.updateStepCount();
    }

    public void displayLastWalk() {
        SharedPreferences lastWalk  = getSharedPreferences("lastWalk", MODE_PRIVATE);
        String lastSteps = lastWalk.getString("steps", "N/A");
        String lastDistance = lastWalk.getString("distance", "N/A");
        String lastTime = lastWalk.getString("time", "N/A");

        TextView stepsText = (TextView) findViewById(R.id.box_lastSteps);
        stepsText.setText(lastSteps);

        TextView distanceText = (TextView) findViewById(R.id.box_lastDistance);
        distanceText.setText(lastDistance);

        TextView timeText = (TextView) findViewById(R.id.box_lastTime);
        timeText.setText(lastTime);
    }

    public void saveUserLogin() {
        if (!firebaseSignInService.isSignedIn()) {
            firebaseSignInService.signIn(this);
        }

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        String name = user.getDisplayName();
        String email = user.getEmail();
    }
}

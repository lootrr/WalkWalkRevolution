package edu.ucsd.cse110.walkwalkrevolution;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

public class TeamWalkActivity extends AppCompatActivity {
    private static final String TAG = TeamWalkActivity.class.getSimpleName();
    TextView nameName, boxName, nameDate, boxDate, boxLocation, boxStatus, nameLocation;
    ListView responseList;
    Button bt_accept, bt_timeDecline, bt_routeDecline, bt_schedule, bt_withdraw;
    User appUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_team_walk);
        nameName = (TextView) findViewById(R.id.name_teamWalkName);
        boxName = (TextView) findViewById(R.id.box_teamWalkName);
        nameDate = (TextView) findViewById(R.id.name_teamWalkDate);
        boxDate = (TextView) findViewById(R.id.box_teamWalkDate);
        boxLocation = (TextView) findViewById(R.id.box_teamWalkLocation);
        boxStatus = (TextView) findViewById(R.id.box_teamWalkStatus);
        nameLocation = (TextView) findViewById(R.id.name_teamWalkLocation);
        responseList = (ListView) findViewById(R.id.teamWalkList);
        bt_accept = (Button) findViewById(R.id.bt_acceptTeamWalk);
        bt_timeDecline = (Button) findViewById(R.id.bt_timeDeclineTeamWalk);
        bt_routeDecline = (Button) findViewById(R.id.bt_routeDeclineTeamWalk);
        bt_schedule = (Button) findViewById(R.id.bt_scheduleTeamWalk);
        bt_withdraw = (Button) findViewById(R.id.bt_withdrawTeamWalk);

        appUser = User.getUser();

        // Check if user pressed home button
        Button bt_mainMenu = (Button) findViewById(R.id.bt_home);
        bt_mainMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gotoMainMenu();
            }
        });

        checkIfWalkExists();

        // Setup RouteManager
//        MembersManager manager = new MembersManager(this);
//
//        // Load Route List
//        ListView list = (ListView) findViewById(R.id.teamWalkList);
//
//        manager.load(WalkWalkRevolutionApplication.adapter, list);

    }

    private void unhideAll() {
        nameName.setVisibility(View.VISIBLE);
        boxName.setVisibility(View.VISIBLE);
        nameDate.setVisibility(View.VISIBLE);
        boxDate.setVisibility(View.VISIBLE);
        boxLocation.setVisibility(View.VISIBLE);
        findViewById(R.id.name_teamWalkStatus).setVisibility(View.VISIBLE);
        boxStatus.setVisibility(View.VISIBLE);
        responseList.setVisibility(View.VISIBLE);
    }

    private void checkIfWalkExists() {
        String teamId = Team.getTeam(this);

        if (teamId.equals(getResources().getString(R.string.empty))) { // If team does not exist, hide everything
            Log.d(TAG, "Not part of a team, keeping everything hidden.");
            nameLocation.setText("Not currently in a team!");
            nameLocation.setTextSize(TypedValue.COMPLEX_UNIT_SP,30);
        } else {
            String[] ids = {"teams", Team.getTeam(this)};
            DocumentReference docRef = WalkWalkRevolutionApplication.adapter.get(ids);
            docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Log.d(TAG, "Team '" + java.util.Arrays.toString(ids) + "' exists, checking if walk exists.");
                            Map walk = (Map) document.get("walk");
                            if (walk == null) {
                                nameLocation.setText("No team walk currently scheduled!");
                                nameLocation.setTextSize(TypedValue.COMPLEX_UNIT_SP,30);
                                Log.d(TAG, "No team walk exists, keeping everything hidden.");
                            }
                            else {
                                buildScreen(walk, (Map<String, String>) document.get("members"));
                            }
                        } else {
                            Log.d(TAG, "Team '" + java.util.Arrays.toString(ids) + "' does not exist, not checking if walk exists.");
                        }
                    }
                }
            });
        }
    }

    private void buildScreen(Map walk, Map<String, String> members) {
        boxName.setText((String) walk.get("name"));
        DateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy hh:mm a", Locale.US);
        String strDate = dateFormat.format(((Timestamp) walk.get("time")).toDate());
        boxDate.setText(strDate);
        boxLocation.setText((String) walk.get("location"));
        boxStatus.setText(((boolean) walk.get("status") ? "Scheduled" : "Proposed"));
        unhideAll();

        String creator = (String) walk.get("creator");
        if (checkIfCreator(creator)) {
            bt_schedule.setVisibility(View.VISIBLE);
            bt_withdraw.setVisibility(View.VISIBLE);
        }
        else {
            bt_accept.setVisibility(View.VISIBLE);
            bt_timeDecline.setVisibility(View.VISIBLE);
            bt_routeDecline.setVisibility(View.VISIBLE);
        }
        listResponses(members, (Map) walk.get("response"), creator);
    }

    private boolean checkIfCreator(String creator) {
        return appUser.getEmail().equals(creator);
    }

    private void listResponses(Map<String, String> members, Map<String, String> responses, String creator) {
        // Save response List
        ListView list = (ListView) findViewById(R.id.teamWalkList);


        ArrayList<String> membersList = new ArrayList<String>(members.values());
        ArrayList<String> responseList = new ArrayList<String>();
        for (String mEmail : members.keySet()) {
            if (responses.containsKey(mEmail)) {
                responseList.add(responses.get(mEmail));
            }
            else if (creator.equals(mEmail)) {
                responseList.add("(Creator)");
            }
            else {
                responseList.add("No Response");
            }
        }
        ResponseListAdapter customAdapter = new ResponseListAdapter(this, membersList, responseList);
        list.setAdapter(customAdapter);
    }


    public void gotoMainMenu() {
        Intent intentMainMenu = new Intent(this, HomeActivity.class);
        intentMainMenu.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intentMainMenu);
    }
}


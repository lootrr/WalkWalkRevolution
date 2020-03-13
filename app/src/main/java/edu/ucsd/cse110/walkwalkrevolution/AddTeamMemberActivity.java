package edu.ucsd.cse110.walkwalkrevolution;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AddTeamMemberActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_team_member);

        // Check if user pressed add button
        Button bt_finishAddMember = (Button) findViewById(R.id.bt_finishAddMember);

        // check if user pressed next
        bt_finishAddMember.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // name and emial must be entered
                if (((EditText) findViewById(R.id.box_name)).getText().toString().matches("")) {
                    Toast.makeText(AddTeamMemberActivity.this, "Please enter a name", Toast.LENGTH_SHORT).show();
                }
                else if (((EditText) findViewById(R.id.box_gmail)).getText().toString().matches("")) {
                    Toast.makeText(AddTeamMemberActivity.this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
                }
                else {
                    invite();
                    gotoTeamMenu();
                }
            }
        });
    }

    public void gotoTeamMenu() {
        Intent intent = new Intent(this, TeamActivity.class);
        startActivity(intent);
    }

    public void invite() {
        checkIfTeamExists();

        EditText eName = (EditText) findViewById(R.id.box_name);
        EditText eGmail = (EditText) findViewById(R.id.box_gmail);

        String name = eName.getText().toString();
        String email = eGmail.getText().toString();
    }

    private void checkIfTeamExists() {
        SharedPreferences teamSp = getSharedPreferences(getResources().getString(R.string.team_store), MODE_PRIVATE);
        SharedPreferences.Editor teamSpEdit = teamSp.edit();

        if (!teamSp.contains("teamId")) {
            FirebaseAuth mAuth = FirebaseAuth.getInstance();
            FirebaseUser user = mAuth.getCurrentUser();
            String name = user.getDisplayName();
            String email = user.getEmail();
            String uid = user.getUid();
            User appUser = new User(name, email, uid);

            Team team = new Team(appUser);
            String teamId = team.addToDatabase(WalkWalkRevolutionApplication.adapter);
            teamSpEdit.putString("teamId", teamId);
            teamSpEdit.commit();

            appUser.addTeamToDatabase(WalkWalkRevolutionApplication.adapter, teamId);
        }
    }
}

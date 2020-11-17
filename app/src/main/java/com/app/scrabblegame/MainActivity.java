package com.app.scrabblegame;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private DatabaseReference mDatabase;
    private String sessionId;
    private AlertDialog waitDialog;
    private boolean newSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btnCreate).setEnabled(false);
        findViewById(R.id.btnJoin).setEnabled(false);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_wait_users, null);
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        ((TextView)dialogView.findViewById(R.id.txtTitle)).setText(R.string.loading);
        dialog.setView(dialogView);
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                finish();
            }
        });
        waitDialog = dialog.create();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser==null){
            mAuth.signInWithEmailAndPassword("gm.scrabl@gmail.com", "Scrab2020")
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                initUI();
                            } else {
                                Toast.makeText(MainActivity.this, "Authentication failed.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        } else {
            initUI();
        }

        findViewById(R.id.btnRules).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, RulesActivity.class));
            }
        });

        findViewById(R.id.btnAbout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AboutActivity.class));
            }
        });
    }

    private void initUI(){
        findViewById(R.id.btnCreate).setEnabled(true);
        findViewById(R.id.btnCreate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newSession = true;
                createSession();
            }
        });
        findViewById(R.id.btnJoin).setEnabled(true);
        findViewById(R.id.btnJoin).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newSession = false;
                joinSession();
            }
        });
    }

    private void createSession(){
        final View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_session, null);
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setView(dialogView);
        final TextView title = dialogView.findViewById(R.id.txtTitle);
        final EditText inputId = dialogView.findViewById(R.id.input_id);
        title.setText(R.string.create_session);
        dialog.setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        hideKeyboard(dialogView);
                        if (inputId.getText().toString().length()>0){
                            sessionId = inputId.getText().toString();
                            createSessionFB();
                        }
                    }
                })
                .setNegativeButton("Отмена",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                dialog.dismiss();
                            }
                        });

        AlertDialog alertDialog = dialog.create();
        alertDialog.show();
    }

    private void createSessionFB(){
        waitDialog.show();
        mDatabase.child("sessions").child(sessionId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    waitDialog.dismiss();
                    Toast.makeText(MainActivity.this, "Попробуйте другой id для сессии",
                            Toast.LENGTH_SHORT).show();
                } else {
                    mDatabase.child("sessions").child(sessionId).child("sessionId").setValue(sessionId)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    showNameDialog("Игрок 1", sessionId);
                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                waitDialog.dismiss();
            }
        });
    }

    private void joinSession(){
        final View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_session, null);
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setView(dialogView);
        final TextView title = dialogView.findViewById(R.id.txtTitle);
        final EditText inputId = dialogView.findViewById(R.id.input_id);
        title.setText(R.string.join_session);
        dialog.setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        hideKeyboard(dialogView);
                        if (inputId.getText().toString().length()>0){
                            sessionId = inputId.getText().toString();
                            joinSessionFB();
                        }
                    }
                })
                .setNegativeButton("Отмена",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                dialog.dismiss();
                            }
                        });

        AlertDialog alertDialog = dialog.create();
        alertDialog.show();
    }

    private void joinSessionFB(){
        waitDialog.show();
        mDatabase.child("sessions").child(sessionId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    sessionId = dataSnapshot.getKey();
                    dataSnapshot.getRef().child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.getChildrenCount()==0){
                                showNameDialog("Игрок 1", sessionId);
                            } else {
                                if (dataSnapshot.getChildrenCount()==1){
                                    showNameDialog("Игрок 2", sessionId);
                                } else {
                                    waitDialog.dismiss();
                                    Toast.makeText(MainActivity.this, "В данный момент сессия недоступна",
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            waitDialog.dismiss();
                        }
                    });
                } else {
                    waitDialog.dismiss();
                    Toast.makeText(MainActivity.this, "Такой сессии не существует",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                waitDialog.dismiss();
            }
        });
    }

    private void showNameDialog(final String user, final String sessionId){
        waitDialog.dismiss();
        final View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_session, null);
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setView(dialogView);
        final TextView title = dialogView.findViewById(R.id.txtTitle);
        final EditText inputId = dialogView.findViewById(R.id.input_id);
        title.setText(R.string.name);
        dialog.setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        hideKeyboard(dialogView);
                        String userName = user;
                        if (inputId.getText().toString().length()>0){
                            userName = inputId.getText().toString();
                        }
                        Intent game = new Intent(MainActivity.this, GameActivity.class);
                        game.putExtra("sessionId", sessionId);
                        game.putExtra("userName", userName);
                        startActivity(game);
                    }
                })
                .setNegativeButton("Отмена",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                dialog.dismiss();
                                if (newSession) {
                                    mDatabase.child("sessions").child(sessionId).removeValue();
                                }
                            }
                        });

        AlertDialog alertDialog = dialog.create();
        alertDialog.show();
    }

    private void hideKeyboard(View view){
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(),0);
    }
}
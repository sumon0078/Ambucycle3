package com.akmist.ambucycle;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class PassengerLoginActivity extends AppCompatActivity {


        private EditText mEmail, mPassword;
        private Button mLogin, mRegistration;

        private FirebaseAuth mAuth;
        private FirebaseAuth.AuthStateListener firebaseAuthListener;
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_passenger_login);

            mAuth = FirebaseAuth.getInstance();

            firebaseAuthListener = new FirebaseAuth.AuthStateListener() {
                @Override
                public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if(user!=null){
                        Intent intent = new Intent(PassengerLoginActivity.this, PassengerMapActivity.class);
                        startActivity(intent);
                        finish();
                        return;
                    }
                }
            };

            mEmail = (EditText) findViewById(R.id.Email);
            mPassword = (EditText) findViewById(R.id.Password);

            mLogin = (Button) findViewById(R.id.Login);
            mRegistration = (Button) findViewById(R.id.Registration);

            mRegistration.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final String email = mEmail.getText().toString();
                    final String password = mPassword.getText().toString();
                    mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(PassengerLoginActivity.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if(!task.isSuccessful()){
                                Toast.makeText(PassengerLoginActivity.this, "sign up error", Toast.LENGTH_SHORT).show();
                            }else{
                                String user_id = mAuth.getCurrentUser().getUid();
                                DatabaseReference current_user_db = FirebaseDatabase.getInstance().getReference().child("Users").child("Passengers").child(user_id);
                                current_user_db.setValue(true);
                            }
                        }
                    });
                }
            });

            mLogin.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final String email = mEmail.getText().toString();
                    final String password = mPassword.getText().toString();
                    mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(PassengerLoginActivity.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if(!task.isSuccessful()){
                                Toast.makeText(PassengerLoginActivity.this, "sign in error", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                }
            });
        }


        @Override
        protected void onStart() {
            super.onStart();
            mAuth.addAuthStateListener(firebaseAuthListener);
        }
        @Override
        protected void onStop() {
            super.onStop();
            mAuth.removeAuthStateListener(firebaseAuthListener);
        }
    }




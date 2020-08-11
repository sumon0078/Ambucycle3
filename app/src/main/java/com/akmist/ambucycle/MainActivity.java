package com.akmist.ambucycle;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity {
    private Button mOperator, mPassenger;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mOperator = (Button) findViewById(R.id.Operator);
        mPassenger = (Button) findViewById(R.id.Passenger);


        mOperator.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, OperatorLoginActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });

        mPassenger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, PassengerLoginActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });
    }
}


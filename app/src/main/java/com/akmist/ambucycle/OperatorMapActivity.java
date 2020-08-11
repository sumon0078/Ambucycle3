package com.akmist.ambucycle;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.internal.IGoogleMapDelegate;
import com.google.android.gms.maps.internal.zzap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;
import java.util.Map;


public class OperatorMapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, zzap/*, RoutingListener */ {

    private GoogleMap mMap;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    LocationRequest mLocationRequest;

    private Button mLogout,mSettings;
    private String passengerId = "";

    private Boolean isLoggingOut =false;

    private SupportMapFragment mapFragment;

    private LinearLayout mPassengerInfo;

    private ImageView mPassengerProfileImage;

    private TextView mPassengerName, mPassengerPhone, mPassengerEmail, mPassengerDestination;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_operator_map);

       mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(OperatorMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        } else {

            mapFragment.getMapAsync(this);
        }


        mPassengerInfo = (LinearLayout) findViewById(R.id.passengerInfo);

        mPassengerProfileImage = (ImageView) findViewById(R.id.passengerProfileImage);

        mPassengerName = (TextView) findViewById(R.id.passengerName);
        mPassengerPhone = (TextView) findViewById(R.id.passengerPhone);
        mPassengerEmail = (TextView) findViewById(R.id.passengerEmail);
        mPassengerDestination = (TextView) findViewById(R.id.passengerDestination);
        mSettings = (Button) findViewById(R.id.settings);
        mLogout = (Button) findViewById(R.id.logout);


        mLogout.setOnClickListener(new View.OnClickListener() {
                                       @Override
                                       public void onClick(View v) {
                                           isLoggingOut = true;

                                           disconnectDriver();

                                           FirebaseAuth.getInstance().signOut();
                                           Intent intent = new Intent(OperatorMapActivity.this, MainActivity.class);
                                           startActivity(intent);
                                           finish();
                                           return;
                                       }
                                   });


             mSettings.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(OperatorMapActivity.this, OperatorSettingsActivity.class);
                    startActivity(intent);
                    return;
                }
            });


        getAssignedPassenger();
    }
    private void getAssignedPassenger() {
        String operatorId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference assignedPassengerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Operators").child(operatorId).child("passengerRequest").child("passengerRideId");
        assignedPassengerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){

                        passengerId = dataSnapshot.getValue().toString();
                        getAssignedPassengerPickupLocation();
                    getAssignedPassengerDestination();
                    getAssignedPassengerInfo();
                    }
                else{
                    passengerId = "";
                    if(pickupMarker != null){
                        pickupMarker.remove();
                    }
                    if(assignedPassengerPickupLocationRefListener != null){
                    assignedPassengerPickupLocationRef.removeEventListener(assignedPassengerPickupLocationRefListener);
                    }
                    mPassengerInfo.setVisibility(View.GONE);
                    mPassengerName.setText("");
                    mPassengerPhone.setText("");
                    mPassengerEmail.setText("");
                    mPassengerDestination.setText("Destination: --- " );
                    mPassengerProfileImage.setImageResource(R.mipmap.ic_propic);
                }
                }



            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    Marker pickupMarker;
    private DatabaseReference assignedPassengerPickupLocationRef;
    private ValueEventListener assignedPassengerPickupLocationRefListener;
    private void getAssignedPassengerPickupLocation(){
       assignedPassengerPickupLocationRef = FirebaseDatabase.getInstance().getReference().child("passengerRequest").child(passengerId).child("l");
        assignedPassengerPickupLocationRefListener = assignedPassengerPickupLocationRef.addValueEventListener(new ValueEventListener() {

           @Override
           public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
               if(dataSnapshot.exists() && !passengerId.equals("")){
                   List<Object> map = (List<Object>) dataSnapshot.getValue();
                   double locationLat=0;
                   double locationLng=0;

                   if(map.get(0) != null){
                       locationLat = Double.parseDouble(map.get(0).toString());

                   }
                   if(map.get(1) != null) {
                       locationLng = Double.parseDouble(map.get(1).toString());
                   }
                   LatLng operatorLatLng = new LatLng(locationLat,locationLng);

                   pickupMarker =  mMap.addMarker(new MarkerOptions().position(operatorLatLng).title("Pickup Location ").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_pickup)));
               }
           }

           @Override
           public void onCancelled(@NonNull DatabaseError databaseError) {

           }
       });
    }


    private void getAssignedPassengerDestination() {
        String operatorId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference assignedPassengerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Operators").child(operatorId).child("passengerRequest").child("destination");
        assignedPassengerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){

                    String  destination = dataSnapshot.getValue().toString();
                    mPassengerDestination.setText("Destination: " + destination);
                }
                else{
                    mPassengerDestination.setText("Destination: --- " );
                }
            }



            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }



    private void getAssignedPassengerInfo() {
        mPassengerInfo.setVisibility(View.VISIBLE);
        DatabaseReference mPassengerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Passengers").child(passengerId);
        mPassengerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if (map.get("name") != null) {
                        mPassengerName.setText(map.get("name").toString());
                    }
                    if (map.get("phone") != null) {
                        mPassengerPhone.setText(map.get("phone").toString());
                    }
                    if (map.get("email") != null) {

                        mPassengerEmail.setText(map.get("email").toString());
                    }
                    if (map.get("profileImageUrl") != null) {

                        Glide.with(getApplication()).load("profileImageUrl").into(mPassengerProfileImage);
                    }
                }
            }


            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

    }



    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(OperatorMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        }
        buildGoogleApiClient();
        mMap.setMyLocationEnabled(true);
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {
        if (getApplicationContext() != null) {

            mLastLocation = location;
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(11));

            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DatabaseReference refAvailable = FirebaseDatabase.getInstance().getReference("OperatorsAvailable");
            DatabaseReference refWorking = FirebaseDatabase.getInstance().getReference("OperatorsWorking");
            GeoFire geoFireAvailable = new GeoFire(refAvailable);
            GeoFire geoFireWorking = new GeoFire(refWorking);

            switch(passengerId){
                case "":
                    geoFireWorking.removeLocation(userId);
                    geoFireAvailable.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()));
                    break;
                default:
                    geoFireAvailable.removeLocation(userId);
                    geoFireWorking.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()));
                    break;

            }








        }

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(OperatorMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }




    final int LOCATION_REQUEST_CODE = 1;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case LOCATION_REQUEST_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mapFragment.getMapAsync(this);
                } else {
                    Toast.makeText(getApplicationContext(), "Please provide the permission", Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }

    private void disconnectDriver(){
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("operatorsAvailable");

        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(userId);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!isLoggingOut) {
            disconnectDriver();
        }
    }

    @Override
    public IBinder asBinder() {
        return null;
    }


    @Override
    public void zza(IGoogleMapDelegate iGoogleMapDelegate) throws RemoteException {

    }
}
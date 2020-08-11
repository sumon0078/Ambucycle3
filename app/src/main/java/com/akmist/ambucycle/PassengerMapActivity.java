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
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
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
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PassengerMapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, zzap/*, RoutingListener */ {


    private GoogleMap mMap;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    LocationRequest mLocationRequest;

    private Button mLogout,mRequest, mSettings;
    private LatLng pickupLocation;
    private Marker pickupMarker;

    private Boolean requestBol = false;

    private SupportMapFragment mapFragment;
    private String destination, requestService;




    private LinearLayout mOperatorInfo;

    private ImageView mOperatorProfileImage;

    private TextView mOperatorName, mOperatorPhone, mOperatorEmail, mOperatorAmbulance;






    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passenger_map);

        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(PassengerMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        } else {

            mapFragment.getMapAsync(this);
        }

        mOperatorInfo = (LinearLayout) findViewById(R.id.operatorInfo);

        mOperatorProfileImage = (ImageView) findViewById(R.id.operatorProfileImage);

        mOperatorName = (TextView) findViewById(R.id.operatorName);
        mOperatorPhone = (TextView) findViewById(R.id.operatorPhone);
        mOperatorEmail = (TextView) findViewById(R.id.operatorEmail);
        mOperatorAmbulance = (TextView) findViewById(R.id.operatorAmbulance);

        mLogout = (Button) findViewById(R.id.logout);
        mRequest = (Button) findViewById(R.id.request);
        mSettings = (Button) findViewById(R.id.settings);


        mLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(PassengerMapActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });


        mRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (requestBol) {
                    requestBol = false;
                    geoQuery.removeAllListeners();
                    operatorLocationRef.removeEventListener(operatorLocationRefListener);

                    if (operatorFoundID != null) {
                        DatabaseReference operatorRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Operators").child(operatorFoundID).child("PassengerRequest");
                        operatorRef.removeValue();
                        operatorFoundID = null;
                    }
                    operatorFound = false;
                    radius = 1;
                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("PassengerRequest");
                    GeoFire geoFire = new GeoFire(ref);
                    geoFire.removeLocation(userId);

                    if (pickupMarker != null) {
                        pickupMarker.remove();
                    }
                    if (mOperatorMarker != null) {
                        mOperatorMarker.remove();
                    }
                    mRequest.setText("Call Ambulance");
                    mOperatorInfo.setVisibility(View.GONE);
                    mOperatorName.setText("");
                    mOperatorPhone.setText("");
                    mOperatorEmail.setText("");
                    mOperatorAmbulance.setText("");
                    mOperatorProfileImage.setImageResource(R.mipmap.ic_propic);

                } else {
                    requestBol = true;

                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("PassengerRequest");
                    GeoFire geoFire = new GeoFire(ref);
                    geoFire.setLocation(userId, new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()));

                    pickupLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                    pickupMarker = mMap.addMarker(new MarkerOptions().position(pickupLocation).title("Pickup Here").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_pickup)));

                    mRequest.setText("Getting your Ambulance....");

                    getClosestOperator();
                }
                mSettings.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(PassengerMapActivity.this, PassengerSettingsActivity.class);
                        startActivity(intent);
                        return;
                    }
                });
            }
        });




        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);



        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                // TODO: Get info about the selected place.
            destination = place.getName().toString();

            }

            @Override
            public void onError(Status status) {

            }
        });
    }
        private int radius = 1;
       private Boolean operatorFound = false;
       private String operatorFoundID;

    GeoQuery geoQuery;
        private void getClosestOperator() {
            DatabaseReference operatorLocation = FirebaseDatabase.getInstance().getReference().child("OperatorsAvailable");

            GeoFire geoFire = new GeoFire(operatorLocation);
            geoQuery = geoFire.queryAtLocation(new GeoLocation(pickupLocation.latitude, pickupLocation.longitude), radius);
            geoQuery.removeAllListeners();

            geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
                @Override
                public void onKeyEntered(String key, GeoLocation location) {

                    if (!operatorFound && requestBol) {
                        operatorFound = true;
                        operatorFoundID = key;
                        DatabaseReference operatorRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Operators").child(operatorFoundID).child("passengerRequest");
                        String passengerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                        HashMap map = new HashMap();
                        map.put("passengerRideId", passengerId);
                        map.put("destination", destination);
                       // map.put("destinationLat", destinationLatLng.latitude);
                       // map.put("destinationLng", destinationLatLng.longitude);
                        operatorRef.updateChildren(map);
                        getOperatorLocation();
                        getOperatorInfo();
                        //getHasRideEnded();
                        mRequest.setText("Looking for Operator Location....");
                    }
                }

                @Override
                public void onKeyExited(String key) {

                }

                @Override
                public void onKeyMoved(String key, GeoLocation location) {

                }

                @Override
                public void onGeoQueryReady() {
                    if (!operatorFound) {
                        radius++;
                        getClosestOperator();
                    }
                }

                @Override
                public void onGeoQueryError(DatabaseError error) {

                }


            });
        }
          private Marker mOperatorMarker;
        private DatabaseReference  operatorLocationRef;
        private ValueEventListener operatorLocationRefListener;
         private void getOperatorLocation(){
             operatorLocationRef = FirebaseDatabase.getInstance().getReference().child("operatorsWorking").child(operatorFoundID).child("l");
             operatorLocationRefListener =  operatorLocationRef.addValueEventListener(new ValueEventListener() {
          @Override
          public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
              if(dataSnapshot.exists() && requestBol) {
                  List<Object> map =(List<Object>) dataSnapshot.getValue();
                  double locationLat=0;
                  double locationLng=0;
                  mRequest.setText("Operator found");
                  if(map.get(0) != null){
                      locationLat = Double.parseDouble(map.get(0).toString());

                  }
                  if(map.get(1) != null) {
                      locationLng = Double.parseDouble(map.get(1).toString());
                  }
                  LatLng operatorLatLng = new LatLng(locationLat,locationLng);
                  if(mOperatorMarker != null){
                      mOperatorMarker.remove();
                  }
                  Location loc1 = new Location("");
                  loc1.setLatitude(pickupLocation.latitude);
                  loc1.setLongitude(pickupLocation.longitude);

                  Location loc2 = new Location("");
                  loc2.setLatitude(operatorLatLng.latitude);
                  loc2.setLongitude(operatorLatLng.longitude);

                  float distance = loc1.distanceTo(loc2);

                  mRequest.setText("Operator Found: " + String.valueOf(distance));
                  
                  mOperatorMarker = mMap.addMarker(new MarkerOptions().position(operatorLatLng).title("Your Operator ").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_launcher_ambulance)));
              }
          }

          @Override
          public void onCancelled(@NonNull DatabaseError databaseError) {

          }
      });



    }

    private void getOperatorInfo() {
        mOperatorInfo.setVisibility(View.VISIBLE);
        DatabaseReference mPassengerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Operators").child(operatorFoundID);
        mPassengerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if (map.get("name") != null) {
                        mOperatorName.setText(map.get("name").toString());
                    }
                    if (map.get("phone") != null) {
                        mOperatorPhone.setText(map.get("phone").toString());
                    }

                    if (map.get("email") != null) {

                        mOperatorAmbulance.setText(map.get("ambulance").toString());
                    }
                    if (map.get("profileImageUrl") != null) {

                        Glide.with(getApplication()).load("profileImageUrl").into(mOperatorProfileImage);
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
            ActivityCompat.requestPermissions(PassengerMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
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


        }

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(PassengerMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
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



    @Override
    protected void onStop() {
        super.onStop();
    }


    @Override
    public IBinder asBinder() {
        return null;
    }


    @Override
    public void zza(IGoogleMapDelegate iGoogleMapDelegate) throws RemoteException {

    }
}
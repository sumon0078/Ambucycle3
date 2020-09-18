package com.akmist.ambucycle;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PassengerMapActivity extends FragmentActivity implements OnMapReadyCallback {


    private GoogleMap mMap;

    Location mLastLocation;
    LocationRequest mLocationRequest;
    private FusedLocationProviderClient mFusedLocationClient;
    private Button mLogout, mRequest, mSettings;
    private LatLng pickupLocation;
    private Marker pickupMarker;

    private Boolean requestBol = false;

    private SupportMapFragment mapFragment;
    private String destination, requestService;
    private LatLng destinationLatLng;


    private LinearLayout mOperatorInfo;

    private ImageView mOperatorProfileImage;

    private TextView mOperatorName, mOperatorPhone, mOperatorEmail, mOperatorAmbulance;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passenger_map);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);

        destinationLatLng = new LatLng(0.0, 0.0);
        mOperatorInfo = (LinearLayout) findViewById(R.id.operatorInfo);

        mOperatorProfileImage = (ImageView) findViewById(R.id.operatorProfileImage);

        mOperatorName = (TextView) findViewById(R.id.operatorName);
        mOperatorPhone = (TextView) findViewById(R.id.operatorPhone);
        mOperatorEmail = (TextView) findViewById(R.id.operatorEmail);
        mOperatorAmbulance = (TextView) findViewById(R.id.operatorAmbulance);

        mLogout = (Button) findViewById(R.id.logout);
        mRequest = (Button) findViewById(R.id.request);
        mSettings = (Button) findViewById(R.id.settings);

      // if (!Places.isInitialized()) {
         //   Places.initialize(getApplicationContext(), getString(R.string.AIzaSyANBmLFtATKvCx0IZo6iYu_eScJrkQ-Ehs), Locale.US);
     //  }
       Places.initialize(getApplicationContext(), "AIzaSyANBmLFtATKvCx0IZo6iYu_eScJrkQ-Ehs");

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
                    endRide();

                } else {
                    requestBol = true;

                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("PassengerRequest");
                    if (mLastLocation != null) {
                        GeoFire geoFire = new GeoFire(ref);
                        geoFire.setLocation(userId, new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()), new GeoFire.CompletionListener() {
                            @Override
                            public void onComplete(String key, DatabaseError error) {
                          /*  if (error != null) {
                                System.err.println("There was an error saving the location to GeoFire: " + error);
                            } else {
                                System.out.println("Location saved on server successfully!");
                            }*/

                            }
                        });
                    }


                    if (mLastLocation != null) {
                        pickupLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                        pickupMarker = mMap.addMarker(new MarkerOptions().position(pickupLocation).title("Pickup Here").icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_pickover)));

                        mRequest.setText("Getting your Ambulance....");
                        getClosestOperator();
                    }
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

        String apiKey = getString(R.string.api_key);

        if (!Places.isInitialized()) {


            Places.initialize(getApplicationContext(), apiKey);

        }
        PlacesClient placesClient = Places.createClient(this);


        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)

                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);



        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME));



        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {

            @Override

            public void onPlaceSelected(Place place) {

                // TODO: Get info about the selected place.

                destination = place.getName().toString();
                destinationLatLng = place.getLatLng();

            }



            @Override

            public void onError(Status status) {

                // TODO: Handle the error.

               // Log.i(TAG, "An error occurred: " + status);

            }

        });

    }

       /* AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);



        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                // TODO: Get info about the selected place.
                destination = place.getName().toString();
                destinationLatLng = place.getLatLng();
            }

            @Override
            public void onError(Status status) {

            }
        });*/


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
                    HashMap<String, Object> map = new HashMap<String, Object>();
                    map.put("passengerRideId", passengerId);
                    map.put("destination", destination);
                    map.put("destinationLat", destinationLatLng.latitude);
                    map.put("destinationLng", destinationLatLng.longitude);
                    operatorRef.updateChildren(map);

                    mRequest.setText("Looking for Operator Location....");

                    getOperatorLocation();
                    getOperatorInfo();
                    getHasRideEnded();
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
    private DatabaseReference operatorLocationRef;
    private ValueEventListener operatorLocationRefListener;

    private void getOperatorLocation() {
        operatorLocationRef = FirebaseDatabase.getInstance().getReference().child("OperatorsWorking").child(operatorFoundID).child("l");
        operatorLocationRefListener = operatorLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && requestBol) {
                    List<Object> map = (List<Object>) dataSnapshot.getValue();
                    double locationLat = 0;
                    double locationLng = 0;
                    mRequest.setText("Operator found");
                    if (map.get(0) != null) {
                        locationLat = Double.parseDouble(map.get(0).toString());

                    }
                    if (map.get(1) != null) {
                        locationLng = Double.parseDouble(map.get(1).toString());
                    }
                    LatLng operatorLatLng = new LatLng(locationLat, locationLng);
                   // if (mOperatorMarker != null) {
                        mOperatorMarker.remove();
                   // }
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

    private DatabaseReference driveHasEndedRef;
    private ValueEventListener driveHasEndedRefListener;

    private void getHasRideEnded() {
        driveHasEndedRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Operators").child(operatorFoundID).child("passengerRequest").child("passengerRideId");
        driveHasEndedRefListener = driveHasEndedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {


                } else {
                    endRide();
                }

            }


            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    private void endRide() {
        requestBol = false;
        if(geoQuery != null  && operatorLocationRef != null  && driveHasEndedRef != null) {
            geoQuery.removeAllListeners();

            operatorLocationRef.removeEventListener(operatorLocationRefListener);
            driveHasEndedRef.removeEventListener(driveHasEndedRefListener);
        }
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
        geoFire.removeLocation(userId, new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, DatabaseError error) {

            }
        });

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
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            } else {
                checkLocationPermission();
            }
        }
        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
        mMap.setMyLocationEnabled(true);
    }

    LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            for (Location location : locationResult.getLocations()) {

                  if (getApplicationContext() != null) {

                    mLastLocation = location;
                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                    mMap.animateCamera(CameraUpdateFactory.zoomTo(11));

                    if(!getOperatorsAroundStarted)
                    getOperatorsAround();
                }
            }
        }
    };


    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                new AlertDialog.Builder(this)
                        .setTitle("give permission")
                        .setMessage("give permission message")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ActivityCompat.requestPermissions(PassengerMapActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                            }
                        })
                        .create()
                        .show();
            } else {
                ActivityCompat.requestPermissions(PassengerMapActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                        mMap.setMyLocationEnabled(true);
                    } else {
                        Toast.makeText(getApplicationContext(), "Please provide the permission", Toast.LENGTH_LONG).show();
                    }
                    break;
                }
            }
        }


    }

    boolean getOperatorsAroundStarted = false;
    List<Marker> markers = new ArrayList<Marker>();

    private void getOperatorsAround() {
        getOperatorsAroundStarted = true;
        DatabaseReference operatorLocation = FirebaseDatabase.getInstance().getReference().child("OperatorsAvailable");



       GeoFire geoFire = new GeoFire(operatorLocation);
       GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()), 999999999);



        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {

                for (Marker markerIt : markers) {
                    if (markerIt.getTag().equals(key))
                        return;
                }

                LatLng operatorLocation = new LatLng(location.latitude, location.longitude);

                Marker mOperatorMarker = mMap.addMarker(new MarkerOptions().position(operatorLocation).title(key).icon(BitmapDescriptorFactory.fromResource(R.drawable.ambulance)));
                mOperatorMarker.setTag(key);

                markers.add(mOperatorMarker);


            }


            @Override
            public void onKeyExited(String key) {
                for (Marker markerIt : markers) {
                    if (markerIt.getTag().equals(key)) {
                        markerIt.remove();
                        markers.remove(markerIt);
                        return;
                    }
                }
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
                for (Marker markerIt : markers) {
                    if (markerIt.getTag().equals(key)) {
                        markerIt.setPosition(new LatLng(location.latitude, location.longitude));
                    }
                }
            }

            @Override
            public void onGeoQueryReady() {
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }
}
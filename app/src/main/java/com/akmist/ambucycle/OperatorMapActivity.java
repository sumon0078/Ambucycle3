package com.akmist.ambucycle;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.bumptech.glide.Glide;
import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
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
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class OperatorMapActivity extends FragmentActivity implements OnMapReadyCallback, RoutingListener {

    private GoogleMap mMap;

    Location mLastLocation;
    LocationRequest mLocationRequest;

    private FusedLocationProviderClient mFusedLocationClient;


    private Button mLogout, mSettings, mRideStatus;

    private Switch mWorkingSwitch;

    private int status = 0;
    private String passengerId = "", destination;

    private LatLng destinationLatLng, pickupLatLng;

    private Boolean isLoggingOut = false;

    private SupportMapFragment mapFragment;

    private LinearLayout mPassengerInfo;

    private ImageView mPassengerProfileImage;

    private TextView mPassengerName, mPassengerPhone, mPassengerEmail, mPassengerDestination;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_operator_map);

        polylines = new ArrayList<>();

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
       mapFragment.getMapAsync(this);



        mPassengerInfo = (LinearLayout) findViewById(R.id.passengerInfo);

        mPassengerProfileImage = (ImageView) findViewById(R.id.passengerProfileImage);

        mPassengerName = (TextView) findViewById(R.id.passengerName);
        mPassengerPhone = (TextView) findViewById(R.id.passengerPhone);
        mPassengerEmail = (TextView) findViewById(R.id.passengerEmail);
        mPassengerDestination = (TextView) findViewById(R.id.passengerDestination);

        mWorkingSwitch = (Switch) findViewById(R.id.workingSwitch);
        mWorkingSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    connectOperator();
                }else{
                    disconnectOperator();
                }
            }
        });


        mSettings = (Button) findViewById(R.id.settings);
        mLogout = (Button) findViewById(R.id.logout);
        mRideStatus = (Button) findViewById(R.id.rideStatus);
       // mHistory = (Button) findViewById(R.id.history);
        mRideStatus.setOnClickListener(new View.OnClickListener() {
            @Override
           public void onClick(View v) {
                switch(status){
                  case 1:
                       status=2;
                        erasePolylines();
                        if(destinationLatLng.latitude!=0.0 && destinationLatLng.longitude!=0.0){
                            getRouteToMarker(destinationLatLng);
                        }
                        mRideStatus.setText("drive completed");

                        break;
                  case 2:

                       endRide();
                        break;
                }
            }
        });


        mLogout.setOnClickListener(new View.OnClickListener() {
                                       @Override
                                       public void onClick(View v) {
                                           isLoggingOut = true;

                                           disconnectOperator();

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
                    status=1;
                    passengerId = dataSnapshot.getValue().toString();
                    getAssignedPassengerPickupLocation();
                    getAssignedPassengerDestination();
                    getAssignedPassengerInfo();
                    }
               else{
                    endRide();
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
                   LatLng pickupLatLng = new LatLng(locationLat,locationLng);

                   pickupMarker =  mMap.addMarker(new MarkerOptions().position(pickupLatLng).title("Pickup Location ").icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_pickover)));
                   getRouteToMarker(pickupLatLng);
               }
           }

           @Override
           public void onCancelled(@NonNull DatabaseError databaseError) {

           }
       });
    }

    private void getRouteToMarker(LatLng pickupLatLng) {
        if (pickupLatLng != null && mLastLocation != null) {
            Routing routing = new Routing.Builder()
                    .key("AIzaSyANBmLFtATKvCx0IZo6iYu_eScJrkQ-Ehs")
                    .travelMode(AbstractRouting.TravelMode.DRIVING)
                    .withListener(this)
                    .alternativeRoutes(false)
                    .waypoints(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()), pickupLatLng)
                    .build();
            routing.execute();
        }


    }
    private void getAssignedPassengerDestination() {
        String operatorId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference assignedPassengerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Operators").child(operatorId).child("passengerRequest");
        assignedPassengerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){

                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if(map.get("destination")!=null){
                        destination = map.get("destination").toString();
                        mPassengerDestination.setText("Destination: " + destination);
                    }
                    else{
                        mPassengerDestination.setText("Destination: --");
                    }

                    Double destinationLat = 0.0;
                    Double destinationLng = 0.0;
                    if(map.get("destinationLat") != null){
                        destinationLat = Double.valueOf(map.get("destinationLat").toString());
                    }
                    if(map.get("destinationLng") != null){
                        destinationLng = Double.valueOf(map.get("destinationLng").toString());
                        destinationLatLng = new LatLng(destinationLat, destinationLng);
                    }

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
                    if (dataSnapshot.child("name") != null) {
                        mPassengerName.setText(dataSnapshot.child("name").getValue().toString());
                    }
                    if (dataSnapshot.child("phone") != null) {
                        mPassengerPhone.setText(dataSnapshot.child("phone").getValue().toString());
                    }
                    if (dataSnapshot.child("email") != null) {

                        mPassengerEmail.setText(dataSnapshot.child("email").getValue().toString());
                    }
                    if (dataSnapshot.child("profileImageUrl").getValue() != null) {

                        Glide.with(getApplication()).load(dataSnapshot.child("profileImageUrl").getValue().toString()).into(mPassengerProfileImage);
                    }
                }
            }


            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

    }

    private void endRide(){
        mRideStatus.setText("picked passenger");
        erasePolylines();

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference operatorRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Operators").child(userId).child("PassengerRequest");
        operatorRef.removeValue();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("passengerRequest");
        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(passengerId, new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {

                    }

                });


        passengerId="";
      //  rideDistance = 0;

        if(pickupMarker != null){
            pickupMarker.remove();
        }
        if (assignedPassengerPickupLocationRefListener != null){
            assignedPassengerPickupLocationRef.removeEventListener(assignedPassengerPickupLocationRefListener);
        }
        mPassengerInfo.setVisibility(View.GONE);
        mPassengerName.setText("");
        mPassengerPhone.setText("");
        mPassengerEmail.setText("");
        mPassengerDestination.setText("Destination: --");
        mPassengerProfileImage.setImageResource(R.mipmap.ic_propic);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){

            }else{
                checkLocationPermission();
            }
        }
    }

    LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            for (Location location : locationResult.getLocations()) {
                if (getApplicationContext() != null) {

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

                        switch (passengerId) {
                            case "":
                                geoFireWorking.removeLocation(userId, new GeoFire.CompletionListener() {
                                            @Override
                                            public void onComplete(String key, DatabaseError error) {

                                            }
                                        });
                                geoFireAvailable.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()), new GeoFire.CompletionListener() {
                                    @Override
                                    public void onComplete(String key, DatabaseError error) {

                                    }
                                });
                                break;
                            default:
                                geoFireAvailable.removeLocation(userId, new GeoFire.CompletionListener() {
                                    @Override
                                    public void onComplete(String key, DatabaseError error) {

                                    }
                                });
                                geoFireWorking.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()), new GeoFire.CompletionListener() {
                                @Override
                                public void onComplete(String key, DatabaseError error) {

                                }
                            });
                                break;

                        }

                    }

                }
            }
        }
    };


    private void checkLocationPermission() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                new AlertDialog.Builder(this)
                        .setTitle("give permission")
                        .setMessage("give permission message")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ActivityCompat.requestPermissions(OperatorMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                            }
                        })
                        .create()
                        .show();
            }
            else{
                ActivityCompat.requestPermissions(OperatorMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch(requestCode){
            case 1:{
                if(grantResults.length >0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                        mMap.setMyLocationEnabled(true);
                    }
                } else{
                    Toast.makeText(getApplicationContext(), "Please provide the permission", Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }














    private void connectOperator(){
        checkLocationPermission();
        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
        mMap.setMyLocationEnabled(true);

    }


    private void disconnectOperator(){
        if(mFusedLocationClient != null){
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("operatorsAvailable");

        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(userId, new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, DatabaseError error) {

            }
        });
    }







    private List<Polyline> polylines;
    private static final int[] COLORS = new int[]{R.color.primary_dark_material_light};
    @Override
    public void onRoutingFailure(RouteException e) {
        if(e != null) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }else {
            Toast.makeText(this, "Something went wrong, Try again", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRoutingStart() {

    }

    @Override
    public void onRoutingSuccess(ArrayList<Route> route, int shortestRouteIndex)  {
        if(polylines.size()>0) {
            for (Polyline poly : polylines) {
                poly.remove();
            }
        }

        polylines = new ArrayList<>();
        //add route(s) to the map.
        for (int i = 0; i <route.size(); i++) {

            //In case of more than 5 alternative routes
            int colorIndex = i % COLORS.length;

            PolylineOptions polyOptions = new PolylineOptions();
            polyOptions.color(getResources().getColor(COLORS[colorIndex]));
            polyOptions.width(10 + i * 3);
            polyOptions.addAll(route.get(i).getPoints());
            Polyline polyline = mMap.addPolyline(polyOptions);
            polylines.add(polyline);

            Toast.makeText(getApplicationContext(),"Route "+ (i+1) +": distance - "+ route.get(i).getDistanceValue()+": duration - "+ route.get(i).getDurationValue(),Toast.LENGTH_SHORT).show();
        }


    }

    @Override
    public void onRoutingCancelled() {
    }

    private void erasePolylines(){
        for(Polyline line : polylines){
            line.remove();
        }
        polylines.clear();
    }
}
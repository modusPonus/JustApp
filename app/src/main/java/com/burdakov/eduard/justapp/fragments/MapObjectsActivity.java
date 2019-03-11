package com.burdakov.eduard.justapp.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.burdakov.eduard.justapp.LoginActivity;
import com.burdakov.eduard.justapp.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.yandex.mapkit.Animation;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.location.FilteringMode;
import com.yandex.mapkit.location.Location;
import com.yandex.mapkit.location.LocationListener;
import com.yandex.mapkit.location.LocationManager;
import com.yandex.mapkit.location.LocationStatus;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.MapObjectCollection;
import com.yandex.mapkit.map.PlacemarkMapObject;
import com.yandex.mapkit.mapview.MapView;
import com.yandex.mapkit.user_location.UserLocationLayer;
import com.yandex.runtime.ui_view.ViewProvider;

import java.util.HashMap;

import static com.google.android.gms.common.util.CollectionUtils.mapOf;

public class MapObjectsActivity extends Fragment {

    private static final double DESIRED_ACCURACY = 0;
    private static final long MINIMAL_TIME = 0;
    private static final double MINIMAL_DISTANCE = 50;
    private static final boolean USE_IN_BACKGROUND = false;
    public static final int COMFORTABLE_ZOOM_LEVEL = 18;
    private final String MAPKIT_API_KEY = "1584f083-5511-433d-b63b-52e06a5e0c21";
    private MapView mapView;
    private LocationManager locationManager;
    private LocationListener myLocationListener;
    private Point myLocation;
    private MapObjectCollection mapObjects;
    private UserLocationLayer locationLayer;

    private EditText messageEditText;
    private Button sendMessageButton;
    private Button findMeButton;

    private FirebaseFirestore myDB = FirebaseFirestore.getInstance();
    private Object locText = new Object();
    private HashMap<String, Double> geoHash;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        MapKitFactory.setApiKey(MAPKIT_API_KEY);
        MapKitFactory.initialize(getContext());



        View view = inflater.inflate(R.layout.map_fragment, container, false);
        super.onCreate(savedInstanceState);

        mapView = (MapView) view.findViewById(R.id.mapview);
        mapView.getMap().move(
                new CameraPosition(new Point(54.7064900, 20.5109500), 11, 0.0f, 0.0f),
                new Animation(Animation.Type.SMOOTH, 1),
                null);

        messageEditText = (EditText) view.findViewById(R.id.ET_message);
        sendMessageButton = (Button) view.findViewById(R.id.B_send);
        findMeButton = (Button) view.findViewById(R.id.B_find_me);
        sendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (myLocation != null & messageEditText.getText() != null){
                    createPlaceMarkMapObjectWithViewProvider(myLocation, messageEditText.getText().toString());
                    setMessageLocationAndText(myLocation, messageEditText.getText().toString(), LoginActivity.user.getEmail());
                }else {
                    Toast.makeText(getContext(), "Location not found", Toast.LENGTH_SHORT).show();
                }
                messageEditText.setText("");
            }
        });

        findMeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (myLocation != null){
                    moveCamera(myLocation, COMFORTABLE_ZOOM_LEVEL);
                }else {
                    Toast.makeText(getContext(), "Location not found", Toast.LENGTH_SHORT).show();
                }

            }
        });

        locationManager = MapKitFactory.getInstance().createLocationManager();
        myLocationListener = new LocationListener() {
            @Override
            public void onLocationUpdated(Location location) {
                if (myLocation == null) {
                    moveCamera(location.getPosition(), COMFORTABLE_ZOOM_LEVEL);
                }
                myLocation = location.getPosition();
            }

            @Override
            public void onLocationStatusUpdated(LocationStatus locationStatus) {

            }
        };

        mapObjects = mapView.getMap().getMapObjects().addCollection();
        myThread.start();

        locationLayer = mapView.getMap().getUserLocationLayer();
        locationLayer.setEnabled(true);
        locationLayer.setHeadingEnabled(true);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        MapKitFactory.getInstance().onStart();
        mapView.onStart();

        subscribeToLocationUpdate();
    }

    @Override
    public void onStop() {
        super.onStop();
        MapKitFactory.getInstance().onStop();
        locationManager.unsubscribe(myLocationListener);
        mapView.onStop();
    }


    private void subscribeToLocationUpdate() {
        if (locationManager != null && myLocationListener != null) {
            locationManager.subscribeForLocationUpdates(DESIRED_ACCURACY, MINIMAL_TIME, MINIMAL_DISTANCE, USE_IN_BACKGROUND, FilteringMode.OFF, myLocationListener);
        }
    }

    private void moveCamera(Point point, float zoom) {
        mapView.getMap().move(
                new CameraPosition(point, zoom, 0.0f, 0.0f),
                new Animation(Animation.Type.SMOOTH, 1),
                null);

    }

    public void createPlaceMarkMapObjectWithViewProvider(Point point, String message) {


        TextView textView = new TextView(getContext());
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        textView.setLayoutParams(params);

        textView.setTextColor(Color.BLACK);
        textView.setBackgroundColor(R.drawable.custombackground);
        textView.setText(message);

        try {
            ViewProvider viewProvider = new ViewProvider(textView);
            PlacemarkMapObject viewPlacemark =
                    mapObjects.addPlacemark(point, viewProvider);}
                    catch (Exception e){}

    }

    public void setMessageLocationAndText(Point point, String message, String token){

        locText = myDB.collection("userLocation").add(mapOf("one", point, "two", message, "three", token));
    }

    public Thread myThread = new Thread(new Runnable() {
        @Override
        public void run() {
            getMessageLocation();
        }
    });

    public void getMessageLocation(){

        myDB.collection("userLocation").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {

                if (task.isSuccessful()) {

                    for (QueryDocumentSnapshot document : task.getResult()) {

                        geoHash = (HashMap<String, Double>) document.get("one");

                        Point point = new Point(geoHash.get("latitude"), geoHash.get("longitude"));
                        String message = (String) document.get("two");
                        String mail = (String) document.get("three");
                        createPlaceMarkMapObjectWithViewProvider(point, message + "(Added " + mail + ")");
                    }
                }
            }
        });
    }
}


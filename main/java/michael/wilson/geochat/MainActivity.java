package michael.wilson.geochat;

import androidx.fragment.app.FragmentActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.Switch;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;

/** The heart of the application, all other activities stem from here
 * Provides the user with the ability to:
 * > See all posts on the map
 * > Select a post to view it
 * > Add a new post to the map
 * > Change the map to satellite mode
 * > View their own location */
public class MainActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnInfoWindowClickListener
{
    // objects used to fetch the user location
    FusedLocationProviderClient fusedLocationClient;
    LocationCallback locationCallback;
    LocationRequest locationRequest;

    // location update interval values
    final int LOCATION_UPDATE_INTERVAL = 10000, LOCATION_UPDATE_INTERVAL_FASTEST = 5000;

    // uesd to store the last user location
    Location userLocation;

    // The switch for enabling/disabling satellite mode
    Switch satelliteModeSwitch;

    // keep track if satellite mode is enabled
    static boolean mapSatelliteMode = false;

    // constant key values for shared preferences
    final String KEY_CAMERA_LAT = "camera_lat", KEY_CAMERA_LNG = "camera_lng", KEY_CAMERA_ZOOM = "camera_zoom", KEY_MAPMODE = "mapmode";

    /** Called when the activity is created
     * > Sets the satellite-mode switch to be un-clickable
     * > Pairs the map-fragment view with the 'onMapReady()' callback in this class
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        satelliteModeSwitch = findViewById(R.id.darkModeSwitch);
        satelliteModeSwitch.setEnabled(false);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    /** Called when the main google map is ready
     * > Initializes the main map
     * > Calls 'setupLocationLoop()' which starts the location updates
     * > Gets the maps's camera data from shared preferences
     * > Calls 'REFRESH_MAP()' which downloads all post data from firebase
     * > Sets the  satellite-mode switch to be clickable
     *
     * @param _googleMap - Freshly instantiated map object
     */
    @Override
    public void onMapReady(GoogleMap _googleMap)
    {
        Toolkit.MAP = _googleMap;
        Toolkit.MAP.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.mapstheme));
        Toolkit.MAP.setOnInfoWindowClickListener(this);
        Toolkit.MAP.setMyLocationEnabled(true);
        setupLocationLoop();

        SharedPreferences prefs = getPreferences(0);
        LatLng pos = new LatLng(prefs.getFloat(KEY_CAMERA_LAT, 0.0f), prefs.getFloat(KEY_CAMERA_LNG, 0.0f));
        float zoom = prefs.getFloat(KEY_CAMERA_ZOOM, 1.0f);
        Toolkit.MAP.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, zoom));
        mapSatelliteMode = prefs.getBoolean(KEY_MAPMODE, false);
        satelliteModeSwitch.setChecked(!mapSatelliteMode);
        if (mapSatelliteMode) Toolkit.MAP.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        else Toolkit.MAP.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        Toolkit.REFRESH_MAP();

        satelliteModeSwitch.setEnabled(true);
    }

    /** Called when the user clicks on a marker's info window
     * > Starts 'DiscussionActivity' and passes a unique identifier for the firebase document which corresponds to said marker
     *
     * @param _marker - Selected marker
     */
    @Override
    public void onInfoWindowClick(Marker _marker)
    {
        startActivity(Toolkit.OPEN_POST_INTENT(_marker.getTag().toString()));
    }

    /** Called when the activity is restarted
     * > Refreshes all posts
     */
    @Override
    protected void onRestart()
    {
        super.onRestart();
        Toolkit.REFRESH_MAP();
    }

    /** Called when the activity is destroyed
     * > Stops the location update loop
     */
    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    /** Called when the activity goes out of focus, and is therefore likely to be closed soon
     * > Stores the map's camera data into shared preferences
     */
    @Override
    protected void onPause()
    {
        super.onPause();

        if (Toolkit.MAP != null)
        {
            CameraPosition camera = Toolkit.MAP.getCameraPosition();
            SharedPreferences prefs = getPreferences(0);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putFloat(KEY_CAMERA_LAT, (float)camera.target.latitude);
            editor.putFloat(KEY_CAMERA_LNG, (float)camera.target.longitude);
            editor.putFloat(KEY_CAMERA_ZOOM, camera.zoom);
            editor.putBoolean(KEY_MAPMODE, mapSatelliteMode);
            editor.apply();
        }
    }

    /** Called when the user clicks the satellite-mode switch
     * > Toggles satellite-mode
     * > Refreshes map data
     *
     * @param _v - Switch view clicked
     * */
    public void mapModeSwitch(View _v)
    {
        if (Toolkit.MAP != null)
        {
            Toolkit.REFRESH_MAP();
            mapSatelliteMode = !((Switch)_v).isChecked();
            if (mapSatelliteMode) Toolkit.MAP.setMapType(GoogleMap.MAP_TYPE_HYBRID);
            else  Toolkit.MAP.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        }
    }

    /** Called when the user clicks the button to add a new post
     * > If the user has enabled location, starts the 'PostActivity' and passes location data
     * > If the user does not have location enables, displays a toast message
     *
     * @param _v - Button clicked (not used)
     * */
    public void postButt(View _v)
    {
        if (userLocation != null)
        {
            Intent postIntent = new Intent(MainActivity.this, PostActivity.class);
            postIntent.putExtra(Toolkit.KEY_NEWPOST_LAT, userLocation.getLatitude());
            postIntent.putExtra(Toolkit.KEY_NEWPOST_LNG, userLocation.getLongitude());
            startActivity(postIntent);
        }
        else Toolkit.TOAST(R.string.location_disabled);
    }

    /** Starts a loop which continuously gets and stores the latest user location */
    void setupLocationLoop()
    {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationRequest = LocationRequest.create();
        locationRequest.setInterval(LOCATION_UPDATE_INTERVAL);
        locationRequest.setFastestInterval(LOCATION_UPDATE_INTERVAL_FASTEST);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback()
        {
            @Override
            public void onLocationResult(LocationResult locationResult)
            {
                if (locationResult != null) userLocation = locationResult.getLastLocation();
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

}
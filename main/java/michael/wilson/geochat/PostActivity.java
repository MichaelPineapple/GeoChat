package michael.wilson.geochat;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;

/** This activity is where users will create a new post.
 * Location data for the new post is passes in via an intent from 'MainActivity'
 *
 * This activity allows the user to:
 * > See the location of their new post
 * > Give their new post a title and body
 * > Submit the post to firebase
 */
public class PostActivity extends AppCompatActivity implements OnMapReadyCallback
{
    // GoogleMap object to store mini-map
    GoogleMap miniMap;

    // Location of the post
    LatLng loc;

    /** Called when the activity is created
     * > Gets location data from intent and stores it in a memeber variable
     * > Instantiates and pairs the mini-map view with this class's 'onMapReady()' callback
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);
        Intent intent = getIntent();
        loc = new LatLng(intent.getDoubleExtra(Toolkit.KEY_NEWPOST_LAT, 0), intent.getDoubleExtra(Toolkit.KEY_NEWPOST_LNG, 0));
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.miniMap);
        mapFragment.getMapAsync(this);
    }

    /** Called when the mini-map is ready
     * > Initializes mini-map and marker with data from member variables
     *
     * @param _googleMap - Freshly instantiated google map object
     */
    @Override
    public void onMapReady(GoogleMap _googleMap)
    {
        miniMap = _googleMap;
        miniMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.mapstheme));
        miniMap.moveCamera(CameraUpdateFactory.zoomTo(15.0f));
        miniMap.setMyLocationEnabled(true);
        MarkerOptions options = new MarkerOptions();
        options.position(loc);
        options.title("[YOUR POST HERE]");
        options.icon(BitmapDescriptorFactory.fromResource(R.mipmap.mcl_marker_blue));
        miniMap.addMarker(options);
        miniMap.moveCamera(CameraUpdateFactory.newLatLng(loc));
    }

    /** When the back button is pressed, close this activity */
    @Override
    public void onBackPressed()
    {
        finish();
        super.onBackPressed();
    }

    /** Called when 'SUBMIT' button is clicked
     * > Gets string data from title and body textbox (If textboxes are empty, default strings are used)
     * > Calls 'CREATE_POST(...)' from Toolkit which adds a new post to firebases using the given parameters
     * > Closes the activity
     *
     * @param _v - Button clicked (not used)
     */
    public void submitButt(View _v)
    {
        if (loc != null)
        {
            String title = ((EditText)findViewById(R.id.postTitleBox)).getText().toString();
            String body = ((EditText)findViewById(R.id.postBodyBox)).getText().toString();
            if (title.length() < 1) title = getString(R.string.default_title);
            if (body.length() < 1) body = getString(R.string.default_body);
            Toolkit.CREATE_POST(loc, title, body, Toolkit.GET_DATE(), Toolkit.GET_DEVICE_ID(), Toolkit.GET_USER_TOKEN());
            finish();
        }
    }

}

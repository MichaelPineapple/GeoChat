package michael.wilson.geochat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/** This activity must be started with an intent containing the unique identifier of a post Firebase document
 * This activity allows the user to:
 * > See a particular post's tile, body, location, and comments
 * > If the user created this post, they are given the option to delete it
 * > Submit a comment
 */
public class DiscussionActivity extends AppCompatActivity implements OnMapReadyCallback
{
    // Googlemap object for the mini-map
    private GoogleMap miniMap;

    // A marker to show this post's current location
    Marker marker;

    // Views in this activity
    TextView titleTxt, bodyTxt, dateTxt;
    View mapView;
    Button delButt;

    // The document reference to the post being shown
    DocumentReference docRef;

    /** Called when the activity is created
     * > Pairs the minimap view with this class's 'onMapReady()' callback
     * > Hides most of this activity's views
     * > Gets firebase document reference from identifier stored in the intent
     * > Updates textviews and minimap with data from firebase
     * > Shows the previously hidden views (Except DELETE button if user is not the owner)
     * > Passes comment data to 'handleComments(...)' which populates comment section with said data
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discussion);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.miniMap);
        mapFragment.getMapAsync(this);

        titleTxt = findViewById(R.id.postTitle);
        bodyTxt = findViewById(R.id.postBody);
        dateTxt =  findViewById(R.id.postDate);
        delButt = findViewById(R.id.buttDelPost);
        mapView = findViewById(R.id.miniMap);

        // hide most views
        titleTxt.setVisibility(View.INVISIBLE);
        bodyTxt.setVisibility(View.INVISIBLE);
        dateTxt.setVisibility(View.INVISIBLE);
        delButt.setVisibility(View.INVISIBLE);
        mapView.setVisibility(View.INVISIBLE);

        // get unique firebase doc id stored within intent
        String tag = getIntent().getStringExtra(Toolkit.KEY_DISCUSS_TAG);
        Toolkit.LOG("Post Loaded: "+tag);

        // get firebase document
        docRef = Toolkit.GET_POST(tag);

        // get data from firebase document and populate appropriate views
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>()
        {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task)
            {
                if (task.isSuccessful())
                {
                    try
                    {
                        FirestorePost tmp = task.getResult().toObject(FirestorePost.class);

                        // update textviews with data from firebase
                        titleTxt.setText(tmp.getTitle());
                        bodyTxt.setText(tmp.getBody());
                        dateTxt.setText(tmp.getDate());

                        // update marker position, color, and title, from firebase data
                        LatLng position = new LatLng(tmp.getLat(), tmp.getLng());
                        int markerIconResource = R.mipmap.mcl_marker;
                        if (tmp.getUserID().equals(Toolkit.GET_DEVICE_ID()))
                        {
                            markerIconResource = R.mipmap.mcl_marker_blue; // if owner, set marker to blue
                            delButt.setVisibility(View.VISIBLE); // if owner, show 'DELETE' button
                        }
                        if (marker != null)
                        {
                            marker.setPosition(position);
                            marker.setTitle(tmp.getTitle());
                            marker.setIcon(BitmapDescriptorFactory.fromResource(markerIconResource));
                        }
                        if (miniMap != null) miniMap.moveCamera(CameraUpdateFactory.newLatLng(position));

                        // show views which were previously hidden
                        titleTxt.setVisibility(View.VISIBLE);
                        bodyTxt.setVisibility(View.VISIBLE);
                        dateTxt.setVisibility(View.VISIBLE);
                        mapView.setVisibility(View.VISIBLE);

                        // pass comment data to be handled
                        handleComments(tmp.getComments());
                    }
                    catch (Exception ex) { Toolkit.TOAST(R.string.post_load_err); }
                }
                else  Toolkit.TOAST(R.string.post_load_err);
            }
        });

    }

    /** Called when the mini-map is ready
     * > Initializes the mini-map (The map's camera position is set to an arbitrary dummy data)
     * > Creates a marker (The marker's location, color, and title are set to arbitrary dummy data)
     *
     * @param _googleMap - Freshly instantiated map object
     */
    @Override
    public void onMapReady(GoogleMap _googleMap)
    {
        miniMap = _googleMap;
        miniMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.mapstheme));
        miniMap.moveCamera(CameraUpdateFactory.zoomTo(15.0f));
        miniMap.setMyLocationEnabled(true);
        LatLng tmpLoc = new LatLng(0.0, 0.0);
        marker = miniMap.addMarker(new MarkerOptions().position(tmpLoc).title("[TITLE HERE]"));
        miniMap.moveCamera(CameraUpdateFactory.newLatLng(tmpLoc));
    }

    /** Updates the comment section with data from an list of 'Comment' objects
     *
     * @param _commentsData - Array of 'Comment' objects to be handled
     * */
    void handleComments(ArrayList<Comment> _commentsData)
    {
        CommentListAdapter listAdapter = new CommentListAdapter(getApplicationContext(), R.layout.comment_layout, _commentsData);
        ListView commentListView = findViewById(R.id.commentList);
        commentListView.setAdapter(listAdapter);

        // set the listview's height equal to the total height of all the views contained within it
        int numberOfItems = listAdapter.getCount();
        int totalItemsHeight = 0;
        for (int itemPos = 0; itemPos < numberOfItems; itemPos++)
        {
            View item = listAdapter.getView(itemPos, null, commentListView);
            item.measure(0, 0);
            totalItemsHeight += (item.getMeasuredHeight());
        }
        int totalDividersHeight = commentListView.getDividerHeight() * (numberOfItems - 1);
        ViewGroup.LayoutParams params = commentListView.getLayoutParams();
        params.height = totalItemsHeight + totalDividersHeight;
        commentListView.setLayoutParams(params);
        commentListView.requestLayout();
    }

    /** Called when the button to submit a comment ('SUBMIT') is clicked
     *  > Gets the comment string stored in the 'Add a comment' textbox
     *  > Hides the keyboard and clears the comment textbox
     *  > Adds the new comment to Firebase
     *
     * @param _v - Button clicked (not used)
     */
    public void onCommentButt(View _v)
    {
        // get comment string
        EditText commentTxtBox = findViewById(R.id.commentTextBox);
        final String commentStr = commentTxtBox.getText().toString();

        // hide keyboard
        try
        {
            View view = this.getCurrentFocus();
            if (view != null)
            {
                InputMethodManager im = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                im.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
        catch (NullPointerException ex) { Toolkit.LOG("Error hiding keyboard"); }


        // Clear comment textbox
        commentTxtBox.clearFocus();
        commentTxtBox.setText("");

        // update firebase with new comment
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>()
        {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task)
            {
                if (task.isSuccessful())
                {
                    try
                    {
                        // Create new comment object from user input, current date, and phone id
                        Comment tmpComment = new Comment();
                        tmpComment.setCommentDate(Toolkit.GET_DATE());
                        tmpComment.setCommentText(commentStr);
                        tmpComment.setCommentUser(Toolkit.GET_DEVICE_ID());

                        // get current comment list, add the new comment to it, and then put that data into a hashmap
                        FirestorePost tmp = task.getResult().toObject(FirestorePost.class);
                        tmp.addComment(tmpComment);
                        Map<String, Object> hashMap = new HashMap<>();
                        hashMap.put(FirestorePost.COMMENTS_KEY, tmp.getComments());

                        // update firebase with new comment data
                        docRef.update(hashMap).addOnCompleteListener(new OnCompleteListener<Void>()
                        {
                            @Override
                            public void onComplete(@NonNull Task<Void> task)
                            {
                                if (task.isSuccessful())
                                {
                                    docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>()
                                    {
                                        @Override
                                        public void onComplete(@NonNull Task<DocumentSnapshot> task)
                                        {
                                            if (task.isSuccessful())
                                            {
                                                // update comment section views with data from firebase
                                                handleComments(task.getResult().toObject(FirestorePost.class).getComments());
                                                Toolkit.TOAST(R.string.comment_succ);
                                            }
                                            else Toolkit.TOAST(R.string.comment_err);
                                        }
                                    });
                                }
                                else Toolkit.TOAST(R.string.comment_err);
                            }
                        });
                    }
                    catch (Exception ex) { Toolkit.TOAST(R.string.comment_err); }
                }
                else Toolkit.TOAST(R.string.comment_err);
            }
        });

    }

    /** Called when the 'DELETE' button is clicked
     * > Deletes the current firebase document (AKA post)
     * > Closes the activity
     *
     * @param _v - Button clicked (Not used)
     * */
    public void onDeleteButt(View _v)
    {
        Toolkit.DELETE_POST(docRef);
        finish();
    }
}

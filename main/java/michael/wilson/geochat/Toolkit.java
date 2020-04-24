package michael.wilson.geochat;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.provider.Settings;
import android.security.ConfirmationNotAvailableException;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;
import androidx.annotation.NonNull;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class Toolkit
{
    /*** ~~~PRIVATE~~~ ***/

    // The current user's Firebase Cloud Messaging token, used as an address to send and recieve notifications from the service
    private static String USER_TOKEN = null;

    // Identifier for the main notification channel
    private static final String MAIN_NOTIFICATION_CHANNEL_ID = "channel00";

    // Global context
    private static Context CONTEXT;

    // Object used to communicate with Firebase
    private static FirebaseFirestore DB = FirebaseFirestore.getInstance();

    // Counter to ensure every notification has a unique id
    private static int NOTIFCOUNTER = 0;

    // The log tag (Used for debugging)
    private static final String LOG_TAG = "MCL";

    /*** ~~~PUBLIC~~~ ***/

    // Keys for communication via intents and notification payloads
    public static final String KEY_DISCUSS_TAG = "tag", KEY_NOTIFPAYLOAD_DOCID = "docID", KEY_NOTIFPAYLOAD_SENDER = "sender",
            KEY_NEWPOST_LAT = "lat", KEY_NEWPOST_LNG = "lng";

    // T H E   M A P
    public static GoogleMap MAP;

    /** Initializes this 'Toolbox' singleton
     *
     * > Sets the global context
     * > Sets up GooglePlay for this app if not already done
     * > Gets the user's unique firbase cloud messaging token from firebase and stores it in a global variable
     * > Setup the notification channel
     *
     * @param _activity - The activity calling this method, used to derive context
     */
    public static void INITIALIZE_APP(Activity _activity)
    {
        // set global context
        CONTEXT = _activity.getApplicationContext();

        // enable GooglePlayAPI
        GoogleApiAvailability.getInstance().makeGooglePlayServicesAvailable(_activity);

        // set global user token (for firebase cloud messaging)
        FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(new OnCompleteListener<InstanceIdResult>()
        {
            @Override
            public void onComplete(@NonNull Task<InstanceIdResult> task)
            {
                if (task.isSuccessful()) USER_TOKEN = task.getResult().getToken();
                else TOAST(CONTEXT.getString(R.string.general_err)+" (1)");
            }
        });

        // setup notification channel
        if (Build.VERSION.SDK_INT > 25)
        {
            NotificationManager notificationManager = CONTEXT.getSystemService(NotificationManager.class);
            NotificationChannel channel = new NotificationChannel(MAIN_NOTIFICATION_CHANNEL_ID, "Main Notification Channel", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }
    }

    /** Returns an intent to open 'DiscussionActivity' with the given document id
     *
     * @param _tag - Unique firebase document identifier
     * @return Intent which opens 'DiscussionActivity' using the provided document id
     */
    public static Intent OPEN_POST_INTENT(String _tag)
    {
        Intent i = new Intent(CONTEXT, DiscussionActivity.class);
        i.putExtra(KEY_DISCUSS_TAG, _tag);
        return i;
    }

    /** Creates and sends a notification
     *
     * @param _title - Title of the notification
     * @param _text - Main body of the notification
     * @param _intent - Pending intent for when the user clicks the notification
     */
    public static void NOTIFY(String _title, String _text, PendingIntent _intent)
    {
        NotificationManager notificationManager = CONTEXT.getSystemService(NotificationManager.class);
        Notification.Builder builder = new Notification.Builder(CONTEXT, MAIN_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(_title)
                .setContentText(_text)
                .setContentIntent(_intent)
                .setAutoCancel(true);
        notificationManager.notify(NOTIFCOUNTER, builder.build());
        NOTIFCOUNTER++;
    }

    /** Returns this device's unique id
     *
     * @return This device's unique id
     */
    public static String GET_DEVICE_ID()
    {
        return Settings.Secure.getString(CONTEXT.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    /** Returns a references to a firebase document given an id
     *
     * @param _id - Id of the firebase document
     * @return - Firebase document reference
     */
    public static DocumentReference GET_POST(String _id)
    {
        return DB.document(FirestorePost.COLLECTION_NAME+"/"+_id);
    }

    /** Refreshes the main 'MAP' with data from firebase
     * > Clears all markers from the map
     * > Gets all post data from the firebase collection
     * > For each post, add a marker to the map (Each marker is given a tag which corresponds to it's firebase document)
     */
    public static void REFRESH_MAP()
    {
        if (MAP != null)
        {
            // Clear all markers from the map
            MAP.clear();

            // get reference to the firebase collection
            CollectionReference database = DB.collection(FirestorePost.COLLECTION_NAME);

            // Query Firebase for all documents within the collection
            Task<QuerySnapshot> task = database.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>()
            {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task)
                {
                    if (task.isSuccessful())
                    {
                        // For each document retrieved, create a marker
                        for (QueryDocumentSnapshot document : task.getResult())
                        {
                            FirestorePost tmp = document.toObject(FirestorePost.class);
                            MarkerOptions options = new MarkerOptions();
                            options.position(new LatLng(tmp.getLat(), tmp.getLng()));
                            options.title(tmp.getTitle());
                            options.snippet(tmp.getDate());

                            // if this user created the post, make the marker blue, otherwise make it red
                            if (tmp.getUserID().equals(GET_DEVICE_ID()))
                            {
                                options.icon(BitmapDescriptorFactory.fromResource(R.mipmap.mcl_marker_blue));
                                options.zIndex(1);
                            }
                            else
                            {
                                options.icon(BitmapDescriptorFactory.fromResource(R.mipmap.mcl_marker));
                                options.zIndex(0);
                            }

                            Marker marker = MAP.addMarker(options);

                            // set the marker's tag equal to the document's id
                            marker.setTag(document.getId());
                        }
                        LOG("DATABASE REFRESHED!");
                    }
                    else TOAST(CONTEXT.getString(R.string.firebase_connect_err));
                }
            });
        }
    }

    /** Deletes a firebase document given a document reference
     *
     * @param _doc - Reference to the firebase document
     */
    public static void DELETE_POST(DocumentReference _doc)
    {
        _doc.delete().addOnCompleteListener(new OnCompleteListener<Void>()
        {
            @Override
            public void onComplete(@NonNull Task<Void> task)
            {
                if (task.isSuccessful()) TOAST(CONTEXT.getString(R.string.post_delete_succ));
                else TOAST(CONTEXT.getString(R.string.post_delete_err));
            }
        });
    }

    /** Creates a post (document) in firebase with the given parameters
     *
     * > Creates a hashmap with the given data
     * > Sends the hasmap to firebase which creates a new document
     *
     * @param _pos - Location of the post
     * @param _title - Title of the post
     * @param _body - Body of the post
     * @param _date - Date the post was created
     * @param _userID - DeviceID of the user who created the post
     * @param _userToken - Token of the user who created the post
     */
    public static void CREATE_POST(LatLng _pos, String _title, String _body, String _date, String _userID, String _userToken)
    {
        Map<String, Object> hashMap = new HashMap<>();
        hashMap.put(FirestorePost.LAT_KEY, _pos.latitude);
        hashMap.put(FirestorePost.LNG_KEY, _pos.longitude);
        hashMap.put(FirestorePost.TITLE_KEY, _title);
        hashMap.put(FirestorePost.BODY_KEY, _body);
        hashMap.put(FirestorePost.DATE_KEY, _date);
        hashMap.put(FirestorePost.USERID_KEY, _userID);
        hashMap.put(FirestorePost.USERTOKEN_KEY, _userToken);
        hashMap.put(FirestorePost.COMMENTS_KEY, new ArrayList<String>());

        DB.collection(FirestorePost.COLLECTION_NAME).add(hashMap).addOnCompleteListener(new OnCompleteListener<DocumentReference>()
        {
            @Override
            public void onComplete(@NonNull Task<DocumentReference> task)
            {
                if (task.isSuccessful()) TOAST(R.string.post_create_succ);
                else TOAST(R.string.post_create_err);
            }
        });
    }

    /** Gets and sets the user's Firebase Cloud Messaging token */
    public static String GET_USER_TOKEN() { return USER_TOKEN; }
    public static void SET_USER_TOKEN(String _newToken) { USER_TOKEN = _newToken; }

    /** Returns the system's current date as a string */
    public static String GET_DATE()
    {
        return new SimpleDateFormat("dd/MM/yyyy").format(Calendar.getInstance().getTime());
    }

    /** Creates a toast message for the given string or string resource id
     *
     * @param _strId - The string resource id to be shown
     * @param _str - The string to be shown
     * */
    public static void TOAST(int _strId) { TOAST(CONTEXT.getString(_strId)); }
    private static void TOAST(String _str)
    {
        Toast toast = Toast.makeText(CONTEXT, _str, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.BOTTOM, 0, 300);
        toast.show();
        LOG("toast("+_str+")");
    }

    /** Writes a given string to logs (Used for debigging)
     *
     * @param _str - The string to be logged
     * */
    public static void LOG(String _str) { Log.i(LOG_TAG, _str); }
}

/** An object to store data for a comment, also used for firebase formatting */
class Comment
{
    private String commentText, commentDate, commentUser;

    public Comment(){}

    public String getCommentText() {return this.commentText;};
    public String getCommentDate() {return this.commentDate;};
    public String getCommentUser() {return this.commentUser;};
    public void setCommentText(String _txt) {this.commentText = _txt;};
    public void setCommentDate(String _date) {this.commentDate = _date;};
    public void setCommentUser(String _user) {this.commentUser = _user;};
}

/** An object to store data for a post, used to format data for firebase documents */
class FirestorePost
{
    // firebase data keys
    public static final String COLLECTION_NAME = "mcl", LAT_KEY = "lat", LNG_KEY = "lng", TITLE_KEY = "title", BODY_KEY = "body",
            DATE_KEY = "date", COMMENTS_KEY = "comments", USERID_KEY = "userID", USERTOKEN_KEY = "usertoken";

    private String title, body, date, userID, usertoken;
    private ArrayList<Comment> comments;
    private double lat, lng;

    public FirestorePost(){}

    public String getTitle(){return title;}
    public String getBody(){return body;}
    public String getDate(){return date;}
    public String getUserID() {return userID;}
    public String getUserToken() {return usertoken;}
    public double getLat(){return lat;}
    public double getLng(){return lng;}
    public ArrayList<Comment> getComments(){return comments;}

    public void addComment(Comment _comment) {this.comments.add(0, _comment);}
}
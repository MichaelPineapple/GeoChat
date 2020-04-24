package michael.wilson.geochat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/** The entry point of the application.
 * > If the user has allowed all required permissions, it will send them to the main activity (MainActivity)
 * > If the user has not allowed all permissions, it will request that they do so.
 * > This activity also serves as the landing point for notifications received when the app is closed. */
public class LauncherActivity extends AppCompatActivity
{
    // the views in this activity
    TextView txt0;
    Button butt0;

    // unique identifier for the permission request
    final int PERMISSION_REQUESTCODE = 69;

    // permissions to be requested
    String[] PERMISSIONS = {Manifest.permission.ACCESS_FINE_LOCATION};

    /** Called when this activity is created.
     * > Hides all views in this activity
     * > Calls 'INITIALIZE_APP(...)' which initializes backend systems and global variables.
     * > Lastly, calls 'checkPermissions()' which begins to process of requesting/checking for permissions. */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);
        txt0 = findViewById(R.id.txt0);
        butt0 = findViewById(R.id.butt0);
        txt0.setVisibility(View.INVISIBLE);
        butt0.setVisibility(View.INVISIBLE);
        Toolkit.INITIALIZE_APP(this);
        checkPermissions();
    }

    /** Called once the user selects 'DENY' or 'ALLOW' on the permissions popup dialog.
     * If the user clicked 'ALLOW', 'continueToApp()' is called.
     * If the user clicked 'DENY', a button to try again and an explanation are shown.
     *
     * @param _requestCode - Unique identifier for the request
     * @param _permissions - Permissions being allowed/denied
     * @param _grantResults - Result data of the request
     */
    @Override
    public void onRequestPermissionsResult(int _requestCode, @NonNull String[] _permissions, @NonNull int[] _grantResults)
    {
        super.onRequestPermissionsResult(_requestCode, _permissions, _grantResults);
        if (_requestCode == PERMISSION_REQUESTCODE)
        {
            if (_grantResults.length == 1 && _grantResults[0] == PackageManager.PERMISSION_GRANTED) continueToApp();
            else
            {
                txt0.setVisibility(View.VISIBLE);
                butt0.setVisibility(View.VISIBLE);
            }
        }
    }

    /** Called when all permissions are enabled and the user is ready to continue.
     * > If the app was launched normally, it will start 'MainActivity'.
     * > If the app was launched from a notification, it will start 'DiscussionActivity' and pass the appropriate data.
     * > closes this activity*/
    void continueToApp()
    {
        Intent i = getIntent();
        if (i.getStringExtra(Toolkit.KEY_NOTIFPAYLOAD_SENDER) != null)
        {
            startActivity(Toolkit.OPEN_POST_INTENT(i.getStringExtra(Toolkit.KEY_NOTIFPAYLOAD_DOCID)));
        }
        else startActivity(new Intent(LauncherActivity.this, MainActivity.class));
        finish();
    }

    /** Called when the 'ALLOW LOCATION' button is clicked.
     * > Calls 'checkPermissions()' which repeats the first permission requests.
     *
     * @param _v - Button clicked (Not used)
     */
    public void onAllowLocationButt(View _v) { checkPermissions(); }

    /** Requests the user to allow permissions if needed. It calls 'continueToApp()' if the user already allowed permissions */
    void checkPermissions()
    {
        if (!checkAllPermissions(PERMISSIONS))  ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_REQUESTCODE);
        else continueToApp();
    }

    /** Takes an array of permissions and checks to see if the user has allowed them all
     *
     * @param _permissions - Array of permissions to be checked
     * */
    boolean checkAllPermissions(String[] _permissions)
    {
        Boolean result = false;
        for (String p : _permissions)
        {
            result &= (getApplicationContext().checkSelfPermission(p) == PackageManager.PERMISSION_GRANTED);
        }
        return result;
    }
}

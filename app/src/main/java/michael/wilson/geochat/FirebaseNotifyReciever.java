package michael.wilson.geochat;

import android.app.PendingIntent;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import java.util.Map;

/** A service which receives data from Firebase Cloud Messaging */
public class FirebaseNotifyReciever extends FirebaseMessagingService
{
    // counter to give each notification a unique identifier
    static int intentNum = 0;

    /** Called when Firebase Cloud Messaging sends a new token
     * > Sets the global user token when a new token is dispatched
     *
     * @param _token - New token
     * */
    @Override
    public void onNewToken(@NonNull String _token)
    {
        super.onNewToken(_token);
        Toolkit.SET_USER_TOKEN(_token);
    }

    /** Called when a message from Firebase Cloud Messaging is reveived
     * > Gets data from the message
     * > If the message was sent by this same phone, do not display it
     * > If the message was sent from a different phone, display it
     *
     * @param _remoteMessage - Incoming message from Firebase
     * */
    @Override
    public void onMessageReceived(@NonNull RemoteMessage _remoteMessage)
    {
        Map<String, String> data = _remoteMessage.getData();
        RemoteMessage.Notification notification = _remoteMessage.getNotification();

        if (!Toolkit.GET_DEVICE_ID().equals(data.get(Toolkit.KEY_NOTIFPAYLOAD_SENDER)))
        {
            String docID = data.get(Toolkit.KEY_NOTIFPAYLOAD_DOCID);
            Intent i = Toolkit.OPEN_POST_INTENT(docID);
            PendingIntent pintent = PendingIntent.getActivity(getBaseContext(), intentNum, i, PendingIntent.FLAG_UPDATE_CURRENT);
            Toolkit.NOTIFY(notification.getTitle(), notification.getBody(), pintent);
            intentNum++;
        }
    }
}

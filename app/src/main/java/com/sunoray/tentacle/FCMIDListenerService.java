package com.sunoray.tentacle;

import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.sunoray.tentacle.common.PreferenceUtil;

public class FCMIDListenerService extends FirebaseInstanceIdService {
    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is also called
     * when the InstanceID token is initially generated, so this is where
     * you retrieve the token.
     */
    // [START refresh_token]
    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.d("FCMIDListenerService","Refreshed token: " + refreshedToken);
        // TODO: Implement this method to send any registration to your app's servers.
        PreferenceUtil.setSharedPreferences(this, PreferenceUtil.REGID, refreshedToken);
        PreferenceUtil.setSharedPreferences(this, PreferenceUtil.FCM_IS_UPDATED, "true");
    }

}

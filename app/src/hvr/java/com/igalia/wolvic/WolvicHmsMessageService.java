package com.igalia.wolvic;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;

import com.huawei.hms.aaid.HmsInstanceId;
import com.huawei.hms.common.ApiException;
import com.huawei.hms.push.HmsMessageService;
import com.huawei.hms.push.HmsMessaging;
import com.huawei.hms.push.RemoteMessage;
import com.igalia.wolvic.utils.SystemUtils;

public class WolvicHmsMessageService extends HmsMessageService {

    public static final String COMMAND = "WolvicHmsMessageService.command";
    public static final int COMMAND_STOP_SERVICE = 1;
    public static final int COMMAND_GET_TOKEN = 2;
    public static final int COMMAND_DELETE_TOKEN = 3;
    public static final int COMMAND_AUTO_INIT = 4;
    public static final String ENABLED_EXTRA = "enabled";

    public static final String MESSAGE_RECEIVED_ACTION = "WolvicHmsMessageService.messageReceived";
    public static final String NOTIFICATION_EXTRA = "notification";

    protected final String LOGTAG = SystemUtils.createLogtag(this.getClass());

    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    private String mToken;

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.arg1) {
                case COMMAND_AUTO_INIT:
                    HmsMessaging.getInstance(WolvicHmsMessageService.this).setAutoInitEnabled(true);
                    break;
                case COMMAND_GET_TOKEN:
                    getToken();
                    break;
                case COMMAND_DELETE_TOKEN:
                    deleteToken();
                    break;
                case COMMAND_STOP_SERVICE:
                    stopSelf();
            }
        }
    }

    public WolvicHmsMessageService() {
        super();
    }

    @Override
    public void onCreate() {
        HandlerThread thread = new HandlerThread("ServiceStartArguments",
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        mServiceHandler.sendMessage(msg);

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

    @Override
    public void onNewToken(String token, Bundle bundle) {
        Log.e(LOGTAG, "PushKit: new token: " + token);

        mToken = token;
    }

    @Override
    public void onMessageReceived(RemoteMessage message) {
        Log.e(LOGTAG, "PushKit: onMessageReceived: " + message);

        if (message == null) {
            Log.e(LOGTAG, "PushKit: Received message entity is null!");
            return;
        }

        // Obtain the message content.
        Log.i(LOGTAG, "PushKit message: get Data: " + message.getData()
                + "\n getFrom: " + message.getFrom()
                + "\n getTo: " + message.getTo()
                + "\n getMessageId: " + message.getMessageId()
                + "\n getSentTime: " + message.getSentTime()
                + "\n getDataMap: " + message.getDataOfMap()
                + "\n getMessageType: " + message.getMessageType()
                + "\n getTtl: " + message.getTtl()
                + "\n getToken: " + message.getToken());

        // notify the activity
        Intent intent = new Intent();
        intent.setAction(MESSAGE_RECEIVED_ACTION);
        intent.putExtra(NOTIFICATION_EXTRA, message.getNotification());
        sendBroadcast(intent);
    }

    // Request a PushKit token from the server.
    private void getToken() {
        try {
            String appId = BuildConfig.HVR_APP_ID;
            String tokenScope = "HCM";

            String token = HmsInstanceId.getInstance(this).getToken(appId, tokenScope);

            Log.i(LOGTAG, "PushKit: got token: " + token);
            mToken = token;
        } catch (ApiException e) {
            Log.i(LOGTAG, "PushKit: getToken failed, " + e);
            e.printStackTrace();
        }
    }

    private void deleteToken() {
        try {
            String appId = BuildConfig.HVR_APP_ID;
            String tokenScope = "HCM";

            HmsInstanceId.getInstance(this).deleteToken(appId, tokenScope);

            Log.i(LOGTAG, "PushKit: token deleted");
        } catch (ApiException e) {
            Log.e(LOGTAG, "PushKit: deleteToken failed, " + e);
            e.printStackTrace();
        }
    }
}

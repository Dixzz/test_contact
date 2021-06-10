package org.testing;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import java.util.Timer;
import java.util.TimerTask;

public class MyService extends IntentService {
    private static final String TAG = "MyService";
    String NOTIFICATION_CHANNEL_ID = "1010";
    Integer NOTIFICATION_ID = 101;
    int counter = 1;
    private Timer timer;
    private NotificationManager notifManager;
    public StringViewModel model = new StringViewModel();

    private final IBinder binder = new LocalBinder();

    public MyService() {
        super(TAG);
    }

    /*private void sendMessageToActivity(String msg) {
        Intent intent1 = new Intent("someshit");
        intent1.putExtra("key", msg);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent1);
    }*/

    public void startTimer() {
        Log.e(TAG, "startTimer: ");
        timer = new Timer();

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(MyService.this, NOTIFICATION_CHANNEL_ID);
        TimerTask timerTask = new TimerTask() {
            public void run() {
                mHandler.sendEmptyMessage(counter);
                String step = String.valueOf(counter);
                Log.i("Count", "=========  " + (counter++));

                Notification notification = notificationBuilder.setOngoing(false)
                        .setContentTitle("Inserting contacts")
                        .setContentText(step)
                        .setTicker(step)
                        .setPriority(NotificationManager.IMPORTANCE_LOW)
                        .setCategory(Notification.CATEGORY_SERVICE)
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setProgress(10, Integer.parseInt(step), false)
                        .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                        .setPriority(NotificationCompat.PRIORITY_MIN)
                        .build();

                if (counter < 11) {
                    //sendMessageToActivity("hello " + counter);
                    notifManager.notify(NOTIFICATION_ID, notification);
                }
            }
        };
        timer.schedule(timerTask, 1000, 1000);
        timerTask.run();
    }

    private class InternalHandler extends Handler {
        public InternalHandler() {
        }

        @Override
        public void handleMessage(Message msg) {
            //Log.e("AAA", "" + msg);
            Message message = Message.obtain();
            message.obj = model;
            if (msg.replyTo != null) {
                try {
                    msg.replyTo.send(message);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        stoptimertask();
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void startMyOwnForeground() {
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, getClass().getSimpleName(), NotificationManager.IMPORTANCE_MIN);
        chan.setLightColor(Color.WHITE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

        notifManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notifManager != null)
            notifManager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(false)
                .setContentTitle("App is running in background")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(NOTIFICATION_ID, notification);
    }

    private void unloadResources() {
        timer = null;
        mHandler = null;
        notifManager = null;
    }

    public void stoptimertask() {
        if (timer != null) {
            timer.cancel();
        }
        unloadResources();
        stopSelf();
        stopForeground(true);
        System.gc();
        //System.exit(0);
    }
    /*@Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "onStartCommand: ");
        startTimer();
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return START_NOT_STICKY;
    }*/

    private Handler mHandler;

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind: ");
        model = null;
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        startService(new Intent(this, MyService2.class));
        stoptimertask();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        /*Messenger mMessenger;
        mHandler = new InternalHandler();
        mMessenger = new Messenger(mHandler);
        mHandler.sendEmptyMessage(10111);
        return mMessenger.getBinder();*/
        return binder;
    }

    /*@Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            startMyOwnForeground();
        } else {
            startForeground(NOTIFICATION_ID, new Notification());
        }
    }*/

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            startMyOwnForeground();
        } else {
            startForeground(NOTIFICATION_ID, new Notification());
        }
        if (model != null) {

            for (int i = 1; i <= 10; i++) {
                model.insertData(i + "");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public class LocalBinder extends Binder {
        MyService getService() {
            return MyService.this;
        }
    }
}
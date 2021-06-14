package org.testing;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;

import static org.testing.ContactActivity.CONTACTS_LOADER_ID;
import static org.testing.ContactActivity.CONTACTS_PROJECTION;
import static org.testing.ContactActivity.CONTACTS_SORT;
import static org.testing.ContactActivity.CONTACTS_URI;
import static org.testing.ContactActivity.CONTACT_DETAILS_LOADER_ID;
import static org.testing.ContactActivity.CONTACT_DETAILS_PROJECTION;
import static org.testing.ContactActivity.CONTACT_DETAILS_URI;

public class MyService extends Service implements ContactUtil.loaderLoad {
    private static final String TAG = "MyService";
    private static final String NOTIFICATION_CHANNEL_ID = "1010";
    private static final Integer NOTIFICATION_ID = 101;
    private Timer timer;
    private NotificationManager notifManager;
    public StringViewModel model = new StringViewModel();

    private LocalBinder binder = null;

    private binderToComponent binderToComponent;
    public componentToBinder componentToBinder = () -> new ContactUtil()
            .addLoader(new CursorLoader(MyService.this, CONTACTS_URI, CONTACTS_PROJECTION,
                    null, null, CONTACTS_SORT), CONTACTS_LOADER_ID, MyService.this)
            .addLoader(new CursorLoader(MyService.this, CONTACT_DETAILS_URI, CONTACT_DETAILS_PROJECTION,
                    null, null, CONTACTS_SORT), CONTACT_DETAILS_LOADER_ID, MyService.this);

    public void setBinderToComponent(binderToComponent binderToComponent) {
        this.binderToComponent = binderToComponent;
    }

    private Map<String, ContactUtil.ContactPojo> mContactsByLookupKey = new HashMap<>();
    private List<ContactUtil.ContactPojo> list = new ArrayList<>();

    public MyService() {
    }

    /*private void sendMessageToActivity(String msg) {
        Intent intent1 = new Intent("someshit");
        intent1.putExtra("key", msg);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent1);
    }*/


    @Override
    public void callbackLoad(Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
            case CONTACTS_LOADER_ID:
                readContacts(cursor);
                break;

            case CONTACT_DETAILS_LOADER_ID:
                readContactDetails(cursor);
                break;
        }
    }

    private void readContacts(Cursor cursor) {
        mContactsByLookupKey.clear();

        while (cursor != null && !cursor.isClosed() && cursor.moveToNext()) {
            ContactUtil.ContactPojo t = new ContactUtil.ContactPojo();
            //String displayName = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY));
            String lookupKey = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));

            //Log.e(TAG, "readContactDetails: "+cursor.getColumnIndex(ContactsContract.Contacts._ID));
            mContactsByLookupKey.put(lookupKey, t);
            //Log.e(TAG, "readContactDetails: " + cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID)));
            //Log.d("AAA", "" + displayName);
            if (cursor.isLast()) {
                cursor.close();
                cursor = null;
            }
        }
    }

    private ContactUtil.ContactPojo readContactDetails(Cursor cursor, ContactUtil.ContactPojo contact) {
        String mime = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.MIMETYPE));
        //Log.d("AAA", "readContactDetails() returned: " + mime);
        if (mime.equals(ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)) {
            String email = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS));
            if (email != null) {
                contact.setEmail(email);
            }
        } else if (mime.equals(ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)) {
            String name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME));
            if (name != null) contact.setName(name);
            contact.set_ID(Integer.parseInt(cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID))));
        } else if (mime.equals(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)) {
            String phone = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

            if (phone.contains("-")) {
                phone = phone.replaceAll("-", "");
            }
            phone = phone.trim();

            contact.setPhoneNumber(phone);
            return contact;
        }
        return null;
    }

    private int getContactCount() {
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(
                    CONTACTS_URI, null, null, null,
                    null);
            if (cursor != null) {
                return cursor.getCount();
            } else {
                return 0;
            }
        } catch (Exception ignore) {
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return 0;
    }

    private void readContactDetails(Cursor cursor) {
        if (cursor != null) {
            int i = 0;
            Log.d("AAA", "readContactDetails() returned: " + cursor.getCount());

            while (cursor != null && cursor.moveToNext()) {
                String lookupKey = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.LOOKUP_KEY));
                ContactUtil.ContactPojo t = mContactsByLookupKey.get(lookupKey);
                if (t != null) {
                    list.add(i, t);
                    list.set(i, readContactDetails(cursor, list.get(i)));
                    i += 1;
                } else {
                    mContactsByLookupKey.remove(lookupKey);
                }

                if (cursor.isLast()) {
                    cursor.close();
                    cursor = null;
                }
            }
        }
        new Thread() {
            @Override
            public void run() {
                JSONArray temp = new JSONArray();
                ObjectMapper mapper = new ObjectMapper();
                mapper.enable(SerializationFeature.INDENT_OUTPUT);

                List<ContactUtil.ContactPojo> arrayList = list;
                for (int i = 0; i < arrayList.size(); i++) {
                    if (arrayList.get(i) != null) {
                        JSONObject jsonObject = null;
                        try {
                            jsonObject = new JSONObject(mapper.writeValueAsString(list.get(i)));
                        } catch (JSONException | JsonProcessingException e) {
                            e.printStackTrace();
                        }
                        temp.put(jsonObject);
                    } else {
                        list.remove(arrayList.get(i));
                    }
                }
                arrayList.clear();

                JSONArray arrayFinal = new JSONArray();
                for (int i = 0; i < temp.length() - 1; i++) {
                    try {
                        if (!(temp.getJSONObject(i).getString("_ID").equals(temp.getJSONObject(i + 1).getString("_ID")))) {
                            arrayFinal.put(temp.get(i));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                /*String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + getPackageName();

                File dir = new File(path);
                if (!dir.exists()) {
                    dir.mkdirs();
                }

                File file2 = new File(path, "contacts2.txt");
                try {
                    FileOutputStream fileOutputStream = new FileOutputStream(file2);
                    fileOutputStream.write(arrayFinal.toString().getBytes());
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }*/
                if (binderToComponent != null) {
                    //binderToComponent.callback(arrayFinal);
                    binderToComponent = null;
                }
                list.clear();

                temp = null;
                arrayFinal = null;
            }
        }.start();
    }

    /*public void startTimer() {
        Log.e(TAG, "startTimer: ");
        timer = new Timer();

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(MyService.this, NOTIFICATION_CHANNEL_ID);
        TimerTask timerTask = new TimerTask() {
            public void run() {
                mHandler.sendEmptyMessage(counter);
                String step = String.valueOf(counter);
                Log.i("Count", "=========  " + (counter++));

                Notification notification = notificationBuilder.setOngoing(true)
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
    }*/

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.e(TAG, "onTaskRemoved: ");
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void startMyOwnForeground() {
        Log.d(TAG, "startMyOwnForeground: ");
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
        list = null;
        mContactsByLookupKey = null;
        timer = null;
        notifManager = null;
        model = null;
        componentToBinder = null;
        binderToComponent = null;
        //binder = null;
    }

    public void stoptimertask() {
        if (timer != null) {
            timer.cancel();
        }
        stopSelf();
        stopForeground(true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, startId, startId);
        return START_NOT_STICKY;
    }


    @Override
    public void onRebind(Intent intent) {
        Log.d(TAG, "onRebind: " + intent);
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind: ");

        binder = null;
        LocalBinder.clearCallingIdentity();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            LocalBinder.clearCallingWorkSource();
        }
        LocalBinder.flushPendingCommands();
        unloadResources();
        stoptimertask();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "onDestroy: ");
        stopService(new Intent(this, MyService.class));
        super.onDestroy();
    }

    //Handler mHandler;

    @Override
    public IBinder onBind(Intent intent) {

        /*Messenger mMessenger;
        mHandler = new Hand();
        mMessenger = new Messenger(mHandler);
        mHandler.sendEmptyMessage(10111);
        return mMessenger.getBinder();*/
        return binder;
    }

    /*@Override
    public void onCreate() {
        super.onCreate();
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            startMyOwnForeground();
        } else {
            startForeground(NOTIFICATION_ID, new Notification());
        }
    }*/

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate: " + Thread.currentThread().getName());
        binder = new LocalBinder();
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            startMyOwnForeground();
        } else {
            startForeground(NOTIFICATION_ID, new Notification());
        }

        //totalCount = getContactCount();
        if (model != null) {
            for (int i = 1; i <= 20; i++) {
                ContactUtil.ContactPojo t = new ContactUtil.ContactPojo();
                t.setEmail("10000");
                t.set_ID(i + 1);
                t.setPhoneNumber(" " + i);

                model.insertData2(t);
                //model.insertData(t);
            }
        }
        super.onCreate();
    }

/*    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            startMyOwnForeground();
        } else {
            startForeground(NOTIFICATION_ID, new Notification());
        }

        //totalCount = getContactCount();

        Log.d("AA", "" + totalCount);
        if (model != null) {
            for (int i = 1; i <= 20; i++) {
                ContactUtil.ContactPojo t = new ContactUtil.ContactPojo();
                t.setEmail("10000");
                t.set_ID(i + 1);
                t.setPhoneNumber(" " + i);

                model.insertData2(t);
                //model.insertData(t);
            }
        }
    }*/

    public class LocalBinder extends Binder {
        MyService getService() {
            return MyService.this;
        }
    }

    public interface binderToComponent {
        void callback(Object... o);
    }

    public interface componentToBinder {
        void fetchContacts();
    }
}
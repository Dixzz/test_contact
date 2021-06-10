package org.testing;

import android.app.Service;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.Timer;
import java.util.TimerTask;

public class MyService2 extends Service {
    private static final String TAG = "MyService2";
    private int counter;
    private ContactObserver contactObserver = null;
    private ContentResolver contentResolver = null;
    private long currentTimeMill;

    public MyService2() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private int getContactCount() {
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(
                    ContactsContract.Contacts.CONTENT_URI, null, null, null,
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

    @Override
    public void onCreate() {
        super.onCreate();
        startTimer();
        contentResolver = getApplicationContext().getContentResolver();
        contactObserver = new ContactObserver();


        if (contentResolver != null && contactObserver != null) {

            Log.d(TAG, "onCreate: " + "Resolver registered");
            contentResolver.registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, contactObserver);
        }
        Log.d("AAA", "count: " + getContactCount());
        /*registerReceiver(new BroadcastReceiver() {
            private static final String TAG = "MyService2";

            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "onReceive: " + intent);
            }
        }, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));*/
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (contactObserver != null && contentResolver != null) {
            contentResolver.unregisterContentObserver(contactObserver);
            contentResolver = null;
        }
    }

    public void startTimer() {
        Timer timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            public void run() {
                Log.i("Count", "=========  " + (counter++));
            }
        };
        //timer.scheduleAtFixedRate(timerTask, 1000, 1000);
    }

    private class ContactObserver extends ContentObserver {

        private static final String TAG = "ContactObserver";

        public ContactObserver() {
            super(null);
        }

        @Override
        public void onChange(boolean selfChange, @Nullable Uri uri) {
            currentTimeMill = System.currentTimeMillis() - 10000; // Keep min threshold
            Log.d(TAG, "onChange: " + uri);
            getContactList();
        }
    }

    private void getContactList() {
        // Move to separate thread

        Cursor cur = contentResolver.query(ContactsContract.Contacts.CONTENT_URI,
                null, null, null, null);
        if ((cur != null ? cur.getCount() : 0) > 0) {
            while (cur != null && cur.moveToNext()) {
                String id = cur.getString(
                        cur.getColumnIndex(ContactsContract.Contacts._ID));
                String name = cur.getString(cur.getColumnIndex(
                        ContactsContract.Contacts.DISPLAY_NAME));
                long updateTime = Long.parseLong(cur.getString(cur.getColumnIndex(
                        ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP)));

                if (cur.getInt(cur.getColumnIndex(
                        ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {
                    Cursor pCur = contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            new String[]{id}, null);

                    if (pCur != null) {
                        pCur.moveToFirst();
                    }

                    while (pCur != null && pCur.moveToNext()) {
                        String phoneNo = pCur.getString(pCur.getColumnIndex(
                                ContactsContract.CommonDataKinds.Phone.NUMBER));
                        Log.e("Contact time: ", "" + updateTime);
                        Log.e("Time stamp: ", "" + currentTimeMill);

                        if (updateTime >= currentTimeMill) {
                            Log.i("Contact", "Name: " + name);
                            Log.i("Contact", "Phone Number: " + phoneNo);
                            Log.i("Contact", "UpdateTime: " + updateTime);
                        }
                    }
                    pCur.close();
                }
            }
        }
        if (cur != null) {
            cur.close();
        }
    }
}
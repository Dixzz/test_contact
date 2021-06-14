package org.testing;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.Process;
import android.os.RemoteException;
import android.os.TransactionTooLargeException;
import android.provider.ContactsContract;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.loader.content.Loader;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static org.testing.ContactActivity.CONTACTS_LOADER_ID;
import static org.testing.ContactActivity.CONTACT_DETAILS_LOADER_ID;
import static org.testing.ContactActivity.CONTACT_DETAILS_URI;

public class MyService2 extends Service implements ContactUtil.loaderLoad {
    private static final String TAG = "MyService2";
    public static final String KEY_MODEL_LIST = "IPC_MODEL_LIST";
    public static final String KEY_FULL_MODEL_LIST = "IPC_FULL_MODEL_LIST";

    private int counter;
    private Messenger mMessenger = null;
    private Handler mHandler = null;
    private ContentResolver contentResolver = null;
    private Map<String, ContactUtil.ContactPojo> mContactsByLookupKey = new HashMap<>();
    private ArrayList<ContactUtil.ContactPojo> list = new ArrayList<>();

    public MyService2() {
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.e(TAG, "onBind: ");
        return mMessenger.getBinder();
    }

    private static final String CONTACTS_SORT = ContactsContract.Contacts.DISPLAY_NAME_PRIMARY + " ASC";
    private static final String NOTIFICATION_CHANNEL_ID = "1010";
    private static final Integer NOTIFICATION_ID = 101;
    private Timer timer;

    private static final Uri CONTACTS_URI = ContactsContract.Contacts.CONTENT_URI;
    private static final String[] CONTACTS_PROJECTION = new String[]{
            ContactsContract.Data.LOOKUP_KEY,
            ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,};

    private Messenger messenger = null;
    private StringViewModel model = null;

    /*private class InternalHandler extends Handler {

        public InternalHandler() {
            super(Looper.myLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (messenger == null) {
                messenger = msg.replyTo;
            } else {
                try {
                    messenger.send(msg);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            //Log.e("AAA", "" + msg.replyTo);
            //Message message = Message.obtain();
            *//*Message message1 = mHandler.obtainMessage();
            message1.what = 999;
            if (msg.replyTo != null) {
                try {
                    //msg.replyTo.send(message);
                    msg.replyTo.send(message1);
                    Log.e(TAG, "handleMessage: " + message1);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }*//*
        }
    }*/

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            startMyOwnForeground();
        } else {
            startForeground(NOTIFICATION_ID, new Notification());
        }
        Log.e(TAG, "onCreate: " + Thread.currentThread().getName());
        contentResolver = getContentResolver();

        if (mHandler == null) {
            mHandler = new Handler(Looper.myLooper()) {
                @Override
                public void handleMessage(@NonNull Message msg) {
                    super.handleMessage(msg);
                    if (messenger == null) {
                        messenger = msg.replyTo;
                    } else {
                        try {
                            messenger.send(msg);
                        } catch (TransactionTooLargeException e) {
                            e.printStackTrace();
                            Log.d(TAG, "handleMessage: " + msg.getData());

                            if (msg.getData().getSerializable(KEY_MODEL_LIST) != null) {
                                Intent intent = new Intent("someshit");
                                Bundle bundle = new Bundle();
                                bundle.putString(KEY_FULL_MODEL_LIST, arrayFinal.toString());
                                intent.putExtras(bundle);
                                //Log.d(TAG, "handleMessage: "+bundle);
                                sendBroadcast(intent);
                            }
                        } catch (RemoteException e) {
                            //e.printStackTrace();
                        }
                    }
                }
            };
        }
        if (mMessenger == null) {
            mMessenger = new Messenger(mHandler);
        }
        if (model == null) {
            model = new StringViewModel();
        }
        //startTimer();

        model.getUsers2().observeForever(s -> {
            Message message = new Message();
            Log.e(TAG, "onCreate: " + s);
            Bundle bundle = new Bundle();
            bundle.putSerializable(KEY_MODEL_LIST, s);
            message.setData(bundle);
            //mHandler.post(() -> mHandler.sendMessage(message));
        });


        if (contentResolver != null) {

            Log.d(TAG, "onCreate: " + "Resolver registered");
            contentResolver.registerContentObserver(ContactsContract.Contacts.CONTENT_VCARD_URI, true, new ContentObserver(null) {
                @Override
                public boolean deliverSelfNotifications() {
                    return false;
                }

                @Override
                public void onChange(boolean selfChange) {
                    super.onChange(selfChange);
                    Log.d(TAG, "onChange: " + selfChange);
                    checkContactAddedTime(new Date(System.currentTimeMillis()));
                }
            });
        }
        Log.d("AAA", "count: " + getContactCount());
    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "onDestroy: ");
        if (timer != null) {
            timer.cancel();
        }
        timer = null;
        mMessenger = null;
        mHandler = null;
        messenger = null;

        stopSelf();
        stopForeground(true);

        System.gc();
        android.os.Process.killProcess(Process.myPid());
        super.onDestroy();
        //currentTime = null;
    }


    public void startTimer() {
        if (mHandler != null) {
            timer = new Timer();
            TimerTask timerTask = new TimerTask() {
                public void run() {
                    Log.i("Count", "=========  " + (counter++));
                    mHandler.sendEmptyMessage(counter);
                }
            };
            timer.scheduleAtFixedRate(timerTask, 1000, 1000);
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.d("AAA", "Killed");
        stopService(rootIntent);
        stopService(new Intent(this, MyService2.class));
        super.onTaskRemoved(rootIntent);
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void startMyOwnForeground() {
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, getClass().getSimpleName(), NotificationManager.IMPORTANCE_MIN);
        chan.setLightColor(Color.WHITE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

        NotificationManager notifManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
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

    @Override
    public void onRebind(Intent intent) {
        Log.e(TAG, "onRebind: ");
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.e(TAG, "onUnbind: ");
        return true;
    }

    @Override
    public void callbackLoad(Loader<Cursor> loader, Cursor cursor) {
        Log.d(TAG, "callbackLoad: " + cursor.getCount());
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

    JSONArray arrayFinal;

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

                arrayFinal = new JSONArray();
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
                list.clear();
                list = ContactUtil.getListFromString(arrayFinal.toString());
                model.insertDataAll(list);
                list.clear();

                Message message = mHandler.obtainMessage();
                Bundle bundle = new Bundle();
                bundle.putString(KEY_FULL_MODEL_LIST, arrayFinal.toString());
                message.setData(bundle);
                mHandler.post(() -> mHandler.sendMessage(message));

                temp = null;
                //arrayFinal = null;
            }
        }.start();
    }

    private void checkContactAddedTime(Date time) {
        //Cursor cur = cr.query(CONTACTS_URI, null, ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP + " >= " + (System.currentTimeMillis() - 20000) + " AND " + System.currentTimeMillis() + " > " + ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP, null, ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP + " DESC");
        Cursor cur = contentResolver.query(CONTACTS_URI, null, null, null, ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP + " DESC");

        if ((cur != null ? cur.getCount() : 0) > 0) {
            Log.e(TAG, "checkContactAddedTime: " + Thread.currentThread().getName());
            cur.moveToFirst();

            Date contactUpdatedAt = new Date(Long.parseLong(cur.getString(cur.getColumnIndex(ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP))));
            //Log.d("AAAB", time + " :: " + contactUpdatedAt + " :: "+new Date(System.currentTimeMillis()));
            Log.d("AAA", contactUpdatedAt + " " + time + " " + cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)));


            if (mHandler != null) {
                Integer id = cur.getInt(cur.getColumnIndex(ContactsContract.Contacts._ID));
                ContactUtil.ContactPojo p = new ContactUtil.ContactPojo();
                p.set_ID(id);
                p.setName(cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)));
                if (cur.getInt(cur.getColumnIndex(
                        ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {
                    Cursor data = contentResolver.query(CONTACT_DETAILS_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=" + id, null, ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP + " DESC");
                    //Cursor data = contentResolver.query(CONTACT_DETAILS_URI, null, ContactsContract.Contacts._ID + "=" + id, null, ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP + " DESC");
                    if (data.getCount() > 0) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            // This is where ID is mound so update it
                            Log.d(TAG, "checkContactAddedTime: " + (model.getUsers2().getValue() != null && model.getUsers2().getValue().stream().anyMatch(s -> s._ID.equals(id))));

                            while (data.moveToNext()) {
                                p.setPhoneNumber(data.getString(data.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)));
                            }

                            if (model.getUsers2().getValue() != null && model.getUsers2().getValue().stream().anyMatch(s -> s._ID.equals(id))) {
                                ContactUtil.ContactPojo t = model.getUsers2().getValue().stream().filter(s -> s._ID.equals(id)).findFirst().get();
                                model.updateData3(t, p);
                            } else {
                                model.insertData2(p);
                            }
                        }
                    }

                    data.close();
                }
                //model.insertData3(p);
            }
            //currentTime = contactUpdatedAt.getTime();

            if (time.getTime() < contactUpdatedAt.getTime()) {
                Log.d("AAAC", time + " :: " + contactUpdatedAt + " :: " + new Date(System.currentTimeMillis()));
                //cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
            }
            cur.close();
            /*while (cur.moveToNext()) {
                long diff = time.getTime() - contactUpdatedAt.getTime();
                if (contactUpdatedAt.compareTo(time) >= 0) {
                    Log.d("AAAC", contactUpdatedAt + " :: " + time + " :: " + cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)));
                }
                if (TimeUnit.SECONDS.convert(diff, TimeUnit.MILLISECONDS) <= 10) {
                    Log.d("AAA", contactUpdatedAt + " " + time + " " + cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)));
                }
                *//*if (contactUpdatedAt.after(time)) { // Names get updated in each record it
                    Log.d("AAA", contactUpdatedAt + " " + time + " " + cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)));
                }*//*
                if (cur.isAfterLast()) {
                    cur.close();
                }
            }*/
        }
    }

    /*private void getContactList() {
        // Move to separate thread

        if (contentResolver != null) {
            Cursor cur = contentResolver.query(CONTACTS_URI,
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
                        Cursor pCur = null;
                        try {
                            pCur = contentResolver.query(
                                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                    null,
                                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                    new String[]{id}, null);
                        } catch (Exception e) {
                            e.printStackTrace();
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
                        if (pCur != null)
                            pCur.close();
                    }
                }
            }
            if (cur != null) {
                cur.close();
            }
        }
    }*/
}
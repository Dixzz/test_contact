package org.testing;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import org.testing.ContactUtil.ContactPojo;

import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.List;

public class ContactActivity extends AppCompatActivity {
    private Activity activity;

    public static final int REQUEST_CODE_ACCESS_CONTACTS = 101;
    /*private final Map<String, ContactPojo> mContactsByLookupKey = new HashMap<>();
    private final List<ContactPojo> list = new ArrayList<>();*/
    private ContentResolver cr;

    public static String CONTACTS_SORT = ContactsContract.Contacts.DISPLAY_NAME_PRIMARY + " ASC";

    public static final Uri CONTACTS_URI = ContactsContract.Contacts.CONTENT_URI;
    public static final String[] CONTACTS_PROJECTION = new String[]{
            ContactsContract.Data.LOOKUP_KEY,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,};

    public static final Uri CONTACT_DETAILS_URI = ContactsContract.Data.CONTENT_URI;
    public static final String[] CONTACT_DETAILS_PROJECTION = {
            ContactsContract.Data.MIMETYPE,
            ContactsContract.Contacts._ID,
            ContactsContract.Data.LOOKUP_KEY,
            ContactsContract.CommonDataKinds.Identity.CONTACT_LAST_UPDATED_TIMESTAMP,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.TYPE,
            ContactsContract.CommonDataKinds.Email.ADDRESS,
    };

    public static final int CONTACTS_LOADER_ID = 0;
    public static final int CONTACT_DETAILS_LOADER_ID = 1;
    Handler handler = new Handler(Looper.myLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            Log.d("AAAE", "" + msg);
        }
    };
    private WeakReference<StringViewModel> model;
    private MyService service1;
    private ServiceConnection mConnection = new ServiceConnection() {
        // Called when the connection with the service is established
        public void onServiceConnected(ComponentName className, IBinder service) {
            //Log.e("AAA", className+" "+service);
            service1 = ((MyService.LocalBinder) service).getService();
            service1.componentToBinder.fetchContacts();
            service1.setBinderToComponent(new MyService.binderToComponent() {
                @Override
                public void callback(Object... o) {
                    //Log.d("AAA", "" + Arrays.toString(o));
                }
            });
            Log.d("AAA", "onServiceConnected: "+service1.model);

            model = new WeakReference<>(service1.model);
            //List<ContactPojo> list2 = new ArrayList<>();
            Log.d("AAA", "onServiceConnected: " + model.get().getUsers2().getValue());
            if (model.get() != null) {
                //list.clear();
                //Log.d("AAA", "onServiceConnected: "+model.getUsers2().getValue());
                ((RecyclerView) findViewById(R.id.listView)).setAdapter(new adapter(model.get().getUsers2().getValue()));
                //list.clear();
                //model.getUsers2().observeForever(s -> {

                //});
            }
        }

        // Called when the connection with the service disconnects unexpectedly
        // Unbind gets called after service completes or destroyed
        public void onServiceDisconnected(ComponentName className) {
            model.clear();
            service1 = null;
            handler = null;
        }
    };
    /*private final BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("AAA", "onReceive: " + intent.getStringExtra("key"));
            //Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    };*/

    Long currentTime = 0L;
    ContentObserver contentObserver = new ContentObserver(null) {
        @Override
        public boolean deliverSelfNotifications() {
            currentTime = System.currentTimeMillis();
            return true;
        }

        @Override
        public void onChange(boolean selfChange) {
            new Thread() {
                @Override
                public void run() {
                    checkContactAddedTime(new Date(currentTime));
                }
            }.start();
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        currentTime = System.currentTimeMillis();
        Log.d("AAA", "onStart: " + currentTime);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(mConnection);
    }

    static class adapter extends commonAdapter {
        List<ContactPojo> list;

        private static final String TAG = "adapter";

        public adapter(List<ContactPojo> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.holder_test, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            super.onBindViewHolder(holder, position);
            TextView textView = holder.itemView.findViewById(R.id.textView);
            TextView textView2 = holder.itemView.findViewById(R.id.textView2);
            TextView textView3 = holder.itemView.findViewById(R.id.textView3);
            TextView textView4 = holder.itemView.findViewById(R.id.textView4);
            textView.setText(list.get(position)._ID + "");
            textView2.setText(list.get(position).name);
            textView3.setText(list.get(position).email);
            textView4.setText(list.get(position).phoneNumbers.toString());
        }

        @Override
        public int getItemCount() {
            if (list == null || list.size() == 0)
                return 0;
            else return list.size();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        activity = this;
        intent =  new Intent(activity, MyService.class);
        cr = getContentResolver();
        findViewById(R.id.top).setOnClickListener(view -> {
            startActivity(new Intent(this, MainActivity.class));
        });
        getContentResolver().registerContentObserver(CONTACTS_URI, true, contentObserver);
        loadContactsIfPerm();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //stopService(intent);
        unbindService(mConnection);
        mConnection = null;
        service1 = null;

        if (contentObserver != null) {
            getContentResolver().unregisterContentObserver(contentObserver);
            contentObserver = null;
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                Log.i("Service status", "Running");
                return true;
            }
        }
        Log.i("Service status", "Not running");
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_ACCESS_CONTACTS && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            loadContactsIfPerm();
        else {
            showSnack("Declined permissions");
        }
    }

    private void showSnack(String msg) {
        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), msg, BaseTransientBottomBar.LENGTH_LONG).setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE);
        snackbar.getView().setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == DragEvent.ACTION_DRAG_STARTED) {
                snackbar.dismiss();
            }
            view.performClick();
            return true;
        });
        snackbar.show();
    }
    private Intent intent;

    private void loadContactsIfPerm() {
        if (ContextCompat.checkSelfPermission(activity
                , Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity
                    , new String[]{Manifest.permission.READ_CONTACTS, Manifest.permission.MANAGE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_ACCESS_CONTACTS);
        } else {
            if (!isMyServiceRunning(MyService.class)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent);
                } else {
                    startService(intent);
                }
            }
            /*new ContactUtil()
                    .addLoader(new CursorLoader(this, CONTACTS_URI, CONTACTS_PROJECTION,
                            null, null, CONTACTS_SORT), CONTACTS_LOADER_ID, this)
                    .addLoader(new CursorLoader(this, CONTACT_DETAILS_URI, CONTACT_DETAILS_PROJECTION,
                            null, null, CONTACTS_SORT), CONTACT_DETAILS_LOADER_ID, this);*/
        }
    }

    /*@NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        Log.d("AAAO", "" + id);
        switch (id) {
            case CONTACTS_LOADER_ID:
                return new CursorLoader(this, CONTACTS_URI, CONTACTS_PROJECTION,
                        null, null, CONTACTS_SORT);
            case CONTACT_DETAILS_LOADER_ID:
                return new CursorLoader(this, CONTACT_DETAILS_URI, CONTACT_DETAILS_PROJECTION,
                        null, null, CONTACTS_SORT);
        }
        return null;
    }*/

    // We can get here too contact name but not the phone number, stored at diff resolver
    /*private void readContacts(Cursor cursor) {
        mContactsByLookupKey.clear();

        while (!cursor.isClosed() && cursor.moveToNext()) {
            ContactPojo t = new ContactPojo();
            //String displayName = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY));
            String lookupKey = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
            mContactsByLookupKey.put(lookupKey, t);
            //Log.d("AAA", "" + displayName);
            if (cursor.isLast()) {
                cursor.close();
            }
        }
    }

    private ContactPojo readContactDetails(Cursor cursor, ContactPojo contact) {
        String mime = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.MIMETYPE));
        //Log.d("AAA", "readContactDetails() returned: " + mime);
        if (mime.equals(ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)) {
            String email = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS));
            if (email != null)
                contact.setEmail(email);

        } else if (mime.equals(ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)) {
            String name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME));
            if (name != null) contact.setName(name);
        } else if (mime.equals(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)) {
            String phone = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            String type = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
            contact.setPhoneNumber(phone);
            return contact;
        }
        return null;
    }

    private void readContactDetails(Cursor cursor) throws JSONException, JsonProcessingException {

        if (cursor != null && cursor.moveToFirst()) {
            int i = 0;
            cursor.moveToPrevious();
            Log.d("AAA", "readContactDetails() returned: " + cursor.getCount());

            while (cursor.moveToNext()) {
                String lookupKey = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.LOOKUP_KEY));
                ContactPojo t = mContactsByLookupKey.get(lookupKey);
                if (t != null) {
                    list.add(i, t);
                    list.set(i, readContactDetails(cursor, list.get(i)));
                    i += 1;
                } else {
                    mContactsByLookupKey.remove(lookupKey);
                }
                if (cursor.isAfterLast()) {
                    cursor.close();
                }
            }
        }
        JSONArray arrayFinal = new JSONArray();

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        ContactPojo t2 = null;

        //Need better handling
        for (int i = 0; i < list.size(); i++) {
            ContactPojo t = list.get(i);
            if (i < list.size() - 1)
                t2 = list.get(i + 1);

            if (t != null && t2 != null) {
                if (t.phoneNumbers.equals(t2.phoneNumbers)) {
                    list.remove(i);
                }
            }
        }
        for (ContactPojo test : list) {
            if (test != null) {
                JSONObject jsonObject = new JSONObject(mapper.writeValueAsString(test));
                arrayFinal.put(jsonObject);
            }
        }

        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + getPackageName();
        boolean saved = false;
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File(path, "contacts.txt");
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(arrayFinal.toString().getBytes());
            fileOutputStream.close();
            showSnack("Saved JSON to " + file.getPath());
            saved = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d("AAA", "" + path);
        if (saved)
            showSnack("Found " + arrayFinal.length() + " contacts" + " and JSON saved to" + file.getPath());
        else
            showSnack("Found " + arrayFinal.length() + " contacts");
        //Log.d("AAA", "readContactDetails: " + arrayFinal);
    }*/


    // 2nd approach for getting contact details using accessing 2 tables from phone DB
    private void checkContactAddedTime(Date time) {
        cr = activity.getContentResolver();

        //Cursor cur = cr.query(CONTACTS_URI, null, ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP + " >= " + (System.currentTimeMillis() - 20000) + " AND " + System.currentTimeMillis() + " > " + ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP, null, ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP + " DESC");
        Cursor cur = cr.query(CONTACTS_URI, null, null, null, ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP + " DESC");

        if ((cur != null ? cur.getCount() : 0) > 0) {
            cur.moveToFirst();

            Date contactUpdatedAt = new Date(Long.parseLong(cur.getString(cur.getColumnIndex(ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP))));
            //Log.d("AAAB", time + " :: " + contactUpdatedAt + " :: "+new Date(System.currentTimeMillis()));
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

    private void getContactList() {

        // Move to separate thread
        ContentResolver cr = activity.getContentResolver();
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI,
                null, null, null, null);
        if ((cur != null ? cur.getCount() : 0) > 0) {
            /*while (cur != null && cur.moveToNext()) {
                String id = cur.getString(
                        cur.getColumnIndex(ContactsContract.Contacts._ID));
                String name = cur.getString(cur.getColumnIndex(
                        ContactsContract.Contacts.DISPLAY_NAME));

                if (cur.getInt(cur.getColumnIndex(
                        ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {
                    Cursor pCur = cr.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            new String[]{id}, null);

                    while (pCur.moveToNext()) {
                        String phoneNo = pCur.getString(pCur.getColumnIndex(
                                ContactsContract.CommonDataKinds.Phone.NUMBER));
                        *//*Log.i("Contact", "Name: " + name);
                        Log.i("Contact", "Phone Number: " + phoneNo);
                        //Log.i("Contact", "Phone Number: " +pCur.getString(pCur.getColumnIndex( ContactsContract.CommonDataKinds.Phone.CONTACT_ID)));
                        Log.i("Contact", "Phone Number: " +cur.getString(
                                cur.getColumnIndex(ContactsContract.Contacts._ID)));*//*
                    }
                    pCur.close();
                }
            }*/
            Log.d("AAA", "" + cur.getCount());
            /*while (cur.moveToNext()) {
                Log.d("AAA", "" + cur.getString(
                        cur.getColumnIndex(ContactsContract.Contacts._ID)));
            }*/

        } else {
            showSnack("No contacts");
        }
        if (cur != null) {
            cur.close();
        }
    }
}
package org.testing;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;

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

import static org.testing.ContactActivity.CONTACTS_LOADER_ID;
import static org.testing.ContactActivity.CONTACTS_PROJECTION;
import static org.testing.ContactActivity.CONTACTS_SORT;
import static org.testing.ContactActivity.CONTACTS_URI;
import static org.testing.ContactActivity.CONTACT_DETAILS_LOADER_ID;
import static org.testing.ContactActivity.CONTACT_DETAILS_PROJECTION;
import static org.testing.ContactActivity.CONTACT_DETAILS_URI;

public class MainActivity extends AppCompatActivity implements ContactUtil.loaderLoad {
    private Map<String, ContactUtil.ContactPojo> mContactsByLookupKey = new HashMap<>();
    private ArrayList<ContactUtil.ContactPojo> list = new ArrayList<>();
    private boolean isBound = false;

    private static final String TAG = "MainActivity";

    Activity activity;
    private ServiceConnection mConnection = new ServiceConnection() {
        // Called when the connection with the service is established
        public void onServiceConnected(ComponentName className, IBinder service) {
            isBound = true;
            Log.e("AAA", " " + service);
            Messenger messenger = new Messenger(service);
            Messenger m2 = new Messenger(handler);
            Log.d(TAG, "onServiceConnected: " + m2);
            Message m = Message.obtain();
            if (messenger != null) {
                try {
                    m.replyTo = m2;
                    messenger.send(m);
                    Log.d(TAG, "onServiceConnected: ");
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            //service1 = ((MyService.LocalBinder) service).getService();

            /*if (service1.model != null) {
                //list.clear();

                Log.d("AAA", "onServiceConnected: " + service1.model.getUsers2());
                //list.clear();
                //model.getUsers2().observeForever(s -> {

                //});
            }
            service1.setBinderToComponent(new MyService.binderToComponent() {
                @Override
                public void callback(Object... o) {
                    //Log.d("AAA", "" + Arrays.toString(o));
                }
            });

            service1.componentToBinder.fetchContacts();*/
            //List<ContactPojo> list2 = new ArrayList<>();
            //Log.d("AAA", "onServiceConnected: " + service1.model);
        }

        // Called when the connection with the service disconnects unexpectedly
        // Unbind gets called after service completes or destroyed
        public void onServiceDisconnected(ComponentName className) {
            Log.e(TAG, "onServiceDisconnected: ");
//            service1 = null;
        }
    };

    @Override
    protected void onDestroy() {
        Log.e(TAG, "onDestroy: ");
        unbindService(mConnection);
        //stopService(intent);
        intent = null;
//        service1 = null;
        super.onDestroy();
    }

    Handler handler = new Handler(Looper.myLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            Bundle bundle = msg.getData();
            if (bundle.getSerializable(MyService2.KEY_MODEL_LIST) != null) {
                ArrayList<ContactUtil.ContactPojo> p = ((ArrayList<ContactUtil.ContactPojo>) msg.getData().getSerializable(MyService2.KEY_MODEL_LIST));
                Log.d(TAG, "handleMessage: " + p.get(p.size() - 1)._ID);
                adapter.list = p;
                adapter.notifyItemInserted(adapter.getItemCount());
            }
            Log.d("AAAE", "" + msg);
        }
    };
    Intent intent;

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

                temp = null;
                //arrayFinal = null;
            }
        }.start();
    }

    static class adapter extends commonAdapter {
        public List<ContactUtil.ContactPojo> list;

        @Override
        public int getItemViewType(int position) {
            return list.get(position)._ID;
        }


        @Override
        public long getItemId(int position) {
            return list.get(position)._ID;
        }

        private static final String TAG = "adapter";

        public adapter(List<ContactUtil.ContactPojo> list) {
            this.list = list;
        }

        public adapter() {
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

    RecyclerView recyclerView;
    MainActivity.adapter adapter;

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive: " + intent);
            if (intent.hasExtra(MyService2.KEY_FULL_MODEL_LIST)) {
                Log.d(TAG, "onReceive: " + intent.getExtras().getString(MyService2.KEY_FULL_MODEL_LIST));


                //Log.d(TAG, "onReceive: "+s);
                adapter.list = ContactUtil.getListFromString(intent.getExtras().getString(MyService2.KEY_FULL_MODEL_LIST));
                adapter.notifyDataSetChanged();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        activity = this;

        new ContactUtil().addLoader(new CursorLoader(this, CONTACTS_URI, CONTACTS_PROJECTION,
                null, null, CONTACTS_SORT), CONTACTS_LOADER_ID, this)
                .addLoader(new CursorLoader(this, CONTACT_DETAILS_URI, CONTACT_DETAILS_PROJECTION,
                        null, null, CONTACTS_SORT), CONTACT_DETAILS_LOADER_ID, this);

        adapter = new adapter();
        adapter.setHasStableIds(true);
        recyclerView = ((RecyclerView) findViewById(R.id.rec));
        recyclerView.setAdapter(adapter);
        /*StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .build());
        StrictMode.enableDefaults();*/
        registerReceiver(receiver, new IntentFilter("someshit"));
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, new IntentFilter("someshit"));

        intent = new Intent(this, MyService2.class);
        Log.e(TAG, "onCreate: " + isBound);

        if (!isBound)
            bindService(intent, mConnection, BIND_AUTO_CREATE);

        if (!isMyServiceRunning(MyService.class)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
        }
        findViewById(R.id.button).setOnClickListener(view -> {
            Log.d("MainAc", "onCreate: " + isMyServiceRunning(MyService2.class));
        });
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
}
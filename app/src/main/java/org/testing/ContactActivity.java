package org.testing;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContactActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    private Activity activity;

    public static final int REQUEST_CODE_ACCESS_CONTACTS = 101;
    private final Map<String, Test> mContactsByLookupKey = new HashMap<>();
    private List<Test> list = new ArrayList<>();

    private static final String CONTACTS_SORT = ContactsContract.Contacts.DISPLAY_NAME_PRIMARY + " ASC";

    private static final int CONTACTS_LOADER_ID = 0;
    private static final Uri CONTACTS_URI = ContactsContract.Contacts.CONTENT_URI;
    private static final String[] CONTACTS_PROJECTION = new String[]{
            ContactsContract.Data.LOOKUP_KEY,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,};


    private static final int CONTACT_DETAILS_LOADER_ID = 1;
    private static final Uri CONTACT_DETAILS_URI = ContactsContract.Data.CONTENT_URI;
    private static final String[] CONTACT_DETAILS_PROJECTION = {
            ContactsContract.Data.MIMETYPE,
            ContactsContract.Data.LOOKUP_KEY,  //use this for filtering
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Email.ADDRESS,
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = this;
        loadContactsIfPerm();
        //getContactList();
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
        Snackbar.make(findViewById(android.R.id.content), msg, BaseTransientBottomBar.LENGTH_LONG).setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE).show();
    }

    private void loadContactsIfPerm() {
        if (ContextCompat.checkSelfPermission(activity
                , Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity
                    , new String[]{Manifest.permission.READ_CONTACTS, Manifest.permission.MANAGE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_ACCESS_CONTACTS);
        } else {
            LoaderManager.getInstance(this).initLoader(CONTACTS_LOADER_ID, null, this);
            LoaderManager.getInstance(this).initLoader(CONTACT_DETAILS_LOADER_ID, null, this);
        }
    }

    @NonNull
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
    }

    static class Test {
        public String name = "";
        public String email = "";
        public List<String> phoneNumbers = new ArrayList<>();

        public void setPhoneNumber(String phoneNumber) {
            phoneNumbers.add(phoneNumber);
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

    // We can get here too contact name but not the phone number, stored at diff resolver
    private void readContacts(Cursor cursor) {
        mContactsByLookupKey.clear();

        while (cursor.moveToNext()) {
            Test t = new Test();
            //String displayName = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY));
            String lookupKey = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
            mContactsByLookupKey.put(lookupKey, t);
            //Log.d("AAA", "" + displayName);
        }
    }

    private Test readContactDetails(Cursor cursor, Test contact) {
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
                Test t = mContactsByLookupKey.get(lookupKey);
                if (t != null) {
                    list.add(i, t);
                    list.set(i, readContactDetails(cursor, list.get(i)));
                    i += 1;
                }
            }
        }
        JSONArray arrayFinal = new JSONArray();

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        Test t2 = null;

        //Need better handling
        for (int i = 0; i < list.size(); i++) {
            Test t = list.get(i);
            if (i < list.size() - 1)
                t2 = list.get(i + 1);

            if (t != null && t2 != null) {
                if (t.phoneNumbers.equals(t2.phoneNumbers)) {
                    list.remove(i);
                }
            }
        }
        for (Test test : list) {
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
    }


    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
        Log.d("AAA", "Loader ID " + loader.getId());
        switch (loader.getId()) {
            case CONTACTS_LOADER_ID:
                readContacts(cursor);
                break;

            case CONTACT_DETAILS_LOADER_ID:
                try {
                    readContactDetails(cursor);
                } catch (JSONException | JsonProcessingException e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {

    }

    // 2nd approach for getting contact details using accessing 2 tables from phone DB
    private void getContactList() {

        // Move to separate thread
        ContentResolver cr = activity.getContentResolver();
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI,
                null, null, null, null);
        if ((cur != null ? cur.getCount() : 0) > 0) {
            while (cur != null && cur.moveToNext()) {
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
                        Log.i("Contact", "Name: " + name);
                        Log.i("Contact", "Phone Number: " + phoneNo);
                    }
                    pCur.close();
                }
            }
        } else {
            showSnack("No contacts");
        }
        if (cur != null) {
            cur.close();
        }
    }
}
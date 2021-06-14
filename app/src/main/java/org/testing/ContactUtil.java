package org.testing;

import android.database.Cursor;

import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ContactUtil {
    public ContactUtil addLoader(CursorLoader cursorLoader, Integer id, loaderLoad loaderLoad) {
        cursorLoader.registerListener(id, loaderLoad::callbackLoad);
        cursorLoader.startLoading();
        return this;
    }

    public interface loaderLoad {
        void callbackLoad(Loader<Cursor> loader, Cursor cursor);
    }

    public static class ContactPojo implements Serializable {

        public ContactPojo() {

        }

        public Integer _ID = 0;
        public String name = "";
        public String email = "";
        public List<String> phoneNumbers = new ArrayList<>();

        public void set_ID(Integer _ID) {
            this._ID = _ID;
        }

        public void setPhoneNumber(String phone) {
            phoneNumbers.add(phone);
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

    public static ArrayList<ContactPojo> getListFromString(String resource) {
        ObjectMapper objectMapper = new ObjectMapper();
        ArrayList<ContactPojo> list = null;
        if (resource.length() > 0) {
            try {
                list = objectMapper.readValue(resource, objectMapper.getTypeFactory().constructCollectionType(List.class, ContactPojo.class));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return list;
    }
}
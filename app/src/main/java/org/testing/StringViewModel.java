package org.testing;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

public class StringViewModel extends ViewModel {

    private MutableLiveData<ArrayList<ContactUtil.ContactPojo>> users2 = new MutableLiveData<>();
    private ArrayList<ContactUtil.ContactPojo> list = new ArrayList<>();

    public StringViewModel() {
        list.clear();
    }

    public LiveData<ArrayList<ContactUtil.ContactPojo>> getUsers2() {
        return users2;
    }

    // Use for background thread
    public void insertData2(ContactUtil.ContactPojo s) {
        list.add(s);
        users2.postValue(list);
    }

    public void insertDataAll(List<ContactUtil.ContactPojo> s) {
        list.clear();
        list.addAll(s);
        users2.postValue(list);
    }

    public void updateData3(ContactUtil.ContactPojo old, ContactUtil.ContactPojo newData) {
        list.set(list.indexOf(old), newData);
        users2.postValue(list);
    }

    public void insertData3(ContactUtil.ContactPojo s) {
        list.add(s);
        users2.setValue(list);
    }

    public void deleteData(ContactUtil.ContactPojo o) {
        list.remove(o);
        users2.postValue(list);
    }

}

package org.testing;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class StringViewModel extends ViewModel {

    private final MutableLiveData<String> users = new MutableLiveData<>();

    public LiveData<String> getUsers() {
        return users;
    }

    public void insertData(String s) {
        users.postValue(s);
    }
}

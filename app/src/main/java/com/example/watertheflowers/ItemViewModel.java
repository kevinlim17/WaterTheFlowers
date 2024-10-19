package com.example.watertheflowers;

import android.content.ClipData;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ItemViewModel extends ViewModel {
    private final MutableLiveData<Boolean> selectedItem = new MutableLiveData<Boolean>();
    public void selectItem(Boolean item){
        selectedItem.setValue(item);
    }
    public LiveData<Boolean> getSelectedItem() {
        return selectedItem;
    }
}

package com.example.watertheflowers;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link Fragment_Watering #newInstance} factory method to
 * create an instance of this fragment.
 */
public class Fragment_Watering extends Fragment {
    EditText scan_humidity;
    EditText print_humidity;
    EditText scan_amount;
    EditText print_amount;

    private ItemViewModel viewModel;
    Boolean DefaultMode;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment__watering,container,false);
        scan_humidity = rootView.findViewById(R.id.scan_humidity);
        print_humidity = rootView.findViewById(R.id.print_humidity);
        scan_amount = rootView.findViewById(R.id.scan_amount);
        print_amount = rootView.findViewById(R.id.print_amount);
        DefaultMode = false;

        super.onCreate(savedInstanceState);
        getParentFragmentManager().setFragmentResultListener("key", this, (requestKey, result) -> {
            String amount_value = result.getString("recommended_amount");
            String humidity_value = result.getString("recommended_humidity");
            String recommended_amount = "추천 급수량:                                  " + amount_value + " mL";
            String recommended_humidity = "추천 습도:                                        "+ humidity_value+" %";
            print_amount.setText(recommended_amount);
            print_humidity.setText(recommended_humidity);
        });
        return rootView;
    }
}


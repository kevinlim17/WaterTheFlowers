package com.example.watertheflowers;

import android.content.Context;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatAutoCompleteTextView;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link Fragment_SelectPlant #newInstance} factory method to
 * create an instance of this fragment.
 */
public class Fragment_SelectPlant extends Fragment {


    //ListView 작업할 부분
    List<String> list;          // 데이터를 넣은 리스트변수
    ListView listView;          // 검색을 보여줄 리스트변수
    EditText editSearch;        // 검색어를 입력할 Input 창
    SearchAdapter adapter;      // 리스트뷰에 연결할 아답터
    ArrayList<String> arraylist;
    PlantsData[] plantsDataList = {
            new PlantsData("선인장",30,20,50),
            new PlantsData("제로니카",25,40,30),
            new PlantsData("극락",20,30,20),
            new PlantsData("산세베리",30,20,30),
            new PlantsData("떡갈고무나무",20,30,40),
            new PlantsData("녹보",14,40,50)
    };
    EditText selected_plant;
    Button save_plant;
    EditText print_period;
    EditText scan_watering_period;

    private ItemViewModel viewModel;
    Boolean DefaultMode;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment__select_plant, container, false);

        editSearch = rootView.findViewById(R.id.search_your_plant);
        listView = rootView.findViewById(R.id.searchlist);
        selected_plant = rootView.findViewById(R.id.selected_plant);
        save_plant = rootView.findViewById(R.id.save_plant);
        print_period = rootView.findViewById(R.id.print_watering_period);
        scan_watering_period = rootView.findViewById(R.id.scan_watering_period);
        DefaultMode = false;

        // 리스트를 생성한다.
        list = new ArrayList<String>();

        // 검색에 사용할 데이터을 미리 저장한다.
        settingList();

        // 리스트의 모든 데이터를 arraylist에 복사한다.// list 복사본을 만든다.
        arraylist = new ArrayList<String>();
        arraylist.addAll(list);

        // 리스트에 연동될 아답터를 생성한다.
        adapter = new SearchAdapter(list, (Context) getActivity());

        // 리스트뷰에 아답터를 연결한다.
        listView.setAdapter(adapter);

        // input창에 검색어를 입력시 "addTextChangedListener" 이벤트 리스너를 정의한다.
        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                // input창에 문자를 입력할때마다 호출된다.
                // search 메소드를 호출한다.
                String text = editSearch.getText().toString();
                search(text);
            }
        });


        save_plant.setOnClickListener(v -> {
            viewModel = new ViewModelProvider(requireActivity()).get(ItemViewModel.class);
            viewModel.getSelectedItem().observe(requireActivity(), item ->{
                DefaultMode = item;
            });

            String selected_name = editSearch.getText().toString();
            int position = is_this_in_database(selected_name);

            if(position < plantsDataList.length) {
                selected_plant.setText(selected_name);
                String period = "추천 주기:       " + plantsDataList[position].getRecommended_watering_period() + "분";
                print_period.setText(period);

                if(DefaultMode){
                    scan_watering_period.setText(plantsDataList[position].getRecommended_watering_period());
                }

                Bundle bundle = new Bundle();
                bundle.putString("recommended_humidity", String.valueOf(plantsDataList[position].getRecommended_humidity()));
                bundle.putString("recommended_amount", String.valueOf(plantsDataList[position].getRecommended_amount()));
                getParentFragmentManager().setFragmentResult("key", bundle);
            }
            else
                Toast.makeText(requireActivity().getApplicationContext(), "DataBase에 존재하지 않는 식물입니다", Toast.LENGTH_SHORT ).show();
        });



        // Inflate the layout for this fragment
        return rootView;


    }
    // 검색을 수행하는 메소드
    public void search(String charText) {

        // 문자 입력시마다 리스트를 지우고 새로 뿌려준다.
        list.clear();

        // 문자 입력이 없을때는 모든 데이터를 보여준다.
        if (charText.length() == 0) {
            list.addAll(arraylist);
        }
        // 문자 입력을 할때..
        else {
            // 리스트의 모든 데이터를 검색한다.
            for (int i = 0; i < arraylist.size(); i++) {
                // arraylist의 모든 데이터에 입력받은 단어(charText)가 포함되어 있으면 true를 반환한다.
                if (arraylist.get(i).toLowerCase().contains(charText)) {
                    // 검색된 데이터를 리스트에 추가한다.
                    list.add(arraylist.get(i));
                }
            }
        }
        // 리스트 데이터가 변경되었으므로 아답터를 갱신하여 검색된 데이터를 화면에 보여준다.
        adapter.notifyDataSetChanged();
    }
    // 검색에 사용될 데이터를 리스트에 추가한다.
    private void settingList() {
        for (PlantsData plantsData : plantsDataList) {
            list.add(plantsData.getPlants_name());
        }
    }

    //database에 특정 식물이 존재하는지를 plantsDataList의 index를 반환함으로써 나타냄. 발견되지 않은 경우 plantsDataList의 element 수를 가져옴.
    private int is_this_in_database(String str){
        for(int i = 0; i < plantsDataList.length; i++){
            if(str.equals(plantsDataList[i].getPlants_name()))
                return i;
        }
        return plantsDataList.length;
    }
}

class PlantsData {
    private final String plants_name;
    private final int recommended_watering_period;
    private final int recommended_humidity;
    private final int recommended_amount;

    public PlantsData(String name, int period, int humidity, int amount){
        plants_name = name;
        recommended_watering_period = period;
        recommended_humidity = humidity;
        recommended_amount = amount;
    }
    public String getPlants_name() {
        return plants_name;
    }
    public int getRecommended_watering_period() {
        return recommended_watering_period;
    }
    public int getRecommended_humidity() {
        return recommended_humidity;
    }
    public int getRecommended_amount() {
        return recommended_amount;
    }
}
package com.example.watertheflowers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private FragmentManager fragmentManager;
    private Fragment fa,fb;
    private ItemViewModel viewModel;
    BottomNavigationView bottomNavigationView;
    ImageView bluetoothButton;
    ImageView setDefaultButton;
    Toolbar toolbar;
    Boolean DefaultMode;

    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    BluetoothDevice bluetoothDevice;

    static final int REQUEST_ENABLE_BT = 100;
    Set<BluetoothDevice> pairedDevices;
    boolean paired = false;

    String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION};
    static final int REQUEST_PERMISSIONS = 101;

    Set<BluetoothDevice> unpairedDevices = new HashSet<>();
    List<String> unpairedList = new ArrayList<>();
    ArrayAdapter<String> adapter;

    BluetoothSocket bluetoothSocket;
    OutputStream outputStream;
    InputStream inputStream;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bluetoothButton = findViewById(R.id.iv_bluetooth);
        setDefaultButton = findViewById(R.id.set_default);
        toolbar = findViewById(R.id.toolbar);
        DefaultMode = false;

        checkPermissions(permissions);
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);


        bottomNavigationView = findViewById(R.id.bottomNavi); // bottomNavigationView 인스턴스 생성
        fragmentManager = getSupportFragmentManager(); // FragmentManager 인스턴스 생성

        //처음화면
        fa = new Fragment_SelectPlant();
        fragmentManager.beginTransaction().add(R.id.main_frame,fa).commit();



        //BottomNavigation 안의 아이템 선택을 통한 Fragment 간 전환 구현
        bottomNavigationView.setOnItemSelectedListener((BottomNavigationView.OnItemSelectedListener) menuItem -> {
            if(menuItem.getItemId() == R.id.select_plants ) { //menuItem엔 각각의 Fragment를 지칭하는 Id를 가진 요소가 있음.
                if (fa == null) {
                    fa = new Fragment_SelectPlant();
                    fragmentManager.beginTransaction().add(R.id.main_frame, fa).commit();
                }
                if (fa != null)
                    fragmentManager.beginTransaction().show(fa).commit();
                if (fb != null)
                    fragmentManager.beginTransaction().hide(fb).commit();
            }
            else if(menuItem.getItemId() == R.id.watering){
                if(fb == null) {
                    fb = new Fragment_Watering();
                    fragmentManager.beginTransaction().add(R.id.main_frame, fb).commit();
                }
                if(fb != null)
                    fragmentManager.beginTransaction().show(fb).commit();
                if(fa != null)
                    fragmentManager.beginTransaction().hide(fa).commit();
            }
            return true;
        });





        setSupportActionBar(toolbar);
        bluetoothButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkBluetooth();
            }
        });

        viewModel = new ViewModelProvider(this).get(ItemViewModel.class);
        setDefaultButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "기본값 세팅", Toast.LENGTH_SHORT).show();
                if(!DefaultMode){
                    viewModel.selectItem(true);
                    DefaultMode = true;
                }
                else {
                    viewModel.selectItem(false);
                    DefaultMode = false;
                }
            }
        });
    }

    private void checkBluetooth() {
        if(bluetoothAdapter == null){
            Toast.makeText(getApplicationContext(), "해당 기기는 블루투스를 지원하지 않습니다.", Toast.LENGTH_SHORT).show();
            finish();
        }
        else{
            if(bluetoothAdapter.isEnabled()) {
                selectPairedDevice();
            } else {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
    }

    private void selectPairedDevice() {
        pairedDevices = bluetoothAdapter.getBondedDevices();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("장치 선택");

        List<String> pairedList = new ArrayList<>();
        for(BluetoothDevice device : pairedDevices) {
            pairedList.add(device.getName());
        }
        pairedList.add("취소");

        final CharSequence[] devices = pairedList.toArray(new CharSequence[pairedList.size()]);
        builder.setItems(devices, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == pairedList.size()-1) {
                    selectUnpairedDevice();
                } else {
                    bluetoothAdapter.cancelDiscovery();
                    paired = true;
                    connectDevice(devices[which].toString(), paired);
                }
            }
        });
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void checkPermissions(String[] permissions) {
        ArrayList<String> requestList = new ArrayList<>();

        for (String curPermission : permissions) {
            int permissionCheck = ContextCompat.checkSelfPermission(this, curPermission);
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, curPermission)) {
                    Toast.makeText(getApplicationContext(), curPermission + " 권한 설명 필요", Toast.LENGTH_SHORT).show();
                } else {
                    requestList.add(curPermission);
                }
            }
        }
        if(requestList.size()>0) {
            String[] requests = requestList.toArray(new String[requestList.size()]);
            ActivityCompat.requestPermissions(this, requests, REQUEST_PERMISSIONS);
        }
    }

    private void selectUnpairedDevice() {
        if(bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        bluetoothAdapter.startDiscovery();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("기기 검색");

        adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, unpairedList);

        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                bluetoothAdapter.cancelDiscovery();
                String name = adapter.getItem(which);
                unpairedList.remove(name);
                connectDevice(name, paired);
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                selectPairedDevice();
            }
            else if(resultCode == RESULT_CANCELED){
                finish();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.length > 0) {
                for (int i = 0; i < grantResults.length; i++) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, (i+1)+"번째 권한 승인", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, (i+1)+"번째 권한 거부", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void add(BluetoothDevice device){
        if(!(pairedDevices.contains(device))) {
            if(unpairedDevices.add(device)) {
                unpairedList.add(device.getName());
            }
        }
        adapter.notifyDataSetChanged();
        Toast.makeText(this, device.getName()+" 검색", Toast.LENGTH_SHORT).show();
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(device.getBondState()!= BluetoothDevice.BOND_BONDED){
                    if(device.getName() == null)
                        add(device);
                }
            }
        }
    };

    // 블루투스 페어링된 목록에서 디바이스 기기 가져오기
    private BluetoothDevice getPairedDevice(String name) {
        BluetoothDevice selectedDevice = null;

        for(BluetoothDevice device : pairedDevices) {
            if(name.equals(device.getName())) {
                selectedDevice = device;
                break;
            }
        }
        return selectedDevice;
    }
    // 페어링되지 않은 기기 목록에서 디바이스 기기 가져오기
    private BluetoothDevice getUnpairedDevice(String name) {
        BluetoothDevice selectedDevice = null;

        for(BluetoothDevice device : unpairedDevices) {
            if(name.equals(device.getName())) {
                selectedDevice = device;
                break;
            }
        }
        return selectedDevice;
    }

    private void connectDevice(String selectedDeviceName, boolean paired) {
        final Handler mHandler = new Handler() {	// 핸들러 객체 생성
            public void handleMessage(Message msg) {	// handleMessage() 메서드 재정의
                if(msg.what==1) {			// 받은 메시지가 1이라면
                    try{
                        // 입출력 스트림 객체 생성
                        outputStream = bluetoothSocket.getOutputStream();
                        inputStream = bluetoothSocket.getInputStream();
                    } catch(IOException e) {
                        e.printStackTrace();
                    }
                } else { // 연결 오류나면
                    Toast.makeText(getApplicationContext(), "연결 오류", Toast.LENGTH_SHORT).show();
                    try {
                        bluetoothSocket.close(); // 소켓 닫아주고 리소스 해제
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        // 별도 스레드 생성
        Thread thread = new Thread(new Runnable() {
            public void run() {
                if(paired)  // 페어링된 기기라면
                    bluetoothDevice = getPairedDevice(selectedDeviceName);
                else	// 페어링되지 않은 기기라면
                    bluetoothDevice = getUnpairedDevice(selectedDeviceName);
                // UUID 생성
                UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
                try {
                    bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid); // 소켓 생성
                    bluetoothSocket.connect(); 	  // 소켓 연결
                    mHandler.sendEmptyMessage(1); // 핸들러에 메시지 1 보내기
                } catch (IOException e) {
                    e.printStackTrace();
                    mHandler.sendEmptyMessage(-1); // 핸들러에 메시지 -1 보내기
                }
            }
        });
        thread.start(); // 별도 스레드 시작
    }

    @Override
    protected void onDestroy(){
        try{
            inputStream.close();
            outputStream.close();
            bluetoothSocket.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }
}
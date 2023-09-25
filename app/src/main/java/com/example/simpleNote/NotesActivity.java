package com.example.simpleNote;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.example.simpleNote.adapter.NoteAdapter;
import com.example.simpleNote.viewModel.NotesViewModel;

public class NotesActivity extends AppCompatActivity {

    private static final String TAG = "NotesActivity";

    private RecyclerView rvNotes;
    private NoteAdapter noteAdapter;
    private NotesViewModel viewModel;
    private EditText etKeyword;

    InputMethodManager inputMethodManager;

    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //让背景图和状态栏融合到一起（非借助Material库完成）
        View decorView = getWindow().getDecorView();//拿到当前Activity的DecorView
        //表示Activity 的布局会显示在状态栏上面
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
        //将状态栏设置成透明色
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        setContentView(R.layout.activity_notes);

        //申请权限
        applyPermission();

        //获取系统的输入法管理器
        /*输入法管理器负责管理输入法（如键盘）的显示、隐藏和交互*/
        inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

        etKeyword = findViewById(R.id.et_keyword);
        etKeyword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                listenKey();
            }
        });

        rvNotes = findViewById(R.id.rv_notes);
        noteAdapter = new NoteAdapter(this);
        viewModel = new ViewModelProvider(this).get(NotesViewModel.class);
        rvNotes.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        rvNotes.setAdapter(noteAdapter);
        viewModel.getNotes().observe(this, notes -> {
            noteAdapter.setNotes(notes);
            Log.i(TAG, "onCreate: " + notes);
        });
        findViewById(R.id.iv_edit).setOnClickListener(v -> startActivity(new Intent(NotesActivity.this, EditNoteActivity.class)));
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    private void applyPermission(){
        String[] permissions = {
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA};
        final int REQUEST_CODE = 1;

        //检查该权限是否已经获取
        for (String permission : permissions){
            // GRANTED---授权  DINIED---拒绝
            if (ContextCompat.checkSelfPermission(getApplicationContext(),permission) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this,permissions,REQUEST_CODE);
            }
        }
        /*if (!Environment.isExternalStorageManager()){
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", getPackageName(), null);
            intent.setData(uri);
            startActivity(intent);
        }*/
    }

    private String lastKey = "";
    private Thread listenKeyThread;

    private void listenKey(){
        if (listenKeyThread != null && listenKeyThread.isAlive()) return;
        listenKeyThread = new Thread(() -> {
           String key;
           do {
               try {
                   Thread.sleep(1000);
               }catch (InterruptedException e){
                   throw new RuntimeException(e);
               }
               key = etKeyword.getText().toString().trim();
               if (!key.equals(lastKey)){
                   noteAdapter.filter(key);
                   lastKey = key;
               }
           } while (inputMethodManager.isActive());
        });
        listenKeyThread.start();
    }
}
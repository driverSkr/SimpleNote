package com.example.simpleNote;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.simpleNote.database.AppDatabase;
import com.example.simpleNote.database.dao.NoteDao;
import com.example.simpleNote.database.entity.Note;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class EditNoteActivity extends AppCompatActivity {

    public final static String INTENT_NOTE_ID_KEY = "INTENT_NOTE_ID_KEY";
    private static final String TAG = "EditNoteActivity";

    private final static int UPDATE_TIME_WHAT = 0;
    private final static int REQUEST_IMAGE_OPEN = 2;

    private int color;
    private String imgUrl;

    private ImageView ivColors;
    private ImageView ivSave;
    private ImageView ivPhoto;
    private ImageView ivDelete;
    private ImageView ivNoteImg;

    private EditText etTitle;
    private EditText etContent;

    private LinearLayout layoutColors;
    private ConstraintLayout layoutImg;

    private Button btTitleSpan;

    private TextView tvTime;
    private Handler timerHandler;
    private NoteDao noteDao;
    private AppDatabase appDb;

    private long noteId = 0;

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
        setContentView(R.layout.activity_edit_note);

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        if (bundle != null)
            noteId = bundle.getLong(INTENT_NOTE_ID_KEY, 0);

        //getMainLooper() 方法返回的就是主线程的 Looper 对象，通过它，你可以创建 Handler 对象并在主线程中处理消息，确保在正确的线程上更新 UI
        timerHandler = new Handler(getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case UPDATE_TIME_WHAT:
                        long sysTime = System.currentTimeMillis();
                        CharSequence sysTimeStr = DateFormat.format(getResources().getString(R.string.time_format), sysTime);
                        tvTime.setText(sysTimeStr);
                        break;
                }
            }
        };

        initViewsBind();

        initColors();

        initPhoto();

        initTimer();

        initDB();

        ivSave.setOnClickListener(v -> save());

        if (noteId != 0) {
            loadNote();
        }
    }

    private void loadNote() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            Note note = noteDao.selectById(noteId);
            if (note != null) {
                etTitle.setText(note.title);
                etContent.setText(note.content);
                color = note.color;
                btTitleSpan.setBackgroundColor(color);
                layoutImg.setVisibility(View.VISIBLE);
                if (note.imgPath != null && !"".equals(note.imgPath)) {

                    imgUrl = note.imgPath;
                    try {
                        ivNoteImg.setVisibility(View.VISIBLE);
                        String s = URLDecoder.decode(note.imgPath, "UTF-8");
                        Bitmap bm = BitmapFactory.decodeFile(s);

                        timerHandler.post(() -> {
                            ivNoteImg.setImageBitmap(bm);
                        });
                    } catch (UnsupportedEncodingException e) {
                        throw new RuntimeException(e);
                    }

                }
            } else {
                noteId = 0;
            }
        });
    }

    private void initDB() {
        appDb = AppDatabase.getDatabase(getApplication());
        noteDao = appDb.getNoteDao();

    }

    private void initTimer() {
        //开启线程的目的是为了在后台执行一个循环，定期发送消息来更新界面上显示的时间，而不阻塞主线程
        new Thread(() -> {
            while (true) {
                try {
                    Message msg = new Message();
                    msg.what = UPDATE_TIME_WHAT;
                    timerHandler.sendMessage(msg);
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    private void initViewsBind() {
        ivColors = findViewById(R.id.iv_colors);
        ivSave = findViewById(R.id.iv_save);
        ivPhoto = findViewById(R.id.iv_open_photo);
        ivDelete = findViewById(R.id.iv_delete);
        ivNoteImg = findViewById(R.id.iv_note_img);
        etTitle = findViewById(R.id.et_title);
        etContent = findViewById(R.id.et_note_content);
        btTitleSpan = findViewById(R.id.bt_span_color);
        layoutColors = findViewById(R.id.layout_colors);
        layoutImg = findViewById(R.id.layout_img);
        tvTime = findViewById(R.id.tv_time);
    }

    private void initColors() {
        color = getResources().getColor(R.color.note_color_1);
        ivColors.setOnClickListener(view -> {
            if (layoutColors.getVisibility() == View.GONE) {
                layoutColors.setVisibility(View.VISIBLE);
            } else {
                layoutColors.setVisibility(View.GONE);
            }
        });

        View.OnClickListener colorClickListener = view -> {
            color = ((ColorDrawable) view.getBackground()).getColor();
            btTitleSpan.setBackgroundColor(color);
            layoutColors.setVisibility(View.GONE);
            Log.i(TAG, "initColors: color1" + color);
        };
        for (int i = 0; i < layoutColors.getChildCount(); i++) {

            ((ViewGroup) layoutColors.getChildAt(i)).getChildAt(0).setOnClickListener(colorClickListener);
        }
    }

    private void initPhoto() {
        ActivityResultLauncher launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                Uri uri = result.getData().getData();

                ivNoteImg.setImageURI(uri);
                layoutImg.setVisibility(View.VISIBLE);
                imgUrl = getPathFromUri(uri);
                Log.i(TAG, "onActivityResult: " + imgUrl + "  /  " + uri.getPath());
            }
        });
        ivPhoto.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_PICK, null);
            intent.setType("image/*");
            launcher.launch(intent);
        });
        ivDelete.setOnClickListener(view -> {
            layoutImg.setVisibility(View.GONE);
            imgUrl = null;
        });
    }

    private String getPathFromUri(Uri contentUri) {
        String filePath;
        Cursor cursor = getContentResolver().query(contentUri, null, null, null, null);
        if (cursor == null) {
            filePath = contentUri.getPath();
        } else {
            cursor.moveToFirst();
            int index = cursor.getColumnIndex("_data");
            filePath = cursor.getString(index);
            cursor.close();
        }

        return filePath;
    }

    private void save() {
        AppDatabase.databaseWriteExecutor.execute(() -> {

            Note note = new Note();
            note.color = color;
            note.content = etContent.getText().toString().trim();
            note.title = etTitle.getText().toString().trim();
            note.time = System.currentTimeMillis();
            note.imgPath = imgUrl;
            if (noteId != 0) {
                note.id = noteId;
                noteDao.update(note);
            } else {
                note.id = noteDao.insert(note);
            }
            Log.i(TAG, "save: note");
            toast("保存成功");
            finish();
        });
    }

    private void toast(String str) {
        timerHandler.post(() -> {
            Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
        });
    }
}
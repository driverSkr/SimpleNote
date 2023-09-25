package com.example.simpleNote.adapter;

import static com.example.simpleNote.EditNoteActivity.INTENT_NOTE_ID_KEY;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.simpleNote.EditNoteActivity;
import com.example.simpleNote.R;
import com.example.simpleNote.database.AppDatabase;
import com.example.simpleNote.database.dao.NoteDao;
import com.example.simpleNote.database.entity.Note;

import android.text.format.DateFormat;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {
    private static final String TAG = "NoteAdapter";

    static class NoteViewHolder extends RecyclerView.ViewHolder{
        TextView tvTitle;
        TextView tvTime;
        ImageView ivImg;

        CardView cardView;
        Note note;

        public NoteViewHolder(@NonNull View itemView){
            super(itemView);

            tvTitle = itemView.findViewById(R.id.tv_note_title);
            tvTime = itemView.findViewById(R.id.tv_note_time);
            ivImg = itemView.findViewById(R.id.iv_note_img);
            cardView = itemView.findViewById(R.id.card_note);
        }

        private void initNode(Context context,Note note){
            Log.i(TAG,"initNode:");
            this.note = note;
            //设置标题
            tvTitle.setText(note.title);
            //设置时间
            tvTime.setText(DateFormat.format(context.getString(R.string.time_format_list),note.time));
            //设置图片
            if (note.imgPath != null && !"".equals(note.imgPath)){
                Log.i(TAG,"initNode: " + note.imgPath);
                try {
                    String s = URLDecoder.decode(note.imgPath, "UTF-8");
                    Bitmap bm = BitmapFactory.decodeFile(s);
                    ivImg.setImageBitmap(bm);
                    ivImg.setVisibility(View.VISIBLE);
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
            }else {
                ivImg.setVisibility(View.GONE);
            }
            //设置卡片背景
            cardView.findViewById(R.id.card_layout).setBackgroundColor(note.color);
            //卡片点击事件
            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, EditNoteActivity.class);
                Bundle bundle = new Bundle();
                //绑定点击的笔记的ID
                bundle.putLong(INTENT_NOTE_ID_KEY, note.id);
                intent.putExtras(bundle);
                context.startActivity(intent);
            });
            //设置长按点击事件：删除卡片笔记
            itemView.setOnLongClickListener(v -> {
                AlertDialog dialog = new AlertDialog.Builder(context)

                        .setTitle(note.title)//设置对话框的标题
                        .setMessage("你要删除这个笔记吗？")//设置对话框的内容
                        //设置对话框的按钮
                        .setNegativeButton("取消", (dialog1, which) -> dialog1.dismiss())
                        .setPositiveButton("确定", (dialog12, which) -> {

                            //Toast.makeText(context, "点击了确定的按钮", Toast.LENGTH_SHORT).show();
                            AppDatabase database = AppDatabase.getDatabase(context);
                            NoteDao noteDao = database.getNoteDao();
                            //写线程：确保删除操作在后台线程中进行，避免阻塞主线程
                            AppDatabase.databaseWriteExecutor.execute(() -> {
                                noteDao.deleteById(note.id);
                                new Handler(context.getMainLooper()).post(() -> {
                                    Toast.makeText(context, "删除成功", Toast.LENGTH_SHORT).show();
                                });
                            });
                            dialog12.dismiss();
                        }).create();
                dialog.show();

                return true;
            });
        }
    }

    private List<Note> notes;
    private List<Note> query;

    private Context context;

    public NoteAdapter(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new NoteViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.note_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        holder.initNode(context, query.get(position));
    }

    @Override
    public int getItemCount() {
        return query == null ? 0 : query.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setNotes(List<Note> notes) {
        this.notes = notes;
        query = new ArrayList<>();
        query.addAll(notes);
        notifyDataSetChanged();
    }

    public void filter(String keyword) {
        if (notes == null || keyword == null) return;
        if (keyword.equals("")) {
            query.clear();
            query.addAll(notes);
        } else {
            query.clear();
            for (Note n : notes) {
                if (n.title.contains(keyword) || n.content.contains(keyword)) {
                    query.add(n);
                }
            }
            /*new Handler(Looper.getMainLooper()):创建了一个新的Handler对象，并将它与主线程的Looper绑定.
            这意味着通过这个Handler发送的消息或者代码块将在主线程中执行，从而确保在正确的线程上进行UI操作，因为UI操作必须在主线程中进行*/
            /*.post(this::notifyDataSetChanged):使用post()方法，你可以将一个Runnable对象（或者是一个带有run()方法的lambda表达式）提交给Handler，以便在合适的时候执行这段代码。*/
            //在这段代码中，this::notifyDataSetChanged是一个lambda表达式，指定了要执行的代码块，即调用notifyDataSetChanged方法。这个方法通常用于在适配器中通知数据集变化，然后触发界面刷新。
            new Handler(Looper.getMainLooper()).post(this::notifyDataSetChanged);
        }
    }
}

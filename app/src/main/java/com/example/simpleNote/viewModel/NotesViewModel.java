package com.example.simpleNote.viewModel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.simpleNote.database.AppDatabase;
import com.example.simpleNote.database.dao.NoteDao;
import com.example.simpleNote.database.entity.Note;

import java.util.List;

public class NotesViewModel extends AndroidViewModel {

    private NoteDao noteDao;
    private LiveData<List<Note>> notes;

    public NotesViewModel(@NonNull Application application) {
        super(application);
        AppDatabase db = AppDatabase.getDatabase(application);
        noteDao = db.getNoteDao();
        notes = noteDao.selectAll();
    }

    public LiveData<List<Note>> getNotes(){return notes;}
}

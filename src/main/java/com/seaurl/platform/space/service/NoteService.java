package com.seaurl.platform.space.service;

import com.seaurl.platform.space.model.MostUrlsRank;
import com.seaurl.platform.common.model.Note;
import com.seaurl.platform.common.model.Url;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface NoteService {

    List<Note> getRecentlyNotes(String uid);

    List<Note> searchNotes(String uid, String keyword);

   Object getRecentlyNotesCount(String uid);

    boolean add(Note note) throws IOException;

    boolean delete(String noteId);

    boolean editContent(Note note);

    boolean editCategoryId(Note note);

    boolean editTitle(Note note);

    boolean editIsPrivate(String noteId, Boolean isPrivate);

    Note queryNoteById(String noteId);

    List<Note> queryNoteByCategoryId(String categoryId);

    List<Note> queryNoteByTitle(String uid, String title);

    boolean setFavorite(String noteId, boolean isFavorite);

    List<Note> queryIsFavoriteList(String uid);

    Object queryIsFavoriteCount(String uid);

}

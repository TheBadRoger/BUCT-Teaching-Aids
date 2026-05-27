package com.buctta.api.service;

import com.buctta.api.entities.Note;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NoteService {

    // 创建笔记
    Note createNote(Note note);

    // 更新笔记
    Note updateNote(Long id, Note noteDetails);

    // 获取笔记详情
    Note getNoteById(Long id);

    // 删除笔记
    boolean deleteNote(Long noteId, Long studentId);

    // 获取课程笔记
    Page<Note> getNotesByCourse(Long courseId, Pageable pageable);

    // 获取学生笔记
    Page<Note> getNotesByStudent(Long studentId, Pageable pageable);

    // 获取公开笔记
    Page<Note> getPublicNotes(Pageable pageable);

    // 搜索笔记
    Page<Note> searchNotes(String keyword, Pageable pageable);

    // 点赞/取消点赞
    boolean toggleLike(Long noteId, Long studentId);

    // 检查是否点赞
    boolean isNoteLikedByStudent(Long noteId, Long studentId);

    // 获取热门笔记
    Page<Note> getPopularNotes(Pageable pageable);
}
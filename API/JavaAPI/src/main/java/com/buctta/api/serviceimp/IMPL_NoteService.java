package com.buctta.api.serviceimp;

import com.buctta.api.dao.NoteReposit;
import com.buctta.api.dao.StudentReposit;
import com.buctta.api.dao.CourseReposit;
import com.buctta.api.dao.CommentReposit;
import com.buctta.api.entities.Note;
import com.buctta.api.entities.Student;
import com.buctta.api.entities.Course;
import com.buctta.api.service.NoteService;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class IMPL_NoteService implements NoteService {

    @Resource
    private NoteReposit noteReposit;

    @Resource
    private StudentReposit studentRepository;

    @Resource
    private CourseReposit courseRepository;

    @Resource
    private CommentReposit commentReposit;

    @Override
    @Transactional
    public Note createNote(Note note) {
        // 验证学生存在
        Student student = studentRepository.findById(note.getStudent().getId())
                .orElseThrow(() -> new EntityNotFoundException("学生不存在"));

        // 验证课程存在
        Course course = courseRepository.findById(note.getCourse().getId())
                .orElseThrow(() -> new EntityNotFoundException("课程不存在"));

        note.setStudent(student);
        note.setCourse(course);

        return noteReposit.save(note);
    }

    @Override
    @Transactional
    public Note updateNote(Long id, Note noteDetails) {
        Note existingNote = noteReposit.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("笔记不存在"));

        // 检查权限（只有作者可以修改）
        // 这里假设有权限检查逻辑

        // 更新字段
        if (noteDetails.getTitle() != null) {
            existingNote.setTitle(noteDetails.getTitle());
        }
        if (noteDetails.getContent() != null) {
            existingNote.setContent(noteDetails.getContent());
        }
        if (noteDetails.getHtmlContent() != null) {
            existingNote.setHtmlContent(noteDetails.getHtmlContent());
        }
        if (noteDetails.getIsPublic() != null) {
            existingNote.setIsPublic(noteDetails.getIsPublic());
        }

        return noteReposit.save(existingNote);
    }

    @Override
    public Note getNoteById(Long id) {
        return noteReposit.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("笔记不存在"));
    }

    @Override
    @Transactional
    public boolean deleteNote(Long noteId, Long studentId) {
        Optional<Note> noteOpt = noteReposit.findById(noteId);
        if (noteOpt.isEmpty()) {
            return false;
        }

        Note note = noteOpt.get();
        // 检查权限（只有作者可以删除）
        if (!note.getStudent().getId().equals(studentId)) {
            throw new SecurityException("无权删除此笔记");
        }

        // 先删除相关评论
        commentReposit.deleteByNoteId(noteId);

        // 删除笔记
        noteReposit.deleteById(noteId);
        return true;
    }

    @Override
    public Page<Note> getNotesByCourse(Long courseId, Pageable pageable) {
        return noteReposit.findByCourseId(courseId, pageable);
    }

    @Override
    public Page<Note> getNotesByStudent(Long studentId, Pageable pageable) {
        return noteReposit.findByStudentId(studentId, pageable);
    }

    @Override
    public Page<Note> getPublicNotes(Pageable pageable) {
        return noteReposit.findByIsPublicTrue(pageable);
    }

    @Override
    public Page<Note> searchNotes(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return noteReposit.findByIsPublicTrue(pageable);
        }
        return noteReposit.searchPublicNotes(keyword.trim(), pageable);
    }

    @Override
    @Transactional
    public boolean toggleLike(Long noteId, Long studentId) {
        Note note = noteReposit.findById(noteId)
                .orElseThrow(() -> new EntityNotFoundException("笔记不存在"));

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new EntityNotFoundException("学生不存在"));

        if (note.isLikedByStudent(student)) {
            // 取消点赞
            note.removeLike(student);
            noteReposit.save(note);
            return false;
        } else {
            // 点赞
            note.addLike(student);
            noteReposit.save(note);
            return true;
        }
    }

    @Override
    public boolean isNoteLikedByStudent(Long noteId, Long studentId) {
        return noteReposit.existsLike(noteId, studentId);
    }

    @Override
    public Page<Note> getPopularNotes(Pageable pageable) {
        return noteReposit.findByIsPublicTrueOrderByLikeCountDesc(pageable);
    }
}
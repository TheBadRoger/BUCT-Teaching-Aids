package com.buctta.api.serviceimp;

import com.buctta.api.dao.CommentReposit;
import com.buctta.api.dao.NoteReposit;
import com.buctta.api.dao.StudentReposit;
import com.buctta.api.entities.Comment;
import com.buctta.api.entities.Note;
import com.buctta.api.entities.Student;
import com.buctta.api.service.CommentService;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class IMPL_CommentService implements CommentService {

    @Resource
    private CommentReposit commentReposit;

    @Resource
    private NoteReposit noteReposit;

    @Resource
    private StudentReposit studentRepository;

    @Override
    @Transactional
    public Comment addComment(Comment comment) {
        // 验证笔记存在
        Note note = noteReposit.findById(comment.getNote().getId())
                .orElseThrow(() -> new EntityNotFoundException("笔记不存在"));

        // 验证学生存在
        Student student = studentRepository.findById(comment.getStudent().getId())
                .orElseThrow(() -> new EntityNotFoundException("学生不存在"));

        comment.setNote(note);
        comment.setStudent(student);

        // 更新笔记的评论数
        note.incrementCommentCount();
        noteReposit.save(note);

        return commentReposit.save(comment);
    }

    @Override
    @Transactional
    public Comment replyComment(Long parentId, Comment comment) {
        // 验证父评论存在
        Comment parentComment = commentReposit.findById(parentId)
                .orElseThrow(() -> new EntityNotFoundException("父评论不存在"));

        comment.setParentComment(parentComment);

        return addComment(comment);
    }

    @Override
    public Comment getCommentById(Long id) {
        return commentReposit.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("评论不存在"));
    }

    @Override
    @Transactional
    public boolean deleteComment(Long commentId, Long studentId) {
        Optional<Comment> commentOpt = commentReposit.findById(commentId);
        if (commentOpt.isEmpty()) {
            return false;
        }

        Comment comment = commentOpt.get();
        // 检查权限（只有作者可以删除）
        if (!comment.getStudent().getId().equals(studentId)) {
            throw new SecurityException("无权删除此评论");
        }

        // 更新笔记的评论数
        Note note = comment.getNote();
        note.decrementCommentCount();
        noteReposit.save(note);

        // 删除评论
        commentReposit.deleteById(commentId);
        return true;
    }

    @Override
    public List<Comment> getCommentsByNote(Long noteId) {
        return commentReposit.findCommentsWithReplies(noteId);
    }

    @Override
    public Page<Comment> getCommentsByNote(Long noteId, Pageable pageable) {
        return commentReposit.findByNoteId(noteId, pageable);
    }

    @Override
    @Transactional
    public Comment updateComment(Long id, Comment commentDetails) {
        Comment existingComment = commentReposit.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("评论不存在"));

        if (commentDetails.getContent() != null) {
            existingComment.setContent(commentDetails.getContent());
        }

        return commentReposit.save(existingComment);
    }

    @Override
    public List<Comment> getRepliesByComment(Long parentId) {
        return commentReposit.findByParentCommentIdOrderByCreatedAtAsc(parentId);
    }
}
package com.buctta.api.controller;

import com.buctta.api.entities.Note;
import com.buctta.api.service.NoteService;
import com.buctta.api.utils.ApiResponse;
import com.buctta.api.utils.BusinessStatus;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/notes")
public class NoteController {

    @Resource
    private NoteService noteService;

    /**
     * 创建笔记
     */
    @PostMapping("/create")
    public ApiResponse<Note> createNote(@RequestBody Note note) {
        try {
            Note createdNote = noteService.createNote(note);
            return ApiResponse.ok(createdNote);
        } catch (EntityNotFoundException e) {
            //log.warn("创建笔记失败，关联实体不存在: {}", e.getMessage());
            return ApiResponse.fail(BusinessStatus.RESOURCE_NOT_FOUND);
        } catch (Exception e) {
            //log.error("创建笔记异常: {}", e.getMessage(), e);
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR);
        }
    }

    /**
     * 更新笔记（仅作者可操作）
     */
    @PutMapping("/update/{id}")
    public ApiResponse<Note> updateNote(@PathVariable Long id, @RequestBody Note noteDetails) {
        try {
            Note updatedNote = noteService.updateNote(id, noteDetails);
            return ApiResponse.ok(updatedNote);
        } catch (EntityNotFoundException e) {
            return ApiResponse.fail(BusinessStatus.RESOURCE_NOT_FOUND);
        } catch (SecurityException e) {
            log.warn("无权限更新笔记: {}", e.getMessage());
            return ApiResponse.fail(BusinessStatus.NO_PERMISSION);
        } catch (Exception e) {
            log.error("更新笔记异常: {}", e.getMessage(), e);
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR);
        }
    }

    /**
     * 获取笔记详情
     */
    @GetMapping("/{id}")
    public ApiResponse<Note> getNote(@PathVariable Long id) {
        try {
            Note note = noteService.getNoteById(id);
            return ApiResponse.ok(note);
        } catch (EntityNotFoundException e) {
            return ApiResponse.fail(BusinessStatus.RESOURCE_NOT_FOUND);
        } catch (Exception e) {
            log.error("获取笔记异常: {}", e.getMessage(), e);
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR);
        }
    }

    /**
     * 删除笔记（仅作者可操作）
     */
    @DeleteMapping("/{noteId}")
    public ApiResponse<Boolean> deleteNote(@PathVariable Long noteId, @RequestParam Long studentId) {
        try {
            boolean result = noteService.deleteNote(noteId, studentId);
            return ApiResponse.ok(result);
        } catch (EntityNotFoundException e) {
            return ApiResponse.fail(BusinessStatus.RESOURCE_NOT_FOUND);
        } catch (SecurityException e) {
            return ApiResponse.fail(BusinessStatus.NO_PERMISSION);
        } catch (Exception e) {
            log.error("删除笔记异常: {}", e.getMessage(), e);
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR);
        }
    }

    /**
     * 查询某课程下的笔记（分页）
     */
    @GetMapping("/course/{courseId}")
    public ApiResponse<Page<Note>> getNotesByCourse(
            @PathVariable Long courseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        try {
            Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
            Page<Note> notes = noteService.getNotesByCourse(courseId, pageable);
            return ApiResponse.ok(notes);
        } catch (Exception e) {
            log.error("获取课程笔记异常: {}", e.getMessage(), e);
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR);
        }
    }

    /**
     * 查询某学生写的笔记（分页）
     */
    @GetMapping("/student/{studentId}")
    public ApiResponse<Page<Note>> getNotesByStudent(
            @PathVariable Long studentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<Note> notes = noteService.getNotesByStudent(studentId, pageable);
            return ApiResponse.ok(notes);
        } catch (Exception e) {
            log.error("获取学生笔记异常: {}", e.getMessage(), e);
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR);
        }
    }

    /**
     * 获取所有公开笔记（分页）
     */
    @GetMapping("/public")
    public ApiResponse<Page<Note>> getPublicNotes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        try {
            Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
            Page<Note> notes = noteService.getPublicNotes(pageable);
            return ApiResponse.ok(notes);
        } catch (Exception e) {
            log.error("获取公开笔记异常: {}", e.getMessage(), e);
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR);
        }
    }

    /**
     * 搜索公开笔记（标题/内容）
     */
    @GetMapping("/search")
    public ApiResponse<Page<Note>> searchNotes(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<Note> notes = noteService.searchNotes(keyword, pageable);
            return ApiResponse.ok(notes);
        } catch (Exception e) {
            log.error("搜索笔记异常: {}", e.getMessage(), e);
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR);
        }
    }

    /**
     * 点赞/取消点赞
     * @return true - 已点赞（点赞操作），false - 已取消点赞（取消操作）
     */
    @PostMapping("/{noteId}/like")
    public ApiResponse<Boolean> toggleLike(
            @PathVariable Long noteId,
            @RequestParam Long studentId) {
        try {
            boolean liked = noteService.toggleLike(noteId, studentId);
            return ApiResponse.ok(liked);
        } catch (EntityNotFoundException e) {
            return ApiResponse.fail(BusinessStatus.RESOURCE_NOT_FOUND);
        } catch (Exception e) {
            log.error("点赞操作异常: {}", e.getMessage(), e);
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR);
        }
    }

    /**
     * 检查当前学生是否已点赞该笔记
     */
    @GetMapping("/{noteId}/liked")
    public ApiResponse<Boolean> isLiked(
            @PathVariable Long noteId,
            @RequestParam Long studentId) {
        try {
            boolean liked = noteService.isNoteLikedByStudent(noteId, studentId);
            return ApiResponse.ok(liked);
        } catch (EntityNotFoundException e) {
            return ApiResponse.fail(BusinessStatus.RESOURCE_NOT_FOUND);
        } catch (Exception e) {
            log.error("检查点赞状态异常: {}", e.getMessage(), e);
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR);
        }
    }

    /**
     * 获取热门笔记（按点赞数降序）
     */
    @GetMapping("/popular")
    public ApiResponse<Page<Note>> getPopularNotes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Note> notes = noteService.getPopularNotes(pageable);
            return ApiResponse.ok(notes);
        } catch (Exception e) {
            log.error("获取热门笔记异常: {}", e.getMessage(), e);
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR);
        }
    }
}
package com.buctta.api.interf;


import com.buctta.api.entities.TeacherList;
import com.buctta.api.service.TeacherService;
import com.buctta.api.utils.CallBackContainer;
import jakarta.annotation.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/findteacher")
public class TeacherListCalls {
    @Resource
    private TeacherService teacherService;

    @PostMapping("/addteacher")
    public CallBackContainer<TeacherList> addteacherCall(@RequestBody TeacherList newteacher) {
        TeacherList tl = teacherService.AddTeacher(newteacher);
        CallBackContainer<TeacherList> callBackContainer = new CallBackContainer<>();
        if (tl != null)
            return new CallBackContainer<>("0","Success",tl);
        else
            return new CallBackContainer<>("-1","Success",null);
    }

    @PostMapping("/searchteacher")
    public CallBackContainer<Page<TeacherList>> SearchTeacherCall(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String organization,
            @RequestParam(required = false) String jointime,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String education,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sort) {

        try {
            // 创建分页请求
            Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
            // 调用服务层进行搜索
            Page<TeacherList> teacherPage = teacherService.searchTeachers(name, organization, jointime, gender, education, pageable);
            return new CallBackContainer<>("0","搜索成功",teacherPage);

        } catch (Exception e) {
            e.printStackTrace();
            return new CallBackContainer<>("-5","搜索失败",null);
        }
    }
}
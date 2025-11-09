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
        System.out.println("收到添加教师请求\n教师名" + newteacher.getName() + "\n正在处理添加请求...\n");
        TeacherList tl = teacherService.AddTeacher(newteacher);
        CallBackContainer<TeacherList> callBackContainer = new CallBackContainer<>();
        if (tl != null) {
            callBackContainer.setData(tl);
            callBackContainer.setCode("0");
            callBackContainer.setMsg("Success");
            System.out.println("处理完成。返回消息：\"" + callBackContainer.getMsg() + "\"(" + callBackContainer.getCode() + ")\n");
            return callBackContainer;
        } else {
            callBackContainer.setCode("-1");
            callBackContainer.setMsg("该教师已存在！");
            System.out.println("处理完成。返回消息：\"" + callBackContainer.getMsg() + "\"(" + callBackContainer.getCode() + ")\n");
            return callBackContainer;
        }
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

        System.out.println("收到搜索教师请求\n" +
                "姓名: " + (name != null ? name : "不限") + "\n" +
                "学院: " + (organization != null ? organization : "不限") + "\n" +
                "入职时间: " + (jointime != null ? jointime : "不限") + "\n" +
                "性别: " + (gender != null ? gender : "不限") + "\n" +
                "学历: " + (education != null ? education : "不限") + "\n" +
                "正在处理搜索请求...");

        try {
            // 创建分页请求
            Pageable pageable = PageRequest.of(page, size, Sort.by(sort));

            // 调用服务层进行搜索
            Page<TeacherList> teacherPage = teacherService.searchTeachers(name, organization, jointime, gender, education, pageable);

            CallBackContainer<Page<TeacherList>> callBackContainer = new CallBackContainer<>();
            callBackContainer.setData(teacherPage);
            callBackContainer.setCode("0");
            callBackContainer.setMsg("搜索成功");

            System.out.println("处理完成。找到 " + teacherPage.getTotalElements() + " 条记录，返回消息：\"" +
                    callBackContainer.getMsg() + "\"(" + callBackContainer.getCode() + ")");

            return callBackContainer;

        } catch (Exception e) {
            System.err.println("搜索教师时发生错误: " + e.getMessage());
            e.printStackTrace();

            CallBackContainer<Page<TeacherList>> callBackContainer = new CallBackContainer<>();
            callBackContainer.setCode("-1");
            callBackContainer.setMsg("搜索失败: " + e.getMessage());

            System.out.println("处理完成。返回消息：\"" + callBackContainer.getMsg() + "\"(" + callBackContainer.getCode() + ")");
            return callBackContainer;
        }
    }
}
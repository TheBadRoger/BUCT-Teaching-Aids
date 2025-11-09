package com.buctta.api.interf;


import com.buctta.api.entities.TeacherList;
import com.buctta.api.service.TeacherService;
import com.buctta.api.utils.CallBackContainer;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/findteacher")
public class TeacherListCalls {
    @Resource
    private TeacherService teacherService ;

    @PostMapping("/addteacher")
    public CallBackContainer<TeacherList> addteacherCall(@RequestBody TeacherList newteacher){
        System.out.println(newteacher.getName());
        TeacherList tl = teacherService.AddTeacher(newteacher);
        CallBackContainer<TeacherList> callBackContainer = new CallBackContainer<>();
        if(tl != null)
        {
            callBackContainer.setData(tl);
            callBackContainer.setMsg("Success");
            return callBackContainer;
        }
        else return callBackContainer;
    }
}
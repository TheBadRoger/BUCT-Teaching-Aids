package com.buctta.api.serviceimp;

import com.buctta.api.entities.TeacherList;
import com.buctta.api.reposit.TeacherReposit;
import com.buctta.api.service.TeacherService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class IMPL_TeacherService implements TeacherService {
    @Resource
    private TeacherReposit teacherReposit;
    @Override
    public TeacherList AddTeacher(TeacherList teacherList)
    {
        if (teacherReposit.findTeacherListByName(teacherList.getName())!=null)
        {
            return null;
        }
        else
        {
            TeacherList newTeacher=teacherReposit.save(teacherList);
            return newTeacher;
        }
    }
}

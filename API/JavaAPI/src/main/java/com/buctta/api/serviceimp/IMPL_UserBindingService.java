package com.buctta.api.serviceimp;

import com.buctta.api.dao.StudentReposit;
import com.buctta.api.dao.TeacherReposit;
import com.buctta.api.dao.UserReposit;
import com.buctta.api.entities.Student;
import com.buctta.api.entities.Teacher;
import com.buctta.api.entities.User;
import com.buctta.api.service.IdentityVerificationService;
import com.buctta.api.service.IdentityVerificationService.VerificationResult;
import com.buctta.api.service.IdentityVerificationService.VerifiedInfo;
import com.buctta.api.service.UserBindingService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
public class IMPL_UserBindingService implements UserBindingService {

    @Resource
    private UserReposit userReposit;

    @Resource
    private StudentReposit studentReposit;

    @Resource
    private TeacherReposit teacherReposit;

    @Resource
    private IdentityVerificationService identityVerificationService;

    @Override
    @Transactional
    public BindingResult bindStudent(Long userId, String name, String idCard, String studentNumber) {
        // 1. 查找用户
        Optional<User> userOpt = userReposit.findById(userId);
        if (userOpt.isEmpty()) {
            return BindingResult.fail("USER_NOT_FOUND", "用户不存在");
        }
        User user = userOpt.get();

        // 2. 互斥检查：如果已绑定教师，则不能绑定学生
        if (user.getTeacher() != null) {
            return BindingResult.fail("BINDIND_CONFLICT", "用户已绑定教师身份，无法再绑定学生身份");
        }

        // 3. 检查是否已绑定学生
        if (user.getStudent() != null) {
            return BindingResult.fail("ALREADY_BOUND", "用户已绑定学生身份，请先解绑");
        }

        // 4. 检查学号是否已被其他用户绑定
        Optional<Student> existingStudent = studentReposit.findByStudentNumber(studentNumber);
        if (existingStudent.isPresent() && existingStudent.get().getUser() != null) {
            return BindingResult.fail("STUDENT_NUMBER_BOUND", "该学号已被其他用户绑定");
        }

        // 5. 调用网络身份认证服务验证身份
        VerificationResult verifyResult = identityVerificationService.verifyStudent(name, idCard, studentNumber);
        if (!verifyResult.success()) {
            return BindingResult.fail("IDENTITY_VERIFY_FAILED", verifyResult.message());
        }

        // 6. 创建或更新学生信息
        Student student;
        if (existingStudent.isPresent()) {
            // 学生记录已存在但未绑定用户，更新信息
            student = existingStudent.get();
        }
        else {
            // 创建新的学生记录
            student = new Student();
            student.setStudentNumber(studentNumber);
        }

        // 使用验证返回的信息填充学生数据
        VerifiedInfo info = verifyResult.verifiedInfo();
        student.setName(info.name());
        student.setGender(info.gender());
        student.setClassName(info.className());

        if (info.admissionDate() != null) {
            try {
                student.setAdmissionDate(LocalDate.parse(info.admissionDate(), DateTimeFormatter.ISO_LOCAL_DATE));
            }
            catch (Exception e) {
                log.warn("解析入学日期失败: {}", info.admissionDate());
            }
        }

        // 7. 保存学生信息
        student = studentReposit.save(student);

        // 8. 绑定用户和学生，并自动设置用户类型为STUDENT
        user.setStudent(student);
        user.setUserType(User.UserType.STUDENT);
        user = userReposit.save(user);
        user.setPassword(null);  // 不返回密码

        log.info("用户 {} 成功绑定学生身份: {}", userId, studentNumber);
        return BindingResult.successStudent(user, student);
    }

    @Override
    @Transactional
    public BindingResult bindTeacher(Long userId, String name, String idCard, String employeeNumber) {
        // 1. 查找用户
        Optional<User> userOpt = userReposit.findById(userId);
        if (userOpt.isEmpty()) {
            return BindingResult.fail("USER_NOT_FOUND", "用户不存在");
        }
        User user = userOpt.get();

        // 2. 互斥检查：如果已绑定学生，则不能绑定教师
        if (user.getStudent() != null) {
            return BindingResult.fail("BINDIND_CONFLICT", "用户已绑定学生身份，无法再绑定教师身份");
        }

        // 3. 检查是否已绑定教师
        if (user.getTeacher() != null) {
            return BindingResult.fail("ALREADY_BOUND", "用户已绑定教师身份，请先解绑");
        }

        // 4. 检查是否存在同名教师已被绑定
        Teacher existingTeacher = teacherReposit.findTeacherListByName(name);
        if (existingTeacher != null && existingTeacher.getUser() != null) {
            return BindingResult.fail("TEACHER_BOUND", "该教师身份已被其他用户绑定");
        }

        // 5. 调用网络身份认证服务验证身份
        VerificationResult verifyResult = identityVerificationService.verifyTeacher(name, idCard, employeeNumber);
        if (!verifyResult.success()) {
            return BindingResult.fail("IDENTITY_VERIFY_FAILED", verifyResult.message());
        }

        // 6. 创建或更新教师信息
        Teacher teacher;
        // 教师记录已存在但未绑定用户，更新信息
        // 创建新的教师记录
        teacher = Objects.requireNonNullElseGet(existingTeacher, Teacher::new);

        // 使用验证返回的信息填充教师数据
        VerifiedInfo info = verifyResult.verifiedInfo();
        teacher.setName(info.name());
        teacher.setGender(info.gender());
        teacher.setOrganization(info.organization());
        teacher.setEducation(info.education());
        teacher.setJointime(info.admissionDate());

        // 7. 保存教师信息
        teacher = teacherReposit.save(teacher);

        // 8. 绑定用户和教师，并自动设置用户类型为TEACHER
        user.setTeacher(teacher);
        user.setUserType(User.UserType.TEACHER);
        user = userReposit.save(user);
        user.setPassword(null);  // 不返回密码

        log.info("用户 {} 成功绑定教师身份: {}", userId, name);
        return BindingResult.successTeacher(user, teacher);
    }

    @Override
    @Transactional
    public BindingResult unbind(Long userId) {
        // 1. 查找用户
        Optional<User> userOpt = userReposit.findById(userId);
        if (userOpt.isEmpty()) {
            return BindingResult.fail("USER_NOT_FOUND", "用户不存在");
        }
        User user = userOpt.get();

        // 2. 检查是否有绑定
        if (user.getStudent() == null && user.getTeacher() == null) {
            return BindingResult.fail("NOT_BOUND", "用户未绑定任何身份");
        }

        // 3. 解绑并清除用户类型
        if (user.getStudent() != null) {
            log.info("用户 {} 解绑学生身份: {}", userId, user.getStudent().getStudentNumber());
            user.setStudent(null);
        }
        if (user.getTeacher() != null) {
            log.info("用户 {} 解绑教师身份: {}", userId, user.getTeacher().getName());
            user.setTeacher(null);
        }

        // 清除用户类型，用户需要重新绑定身份
        user.setUserType(null);

        userReposit.save(user);
        return BindingResult.successUnbind();
    }

    @Override
    public Student getBoundStudent(Long userId) {
        Optional<User> userOpt = userReposit.findById(userId);
        return userOpt.map(User::getStudent).orElse(null);
    }

    @Override
    public Teacher getBoundTeacher(Long userId) {
        Optional<User> userOpt = userReposit.findById(userId);
        return userOpt.map(User::getTeacher).orElse(null);
    }
}


package com.buctta.api.serviceimp;

import com.buctta.api.dao.OrganizationRepository;
import com.buctta.api.dao.StudentReposit;
import com.buctta.api.dao.UserReposit;
import com.buctta.api.dto.LearningStatsDTO;
import com.buctta.api.dto.UserProfileDTO;
import com.buctta.api.entities.Organization;
import com.buctta.api.entities.Student;
import com.buctta.api.entities.Teacher;
import com.buctta.api.entities.User;
import com.buctta.api.service.LearningStatsService;
import com.buctta.api.service.ProfileService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class IMPL_ProfileService implements ProfileService {

    @Resource
    private UserReposit userReposit;

    @Resource
    private StudentReposit studentReposit;

    @Resource
    private OrganizationRepository organizationRepository;

    @Resource
    private LearningStatsService learningStatsService;

    /**
     * 在事务内读取用户，让懒加载的 student / teacher 关联可用。
     * <p>
     * {@code @Transactional(readOnly = true)} 保证会话在整个方法内有效，
     * 因此 {@link UserProfileDTO#of} 里访问 {@code user.getStudent()} 不会触发
     * LazyInitializationException。
     */
    @Override
    @Transactional(readOnly = true)
    public UserProfileDTO byUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        User user = userReposit.findById(userId).orElse(null);
        if (user == null) {
            return null;
        }
        return UserProfileDTO.of(user, resolveOrganization(user));
    }

    @Override
    @Transactional(readOnly = true)
    public StudentProfileView studentProfile(Long studentId) {
        if (studentId == null) {
            return null;
        }
        Student student = studentReposit.findById(studentId).orElse(null);
        if (student == null) {
            return null;
        }
        // 学生通过反向关联找到账号；无账号的学生记录仍可展示基础资料
        User user = student.getUser();
        UserProfileDTO profile = user == null
                ? new UserProfileDTO(null, null, null, null, null, "STUDENT",
                UserProfileDTO.StudentProfile.of(student), null, null)
                : UserProfileDTO.of(user, resolveOrganization(user));
        LearningStatsDTO stats = learningStatsService.forStudent(studentId);
        return new StudentProfileView(profile, stats);
    }

    /* ------------------------------------------------------------ */

    /**
     * 解析用户所属机构。
     * <p>
     * 关联方式是字符串匹配：{@code teacher.organization} ↔ {@code organization.name}，
     * 这是既有设计（绑定身份时由 {@code IMPL_UserBindingService} 写入），此处沿用不另建外键。
     */
    private Organization resolveOrganization(User user) {
        Teacher teacher = user.getTeacher();
        if (teacher == null || teacher.getOrganization() == null
                || teacher.getOrganization().isBlank()) {
            return null;
        }
        try {
            return organizationRepository.findByName(teacher.getOrganization()).orElse(null);
        }
        catch (RuntimeException e) {
            // 机构资料缺失不应导致整份个人资料接口失败
            log.warn("按名称解析机构失败 name={}", teacher.getOrganization(), e);
            return null;
        }
    }
}

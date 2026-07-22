package com.buctta.api.serviceimp;

import com.buctta.api.dao.TeacherReposit;
import com.buctta.api.dao.UserReposit;
import com.buctta.api.dto.TeacherDTO;
import com.buctta.api.entities.Teacher;
import com.buctta.api.entities.User;
import com.buctta.api.service.TeacherService;
import jakarta.annotation.Resource;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class IMPL_TeacherService implements TeacherService {
    @Resource
    private TeacherReposit teacherReposit;
    @Resource
    private UserReposit userReposit;
    @Resource
    private PasswordEncoder passwordEncoder;

    @Override
    public TeacherResult addTeacher(Teacher teacher) {
        if (teacherReposit.findTeacherListByName(teacher.getName()) != null) {
            return TeacherResult.fail("TEACHER_EXISTS", "教师已存在: " + teacher.getName());
        }
        try {
            Teacher savedTeacher = teacherReposit.save(teacher);
            return TeacherResult.success(savedTeacher, "教师添加成功");
        } catch (Exception e) {
            return TeacherResult.fail("SAVE_FAILED", "保存教师失败: " + e.getMessage());
        }
    }

    //构建 Specification，支持 User 字段过滤
    private Specification<Teacher> buildTeacherSpec(
            String name, String organization, String jointime, String gender, String education,
            String username, String telephone, String email, String userType) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 教师自身字段
            if (name != null && !name.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(root.get("name"), "%" + name + "%"));
            }
            if (organization != null && !organization.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(root.get("organization"), "%" + organization + "%"));
            }
            if (jointime != null && !jointime.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("jointime"), jointime));
            }
            if (gender != null && !gender.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("gender"), gender));
            }
            if (education != null && !education.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("education"), education));
            }

            //关联 User 字段（LEFT JOIN）
            boolean hasUserCondition = (username != null && !username.trim().isEmpty()) ||
                    (telephone != null && !telephone.trim().isEmpty()) ||
                    (email != null && !email.trim().isEmpty()) ||
                    (userType != null && !userType.trim().isEmpty());

            if (hasUserCondition) {
                var userJoin = root.join("user", JoinType.LEFT);
                if (username != null && !username.trim().isEmpty()) {
                    predicates.add(criteriaBuilder.like(userJoin.get("username"), "%" + username + "%"));
                }
                if (telephone != null && !telephone.trim().isEmpty()) {
                    predicates.add(criteriaBuilder.like(userJoin.get("telephone"), "%" + telephone + "%"));
                }
                if (email != null && !email.trim().isEmpty()) {
                    predicates.add(criteriaBuilder.like(userJoin.get("email"), "%" + email + "%"));
                }
                if (userType != null && !userType.trim().isEmpty()) {
                    predicates.add(criteriaBuilder.equal(userJoin.get("userType"), userType));
                }
                // 避免因左连接导致分页计数重复
                query.distinct(true);
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private TeacherDTO convertToDTO(Teacher teacher) {
        TeacherDTO dto = new TeacherDTO();
        dto.setId(teacher.getId());
        dto.setName(teacher.getName());
        dto.setOrganization(teacher.getOrganization());
        dto.setGender(teacher.getGender());
        dto.setEducation(teacher.getEducation());
        dto.setJointime(teacher.getJointime());

        User user = teacher.getUser();
        if (user != null) {
            dto.setUsername(user.getUsername());
            dto.setTelephone(user.getTelephone());
            dto.setEmail(user.getEmail());
            dto.setUserType(user.getUserType() != null ? user.getUserType().name() : null);
        }
        return dto;
    }

    @Override
    public Page<TeacherDTO> searchTeachers(
            String name, String organization, String jointime, String gender, String education,
            String username, String telephone, String email, String userType,
            Pageable pageable) {
        Specification<Teacher> spec = buildTeacherSpec(name, organization, jointime, gender, education,
                username, telephone, email, userType);
        Page<Teacher> teacherPage = teacherReposit.findAll(spec, pageable);
        return teacherPage.map(this::convertToDTO);
    }

    @Override
    public List<Teacher> searchTeachersBySpec(
            String name, String organization, String jointime, String gender, String education,
            String username, String telephone, String email, String userType) {
        Specification<Teacher> spec = buildTeacherSpec(name, organization, jointime, gender, education,
                username, telephone, email, userType);
        return teacherReposit.findAll(spec);
    }

    @Override
    public TeacherResult deleteTeachers(List<Long> ids) {
        try {
            if (ids == null || ids.isEmpty()) {
                return TeacherResult.fail("INVALID_IDS", "教师ID列表不能为空");
            }
            teacherReposit.deleteAllByIdIn(ids);
            return TeacherResult.success(null, "批量删除成功");
        } catch (Exception e) {
            return TeacherResult.fail("DELETE_FAILED", "批量删除失败: " + e.getMessage());
        }
    }

    @Override
    public byte[] exportTeachersToExcel(List<Teacher> teachers) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("教师名单");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("ID");
        header.createCell(1).setCellValue("姓名");
        header.createCell(2).setCellValue("单位");
        header.createCell(3).setCellValue("性别");
        header.createCell(4).setCellValue("学历");
        header.createCell(5).setCellValue("入职时间");
        header.createCell(6).setCellValue("用户名");
        header.createCell(7).setCellValue("电话");
        header.createCell(8).setCellValue("邮箱");
        header.createCell(9).setCellValue("用户类型");

        int rowIdx = 1;
        for (Teacher t : teachers) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(t.getId());
            row.createCell(1).setCellValue(t.getName());
            row.createCell(2).setCellValue(t.getOrganization());
            row.createCell(3).setCellValue(t.getGender());
            row.createCell(4).setCellValue(t.getEducation());
            row.createCell(5).setCellValue(t.getJointime());
            User user = t.getUser();
            row.createCell(6).setCellValue(user != null ? user.getUsername() : "");
            row.createCell(7).setCellValue(user != null ? user.getTelephone() : "");
            row.createCell(8).setCellValue(user != null ? user.getEmail() : "");
            row.createCell(9).setCellValue(user != null && user.getUserType() != null
                    ? user.getUserType().name() : "");
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        workbook.write(bos);
        workbook.close();
        return bos.toByteArray();
    }

    @Override
    public List<Teacher> getAllTeachersForExport() {
        return teacherReposit.findAll();
    }

    @Override
    public TeacherResult updateTeacher(Long id, Teacher teacherDetails) {
        Teacher existingTeacher = teacherReposit.findTeacherListById(id);
        if (existingTeacher == null) {
            return TeacherResult.fail("TEACHER_NOT_FOUND", "教师不存在，ID: " + id);
        }

        // 检查姓名是否重复（排除自己）
        if (teacherDetails.getName() != null &&
                !teacherDetails.getName().equals(existingTeacher.getName())) {
            Teacher teacherWithSameName = teacherReposit.findTeacherListByName(teacherDetails.getName());
            if (teacherWithSameName != null && teacherWithSameName.getId() != id) {
                return TeacherResult.fail("TEACHER_NAME_EXISTS",
                        "教师姓名已存在: " + teacherDetails.getName());
            }
        }

        // 更新字段（null-safe）
        if (teacherDetails.getName() != null) {
            existingTeacher.setName(teacherDetails.getName());
        }
        if (teacherDetails.getOrganization() != null) {
            existingTeacher.setOrganization(teacherDetails.getOrganization());
        }
        if (teacherDetails.getGender() != null) {
            existingTeacher.setGender(teacherDetails.getGender());
        }
        if (teacherDetails.getEducation() != null) {
            existingTeacher.setEducation(teacherDetails.getEducation());
        }
        if (teacherDetails.getJointime() != null) {
            existingTeacher.setJointime(teacherDetails.getJointime());
        }
        try {
            Teacher updatedTeacher = teacherReposit.save(existingTeacher);
            return TeacherResult.success(updatedTeacher, "教师信息更新成功");
        } catch (Exception e) {
            return TeacherResult.fail("UPDATE_FAILED", "更新教师失败: " + e.getMessage());
        }
    }
    @Override
    @Transactional
    public AddWithUserResult addTeacherWithUser(Teacher teacher, String username, String password, String telephone, String email) {
        // 检查教师姓名是否已存在（可根据实际业务调整，这里只做简单姓名查重）
        if (teacherReposit.findTeacherListByName(teacher.getName()) != null) {
            return AddWithUserResult.fail("TEACHER_NAME_EXISTS", "教师姓名已存在");
        }

        // 生成默认用户名（用工号或姓名拼音，此处用工号）
        if (username == null || username.trim().isEmpty()) {
            username = "T" + System.currentTimeMillis(); // 简单防重
        }

        if (userReposit.existsByUsername(username)) {
            return AddWithUserResult.fail("USERNAME_EXISTS", "用户名已存在");
        }
        if (telephone != null && !telephone.isEmpty() && userReposit.existsByTelephone(telephone)) {
            return AddWithUserResult.fail("PHONE_EXISTS", "手机号已被注册");
        }
        if (email != null && !email.isEmpty() && userReposit.existsByEmail(email)) {
            return AddWithUserResult.fail("EMAIL_EXISTS", "邮箱已被注册");
        }

        if (password == null || password.isEmpty()) {
            password = "123456";
        }

        try {
            Teacher savedTeacher = teacherReposit.save(teacher);

            User user = new User();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(password));
            user.setTelephone(telephone);
            user.setEmail(email);
            user.setTeacher(savedTeacher);
            user.setUserType(User.UserType.TEACHER);
            User savedUser = userReposit.save(user);
            //savedUser.setPassword(null);
            User responseUser = new User();
            responseUser.setId(savedUser.getId());
            responseUser.setUsername(savedUser.getUsername());
            responseUser.setTelephone(savedUser.getTelephone());
            responseUser.setEmail(savedUser.getEmail());
            responseUser.setUserType(savedUser.getUserType());
            //responseUser.setStudent(savedUser.getStudent());
            return AddWithUserResult.success(savedTeacher, responseUser);
        } catch (Exception e) {
            return AddWithUserResult.fail("CREATE_FAILED", "创建失败: " + e.getMessage());
        }
    }
}
package com.buctta.api.serviceimp;

import com.buctta.api.dao.StudentReposit;
import com.buctta.api.dto.StudentDTO;
import com.buctta.api.entities.Student;
import com.buctta.api.entities.User;
import com.buctta.api.service.StudentService;
import jakarta.annotation.Resource;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class IMPL_StudentService implements StudentService {

    @Resource
    private StudentReposit studentRepository;

    @Override
    public StudentResult addStudent(Student student) {
        // 检查学号是否已存在
        if (studentRepository.existsByStudentNumber(student.getStudentNumber())) {
            return StudentResult.fail("STUDENT_NUMBER_EXISTS", "学号已存在: " + student.getStudentNumber());
        }

        try {
            Student savedStudent = studentRepository.save(student);
            return StudentResult.success(savedStudent, "学生添加成功");
        } catch (Exception e) {
            return StudentResult.fail("SAVE_FAILED", "保存学生失败: " + e.getMessage());
        }
    }

    @Override
    public StudentResult updateStudent(Long id, Student studentDetails) {
        Student existingStudent = studentRepository.findById(id).orElse(null);
        if (existingStudent == null) {
            return StudentResult.fail("STUDENT_NOT_FOUND", "学生不存在，ID: " + id);
        }

        // 检查学号是否重复（排除自己）
        if (studentDetails.getStudentNumber() != null &&
                !studentDetails.getStudentNumber().equals(existingStudent.getStudentNumber())) {
            Optional<Student> studentWithSameNumber =
                    studentRepository.findByStudentNumber(studentDetails.getStudentNumber());
            if (studentWithSameNumber.isPresent() &&
                    !studentWithSameNumber.get().getId().equals(id)) {
                return StudentResult.fail("STUDENT_NUMBER_EXISTS", "学号已存在: " +
                        studentDetails.getStudentNumber());
            }
        }

        // 更新字段
        if (studentDetails.getStudentNumber() != null) {
            existingStudent.setStudentNumber(studentDetails.getStudentNumber());
        }
        if (studentDetails.getName() != null) {
            existingStudent.setName(studentDetails.getName());
        }
        if (studentDetails.getClassName() != null) {
            existingStudent.setClassName(studentDetails.getClassName());
        }
        if (studentDetails.getGender() != null) {
            existingStudent.setGender(studentDetails.getGender());
        }
        if (studentDetails.getAdmissionDate() != null) {
            existingStudent.setAdmissionDate(studentDetails.getAdmissionDate());
        }
        try {
            Student updatedStudent = studentRepository.save(existingStudent);
            return StudentResult.success(updatedStudent, "学生信息更新成功");
        } catch (Exception e) {
            return StudentResult.fail("UPDATE_FAILED", "更新学生失败: " + e.getMessage());
        }
    }

    @Override
    public Student getStudentById(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("学生不存在，ID: " + id));
    }

    @Override
    public Student getStudentByNumber(String studentNumber) {
        return studentRepository.findByStudentNumber(studentNumber)
                .orElseThrow(() -> new RuntimeException("学生不存在，学号: " + studentNumber));
    }

    @Override
    public StudentResult deleteStudent(Long id) {
        if (!studentRepository.existsById(id)) {
            return StudentResult.fail("STUDENT_NOT_FOUND", "学生不存在，ID: " + id);
        }
        try {
            studentRepository.deleteById(id);
            return StudentResult.success(null, "学生删除成功");
        } catch (Exception e) {
            return StudentResult.fail("DELETE_FAILED", "删除学生失败: " + e.getMessage());
        }
    }

    // 构建 Specification，将 telephone、email 改为通过 User 表过滤
    private Specification<Student> buildStudentSpec(
            String name, String studentNumber, String className, String gender,
            String telephone, String email) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 学生自身字段
            if (name != null && !name.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(root.get("name"), "%" + name + "%"));
            }
            if (studentNumber != null && !studentNumber.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("studentNumber"), studentNumber));
            }
            if (className != null && !className.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(root.get("className"), "%" + className + "%"));
            }
            if (gender != null && !gender.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("gender"), gender));
            }

            // 关联 User 字段（telephone, email）
            boolean hasUserCondition = (telephone != null && !telephone.trim().isEmpty()) ||
                    (email != null && !email.trim().isEmpty());
            if (hasUserCondition) {
                var userJoin = root.join("user", JoinType.LEFT);
                if (telephone != null && !telephone.trim().isEmpty()) {
                    predicates.add(criteriaBuilder.like(userJoin.get("telephone"), "%" + telephone + "%"));
                }
                if (email != null && !email.trim().isEmpty()) {
                    predicates.add(criteriaBuilder.like(userJoin.get("email"), "%" + email + "%"));
                }
                query.distinct(true);
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private StudentDTO convertToDTO(Student student) {
        StudentDTO dto = new StudentDTO();
        dto.setId(student.getId());
        dto.setStudentNumber(student.getStudentNumber());
        dto.setName(student.getName());
        dto.setClassName(student.getClassName());
        dto.setGender(student.getGender());
        dto.setAdmissionDate(student.getAdmissionDate());

        User user = student.getUser();
        if (user != null) {
            dto.setUsername(user.getUsername());
            dto.setTelephone(user.getTelephone());
            dto.setEmail(user.getEmail());
            dto.setUserType(user.getUserType() != null ? user.getUserType().name() : null);
        }
        return dto;
    }

    @Override
    public Page<StudentDTO> searchStudents(
            String name, String studentNumber, String className, String gender,
            String telephone, String email, Pageable pageable) {
        Specification<Student> spec = buildStudentSpec(name, studentNumber, className, gender, telephone, email);
        Page<Student> studentPage = studentRepository.findAll(spec, pageable);
        return studentPage.map(this::convertToDTO);
    }

    @Override
    public List<Student> searchStudentsBySpec(
            String name, String studentNumber, String className, String gender,
            String telephone, String email) {
        Specification<Student> spec = buildStudentSpec(name, studentNumber, className, gender, telephone, email);
        return studentRepository.findAll(spec);
    }

    @Override
    public Page<Student> getAllStudents(Pageable pageable) {
        return studentRepository.findAll(pageable);
    }

    @Override
    public List<Student> getStudentsByClass(String className) {
        return studentRepository.findByClassName(className);
    }

    @Override
    public boolean isStudentNumberExists(String studentNumber) {
        return studentRepository.existsByStudentNumber(studentNumber);
    }

    @Override
    public List<Student> getStudentsByAdmissionYear(Integer year) {
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);
        Specification<Student> specification = (root, query, criteriaBuilder) ->
                criteriaBuilder.between(root.get("admissionDate"), startDate, endDate);
        return studentRepository.findAll(specification);
    }
    @Override
    public StudentResult deleteStudents(List<Long> ids) {
        try {
            if (ids == null || ids.isEmpty()) {
                return StudentResult.fail("INVALID_IDS", "课程ID列表不能为空");
            }
            studentRepository.deleteAllByIdIn(ids);
            return StudentResult.success(null, "批量删除成功");
        } catch (Exception e) {
            return StudentResult.fail("DELETE_FAILED", "批量删除失败: " + e.getMessage());
        }
    }

    @Override
    public byte[] exportStudentsToExcel(List<Student> students) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("学生名单");
        // 表头
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("ID");
        header.createCell(1).setCellValue("姓名");
        header.createCell(2).setCellValue("学号");
        header.createCell(3).setCellValue("班级");
        header.createCell(4).setCellValue("性别");
        header.createCell(5).setCellValue("用户名");
        header.createCell(6).setCellValue("电话");
        header.createCell(7).setCellValue("邮箱");
        header.createCell(8).setCellValue("用户类型");

        int rowIdx = 1;
        for (Student s : students) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(s.getId());
            row.createCell(1).setCellValue(s.getName());
            row.createCell(2).setCellValue(s.getStudentNumber());
            row.createCell(3).setCellValue(s.getClassName());
            row.createCell(4).setCellValue(s.getGender());

            User user = s.getUser();
            row.createCell(5).setCellValue(user != null ? user.getUsername() : "");
            row.createCell(6).setCellValue(user != null ? user.getTelephone() : "");
            row.createCell(7).setCellValue(user != null ? user.getEmail() : "");
            row.createCell(8).setCellValue(user != null && user.getUserType() != null
                    ? user.getUserType().name() : "");
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        workbook.write(bos);
        workbook.close();
        return bos.toByteArray();
    }

    @Override
    public List<Student> getAllStudentsForExport() {
        return studentRepository.findAll();
    }
}
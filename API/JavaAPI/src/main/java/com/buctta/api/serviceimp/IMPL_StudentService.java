package com.buctta.api.serviceimp;

import com.buctta.api.dao.StudentReposit;
import com.buctta.api.entities.Student;
import com.buctta.api.service.StudentService;
import jakarta.annotation.Resource;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

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
        }
        catch (Exception e) {
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
            Optional<Student> studentWithSameNumber = studentRepository.findByStudentNumber(studentDetails.getStudentNumber());
            if (studentWithSameNumber.isPresent() && !studentWithSameNumber.get().getId().equals(id)) {
                return StudentResult.fail("STUDENT_NUMBER_EXISTS", "学号已存在: " + studentDetails.getStudentNumber());
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
        }
        catch (Exception e) {
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
        }
        catch (Exception e) {
            return StudentResult.fail("DELETE_FAILED", "删除学生失败: " + e.getMessage());
        }
    }

    @Override
    public Page<Student> searchStudents(String name, String studentNumber, String className,
                                        String gender, String telephone, String email,
                                        Pageable pageable) {
        Specification<Student> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

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

            if (telephone != null && !telephone.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(root.get("telephone"), "%" + telephone + "%"));
            }

            if (email != null && !email.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(root.get("email"), "%" + email + "%"));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        return studentRepository.findAll(specification, pageable);
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
}
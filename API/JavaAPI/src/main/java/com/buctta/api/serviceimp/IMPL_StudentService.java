package com.buctta.api.serviceimp;

import com.buctta.api.entities.Student;
import com.buctta.api.reposit.StudentRepository;
import com.buctta.api.service.StudentService;
import jakarta.annotation.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class IMPL_StudentService implements StudentService {

    @Resource
    private StudentRepository studentRepository;

    @Override
    public Student addStudent(Student student) {
        // 检查学号是否已存在
        if (studentRepository.existsByStudentNumber(student.getStudentNumber())) {
            throw new RuntimeException("学号已存在: " + student.getStudentNumber());
        }

        // 检查邮箱是否已存在
        if (student.getEmail() != null && !student.getEmail().trim().isEmpty()) {
            Optional<Student> studentWithSameEmail = studentRepository.findByEmail(student.getEmail());
            if (studentWithSameEmail.isPresent()) {
                throw new RuntimeException("邮箱已存在: " + student.getEmail());
            }
        }

        // 检查电话号码是否已存在
        if (student.getTelephone() != null && !student.getTelephone().trim().isEmpty()) {
            Optional<Student> studentWithSamePhone = studentRepository.findByTelephone(student.getTelephone());
            if (studentWithSamePhone.isPresent()) {
                throw new RuntimeException("电话号码已存在: " + student.getTelephone());
            }
        }

        return studentRepository.save(student);
    }

    @Override
    public Student updateStudent(Long id, Student studentDetails) {
        Student existingStudent = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("学生不存在，ID: " + id));

        // 检查学号是否重复（排除自己）
        if (studentDetails.getStudentNumber() != null &&
                !studentDetails.getStudentNumber().equals(existingStudent.getStudentNumber())) {
            Optional<Student> studentWithSameNumber = studentRepository.findByStudentNumber(studentDetails.getStudentNumber());
            if (studentWithSameNumber.isPresent() && !studentWithSameNumber.get().getId().equals(id)) {
                throw new RuntimeException("学号已存在: " + studentDetails.getStudentNumber());
            }
        }

        // 检查邮箱是否重复（排除自己）
        if (studentDetails.getEmail() != null &&
                !studentDetails.getEmail().equals(existingStudent.getEmail())) {
            Optional<Student> studentWithSameEmail = studentRepository.findByEmail(studentDetails.getEmail());
            if (studentWithSameEmail.isPresent() && !studentWithSameEmail.get().getId().equals(id)) {
                throw new RuntimeException("邮箱已存在: " + studentDetails.getEmail());
            }
        }

        // 检查电话号码是否重复（排除自己）
        if (studentDetails.getTelephone() != null &&
                !studentDetails.getTelephone().equals(existingStudent.getTelephone())) {
            Optional<Student> studentWithSamePhone = studentRepository.findByTelephone(studentDetails.getTelephone());
            if (studentWithSamePhone.isPresent() && !studentWithSamePhone.get().getId().equals(id)) {
                throw new RuntimeException("电话号码已存在: " + studentDetails.getTelephone());
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
        if (studentDetails.getTelephone() != null) {
            existingStudent.setTelephone(studentDetails.getTelephone());
        }
        if (studentDetails.getEmail() != null) {
            existingStudent.setEmail(studentDetails.getEmail());
        }
        if (studentDetails.getAdmissionDate() != null) {
            existingStudent.setAdmissionDate(studentDetails.getAdmissionDate());
        }

        return studentRepository.save(existingStudent);
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
    public void deleteStudent(Long id) {
        if (!studentRepository.existsById(id)) {
            throw new RuntimeException("学生不存在，ID: " + id);
        }
        studentRepository.deleteById(id);
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
        // 获取该年的起始和结束日期
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);

        // 创建Specification来查询该年份入学的学生
        Specification<Student> specification = (root, query, criteriaBuilder) -> {
            return criteriaBuilder.between(root.get("admissionDate"), startDate, endDate);
        };

        return studentRepository.findAll(specification);
    }
}
package com.buctta.api.serviceimp;

import com.buctta.api.dao.StudentReposit;
import com.buctta.api.entities.Student;
import com.buctta.api.service.StudentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IMPL_StudentServiceTest {

    @Mock
    private StudentReposit studentRepository;

    @InjectMocks
    private IMPL_StudentService studentService;

    private Student alice;

    @BeforeEach
    void setUp() {
        alice = new Student();
        alice.setId(1L);
        alice.setStudentNumber("S001");
        alice.setName("Alice");
        alice.setClassName("Class A");
        alice.setGender("Female");
        alice.setAdmissionDate(LocalDate.of(2022, 9, 1));
    }

    // ─── addStudent ──────────────────────────────────────────────────────────

    @Test
    void addStudent_newNumber_returnsSuccess() {
        when(studentRepository.existsByStudentNumber("S001")).thenReturn(false);
        when(studentRepository.save(alice)).thenReturn(alice);

        StudentService.StudentResult result = studentService.addStudent(alice);

        assertThat(result.success()).isTrue();
        assertThat(result.student()).isEqualTo(alice);
    }

    @Test
    void addStudent_existingNumber_returnsFail() {
        when(studentRepository.existsByStudentNumber("S001")).thenReturn(true);

        StudentService.StudentResult result = studentService.addStudent(alice);

        assertThat(result.success()).isFalse();
        assertThat(result.errorCode()).isEqualTo("STUDENT_NUMBER_EXISTS");
        verify(studentRepository, never()).save(any());
    }

    // ─── updateStudent ───────────────────────────────────────────────────────

    @Test
    void updateStudent_studentExists_updatesAndReturnsSuccess() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(studentRepository.save(alice)).thenReturn(alice);

        Student updates = new Student();
        updates.setName("Alice Updated");

        StudentService.StudentResult result = studentService.updateStudent(1L, updates);

        assertThat(result.success()).isTrue();
        assertThat(result.student().getName()).isEqualTo("Alice Updated");
    }

    @Test
    void updateStudent_studentNotFound_returnsFail() {
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());

        StudentService.StudentResult result = studentService.updateStudent(99L, new Student());

        assertThat(result.success()).isFalse();
        assertThat(result.errorCode()).isEqualTo("STUDENT_NOT_FOUND");
    }

    @Test
    void updateStudent_duplicateStudentNumber_returnsFail() {
        Student bob = new Student();
        bob.setId(2L);
        bob.setStudentNumber("S002");

        when(studentRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(studentRepository.findByStudentNumber("S002")).thenReturn(Optional.of(bob));

        Student updates = new Student();
        updates.setStudentNumber("S002");

        StudentService.StudentResult result = studentService.updateStudent(1L, updates);

        assertThat(result.success()).isFalse();
        assertThat(result.errorCode()).isEqualTo("STUDENT_NUMBER_EXISTS");
    }

    // ─── getStudentById ──────────────────────────────────────────────────────

    @Test
    void getStudentById_found_returnsStudent() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(alice));

        Student result = studentService.getStudentById(1L);

        assertThat(result).isEqualTo(alice);
    }

    @Test
    void getStudentById_notFound_throwsException() {
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.getStudentById(99L))
                .isInstanceOf(RuntimeException.class);
    }

    // ─── getStudentByNumber ───────────────────────────────────────────────────

    @Test
    void getStudentByNumber_found_returnsStudent() {
        when(studentRepository.findByStudentNumber("S001")).thenReturn(Optional.of(alice));

        Student result = studentService.getStudentByNumber("S001");

        assertThat(result).isEqualTo(alice);
    }

    @Test
    void getStudentByNumber_notFound_throwsException() {
        when(studentRepository.findByStudentNumber("NONE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.getStudentByNumber("NONE"))
                .isInstanceOf(RuntimeException.class);
    }

    // ─── deleteStudent ───────────────────────────────────────────────────────

    @Test
    void deleteStudent_exists_returnsSuccess() {
        when(studentRepository.existsById(1L)).thenReturn(true);

        StudentService.StudentResult result = studentService.deleteStudent(1L);

        assertThat(result.success()).isTrue();
        verify(studentRepository).deleteById(1L);
    }

    @Test
    void deleteStudent_notFound_returnsFail() {
        when(studentRepository.existsById(99L)).thenReturn(false);

        StudentService.StudentResult result = studentService.deleteStudent(99L);

        assertThat(result.success()).isFalse();
        assertThat(result.errorCode()).isEqualTo("STUDENT_NOT_FOUND");
        verify(studentRepository, never()).deleteById(any());
    }

    // ─── searchStudents ───────────────────────────────────────────────────────

    @Test
    void searchStudents_returnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Student> page = new PageImpl<>(List.of(alice), pageable, 1);
        when(studentRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        Page<Student> result = studentService.searchStudents(
                "Alice", null, null, null, null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(alice);
    }

    // ─── isStudentNumberExists ────────────────────────────────────────────────

    @Test
    void isStudentNumberExists_exists_returnsTrue() {
        when(studentRepository.existsByStudentNumber("S001")).thenReturn(true);

        assertThat(studentService.isStudentNumberExists("S001")).isTrue();
    }

    @Test
    void isStudentNumberExists_notExists_returnsFalse() {
        when(studentRepository.existsByStudentNumber("NONE")).thenReturn(false);

        assertThat(studentService.isStudentNumberExists("NONE")).isFalse();
    }

    // ─── getStudentsByClass ───────────────────────────────────────────────────

    @Test
    void getStudentsByClass_returnsListOfStudents() {
        when(studentRepository.findByClassName("Class A")).thenReturn(List.of(alice));

        List<Student> result = studentService.getStudentsByClass("Class A");

        assertThat(result).containsExactly(alice);
    }
}

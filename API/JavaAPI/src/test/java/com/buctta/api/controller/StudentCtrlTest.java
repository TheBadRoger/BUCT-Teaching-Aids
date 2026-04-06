package com.buctta.api.controller;

import com.buctta.api.entities.Student;
import com.buctta.api.service.StudentService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class StudentCtrlTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private StudentService studentService;

    @InjectMocks
    private StudentCtrl studentCtrl;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(studentCtrl)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    // ─── /add ────────────────────────────────────────────────────────────────

    @Test
    void addStudent_success_returns200WithStudent() throws Exception {
        Student student = new Student();
        student.setId(1L);
        student.setStudentNumber("S001");
        student.setName("Alice");

        when(studentService.addStudent(any(Student.class)))
                .thenReturn(StudentService.StudentResult.success(student));

        mockMvc.perform(post("/api/students/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.studentNumber").value("S001"));
    }

    @Test
    void addStudent_existingStudentNumber_returns4091() throws Exception {
        Student student = new Student();
        student.setStudentNumber("S001");

        when(studentService.addStudent(any(Student.class)))
                .thenReturn(StudentService.StudentResult.fail("STUDENT_NUMBER_EXISTS", "学号已存在"));

        mockMvc.perform(post("/api/students/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4091));
    }

    // ─── /search ─────────────────────────────────────────────────────────────

    @Test
    void searchStudents_noFilters_returnsPage() throws Exception {
        Student student = new Student();
        student.setId(1L);
        student.setName("Alice");
        Page<Student> page = new PageImpl<>(List.of(student), PageRequest.of(0, 10), 1);

        when(studentService.searchStudents(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/students/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.content[0].name").value("Alice"));
    }

    @Test
    void searchStudents_withNameFilter_returnsFilteredPage() throws Exception {
        Student student = new Student();
        student.setId(1L);
        student.setName("Alice");
        Page<Student> page = new PageImpl<>(List.of(student), PageRequest.of(0, 10), 1);

        when(studentService.searchStudents(
                any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/students/search")
                        .param("name", "Alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }
}

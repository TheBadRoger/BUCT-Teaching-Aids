package com.buctta.api.controller;

import com.buctta.api.dto.BindingRequest;
import com.buctta.api.entities.Student;
import com.buctta.api.entities.User;
import com.buctta.api.service.UserBindingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.Collections;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserBindingCtrlTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private UserBindingService userBindingService;

    @InjectMocks
    private UserBindingCtrl userBindingCtrl;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userBindingCtrl).build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void bind_withoutLogin_returns4013() throws Exception {
        BindingRequest request = new BindingRequest();
        request.setBindingType(BindingRequest.BindingType.STUDENT);
        request.setName("Alice");
        request.setIdCard("110101199901010000");
        request.setStudentNumber("S001");

        mockMvc.perform(post("/api/user/binding/bind")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4013));
    }

    @Test
    void bind_studentMissingStudentNumber_returns4001() throws Exception {
        setCurrentUser(buildUser(1L, "alice", User.UserType.STUDENT));

        BindingRequest request = new BindingRequest();
        request.setBindingType(BindingRequest.BindingType.STUDENT);
        request.setName("Alice");
        request.setIdCard("110101199901010000");

        mockMvc.perform(post("/api/user/binding/bind")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4001));
    }

    @Test
    void bind_studentSuccess_returns200WithBindingInfo() throws Exception {
        User current = buildUser(1L, "alice", User.UserType.STUDENT);
        setCurrentUser(current);

        Student student = new Student();
        student.setId(10L);
        student.setStudentNumber("S001");
        student.setName("Alice");
        student.setClassName("Class1");
        student.setGender("F");
        student.setAdmissionDate(LocalDate.of(2023, 9, 1));

        BindingRequest request = new BindingRequest();
        request.setBindingType(BindingRequest.BindingType.STUDENT);
        request.setName("Alice");
        request.setIdCard("110101199901010000");
        request.setStudentNumber("S001");

        when(userBindingService.bindStudent(1L, "Alice", "110101199901010000", "S001"))
                .thenReturn(UserBindingService.BindingResult.successStudent(current, student));

        mockMvc.perform(post("/api/user/binding/bind")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.studentInfo.studentNumber").value("S001"));

        verify(userBindingService).bindStudent(1L, "Alice", "110101199901010000", "S001");
    }

    @Test
    void unbind_notBound_returns4097() throws Exception {
        setCurrentUser(buildUser(3L, "bob", User.UserType.TEACHER));
        when(userBindingService.unbind(3L))
                .thenReturn(UserBindingService.BindingResult.fail("NOT_BOUND", "not bound"));

        mockMvc.perform(post("/api/user/binding/unbind"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4097));
    }

    @Test
    void getBindingInfo_withLogin_returns200() throws Exception {
        User current = buildUser(5L, "charlie", User.UserType.STUDENT);
        setCurrentUser(current);

        Student student = new Student();
        student.setId(11L);
        student.setStudentNumber("S002");
        student.setName("Charlie");

        when(userBindingService.getBoundStudent(5L)).thenReturn(student);

        mockMvc.perform(get("/api/user/binding/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.username").value("charlie"))
                .andExpect(jsonPath("$.data.studentInfo.studentNumber").value("S002"));
    }

    private static User buildUser(Long id, String username, User.UserType userType) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setUserType(userType);
        return user;
    }

    private static void setCurrentUser(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList())
        );
    }
}


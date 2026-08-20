package com.example.ems.common;

import com.example.ems.auth.entity.Role;
import com.example.ems.auth.entity.RoleName;
import com.example.ems.auth.entity.User;
import com.example.ems.auth.repository.RoleRepository;
import com.example.ems.auth.repository.UserRepository;
import com.example.ems.department.entity.Department;
import com.example.ems.department.repository.DepartmentRepository;
import com.example.ems.employee.entity.Employee;
import com.example.ems.employee.entity.EmployeeStatus;
import com.example.ems.employee.repository.EmployeeRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class IntegrationTestBase {

    protected static final String DEFAULT_PASSWORD = "Password123!";

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    protected UserRepository userRepository;
    @Autowired
    protected RoleRepository roleRepository;
    @Autowired
    protected PasswordEncoder passwordEncoder;
    @Autowired
    protected EmployeeRepository employeeRepository;
    @Autowired
    protected DepartmentRepository departmentRepository;

    protected User createUser(String email, RoleName roleName) {
        Role role = roleRepository.findByName(roleName).orElseThrow();
        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(DEFAULT_PASSWORD))
                .firstName("Test")
                .lastName("User")
                .enabled(true)
                .roles(Set.of(role))
                .build();
        return userRepository.save(user);
    }

    protected Employee createEmployee(User user, Department department, EmployeeStatus status, String employeeNumber) {
        Employee employee = Employee.builder()
                .employeeNumber(employeeNumber)
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .hireDate(LocalDate.now().minusYears(1))
                .jobTitle("Engineer")
                .salary(BigDecimal.valueOf(60000))
                .status(status)
                .department(department)
                .user(user)
                .build();
        return employeeRepository.save(employee);
    }

    protected Department createDepartment(String name) {
        return departmentRepository.save(Department.builder().name(name).build());
    }

    protected String loginAndGetToken(String email) throws Exception {
        String body = objectMapper.writeValueAsString(new LoginPayload(email, DEFAULT_PASSWORD));
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(response);
        return node.get("data").get("accessToken").asText();
    }

    protected record LoginPayload(String email, String password) {
    }
}

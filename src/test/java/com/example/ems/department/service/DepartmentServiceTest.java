package com.example.ems.department.service;

import com.example.ems.common.exception.DuplicateResourceException;
import com.example.ems.common.exception.ResourceNotFoundException;
import com.example.ems.department.dto.DepartmentCreateRequest;
import com.example.ems.department.dto.DepartmentResponse;
import com.example.ems.department.dto.DepartmentUpdateRequest;
import com.example.ems.department.entity.Department;
import com.example.ems.department.repository.DepartmentRepository;
import com.example.ems.employee.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private DepartmentService departmentService;

    @Test
    void create_savesDepartment_whenNameIsUnique() {
        DepartmentCreateRequest request = new DepartmentCreateRequest("Engineering", "Builds the product", null);
        when(departmentRepository.existsByNameIgnoreCase("Engineering")).thenReturn(false);
        when(departmentRepository.save(any(Department.class))).thenAnswer(invocation -> {
            Department department = invocation.getArgument(0);
            department.setId(UUID.randomUUID());
            return department;
        });

        DepartmentResponse response = departmentService.create(request);

        assertThat(response.name()).isEqualTo("Engineering");
        verify(departmentRepository).save(any(Department.class));
    }

    @Test
    void create_throwsDuplicateResourceException_whenNameAlreadyExists() {
        DepartmentCreateRequest request = new DepartmentCreateRequest("Engineering", null, null);
        when(departmentRepository.existsByNameIgnoreCase("Engineering")).thenReturn(true);

        assertThatThrownBy(() -> departmentService.create(request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(departmentRepository, never()).save(any());
    }

    @Test
    void findById_throwsResourceNotFoundException_whenDepartmentMissing() {
        UUID id = UUID.randomUUID();
        when(departmentRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> departmentService.findById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_throwsDuplicateResourceException_whenRenamingToExistingName() {
        UUID id = UUID.randomUUID();
        Department existing = Department.builder().id(id).name("Old Name").build();
        DepartmentUpdateRequest request = new DepartmentUpdateRequest("Taken Name", null, null);

        when(departmentRepository.findById(id)).thenReturn(Optional.of(existing));
        when(departmentRepository.existsByNameIgnoreCaseAndIdNot("Taken Name", id)).thenReturn(true);

        assertThatThrownBy(() -> departmentService.update(id, request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void delete_removesDepartment_whenItExists() {
        UUID id = UUID.randomUUID();
        Department existing = Department.builder().id(id).name("Engineering").build();
        when(departmentRepository.findById(id)).thenReturn(Optional.of(existing));

        departmentService.delete(id);

        verify(departmentRepository).delete(existing);
    }
}

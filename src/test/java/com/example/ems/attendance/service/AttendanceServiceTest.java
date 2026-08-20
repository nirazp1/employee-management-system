package com.example.ems.attendance.service;

import com.example.ems.attendance.dto.AttendanceResponse;
import com.example.ems.attendance.entity.Attendance;
import com.example.ems.attendance.entity.AttendanceStatus;
import com.example.ems.attendance.repository.AttendanceRepository;
import com.example.ems.common.exception.InvalidOperationException;
import com.example.ems.employee.entity.Employee;
import com.example.ems.employee.entity.EmployeeStatus;
import com.example.ems.employee.service.EmployeeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private AttendanceRepository attendanceRepository;
    @Mock
    private EmployeeService employeeService;

    @InjectMocks
    private AttendanceService attendanceService;

    private Employee activeEmployee() {
        return Employee.builder().id(UUID.randomUUID()).firstName("Jane").lastName("Doe")
                .status(EmployeeStatus.ACTIVE).build();
    }

    @Test
    void checkIn_throwsInvalidOperationException_whenEmployeeIsTerminated() {
        Employee employee = activeEmployee();
        employee.setStatus(EmployeeStatus.TERMINATED);
        when(employeeService.getCurrentEmployee()).thenReturn(employee);

        assertThatThrownBy(() -> attendanceService.checkIn())
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Terminated or inactive");

        verify(attendanceRepository, never()).save(any());
    }

    @Test
    void checkIn_throwsInvalidOperationException_whenAlreadyCheckedInToday() {
        Employee employee = activeEmployee();
        when(employeeService.getCurrentEmployee()).thenReturn(employee);
        when(attendanceRepository.findByEmployee_IdAndDate(employee.getId(), LocalDate.now()))
                .thenReturn(Optional.of(new Attendance()));

        assertThatThrownBy(() -> attendanceService.checkIn())
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("already checked in");
    }

    @Test
    void checkIn_createsAttendanceRecord_whenEmployeeIsActiveAndHasNotCheckedInToday() {
        Employee employee = activeEmployee();
        when(employeeService.getCurrentEmployee()).thenReturn(employee);
        when(attendanceRepository.findByEmployee_IdAndDate(employee.getId(), LocalDate.now())).thenReturn(Optional.empty());
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(invocation -> {
            Attendance attendance = invocation.getArgument(0);
            attendance.setId(UUID.randomUUID());
            return attendance;
        });

        AttendanceResponse response = attendanceService.checkIn();

        assertThat(response.checkIn()).isNotNull();
        assertThat(response.status()).isIn(AttendanceStatus.PRESENT, AttendanceStatus.LATE);
    }

    @Test
    void checkOut_throwsInvalidOperationException_whenNoCheckInExists() {
        Employee employee = activeEmployee();
        when(employeeService.getCurrentEmployee()).thenReturn(employee);
        when(attendanceRepository.findByEmployee_IdAndDate(employee.getId(), LocalDate.now())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> attendanceService.checkOut())
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("must check in");
    }

    @Test
    void checkOut_throwsInvalidOperationException_whenAlreadyCheckedOut() {
        Employee employee = activeEmployee();
        Attendance attendance = Attendance.builder()
                .id(UUID.randomUUID())
                .employee(employee)
                .date(LocalDate.now())
                .checkIn(LocalDateTime.now().minusHours(2))
                .checkOut(LocalDateTime.now().minusMinutes(30))
                .status(AttendanceStatus.PRESENT)
                .build();

        when(employeeService.getCurrentEmployee()).thenReturn(employee);
        when(attendanceRepository.findByEmployee_IdAndDate(employee.getId(), LocalDate.now())).thenReturn(Optional.of(attendance));

        assertThatThrownBy(() -> attendanceService.checkOut())
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("already checked out");
    }

    @Test
    void checkOut_marksHalfDay_whenDurationIsUnderFourHours() {
        Employee employee = activeEmployee();
        Attendance attendance = Attendance.builder()
                .id(UUID.randomUUID())
                .employee(employee)
                .date(LocalDate.now())
                .checkIn(LocalDateTime.now().minusHours(2))
                .status(AttendanceStatus.PRESENT)
                .build();

        when(employeeService.getCurrentEmployee()).thenReturn(employee);
        when(attendanceRepository.findByEmployee_IdAndDate(employee.getId(), LocalDate.now())).thenReturn(Optional.of(attendance));
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AttendanceResponse response = attendanceService.checkOut();

        assertThat(response.status()).isEqualTo(AttendanceStatus.HALF_DAY);
        assertThat(response.checkOut()).isNotNull();
    }
}

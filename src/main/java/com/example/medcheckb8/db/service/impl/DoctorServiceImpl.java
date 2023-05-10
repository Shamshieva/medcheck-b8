package com.example.medcheckb8.db.service.impl;

import com.example.medcheckb8.db.dto.request.DoctorSaveRequest;
import com.example.medcheckb8.db.dto.request.DoctorUpdateRequest;
import com.example.medcheckb8.db.dto.response.DoctorExportResponse;
import com.example.medcheckb8.db.dto.response.DoctorResponse;
import com.example.medcheckb8.db.dto.response.ExpertResponse;
import com.example.medcheckb8.db.dto.response.SimpleResponse;
import com.example.medcheckb8.db.entities.Department;
import com.example.medcheckb8.db.entities.Doctor;
import com.example.medcheckb8.db.exceptions.NotFountException;
import com.example.medcheckb8.db.repository.DepartmentRepository;
import com.example.medcheckb8.db.repository.DoctorRepository;
import com.example.medcheckb8.db.service.DoctorService;
import com.example.medcheckb8.db.utill.ExportToExcel;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@Service
@Transactional
@RequiredArgsConstructor
public class DoctorServiceImpl implements DoctorService {
    private final DoctorRepository doctorRepository;
    private final DepartmentRepository departmentRepository;
    private final JdbcTemplate jdbcTemplate;
    private static final Logger logger = Logger.getLogger(Doctor.class.getName());

    @Override
    public SimpleResponse save(DoctorSaveRequest request) {
        Department department = departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> new NotFountException(
                        String.format("Department with id: %d not found", request.departmentId())
                ));

        Doctor doctor = Doctor.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .position(request.position())
                .image(request.image())
                .department(department)
                .description(request.description())
                .isActive(false)
                .build();

        department.addDoctor(doctor);
        doctorRepository.save(doctor);
        logger.info("Saved doctor with full name: {} {}" + doctor.getFirstName() + doctor.getLastName());


        return SimpleResponse.builder()
                .status(HttpStatus.OK)
                .message(String.format("Doctor with full name: %s %s Successfully saved",
                        doctor.getFirstName(), doctor.getLastName()))
                .build();
    }

    @Override
    public DoctorResponse findById(Long id) {
        logger.info("Finding doctor with ID: {}" + id);
        return doctorRepository.findByDoctorId(id).orElseThrow(() -> new NotFountException(
                String.format("Doctor with id: %d doesn't exist", id)
        ));
    }

    @Override
    public List<ExpertResponse> getAllWithSearchExperts(String keyWord) {
        return doctorRepository.getAllWithSearch(keyWord);
    }

    @Override
    public SimpleResponse update(DoctorUpdateRequest request) {
        try {
            Doctor doctor = doctorRepository.findById(request.doctorId()).orElseThrow(() -> new NotFountException(
                    String.format("Doctor with id: %d not found.", request.doctorId())
            ));

            doctor.setFirstName(request.firstName());
            doctor.setLastName(request.lastName());
            doctor.setImage(request.image());
            doctor.setDescription(request.description());

            doctorRepository.save(doctor);
            logger.info(String.format("Doctor with id: %d Successfully updated.", doctor.getId()));

            return SimpleResponse.builder()
                    .status(HttpStatus.OK)
                    .message(String.format("Doctor with id: %d Successfully updated.", doctor.getId()))
                    .build();
        } catch (Exception e) {
            logger.severe("Error updating doctor: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public SimpleResponse delete(Long id) {
        logger.info(String.format("Attempting to delete doctor with id: %d", id));
        Doctor doctor = doctorRepository.findById(id).orElseThrow(() -> new NotFountException(
                String.format("Doctor with id: %d not found.", id)
        ));
        doctorRepository.delete(doctor);
        logger.info(String.format("Doctor with id: %d successfully deleted", id));
        return SimpleResponse.builder()
                .status(HttpStatus.OK)
                .message(String.format("Doctor with id: %d Successfully deleted.", id))
                .build();
    }

    @Override
    public SimpleResponse activateAndDeactivateDoctor(Boolean isActive, Long doctorId) {
        logger.info(String.format("Activating/deactivating doctor with id %s", doctorId));
        Doctor doctor = doctorRepository.findById(doctorId).orElseThrow(() -> new NotFountException(
                String.format("Doctor with id: %d not found.", doctorId)
        ));

        doctor.setIsActive(isActive);
        doctorRepository.save(doctor);

        if (!isActive) {
            logger.info(String.format("Doctor with id %s has been deactivated", doctorId));
            return SimpleResponse.builder()
                    .status(HttpStatus.OK)
                    .message(String.format("Doctor with id: %s is deactivated!", doctor.getId()))
                    .build();
        }
        logger.info(String.format("Doctor with id %s has been activated", doctorId));
        return SimpleResponse.builder()
                .status(HttpStatus.OK)
                .message(String.format("Doctor with id: %s is activated!", doctor.getId()))
                .build();
    }

    @Override
    public List<DoctorExportResponse> exportDoctorToExcel(HttpServletResponse response) throws IOException {
        Instant start = Instant.now();
        String sql = """
                SELECT d.id as doctorId,
                d.first_name as firstName,
                d.last_name as lastName,
                d.position as position,
                sh.data_of_start as dataOfStart,
                sh.data_of_finish as dataOfFinish,
                sdt.date as data,
                sdt.time_from as timeFrom,
                sdt.time_to as timeTo,
                sdt.is_busy as isBusy
                FROM doctors d
                JOIN schedules sh ON d.id = sh.doctor_id
                JOIN schedule_date_and_times sdt ON sh.id = sdt.schedule_id
                """;
        try {
            List<DoctorExportResponse> doctors = jdbcTemplate.query(sql, (resultSet, i) -> {
                Map<LocalTime, LocalTime> times = new HashMap<>();
                times.put(resultSet.getTime("timeFrom").toLocalTime(),
                        resultSet.getTime("timeTo").toLocalTime());

                return new DoctorExportResponse(
                        resultSet.getLong("doctorId"),
                        resultSet.getString("firstName"),
                        resultSet.getString("lastName"),
                        resultSet.getString("position"),
                        resultSet.getDate("dataOfStart").toLocalDate(),
                        resultSet.getDate("dataOfFinish").toLocalDate(),
                        resultSet.getDate("data").toLocalDate(),
                        times,
                        resultSet.getBoolean("isBusy"));
            });
            ExportToExcel exportToExcel = new ExportToExcel(doctors);
            exportToExcel.exportDataToExcel(response);
            logger.info(String.format("Successfully exported %d doctors to Excel. Execution time: %d ms", doctors.size(), Duration.between(start, Instant.now()).toMillis()));
        } catch (Exception e) {
            logger.severe("Error exporting doctors to Excel: " + e.getMessage());
            throw e;
        }
        return null;
    }

}

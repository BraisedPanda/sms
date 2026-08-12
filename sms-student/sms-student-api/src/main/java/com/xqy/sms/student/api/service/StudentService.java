package com.xqy.sms.student.api.service;

import com.xqy.sms.student.api.entity.StudentDTO;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public interface StudentService {

    boolean saveStudent(StudentDTO student);

    boolean updateStudent(StudentDTO student);

    boolean deleteStudent(Long id);

    StudentDTO getStudentById(Long id);

    List<StudentDTO> listStudents();

}

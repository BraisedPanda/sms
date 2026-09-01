package com.xqy.sms.student.api.service;

import com.xqy.sms.student.api.entity.Student;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public interface StudentService {

    boolean saveStudent(Student student);

    boolean updateStudent(Student student);

    boolean deleteStudent(Long id);

    Student getStudentById(Long id);

    List<Student> listStudents();

    /** Query students by the fields supported by the AI student tool. */
    List<Student> queryStudents(String name, String gender, String studentNo, Integer limit);

}

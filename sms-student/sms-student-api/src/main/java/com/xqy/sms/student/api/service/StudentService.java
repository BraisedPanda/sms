package com.xqy.sms.student.api.service;

import com.xqy.sms.student.api.entity.Student;

import java.util.List;

public interface StudentService {

    boolean saveStudent(Student student);

    boolean updateStudent(Student student);

    boolean deleteStudent(Long id);

    Student getStudentById(Long id);

    List<Student> listStudents();

}

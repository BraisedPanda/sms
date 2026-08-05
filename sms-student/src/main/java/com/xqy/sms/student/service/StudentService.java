package com.xqy.sms.student.service;

import com.xqy.sms.common.dto.ApiResponse;
import com.xqy.sms.common.entity.Student;

import java.util.List;

public interface StudentService {

    ApiResponse<Student> findByStudentNo(String studentNo);

    ApiResponse<List<Student>> findByName(String name);

    ApiResponse<List<Student>> findByAge(Integer age);

    ApiResponse<List<Student>> findByGender(String gender);

    ApiResponse<List<Student>> listAll();

    ApiResponse<Student> create(Student student);

    ApiResponse<Student> update(Student student);

    ApiResponse<Void> deleteById(Long id);
}

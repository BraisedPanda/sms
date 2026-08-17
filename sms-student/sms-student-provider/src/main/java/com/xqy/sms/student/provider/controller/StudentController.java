package com.xqy.sms.student.provider.controller;

import com.xqy.sms.common.dto.ApiResponse;
import com.xqy.sms.student.api.entity.Student;
import com.xqy.sms.student.api.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/student")
public class StudentController {

    private final StudentService studentService;

    @Autowired
    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @PostMapping
    public ApiResponse<Boolean> create(@RequestBody Student student) {
        return ApiResponse.success(studentService.saveStudent(student));
    }

    @PutMapping("/{id}")
    public ApiResponse<Boolean> update(@PathVariable Long id, @RequestBody Student student) {
        student.setId(id);
        return ApiResponse.success(studentService.updateStudent(student));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@PathVariable Long id) {
        return ApiResponse.success(studentService.deleteStudent(id));
    }

    @GetMapping("/{id}")
    public ApiResponse<Student> getById(@PathVariable Long id) {
        return ApiResponse.success(studentService.getStudentById(id));
    }

    @GetMapping("/getStudentList")
    public ApiResponse<List<Student>> list() {
        return ApiResponse.success(studentService.listStudents());
    }
}

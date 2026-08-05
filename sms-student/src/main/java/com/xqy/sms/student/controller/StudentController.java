package com.xqy.sms.student.controller;

import com.xqy.sms.common.dto.ApiResponse;
import com.xqy.sms.common.entity.Student;
import com.xqy.sms.student.service.StudentService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/student")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping("/by-no")
    public ApiResponse<Student> getByStudentNo(@RequestParam String studentNo) {
        return studentService.findByStudentNo(studentNo);
    }

    @GetMapping("/by-name")
    public ApiResponse<List<Student>> getByName(@RequestParam String name) {
        return studentService.findByName(name);
    }

    @GetMapping("/by-age")
    public ApiResponse<List<Student>> getByAge(@RequestParam Integer age) {
        return studentService.findByAge(age);
    }

    @GetMapping("/by-gender")
    public ApiResponse<List<Student>> getByGender(@RequestParam String gender) {
        return studentService.findByGender(gender);
    }

    @GetMapping("/list")
    public ApiResponse<List<Student>> listAll() {
        return studentService.listAll();
    }

    @PostMapping("/create")
    public ApiResponse<Student> create(@RequestBody Student student) {
        return studentService.create(student);
    }

    @PutMapping("/update")
    public ApiResponse<Student> update(@RequestBody Student student) {
        return studentService.update(student);
    }

    @DeleteMapping("/delete")
    public ApiResponse<Void> deleteById(@RequestParam Long id) {
        return studentService.deleteById(id);
    }
}

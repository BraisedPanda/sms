package com.xqy.sms.student.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xqy.sms.common.dto.ApiResponse;
import com.xqy.sms.common.dto.CommonCode;
import com.xqy.sms.common.entity.Student;
import com.xqy.sms.student.mapper.StudentMapper;
import com.xqy.sms.student.service.StudentService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class StudentServiceImpl implements StudentService {

    private final StudentMapper studentMapper;

    public StudentServiceImpl(StudentMapper studentMapper) {
        this.studentMapper = studentMapper;
    }

    @Override
    public ApiResponse<Student> findByStudentNo(String studentNo) {
        if (!StringUtils.hasText(studentNo)) {
            return ApiResponse.fail(CommonCode.INVALID_PARAMETER);
        }
        Student student = studentMapper.selectOne(new LambdaQueryWrapper<Student>()
                .eq(Student::getStudentNo, studentNo));
        if (student == null) {
            return ApiResponse.fail(CommonCode.NOT_FOUND);
        }
        return ApiResponse.success(student);
    }

    @Override
    public ApiResponse<List<Student>> findByName(String name) {
        if (!StringUtils.hasText(name)) {
            return ApiResponse.fail(CommonCode.INVALID_PARAMETER);
        }
        List<Student> students = studentMapper.selectList(new LambdaQueryWrapper<Student>()
                .like(Student::getName, name));
        return ApiResponse.success(students);
    }

    @Override
    public ApiResponse<List<Student>> findByAge(Integer age) {
        if (age == null) {
            return ApiResponse.fail(CommonCode.INVALID_PARAMETER);
        }
        List<Student> students = studentMapper.selectList(new LambdaQueryWrapper<Student>()
                .eq(Student::getAge, age));
        return ApiResponse.success(students);
    }

    @Override
    public ApiResponse<List<Student>> findByGender(String gender) {
        if (!StringUtils.hasText(gender)) {
            return ApiResponse.fail(CommonCode.INVALID_PARAMETER);
        }
        List<Student> students = studentMapper.selectList(new LambdaQueryWrapper<Student>()
                .eq(Student::getGender, gender));
        return ApiResponse.success(students);
    }

    @Override
    public ApiResponse<List<Student>> listAll() {
        return ApiResponse.success(studentMapper.selectList(null));
    }

    @Override
    public ApiResponse<Student> create(Student student) {
        if (student == null || !StringUtils.hasText(student.getStudentNo()) || !StringUtils.hasText(student.getName())) {
            return ApiResponse.fail(CommonCode.INVALID_PARAMETER);
        }
        studentMapper.insert(student);
        return ApiResponse.success(student);
    }

    @Override
    public ApiResponse<Student> update(Student student) {
        if (student == null || student.getId() == null) {
            return ApiResponse.fail(CommonCode.INVALID_PARAMETER);
        }
        int updated = studentMapper.updateById(student);
        if (updated == 0) {
            return ApiResponse.fail(CommonCode.NOT_FOUND);
        }
        return ApiResponse.success(student);
    }

    @Override
    public ApiResponse<Void> deleteById(Long id) {
        if (id == null) {
            return ApiResponse.fail(CommonCode.INVALID_PARAMETER);
        }
        int deleted = studentMapper.deleteById(id);
        if (deleted == 0) {
            return ApiResponse.fail(CommonCode.NOT_FOUND);
        }
        return ApiResponse.success(null);
    }
}

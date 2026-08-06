package com.xqy.sms.student.provider.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xqy.sms.student.api.entity.Student;
import com.xqy.sms.student.api.service.StudentService;
import com.xqy.sms.student.provider.mapper.StudentMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(rollbackFor = Exception.class)
public class StudentServiceImpl extends ServiceImpl<StudentMapper, Student> implements StudentService {

    @Override
    public boolean saveStudent(Student student) {
        return this.save(student);
    }

    @Override
    public boolean updateStudent(Student student) {
        return this.updateById(student);
    }

    @Override
    public boolean deleteStudent(Long id) {
        return this.removeById(id);
    }

    @Override
    public Student getStudentById(Long id) {
        return this.getById(id);
    }

    @Override
    public List<Student> listStudents() {
        return this.list();
    }
}

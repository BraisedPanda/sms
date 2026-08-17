package com.xqy.sms.student.provider.service;

import com.xqy.sms.student.api.entity.Student;
import com.xqy.sms.student.api.service.StudentService;
import com.xqy.sms.student.provider.mapper.StudentMapper;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@DubboService
@Transactional(rollbackFor = Exception.class)
public class StudentServiceImpl implements StudentService {

    @Autowired
    private StudentMapper studentMapper;

    @Override
    public boolean saveStudent(Student student) {
        return studentMapper.insert(student) > 0;
    }

    @Override
    public boolean updateStudent(Student student) {
        return studentMapper.updateById(student) > 0;
    }

    @Override
    public boolean deleteStudent(Long id) {
        return studentMapper.deleteById(id) > 0;
    }

    @Override
    public Student getStudentById(Long id) {
        return studentMapper.selectById(id);
    }

    @Override
    public List<Student> listStudents() {
        return studentMapper.selectList(null);
    }
}

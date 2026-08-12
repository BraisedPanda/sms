package com.xqy.sms.student.provider.service;

import com.xqy.sms.student.api.entity.StudentDTO;
import com.xqy.sms.student.api.service.StudentService;
import com.xqy.sms.student.provider.converter.StudentConverter;
import com.xqy.sms.student.provider.entity.Student;
import com.xqy.sms.student.provider.mapper.StudentMapper;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@DubboService
@Transactional(rollbackFor = Exception.class)
public class StudentServiceImpl implements StudentService {

    @Autowired
    private StudentMapper studentMapper;

    @Override
    public boolean saveStudent(StudentDTO student) {
        Student entity = StudentConverter.toEntity(student);
        return studentMapper.insert(entity) > 0;
    }

    @Override
    public boolean updateStudent(StudentDTO student) {
        Student entity = StudentConverter.toEntity(student);
        return studentMapper.updateById(entity) > 0;
    }

    @Override
    public boolean deleteStudent(Long id) {
        return studentMapper.deleteById(id) > 0;
    }

    @Override
    public StudentDTO getStudentById(Long id) {
        Student entity = studentMapper.selectById(id);
        return StudentConverter.toDTO(entity);
    }

    @Override
    public List<StudentDTO> listStudents() {
        return studentMapper.selectList(null).stream()
                .map(StudentConverter::toDTO)
                .collect(Collectors.toList());
    }
}

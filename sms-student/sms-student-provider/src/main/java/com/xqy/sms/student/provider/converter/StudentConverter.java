package com.xqy.sms.student.provider.converter;

import com.xqy.sms.student.api.entity.StudentDTO;
import com.xqy.sms.student.provider.entity.Student;

public class StudentConverter {

    public static Student toEntity(StudentDTO dto) {
        if (dto == null) {
            return null;
        }
        Student entity = new Student();
        entity.setId(dto.getId());
        entity.setStudentNo(dto.getStudentNo());
        entity.setName(dto.getName());
        entity.setAge(dto.getAge());
        entity.setGender(dto.getGender());
        entity.setBirthday(dto.getBirthday());
        entity.setEmail(dto.getEmail());
        entity.setPhone(dto.getPhone());
        entity.setEnrolledAt(dto.getEnrolledAt());
        return entity;
    }

    public static StudentDTO toDTO(Student entity) {
        if (entity == null) {
            return null;
        }
        StudentDTO dto = new StudentDTO();
        dto.setId(entity.getId());
        dto.setStudentNo(entity.getStudentNo());
        dto.setName(entity.getName());
        dto.setAge(entity.getAge());
        dto.setGender(entity.getGender());
        dto.setBirthday(entity.getBirthday());
        dto.setEmail(entity.getEmail());
        dto.setPhone(entity.getPhone());
        dto.setEnrolledAt(entity.getEnrolledAt());
        return dto;
    }
}

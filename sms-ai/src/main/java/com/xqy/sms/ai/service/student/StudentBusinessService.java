package com.xqy.sms.ai.service.student;

import com.xqy.sms.ai.model.AiTask;
import com.xqy.sms.ai.model.AiTaskResult;
import com.xqy.sms.ai.model.QueryCriteria;
import com.xqy.sms.ai.model.QueryFilterNode;
import com.xqy.sms.student.api.entity.Student;
import com.xqy.sms.student.api.service.StudentService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** AI-facing adapter for the student domain. */
@Service
public class StudentBusinessService {

    @DubboReference
    private StudentService studentService;

    public AiTaskResult queryStudent(AiTask task) {
        return queryStudent(task == null ? null : task.getQuery());
    }

    /** Queries the student provider using the planner's normalized criteria. */
    public AiTaskResult queryStudent(QueryCriteria criteria) {
        StudentQueryValues values = extractValues(criteria == null ? null : criteria.getFilter());
        int limit = criteria == null ? 100 : criteria.getLimit();
        List<Student> students = studentService.queryStudents(values.name(), values.gender(), values.studentNo(), limit);
        Map<String, Object> summary = new HashMap<>();
        summary.put("count", students.size());
        summary.put("domain", "student");
        summary.put("toolName", "query_student");
        return new AiTaskResult("query_student", "student_list", students, summary);
    }

    private StudentQueryValues extractValues(QueryFilterNode node) {
        StudentQueryValues values = new StudentQueryValues();
        collectLeaves(node, values);
        return values;
    }

    private void collectLeaves(QueryFilterNode node, StudentQueryValues values) {
        if (node == null) {
            return;
        }
        if (node.getField() != null && node.getOperator() != null) {
            String field = node.getField();
            String operator = node.getOperator().toUpperCase(Locale.ROOT);
            Object rawValue = node.getValue();
            String value = rawValue == null ? null : String.valueOf(rawValue);
            if ("name".equals(field) && ("LIKE".equals(operator) || "EQ".equals(operator))) {
                values.name(value);
            } else if ("gender".equals(field) && "EQ".equals(operator)) {
                values.gender(value);
            } else if ("studentNo".equals(field) && "EQ".equals(operator)) {
                values.studentNo(value);
            } else if ("name".equals(field) || "gender".equals(field) || "studentNo".equals(field)) {
                throw new IllegalArgumentException("Unsupported student query operator: " + operator);
            } else {
                throw new IllegalArgumentException("Unsupported student query field: " + field);
            }
        }
        if (node.getAnd() != null) {
            node.getAnd().forEach(child -> collectLeaves(child, values));
        }
        if (node.getOr() != null && !node.getOr().isEmpty()) {
            throw new IllegalArgumentException("Student query does not support OR filters yet");
        }
        if (node.getNot() != null) {
            throw new IllegalArgumentException("Student query does not support NOT filters yet");
        }
    }

    private static final class StudentQueryValues {
        private String name;
        private String gender;
        private String studentNo;

        String name() { return name; }
        String gender() { return gender; }
        String studentNo() { return studentNo; }
        void name(String value) { this.name = value; }
        void gender(String value) { this.gender = value; }
        void studentNo(String value) { this.studentNo = value; }
    }
}

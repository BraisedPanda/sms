-- student 表 DDL
CREATE TABLE IF NOT EXISTS student (
    id BIGINT NOT NULL AUTO_INCREMENT,
    student_no VARCHAR(64) NOT NULL COMMENT '学号',
    name VARCHAR(128) NOT NULL COMMENT '姓名',
    age INT DEFAULT NULL COMMENT '年龄',
    gender VARCHAR(16) DEFAULT NULL COMMENT '性别',
    birthday DATE DEFAULT NULL COMMENT '出生日期',
    email VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    phone VARCHAR(32) DEFAULT NULL COMMENT '联系电话',
    enrolled_at DATETIME DEFAULT NULL COMMENT '入学时间',
    sys_creator VARCHAR(64) DEFAULT NULL COMMENT '记录创建者',
    sys_modifier VARCHAR(64) DEFAULT NULL COMMENT '记录修改者',
    sys_create_time DATETIME DEFAULT NULL COMMENT '记录创建时间',
    sys_update_time DATETIME DEFAULT NULL COMMENT '记录更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uq_student_no (student_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生表';

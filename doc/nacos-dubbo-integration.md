# Nacos + Dubbo 集成说明（示例）

目标：在不改动现有业务代码的前提下，示例化如何让 `sms-ai` 通过 Dubbo 调用 `sms-student` 中的服务（接口放在 `sms-common`）。

> 说明：示例仅包含需要新增/修改的依赖、配置与示例类片段；真实部署需运行 Nacos 服务并确认版本兼容性。

---

## 1. 在 `sms-common` 中定义共享 RPC 接口

新增文件：`sms-common/src/main/java/com/xqy/sms/common/service/StudentDubboService.java`

```java
package com.xqy.sms.common.service;

import com.xqy.sms.common.entity.Student;
import java.util.List;

public interface StudentDubboService {
    Student findByStudentNo(String studentNo);
    List<Student> findByName(String name);
    List<Student> findByAge(Integer age);
    List<Student> findByGender(String gender);
}
```

把接口放在 `sms-common`，provider/consumer 均引用该模块。

## 2. 在 `sms-student`（Provider）添加依赖（pom.xml）

在 `sms-student/pom.xml` 的 `<dependencies>` 中加入：

```xml
<dependency>
  <groupId>org.apache.dubbo</groupId>
  <artifactId>dubbo-spring-boot-starter</artifactId>
  <version>3.2.3</version>
</dependency>
<dependency>
  <groupId>com.alibaba.nacos</groupId>
  <artifactId>nacos-client</artifactId>
  <version>2.2.3</version>
</dependency>
```

（版本仅作示例，请据你项目的 Spring Boot / JDK 版本选兼容版本）

## 3. 在 `sms-ai`（Consumer）添加依赖（pom.xml）

在 `sms-ai/pom.xml` 的 `<dependencies>` 中加入：

```xml
<dependency>
  <groupId>org.apache.dubbo</groupId>
  <artifactId>dubbo-spring-boot-starter</artifactId>
  <version>3.2.3</version>
</dependency>
<dependency>
  <groupId>com.alibaba.nacos</groupId>
  <artifactId>nacos-client</artifactId>
  <version>2.2.3</version>
</dependency>
```

## 4. Provider (`sms-student`) 的 `application.yml` 示例

把下面配置加入或合并到 `sms-student/src/main/resources/application.yml`：

```yaml
spring:
  application:
    name: sms-student

nacos:
  discovery:
    server-addr: 127.0.0.1:8848

dubbo:
  application:
    name: sms-student
  registry:
    address: nacos://127.0.0.1:8848
  protocol:
    name: dubbo
    port: 20880
```

## 5. Consumer (`sms-ai`) 的 `application.yml` 示例

把下面配置加入或合并到 `sms-ai/src/main/resources/application.yml`：

```yaml
spring:
  application:
    name: sms-ai

nacos:
  discovery:
    server-addr: 127.0.0.1:8848

dubbo:
  application:
    name: sms-ai
  registry:
    address: nacos://127.0.0.1:8848
```

## 6. Provider 实现示例（在 `sms-student` 中）

假设已有业务类 `StudentServiceImpl`（Spring bean），可以在另外一个类用 Dubbo 注解暴露：

```java
import org.apache.dubbo.config.annotation.DubboService;
import com.xqy.sms.common.service.StudentDubboService;

@DubboService(version = "1.0.0")
public class StudentDubboServiceImpl implements StudentDubboService {

    private final com.xqy.sms.student.service.StudentService delegate; // 已有的 Spring 服务

    public StudentDubboServiceImpl(com.xqy.sms.student.service.StudentService delegate) {
        this.delegate = delegate;
    }

    @Override
    public Student findByStudentNo(String studentNo) {
        return delegate.findByStudentNo(studentNo).getData();
    }

    @Override
    public List<Student> findByName(String name) {
        return delegate.findByName(name).getData();
    }

    @Override
    public List<Student> findByAge(Integer age) {
        return delegate.findByAge(age).getData();
    }

    @Override
    public List<Student> findByGender(String gender) {
        return delegate.findByGender(gender).getData();
    }
}
```

> 说明：这里复用已有的 `StudentService`（返回 `ApiResponse<T>`），只是把 `ApiResponse` 提取 `data` 部分返回给 RPC 调用方；若你更愿意直接返回 `ApiResponse`，也可以把 RPC 接口改成返回包装对象。

## 7. Consumer 注入示例（在 `sms-ai` 中）

```java
import org.apache.dubbo.config.annotation.DubboReference;
import com.xqy.sms.common.service.StudentDubboService;
import org.springframework.stereotype.Component;

@Component
public class StudentClient {
    @DubboReference(version = "1.0.0")
    private StudentDubboService studentDubboService;

    public void demo() {
        Student s = studentDubboService.findByStudentNo("2026001");
        // 使用 s
    }
}
```

## 8. 启动顺序与验证

1. 启动 Nacos Server（默认地址 `127.0.0.1:8848`）。
2. 启动 `sms-student`（Provider），检查启动日志，确认 Dubbo 服务已注册到 Nacos。 
3. 启动 `sms-ai`（Consumer），通过 `StudentClient` 或单元测试调用 RPC，确认能获取数据。

## 9. 构建与运行命令（示例）

```bash
# 构建相关模块
./mvnw -pl sms-common,sms-student,sms-ai -am clean package -DskipTests

# 启动（示例）
# 在不同 terminal 启动 provider 和 consumer：
java -jar sms-student/target/sms-student-0.0.1-SNAPSHOT.jar
java -jar sms-ai/target/sms-ai-0.0.1-SNAPSHOT.jar
```

## 10. 生产与兼容性建议

- 校验 `dubbo-spring-boot-starter` 与 `nacos-client` 的版本，确保与 Spring Boot / JDK 匹配。
- 建议在 `dubbo` 配置中指定 `version`、`timeout`、`retries`。
- 若使用安全或集群 Nacos，配置相应的 `username/password` 与集群地址。
- 把接口放在 `sms-common` 并添加到父 POM 的 `<modules>`，确保在 reactor 构建时顺序正确。

---

如果你确认要我把上面示例实际写入仓库（添加接口文件、provider 的 `@DubboService` 实现模板、并修改 `pom.xml` 的依赖和 `application.yml`），我可以继续操作并做一次本地构建验证（请先确认是否已安装并能启动 Nacos，以及是否要我切换项目 `java.version` 以适配本地 JDK）。

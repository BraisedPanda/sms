package com.xqy.sms.ai.infrastructure.service.history;

import com.baomidou.mybatisplus.spring.MybatisSqlSessionFactoryBean;
import com.xqy.sms.ai.infrastructure.persistence.mapper.AiChatConversationMapper;
import com.xqy.sms.ai.infrastructure.persistence.mapper.AiChatMessageMapper;
import com.xqy.sms.common.entity.AiTaskRun;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Run only against an explicitly created disposable verification schema. */
@EnabledIfEnvironmentVariable(named = "SMS_TEST_MYSQL_URL", matches = "jdbc:mysql:.*")
@SpringJUnitConfig(AiChatHistoryMySqlTest.TestConfig.class)
class AiChatHistoryMySqlTest {
    @Autowired private AiChatHistoryService history;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private com.xqy.sms.ai.application.service.run.AiTaskRunService runs;

    @BeforeEach
    void clearHistoryInVerificationSchema() {
        jdbc.update("DELETE FROM ai_chat_message");
        jdbc.update("DELETE FROM ai_chat_conversation");
        jdbc.update("DELETE FROM ai_task_step");
        jdbc.update("DELETE FROM ai_task_run");
    }

    @Test
    void storesCompleteTurnsOnceAndSeparatesTenants() {
        AiTaskRun first = run("tenant-1", "run-1");
        history.recordUserMessage(first);
        history.recordUserMessage(first);
        history.recordAssistantMessage(first, "完整回答");
        history.recordAssistantMessage(first, "重复回调");
        AiTaskRun other = run("tenant-2", "run-1");
        history.recordUserMessage(other);
        assertEquals(2, count("ai_chat_conversation"));
        assertEquals(3, count("ai_chat_message"));
        assertEquals(List.of("问题", "完整回答"), jdbc.queryForList(
                "SELECT content FROM ai_chat_message WHERE tenant_id='tenant-1' ORDER BY sequence_no", String.class));
    }

    @Test
    void concurrentFirstTurnsShareOneConversationAndHaveUniqueSequences() throws Exception {
        int workers = 8;
        CountDownLatch ready = new CountDownLatch(workers);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> results = new ArrayList<>();
        try (var executor = Executors.newFixedThreadPool(workers)) {
            for (int i = 0; i < workers; i++) {
                AiTaskRun run = run("tenant-1", "run-" + i);
                results.add(executor.submit(() -> {
                    ready.countDown();
                    try {
                        if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("test start timed out");
                    } catch (InterruptedException error) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(error);
                    }
                    history.recordUserMessage(run);
                    history.recordAssistantMessage(run, "回答 " + run.getRunId());
                }));
            }
            try {
                assertTrue(ready.await(10, TimeUnit.SECONDS));
            } finally {
                start.countDown();
            }
            for (Future<?> result : results) result.get(30, TimeUnit.SECONDS);
        }
        assertEquals(1, count("ai_chat_conversation"));
        assertEquals(workers * 2, count("ai_chat_message"));
        assertEquals(workers * 2, jdbc.queryForObject("SELECT MAX(sequence_no) FROM ai_chat_message", Integer.class));
        assertEquals(workers * 2, jdbc.queryForObject("SELECT COUNT(DISTINCT sequence_no) FROM ai_chat_message", Integer.class));
    }

    @Test
    void limitsTitleWithoutTruncatingMessageOrSplittingUnicode() {
        AiTaskRun run = run("tenant-1", "run-1");
        String question = "问".repeat(511) + "😀" + "继续提问";
        run.setQuestion(question);
        history.recordUserMessage(run);
        assertEquals("问".repeat(511) + "😀", jdbc.queryForObject("SELECT title FROM ai_chat_conversation", String.class));
        assertEquals(question, jdbc.queryForObject("SELECT content FROM ai_chat_message", String.class));
    }

    @Test
    void rejectsMissingScopeAndBlankResponsesBeforeWriting() {
        AiTaskRun run = run("", "run-1");
        assertThrows(IllegalArgumentException.class, () -> history.recordUserMessage(run));
        run.setTenantId("tenant-1");
        assertThrows(IllegalArgumentException.class, () -> history.recordAssistantMessage(run, " "));
        assertEquals(0, count("ai_chat_conversation"));
        assertEquals(0, count("ai_chat_message"));
    }

    @Test
    void rollsBackConversationWhenMessageInsertFails() {
        AiTaskRun run = run("tenant-1", "run-1");
        history.recordUserMessage(run);
        run.setSessionId("another-session");
        run.setConversationId(null);
        assertThrows(DataIntegrityViolationException.class, () -> history.recordUserMessage(run));
        assertEquals(1, count("ai_chat_conversation"));
        assertEquals(1, count("ai_chat_message"));
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }

    @Test void resumesOwnedConversationAcrossLoginSessionsAndRejectsAnotherOwner() {
        AiTaskRun first=run("tenant-1","old"); history.recordUserMessage(first);
        AiTaskRun next=run("tenant-1","new"); next.setSessionId("new-login"); next.setConversationId(first.getConversationId());
        history.recordUserMessage(next);
        assertEquals(1,count("ai_chat_conversation"));
        next.setUserId("another-user");
        assertThrows(com.xqy.sms.common.exception.ManagementNotFoundException.class,()->history.recordAssistantMessage(next,"denied"));
    }

    @Test void cancellationIsTerminalAndCannotBeOverwrittenByLateCallbacks() {
        var run=runs.createOrReuse("request","tenant-1","10","20","question","balanced","key","stream",null);
        assertTrue(runs.claimPendingRun(run.getRunId()));
        assertTrue(runs.cancel(run.getRunId(),"tenant-1","10"));
        assertTrue(!runs.completeRun(run.getRunId()));
        runs.savePlan(run.getRunId(),"late plan");
        runs.failRun(run.getRunId(),new RuntimeException("late failure"));
        var cancelled=runs.requireRun(run.getRunId());
        assertEquals("CANCELLED",cancelled.getStatus());
        assertEquals(true,cancelled.getCancelRequest());
        assertEquals(null,cancelled.getPlanJson());
        assertEquals("stream",runs.createOrReuse("retry","tenant-1","10","20","question","balanced","key","other-stream",null).getStreamKey());
    }

    private AiTaskRun run(String tenant, String id) {
        AiTaskRun run = new AiTaskRun();
        run.setTenantId(tenant);
        run.setUserId("10");
        run.setSessionId("20");
        run.setRunId(id);
        run.setQuestion("问题");
        return run;
    }

    @Configuration
    @EnableTransactionManagement
    @MapperScan(basePackageClasses = AiChatConversationMapper.class)
    static class TestConfig {
        @Bean
        DataSource dataSource() {
            String url = System.getenv("SMS_TEST_MYSQL_URL");
            if (!url.matches("jdbc:mysql://[^/]+/sms_codex_verify_[a-z0-9_]+(?:\\?.*)?")) {
                throw new IllegalArgumentException("History tests require a disposable sms_codex_verify_ schema");
            }
            DriverManagerDataSource source = new DriverManagerDataSource(url,
                    System.getenv("SMS_TEST_MYSQL_USERNAME"), System.getenv("SMS_TEST_MYSQL_PASSWORD"));
            source.setDriverClassName("com.mysql.cj.jdbc.Driver");
            return source;
        }

        @Bean
        SqlSessionFactory sqlSessionFactory(DataSource source) throws Exception {
            MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
            factory.setDataSource(source);
            com.baomidou.mybatisplus.core.MybatisConfiguration config = new com.baomidou.mybatisplus.core.MybatisConfiguration();
            config.setMapUnderscoreToCamelCase(true);
            factory.setConfiguration(config);
            return factory.getObject();
        }

        @Bean
        DataSourceTransactionManager transactionManager(DataSource source) {
            return new DataSourceTransactionManager(source);
        }

        @Bean
        JdbcTemplate jdbcTemplate(DataSource source) { return new JdbcTemplate(source); }

        @Bean
        AiChatHistoryService history(AiChatConversationMapper conversations, AiChatMessageMapper messages) {
            return new AiChatHistoryService(conversations, messages);
        }
        @Bean
        com.xqy.sms.ai.application.service.run.AiTaskRunService runs(
                com.xqy.sms.ai.infrastructure.persistence.mapper.AiTaskRunMapper runs,
                com.xqy.sms.ai.infrastructure.persistence.mapper.AiTaskStepMapper steps) {
            return new com.xqy.sms.ai.application.service.run.AiTaskRunService(runs,steps);
        }
    }
}

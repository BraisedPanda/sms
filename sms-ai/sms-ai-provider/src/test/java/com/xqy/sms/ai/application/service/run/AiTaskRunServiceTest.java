package com.xqy.sms.ai.application.service.run;

import com.xqy.sms.ai.infrastructure.persistence.mapper.AiTaskRunMapper;
import com.xqy.sms.ai.infrastructure.persistence.mapper.AiTaskStepMapper;
import com.xqy.sms.common.entity.AiTaskRun;
import com.xqy.sms.ai.api.service.AiRunAccessDeniedException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiTaskRunServiceTest {
    @Test
    void rejectsRunFromAnotherTenantOrUser() {
        AiTaskRunMapper mapper = mock(AiTaskRunMapper.class);
        AiTaskRun run = new AiTaskRun();
        run.setRunId("run-1");
        run.setTenantId("tenant-1");
        run.setUserId("10");
        when(mapper.selectOne(any())).thenReturn(run);
        AiTaskRunService service = new AiTaskRunService(mapper, mock(AiTaskStepMapper.class));

        assertSame(run, service.requireOwnedRun("run-1", "tenant-1", "10"));
        assertThrows(AiRunAccessDeniedException.class,
                () -> service.requireOwnedRun("run-1", "tenant-2", "10"));
        assertThrows(AiRunAccessDeniedException.class,
                () -> service.requireOwnedRun("run-1", "tenant-1", "11"));
    }
}

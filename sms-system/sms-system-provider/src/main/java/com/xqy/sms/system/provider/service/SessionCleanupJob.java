package com.xqy.sms.system.provider.service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
@Component public class SessionCleanupJob {
    private final SystemAuthServiceImpl service;
    public SessionCleanupJob(SystemAuthServiceImpl service) { this.service = service; }
    @Scheduled(cron = "${sms.system.session-cleanup-cron:0 */10 * * * *}") public void clearExpiredSessions() { service.clearExpiredSessions(); }
}

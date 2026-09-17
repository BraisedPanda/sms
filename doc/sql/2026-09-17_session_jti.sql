ALTER TABLE sys_user_session ADD COLUMN access_jti VARCHAR(64) NULL AFTER refresh_token;
ALTER TABLE sys_user_session ADD COLUMN refresh_jti VARCHAR(64) NULL AFTER access_jti;
CREATE INDEX idx_sys_user_session_access_jti ON sys_user_session(access_jti);
CREATE INDEX idx_sys_user_session_refresh_jti ON sys_user_session(refresh_jti);

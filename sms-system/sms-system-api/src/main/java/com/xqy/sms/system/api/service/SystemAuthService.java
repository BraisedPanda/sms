package com.xqy.sms.system.api.service;

import com.xqy.sms.system.api.model.SystemAuthModels;
import java.util.List;

public interface SystemAuthService {
    SystemAuthModels.TokenPair login(String username, String password, String loginIp, String userAgent);
    SystemAuthModels.TokenPair refresh(String refreshToken);
    SystemAuthModels.Principal authenticate(Long userId, Long sessionId, String tokenId);
    SystemAuthModels.UserInfo userInfo(Long userId);
    void logout(Long sessionId, String tokenId);
    void kick(Long sessionId);
    int clearExpiredSessions();
    List<SystemAuthModels.Menu> menus(Long userId);
    SystemAuthModels.UserPage users(String username, String status, long current, long size);
}

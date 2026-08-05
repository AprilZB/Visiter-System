package com.maitong.visitor.service;

public interface DingTalkNotificationService {

    /**
     * 根据员工工号及姓名拉取钉钉 UserID
     */
    String getDingUserIdByWorkNo(String workNo, String name);

    /**
     * 发送钉钉工作通知 (根据工号)
     * 测试阶段严格限定只发送给张勃 (zhangb9, 工号: 404256402, 邮箱: Bo.Zhang@accupathmed.com)
     */
    boolean sendWorkNotificationByWorkNo(String workNo, String name, String content);
}

package com.maitong.visitor.service;

import com.maitong.visitor.entity.SysUserSync;

public interface DingTalkAuthService {
    SysUserSync loginByAuthCode(String authCode);
}

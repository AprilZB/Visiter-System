package com.maitong.visitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.maitong.visitor.entity.SysUserSync;
import com.maitong.visitor.mapper.SysUserSyncMapper;
import com.maitong.visitor.service.DingTalkAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DingTalkAuthServiceImpl implements DingTalkAuthService {

    @Autowired
    private SysUserSyncMapper sysUserSyncMapper;

    @Override
    public SysUserSync loginByAuthCode(String authCode) {
        if (authCode == null || authCode.trim().isEmpty()) {
            return getDefaultEmployee();
        }

        // 本地环境及演示联调时，支持直接匹配 userid 或工号
        LambdaQueryWrapper<SysUserSync> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserSync::getDingUserid, authCode)
               .or().eq(SysUserSync::getWorkNo, authCode);
        SysUserSync user = sysUserSyncMapper.selectOne(wrapper);

        if (user != null) {
            return user;
        }

        return getDefaultEmployee();
    }

    private SysUserSync getDefaultEmployee() {
        LambdaQueryWrapper<SysUserSync> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(SysUserSync::getId).last("LIMIT 1");
        SysUserSync first = sysUserSyncMapper.selectOne(wrapper);
        if (first != null) return first;

        SysUserSync demo = new SysUserSync();
        demo.setId(1L);
        demo.setWorkNo("MT001");
        demo.setName("张经理");
        demo.setPhone("13800001111");
        demo.setDeptName("研发部");
        demo.setDingUserid("ding_user_001");
        return demo;
    }
}

package com.maitong.visitor.service;

import com.maitong.visitor.entity.SysUserSync;

import java.util.List;
import java.util.Map;

public interface AphrSyncService {
    /**
     * 手动/定时全量同步 APHR 的组织架构与人员
     */
    boolean syncAll();

    /**
     * 获取多级嵌套组织架构树
     */
    List<Map<String, Object>> getDeptTree();

    /**
     * 兼容原列表查询
     */
    List<SysUserSync> getUsers(String adAccount, String name, String deptName, Long deptId, String status);

    /**
     * 根据部门名称/全路径更新防骚扰屏蔽状态
     */
    boolean updateDeptShieldByName(String deptName, Integer isShielded);

    /**
     * 分页+多条件高效查询人员档案
     */
    Map<String, Object> getUsersPage(Integer page, Integer pageSize, String adAccount, String name, String deptName, Long deptId, String status);
}


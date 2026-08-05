package com.maitong.visitor.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.maitong.visitor.common.Result;
import com.maitong.visitor.entity.SysDeptSync;
import com.maitong.visitor.entity.SysUserSync;
import com.maitong.visitor.entity.SysVisitReason;
import com.maitong.visitor.mapper.SysDeptSyncMapper;
import com.maitong.visitor.mapper.SysUserSyncMapper;
import com.maitong.visitor.mapper.SysVisitReasonMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public")
@CrossOrigin
public class PublicDataController {

    @Autowired
    private SysDeptSyncMapper sysDeptSyncMapper;

    @Autowired
    private SysUserSyncMapper sysUserSyncMapper;

    @Autowired
    private SysVisitReasonMapper sysVisitReasonMapper;

    /**
     * 1. 获取未屏蔽的可选部门列表 (自动过滤屏蔽防骚扰部门)
     */
    @GetMapping("/depts")
    public Result<List<SysDeptSync>> getPublicDepts() {
        LambdaQueryWrapper<SysDeptSync> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDeptSync::getIsShielded, 0)
               .orderByAsc(SysDeptSync::getId);
        return Result.success(sysDeptSyncMapper.selectList(wrapper));
    }

    /**
     * 2. 先选部门后，级联获取该部门下的员工列表
     */
    @GetMapping("/users-by-dept")
    public Result<List<SysUserSync>> getUsersByDept(@RequestParam(value = "deptName", required = false) String deptName,
                                                     @RequestParam(value = "deptId", required = false) Long deptId) {
        LambdaQueryWrapper<SysUserSync> wrapper = new LambdaQueryWrapper<>();
        if (deptName != null && !deptName.trim().isEmpty()) {
            wrapper.eq(SysUserSync::getDeptName, deptName.trim());
        } else if (deptId != null) {
            wrapper.eq(SysUserSync::getDeptId, deptId);
        }
        wrapper.orderByAsc(SysUserSync::getId);
        return Result.success(sysUserSyncMapper.selectList(wrapper));
    }

    /**
     * 3. 获取有效的来访事由选项
     */
    @GetMapping("/visit-reasons")
    public Result<List<SysVisitReason>> getVisitReasons() {
        LambdaQueryWrapper<SysVisitReason> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysVisitReason::getIsActive, 1)
               .orderByAsc(SysVisitReason::getSortOrder);
        return Result.success(sysVisitReasonMapper.selectList(wrapper));
    }
}

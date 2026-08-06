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

    @Autowired
    private com.maitong.visitor.mapper.VisitorRecordMapper visitorRecordMapper;

    /**
     * 1. 获取未屏蔽的可选部门列表 (带出一级/二级全路径，并自动过滤屏蔽部门)
     */
    @GetMapping("/depts")
    public Result<List<SysDeptSync>> getPublicDepts() {
        LambdaQueryWrapper<SysDeptSync> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDeptSync::getIsShielded, 0)
               .orderByAsc(SysDeptSync::getId);
        List<SysDeptSync> list = sysDeptSyncMapper.selectList(wrapper);
        return Result.success(list);
    }

    /**
     * 2. 选定部门后级联查询在职员工 (强力过滤离职人员)
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
        // 关键过滤：绝对不带出离职员工！
        wrapper.and(w -> w.isNull(SysUserSync::getStatus)
                         .or()
                         .notIn(SysUserSync::getStatus, "离职", "已离职", "Terminated", "Inactive"));
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

    /**
     * 4. 免登根据唯一加密 Token 查询单笔访客申请信息
     */
    @GetMapping("/host/apply-info")
    public Result<com.maitong.visitor.entity.VisitorRecord> getApplyInfoByToken(@RequestParam("approveToken") String approveToken) {
        if (approveToken == null || approveToken.trim().isEmpty()) {
            return Result.error("无效的审批 Token 链接");
        }
        LambdaQueryWrapper<com.maitong.visitor.entity.VisitorRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(com.maitong.visitor.entity.VisitorRecord::getApproveToken, approveToken.trim());
        com.maitong.visitor.entity.VisitorRecord record = visitorRecordMapper.selectOne(wrapper);
        if (record == null) {
            return Result.error("未找到该笔到访申请或链接已失效");
        }
        return Result.success(record);
    }

    /**
     * 5. 免登使用唯一加密 Token 进行一键快捷审批 (同意/拒绝)
     */
    @PostMapping("/host/approve-by-token")
    public Result<Boolean> approveByToken(@RequestBody java.util.Map<String, Object> body) {
        String approveToken = body.get("approveToken") != null ? body.get("approveToken").toString() : null;
        Boolean approved = body.get("approved") != null ? Boolean.parseBoolean(body.get("approved").toString()) : false;

        if (approveToken == null || approveToken.trim().isEmpty()) {
            return Result.error("审批 Token 为空");
        }
        LambdaQueryWrapper<com.maitong.visitor.entity.VisitorRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(com.maitong.visitor.entity.VisitorRecord::getApproveToken, approveToken.trim());
        com.maitong.visitor.entity.VisitorRecord record = visitorRecordMapper.selectOne(wrapper);
        if (record == null) {
            return Result.error("该审批链接无效或已被处理");
        }

        if (approved) {
            record.setStatus("APPROVED");
            record.setApprovedBy("DingTalkTokenAuth");
            record.setApprovedAt(java.time.LocalDateTime.now());
        } else {
            record.setStatus("REJECTED");
            record.setApprovedBy("DingTalkTokenAuth");
            record.setApprovedAt(java.time.LocalDateTime.now());
        }

        visitorRecordMapper.updateById(record);
        return Result.success("审批结果处理成功", approved);
    }

    /**
     * 6. 访客从邮箱链接免登根据 visitorToken 获取邀请到访单信息
     */
    @GetMapping("/visitor/info-by-token")
    public Result<com.maitong.visitor.entity.VisitorRecord> getVisitorInfoByToken(@RequestParam("visitorToken") String visitorToken) {
        if (visitorToken == null || visitorToken.trim().isEmpty()) {
            return Result.error("无效的访客 Token");
        }
        LambdaQueryWrapper<com.maitong.visitor.entity.VisitorRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(com.maitong.visitor.entity.VisitorRecord::getVisitorToken, visitorToken.trim());
        com.maitong.visitor.entity.VisitorRecord record = visitorRecordMapper.selectOne(wrapper);
        if (record == null) {
            return Result.error("未找到该笔邀请到访单");
        }
        return Result.success(record);
    }

    /**

     * 7. 根据手机号查询访客最新的预约/申请单信息 (支持正门扫码凭手机号查找补全身份证与签署 NDA)
     */
    @GetMapping("/visitor/by-phone")
    public Result<com.maitong.visitor.entity.VisitorRecord> getVisitorByPhone(@RequestParam("phone") String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return Result.error("请输入有效的手机号码");
        }
        LambdaQueryWrapper<com.maitong.visitor.entity.VisitorRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(com.maitong.visitor.entity.VisitorRecord::getPhone, phone.trim())
               .orderByDesc(com.maitong.visitor.entity.VisitorRecord::getId)
               .last("LIMIT 1");

        com.maitong.visitor.entity.VisitorRecord record = visitorRecordMapper.selectOne(wrapper);
        if (record == null) {
            return Result.error("未查询到该手机号对应的到访预约单，请在下方直接提交现场盲到申请。");
        }
        return Result.success(record);
    }
}




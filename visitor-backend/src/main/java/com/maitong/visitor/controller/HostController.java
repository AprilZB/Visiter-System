package com.maitong.visitor.controller;

import com.maitong.visitor.common.Result;
import com.maitong.visitor.dto.VisitorApplyDTO;
import com.maitong.visitor.entity.SysUserSync;
import com.maitong.visitor.entity.VisitorRecord;
import com.maitong.visitor.service.DingTalkAuthService;
import com.maitong.visitor.service.VisitorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/host")
@CrossOrigin
public class HostController {

    @Autowired
    private DingTalkAuthService dingTalkAuthService;

    @Autowired
    private VisitorService visitorService;

    /**
     * 1. 钉钉 App 微应用免登接口
     */
    @PostMapping("/login")
    public Result<SysUserSync> dingTalkLogin(@RequestBody Map<String, String> body) {
        String authCode = body.get("authCode");
        SysUserSync user = dingTalkAuthService.loginByAuthCode(authCode);
        return Result.success("免登成功", user);
    }

    /**
     * 2. 员工主动发起预约邀约 (场景 A)
     */
    @PostMapping("/invite")
    public Result<VisitorRecord> createInvite(@RequestBody VisitorApplyDTO dto) {
        dto.setScenario("A");
        VisitorRecord record = visitorService.applyVisit(dto);
        return Result.success("预约邀约生成成功", record);
    }

    /**
     * 3. 获取员工待审批的现场盲来访客单列表
     */
    @GetMapping("/pending")
    public Result<List<VisitorRecord>> getPendingApprovals(@RequestParam(value = "hostUserId", defaultValue = "1") Long hostUserId) {
        List<VisitorRecord> list = visitorService.getHostPendingApprovals(hostUserId);
        return Result.success(list);
    }

    /**
     * 4. 员工一键审批 (同意/驳回)
     */
    @PostMapping("/approve")
    public Result<Boolean> approve(@RequestBody Map<String, Object> body) {
        Long recordId = Long.parseLong(body.get("recordId").toString());
        boolean agree = Boolean.parseBoolean(body.get("agree").toString());
        String approverName = body.containsKey("approverName") ? body.get("approverName").toString() : "内部员工";

        boolean ok = visitorService.approveVisit(recordId, agree, approverName);
        return ok ? Result.success(agree ? "审批已通过" : "审批已驳回", true) : Result.error("操作失败");
    }

    /**
     * 5. 查看该员工发起的全部访客记录与 NDA 签署进度
     */
    @GetMapping("/records")
    public Result<List<VisitorRecord>> getHostRecords(@RequestParam(value = "hostUserId", defaultValue = "1") Long hostUserId) {
        List<VisitorRecord> list = visitorService.getHostAllRecords(hostUserId);
        return Result.success(list);
    }
}

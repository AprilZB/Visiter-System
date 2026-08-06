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

    @Autowired
    private com.maitong.visitor.mapper.VisitorRecordMapper visitorRecordMapper;

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
     * 场景 A: 员工发起批量预约并自动发送 HTML 邮件邀请函
     */
    @PostMapping("/batch-invite")
    public Result<List<VisitorRecord>> createBatchInvite(@RequestBody Map<String, Object> body) {
        String company = body.get("company") != null ? body.get("company").toString() : "来访单位";
        String visitDate = body.get("visitDate") != null ? body.get("visitDate").toString() : "";
        String visitStartTime = body.get("visitStartTime") != null ? body.get("visitStartTime").toString() : "09:00";
        String visitEndTime = body.get("visitEndTime") != null ? body.get("visitEndTime").toString() : "18:00";
        String visitPurpose = body.get("visitPurpose") != null ? body.get("visitPurpose").toString() : "业务交流";
        Long hostUserId = body.get("hostUserId") != null ? Long.parseLong(body.get("hostUserId").toString()) : 1L;

        List<Map<String, String>> visitors = (List<Map<String, String>>) body.get("visitors");
        if (visitors == null || visitors.isEmpty()) {
            return Result.error("请至少添加一名来访人员信息");
        }

        List<VisitorRecord> createdList = new java.util.ArrayList<>();
        for (Map<String, String> v : visitors) {
            String name = v.get("visitorName") != null ? v.get("visitorName") : v.get("name");
            String phone = v.get("phone");
            String email = v.get("email");

            VisitorApplyDTO dto = new VisitorApplyDTO();
            dto.setScenario("A");
            dto.setVisitorName(name);
            dto.setPhone(phone);
            dto.setHostUserId(hostUserId);
            dto.setVisitPurpose(visitPurpose);
            dto.setVisitDate(visitDate);
            dto.setVisitStartTime(visitStartTime);
            dto.setVisitEndTime(visitEndTime);

            VisitorRecord record = visitorService.applyVisit(dto);
            record.setCompany(company);
            record.setEmail(email);

            String visitorToken = java.util.UUID.randomUUID().toString().replace("-", "");
            record.setVisitorToken(visitorToken);
            
            // 自动标记场景 A 免审，直接为 APPROVED
            record.setStatus("APPROVED");
            record.setApprovedBy("HostBatchInvite");
            record.setApprovedAt(java.time.LocalDateTime.now());

            visitorRecordMapper.updateById(record);
            createdList.add(record);

            // 模拟/派发 HTML 格式到访邀请函邮件
            String inviteUrl = "http://10.11.100.154:8097/visitor?visitorToken=" + visitorToken;
            System.out.println("=================================================================");
            System.out.println("【到访邀请函邮件派发】 目标邮箱: " + email);
            System.out.println("尊敬的 " + name + " 您好，" + record.getHostName() + " 邀请您于 " + visitDate + " " + visitStartTime + "~" + visitEndTime + " 到访 " + company);
            System.out.println("👉 专属补全身份与领码凭证链接: " + inviteUrl);
            System.out.println("=================================================================");
        }

        return Result.success("批量预约邀请函已成功派发至各访客邮箱！", createdList);
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

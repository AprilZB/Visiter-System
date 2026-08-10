package com.maitong.visitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.maitong.visitor.dto.SecurityScanDTO;
import com.maitong.visitor.dto.VisitorApplyDTO;
import com.maitong.visitor.entity.SysUserSync;
import com.maitong.visitor.entity.VisitorRecord;
import com.maitong.visitor.mapper.SysUserSyncMapper;
import com.maitong.visitor.mapper.VisitorRecordMapper;
import com.maitong.visitor.service.NdaGuardService;
import com.maitong.visitor.service.VisitorService;
import com.maitong.visitor.util.CryptoUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class VisitorServiceImpl implements VisitorService {

    @Autowired
    private VisitorRecordMapper visitorRecordMapper;

    @Autowired
    private SysUserSyncMapper sysUserSyncMapper;

    @Autowired
    private NdaGuardService ndaGuardService;

    @Autowired
    private com.maitong.visitor.service.DingTalkNotificationService dingTalkNotificationService;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");


    @Override
    public VisitorRecord applyVisit(VisitorApplyDTO dto) {
        VisitorRecord record = new VisitorRecord();
        String visitNo = "V" + LocalDateTime.now().format(TIME_FMT) + (int)((Math.random()*900)+100);
        record.setVisitNo(visitNo);
        record.setScenario(dto.getScenario() != null ? dto.getScenario() : "B");
        record.setVisitorName(dto.getVisitorName());
        
        // 身份证号落盘加密 + 物业端强脱敏掩码 (倒数5~8位)
        String rawIdCard = dto.getIdCard() != null ? dto.getIdCard().trim().toUpperCase() : "";
        record.setIdCardEncrypted(CryptoUtils.encryptAES(rawIdCard));
        record.setIdCardMasked(CryptoUtils.maskIdCard(rawIdCard));
        
        record.setPhone(dto.getPhone());
        record.setHostUserId(dto.getHostUserId() != null ? dto.getHostUserId() : 1L);

        // 绑定受访员工信息
        SysUserSync host = sysUserSyncMapper.selectById(record.getHostUserId());
        if (host != null) {
            record.setHostName(host.getName());
            record.setHostDept(host.getDeptName());
        } else {
            record.setHostName("受访员工");
            record.setHostDept("行政部");
        }

        record.setVisitPurpose(dto.getVisitPurpose() != null ? dto.getVisitPurpose() : "业务交流");
        record.setVisitTime(LocalDateTime.now());
        record.setVisitDate(dto.getVisitDate() != null ? dto.getVisitDate() : java.time.LocalDate.now().toString());
        record.setVisitStartTime(dto.getVisitStartTime() != null ? dto.getVisitStartTime() : "09:00");
        record.setVisitEndTime(dto.getVisitEndTime() != null ? dto.getVisitEndTime() : "18:00");
        record.setNdaSigned(0);

        String approveToken = java.util.UUID.randomUUID().toString().replace("-", "");
        record.setApproveToken(approveToken);

        // 场景 A (员工主动邀约): 直接通过，等待访客签署保密协议
        // 场景 B (现场盲扫): 进入待审批状态
        if ("A".equalsIgnoreCase(record.getScenario())) {
            record.setStatus("APPROVED");
            record.setApprovedBy("SystemAuto(SceneA)");
            record.setApprovedAt(LocalDateTime.now());
        } else {
            record.setStatus("PENDING_APPROVAL");
        }

        visitorRecordMapper.insert(record);

        // 异步/触发钉钉通知推送 (测试阶段锁定推送给张勃 zhangb9)
        try {
            String hostWorkNo = (host != null) ? host.getWorkNo() : "404256402";
            String hostName = (host != null) ? host.getName() : "张勃";
            String approveUrl = "http://10.11.100.151:8097/host?approveToken=" + approveToken;

            String timeRangeStr = String.format("%s %s ~ %s", record.getVisitDate(), record.getVisitStartTime(), record.getVisitEndTime());
            String msg = String.format("【脉通访客到访申请审批】\n" +
                    "访客姓名：%s\n" +
                    "手机号码：%s\n" +
                    "拟到访时间段：%s\n" +
                    "来访事由：%s\n" +
                    "受访部门：%s\n\n" +
                    "👉 点击下方专属加密链接一键免密审批：\n%s",
                    record.getVisitorName(), record.getPhone(), timeRangeStr, record.getVisitPurpose(), record.getHostDept(), approveUrl);

            dingTalkNotificationService.sendWorkNotificationByWorkNo(hostWorkNo, hostName, msg);
        } catch (Exception e) {
            // 通知推送异常不卡死主业务流程
        }



        return record;
    }


    @Override
    public boolean approveVisit(Long recordId, boolean agree, String approverName) {
        VisitorRecord record = visitorRecordMapper.selectById(recordId);
        if (record == null) return false;

        if (agree) {
            record.setStatus("APPROVED");
            record.setApprovedBy(approverName != null ? approverName : "HostUser");
            record.setApprovedAt(LocalDateTime.now());
        } else {
            record.setStatus("REJECTED");
            record.setApprovedBy(approverName != null ? approverName : "HostUser");
            record.setApprovedAt(LocalDateTime.now());
        }
        return visitorRecordMapper.updateById(record) > 0;
    }

    @Override
    public Map<String, Object> getLatestPassTokenByPhone(String phone) {
        LambdaQueryWrapper<VisitorRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VisitorRecord::getPhone, phone.trim())
               .eq(VisitorRecord::getNdaSigned, 1) // 必须已签署保密协议
               .orderByDesc(VisitorRecord::getId)
               .last("LIMIT 1");

        VisitorRecord record = visitorRecordMapper.selectOne(wrapper);
        if (record == null) {
            return null;
        }

        // 调用生成动态 Token 逻辑
        String passToken = getPassCodeToken(record.getVisitNo());
        if (passToken != null) {
            Map<String, Object> tokenResult = new HashMap<>();
            tokenResult.put("passToken", passToken);
            tokenResult.put("passExpireMinutes", 60);
            tokenResult.put("record", record);
            tokenResult.put("visitorName", record.getVisitorName());
            tokenResult.put("visitNo", record.getVisitNo());
            return tokenResult;
        }
        return null;
    }


    @Override
    public String getPassCodeToken(String visitNo) {
        VisitorRecord record = getByVisitNo(visitNo);
        if (record == null) return null;

        // 强保密协议拦截：未签保密协议，绝不发下通行 Token！
        if (record.getNdaSigned() == null || record.getNdaSigned() != 1) {
            throw new IllegalStateException("保密协议未签署，禁止生成通行凭证");
        }

        if (record.getPassToken() == null || record.getPassToken().trim().isEmpty()) {
            String token = "PASS_" + UUID.randomUUID().toString().replace("-", "") + "_" + System.currentTimeMillis();
            record.setPassToken(token);
            visitorRecordMapper.updateById(record);
            return token;
        }
        return record.getPassToken();
    }

    @Override
    public SecurityScanDTO securityScanVerify(String passToken) {
        SecurityScanDTO dto = new SecurityScanDTO();
        if (passToken == null || passToken.trim().isEmpty()) {
            dto.setCanPass(false);
            dto.setWarningMessage("无效的通行二维码");
            return dto;
        }

        String queryStr = passToken.trim();
        LambdaQueryWrapper<VisitorRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(VisitorRecord::getPassToken, queryStr)
                         .or().eq(VisitorRecord::getPhone, queryStr)
                         .or().likeRight(VisitorRecord::getPassToken, queryStr)
                         .or().likeRight(VisitorRecord::getPassToken, "PASS_" + queryStr)
                         .or().eq(VisitorRecord::getVisitNo, queryStr));
        wrapper.orderByDesc(VisitorRecord::getId).last("LIMIT 1");

        VisitorRecord record = visitorRecordMapper.selectOne(wrapper);


        if (record == null) {
            // 二次 Fallback: 尝试按手机号检索该手机号最新的到访申请单，确保门岗无论拦截与否都能清晰展示访客身份
            LambdaQueryWrapper<VisitorRecord> phoneWrapper = new LambdaQueryWrapper<>();
            phoneWrapper.eq(VisitorRecord::getPhone, queryStr)
                        .orderByDesc(VisitorRecord::getId).last("LIMIT 1");
            record = visitorRecordMapper.selectOne(phoneWrapper);
            
            if (record == null) {
                dto.setCanPass(false);
                dto.setWarningMessage("通行凭证不匹配且未查询到该手机号的历史申请单");
                return dto;
            }
        }

        // 统一全量装填访客基本信息（保证保安端绝不出现空白行）
        dto.setVisitNo(record.getVisitNo());
        dto.setVisitorName(record.getVisitorName() != null ? record.getVisitorName() : "未录入姓名");
        dto.setIdCardMasked(record.getIdCardMasked() != null ? record.getIdCardMasked() : "未录入身份证");
        dto.setPhone(record.getPhone() != null ? record.getPhone() : "-");
        dto.setHostName(record.getHostName() != null ? record.getHostName() : "未知受访人");
        dto.setHostDept(record.getHostDept() != null ? record.getHostDept() : "未知部门");
        dto.setVisitPurpose(record.getVisitPurpose() != null ? record.getVisitPurpose() : "商务到访");
        dto.setStatus(record.getStatus());
        dto.setNdaSigned(record.getNdaSigned() != null && record.getNdaSigned() == 1);

        if (!dto.isNdaSigned()) {
            dto.setCanPass(false);
            dto.setWarningMessage("【拦截】访客(" + dto.getVisitorName() + ")尚未签署保密协议 (NDA)！禁止放行！");
            return dto;
        }

        if ("ENTERED".equalsIgnoreCase(record.getStatus())) {
            dto.setCanPass(false);
            dto.setWarningMessage("【警告】访客(" + dto.getVisitorName() + ")的通行码已经核销放行过，请勿重复放行！");
            return dto;
        }

        if ("PENDING_APPROVAL".equalsIgnoreCase(record.getStatus())) {
            dto.setCanPass(false);
            dto.setWarningMessage("【拦截】访客(" + dto.getVisitorName() + ")的申请仍处于待员工审批状态！");
            return dto;
        }

        if ("REJECTED".equalsIgnoreCase(record.getStatus())) {
            dto.setCanPass(false);
            dto.setWarningMessage("【拒绝】受访员工已拒绝访客(" + dto.getVisitorName() + ")的到访申请");
            return dto;
        }

        dto.setCanPass(true);
        dto.setWarningMessage("人证比对一致，可予以确认放行");
        return dto;

    }

    @Override
    public boolean confirmEntry(String visitNo, String securityName) {
        VisitorRecord record = getByVisitNo(visitNo);
        if (record == null) return false;

        record.setStatus("ENTERED");
        record.setVerifiedBy(securityName != null ? securityName : "门岗保安");
        record.setVerifiedAt(LocalDateTime.now());
        record.setPassToken(null); // 立即核销作废动态码
        return visitorRecordMapper.updateById(record) > 0;
    }

    @Override
    public VisitorRecord getByVisitNo(String visitNo) {
        LambdaQueryWrapper<VisitorRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VisitorRecord::getVisitNo, visitNo);
        return visitorRecordMapper.selectOne(wrapper);
    }

    @Override
    public List<VisitorRecord> getHostPendingApprovals(Long hostUserId) {
        LambdaQueryWrapper<VisitorRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VisitorRecord::getHostUserId, hostUserId)
               .eq(VisitorRecord::getStatus, "PENDING_APPROVAL")
               .orderByDesc(VisitorRecord::getId);
        return visitorRecordMapper.selectList(wrapper);
    }

    @Override
    public List<VisitorRecord> getHostAllRecords(Long hostUserId) {
        LambdaQueryWrapper<VisitorRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VisitorRecord::getHostUserId, hostUserId)
               .orderByDesc(VisitorRecord::getId);
        return visitorRecordMapper.selectList(wrapper);
    }

    @Override
    public List<VisitorRecord> getAllRecords() {
        LambdaQueryWrapper<VisitorRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(VisitorRecord::getId);
        return visitorRecordMapper.selectList(wrapper);
    }
}

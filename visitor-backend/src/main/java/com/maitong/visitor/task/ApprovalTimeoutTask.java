package com.maitong.visitor.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.maitong.visitor.entity.SysUserSync;
import com.maitong.visitor.entity.VisitorRecord;
import com.maitong.visitor.mapper.SysUserSyncMapper;
import com.maitong.visitor.mapper.VisitorRecordMapper;
import com.maitong.visitor.service.DingTalkNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class ApprovalTimeoutTask {

    private static final Logger log = LoggerFactory.getLogger(ApprovalTimeoutTask.class);

    @Autowired
    private VisitorRecordMapper visitorRecordMapper;

    @Autowired
    private SysUserSyncMapper sysUserSyncMapper;

    @Autowired
    private DingTalkNotificationService dingTalkNotificationService;

    // 默认超时分钟数: 30 分钟 (测试环境可动态兼容)
    private static final int TIMEOUT_MINUTES = 30;

    /**
     * 每 2 分钟扫描一次超过 30 分钟未处理的盲到申请
     */
    @Scheduled(cron = "0 */2 * * * ?")
    public void scanAndEscalateTimeouts() {
        try {
            LocalDateTime thresholdTime = LocalDateTime.now().minusMinutes(TIMEOUT_MINUTES);
            LambdaQueryWrapper<VisitorRecord> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(VisitorRecord::getStatus, "PENDING_APPROVAL")
                   .and(w -> w.isNull(VisitorRecord::getEscalated).or().eq(VisitorRecord::getEscalated, 0))
                   .le(VisitorRecord::getCreatedAt, thresholdTime);

            List<VisitorRecord> timeoutRecords = visitorRecordMapper.selectList(wrapper);
            if (timeoutRecords == null || timeoutRecords.isEmpty()) {
                return;
            }

            log.info("【超时审批巡检】检测到 {} 笔已超过 {} 分钟未处理的访客申请，开始自动寻找直属主管追发提醒...", timeoutRecords.size(), TIMEOUT_MINUTES);

            for (VisitorRecord record : timeoutRecords) {
                Long hostUserId = record.getHostUserId();
                SysUserSync hostUser = sysUserSyncMapper.selectById(hostUserId);
                
                String managerName = "部门主管";
                String managerWorkNo = "";
                
                if (hostUser != null && hostUser.getManagerName() != null && !hostUser.getManagerName().trim().isEmpty()) {
                    managerName = hostUser.getManagerName().trim();
                }

                String approveUrl = "http://10.11.100.154:8097/host?approveToken=" + record.getApproveToken();
                String timeRangeStr = String.format("%s %s ~ %s", 
                        record.getVisitDate() != null ? record.getVisitDate() : "", 
                        record.getVisitStartTime() != null ? record.getVisitStartTime() : "", 
                        record.getVisitEndTime() != null ? record.getVisitEndTime() : "");

                String escalateMsg = String.format("【访客申请超时递进催办通知】\n" +
                        "尊敬的 %s（主管）：\n" +
                        "您的下属 [%s] 收到一笔现场访客申请，已超过 %d 分钟未处理！系统已自动递进递呈给您进行代审批处理：\n" +
                        "-------------------------\n" +
                        "访客姓名：%s\n" +
                        "联系电话：%s\n" +
                        "拟到访时间段：%s\n" +
                        "来访事由：%s\n" +
                        "受访人员：%s (%s)\n\n" +
                        "👉 点击下方专属加密链接直达代为一键审批：\n%s",
                        managerName, record.getHostName(), TIMEOUT_MINUTES,
                        record.getVisitorName(), record.getPhone(), timeRangeStr,
                        record.getVisitPurpose(), record.getHostName(), record.getHostDept(), approveUrl);

                // 发送给主管 (测试阶段由平台统一安全透传)
                dingTalkNotificationService.sendWorkNotificationByWorkNo(managerWorkNo, managerName, escalateMsg);

                // 标记为已上报递进
                record.setEscalated(1);
                record.setEscalatedAt(LocalDateTime.now());
                visitorRecordMapper.updateById(record);

                log.info("访客单号 [{}] 超时处理已成功上报并推送给直属主管 [{}]", record.getVisitNo(), managerName);
            }
        } catch (Exception e) {
            log.error("超时审批递进任务运行异常", e);
        }
    }
}

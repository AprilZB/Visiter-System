package com.maitong.visitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("visitor_records")
public class VisitorRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String visitNo;
    private String scenario; // A: 员工预约, B: 现场盲来扫码
    private String visitorName;
    private String idCardEncrypted;
    private String idCardMasked;
    private String phone;
    private Long hostUserId;
    private String hostName;
    private String hostDept;
    private String visitPurpose;
    private LocalDateTime visitTime;
    private String visitDate;
    private String visitStartTime;
    private String visitEndTime;
    private String status;
 // PENDING_APPROVAL, APPROVED, NDA_SIGNED, ENTERED, REJECTED, EXPIRED
    private Integer ndaSigned; // 0:未签, 1:已签
    private LocalDateTime ndaSignedAt;
    private String passToken;
    private String approveToken;
    private String approvedBy;

    private LocalDateTime approvedAt;
    private String verifiedBy;
    private LocalDateTime verifiedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getVisitNo() { return visitNo; }
    public void setVisitNo(String visitNo) { this.visitNo = visitNo; }

    public String getScenario() { return scenario; }
    public void setScenario(String scenario) { this.scenario = scenario; }

    public String getVisitorName() { return visitorName; }
    public void setVisitorName(String visitorName) { this.visitorName = visitorName; }

    public String getIdCardEncrypted() { return idCardEncrypted; }
    public void setIdCardEncrypted(String idCardEncrypted) { this.idCardEncrypted = idCardEncrypted; }

    public String getIdCardMasked() { return idCardMasked; }
    public void setIdCardMasked(String idCardMasked) { this.idCardMasked = idCardMasked; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Long getHostUserId() { return hostUserId; }
    public void setHostUserId(Long hostUserId) { this.hostUserId = hostUserId; }

    public String getHostName() { return hostName; }
    public void setHostName(String hostName) { this.hostName = hostName; }

    public String getHostDept() { return hostDept; }
    public void setHostDept(String hostDept) { this.hostDept = hostDept; }

    public String getVisitPurpose() { return visitPurpose; }
    public void setVisitPurpose(String visitPurpose) { this.visitPurpose = visitPurpose; }

    public LocalDateTime getVisitTime() { return visitTime; }
    public void setVisitTime(LocalDateTime visitTime) { this.visitTime = visitTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getNdaSigned() { return ndaSigned; }
    public void setNdaSigned(Integer ndaSigned) { this.ndaSigned = ndaSigned; }

    public LocalDateTime getNdaSignedAt() { return ndaSignedAt; }
    public void setNdaSignedAt(LocalDateTime ndaSignedAt) { this.ndaSignedAt = ndaSignedAt; }

    public String getPassToken() { return passToken; }
    public void setPassToken(String passToken) { this.passToken = passToken; }

    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }

    public String getVerifiedBy() { return verifiedBy; }
    public void setVerifiedBy(String verifiedBy) { this.verifiedBy = verifiedBy; }

    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }

    public String getVisitDate() { return visitDate; }
    public void setVisitDate(String visitDate) { this.visitDate = visitDate; }

    public String getVisitStartTime() { return visitStartTime; }
    public void setVisitStartTime(String visitStartTime) { this.visitStartTime = visitStartTime; }

    public String getVisitEndTime() { return visitEndTime; }
    public void setVisitEndTime(String visitEndTime) { this.visitEndTime = visitEndTime; }

    public String getApproveToken() { return approveToken; }

    public void setApproveToken(String approveToken) { this.approveToken = approveToken; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}


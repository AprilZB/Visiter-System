package com.maitong.visitor.dto;

import lombok.Data;

@Data
public class SecurityScanDTO {
    private String visitNo;
    private String visitorName;
    private String idCardMasked; // 3301021234****1234
    private String phone;
    private String hostName;
    private String hostDept;
    private String visitPurpose;
    private String status;
    private boolean ndaSigned;
    private boolean canPass; // 是否允许放行
    private String warningMessage;

    public String getVisitNo() { return visitNo; }
    public void setVisitNo(String visitNo) { this.visitNo = visitNo; }

    public String getVisitorName() { return visitorName; }
    public void setVisitorName(String visitorName) { this.visitorName = visitorName; }

    public String getIdCardMasked() { return idCardMasked; }
    public void setIdCardMasked(String idCardMasked) { this.idCardMasked = idCardMasked; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getHostName() { return hostName; }
    public void setHostName(String hostName) { this.hostName = hostName; }

    public String getHostDept() { return hostDept; }
    public void setHostDept(String hostDept) { this.hostDept = hostDept; }

    public String getVisitPurpose() { return visitPurpose; }
    public void setVisitPurpose(String visitPurpose) { this.visitPurpose = visitPurpose; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isNdaSigned() { return ndaSigned; }
    public void setNdaSigned(boolean ndaSigned) { this.ndaSigned = ndaSigned; }

    public boolean isCanPass() { return canPass; }
    public void setCanPass(boolean canPass) { this.canPass = canPass; }

    public String getWarningMessage() { return warningMessage; }
    public void setWarningMessage(String warningMessage) { this.warningMessage = warningMessage; }
}


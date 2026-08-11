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

    // 多状态语义化分类及色彩控制
    private String resultCode; // PASS, PASS_MULTI, EXPIRED, NOT_FOUND, USED, PENDING_APPROVAL, REJECTED, NO_NDA
    private String resultTitle; // 如："准予放行(单次)"、"准予放行(多日通行)"、"多日凭证已过期"
    private String resultTheme; // green, teal, darkgray, gray, blue, orange, purple, red

    // 多日/多次到访相关
    private String visitType;     // SINGLE, MULTI
    private String visitStartDate;// 2026-08-11
    private String visitEndDate;  // 2026-08-15
    private int todayEntryCount;  // 今日已进出打卡次数

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

    public String getResultCode() { return resultCode; }
    public void setResultCode(String resultCode) { this.resultCode = resultCode; }

    public String getResultTitle() { return resultTitle; }
    public void setResultTitle(String resultTitle) { this.resultTitle = resultTitle; }

    public String getResultTheme() { return resultTheme; }
    public void setResultTheme(String resultTheme) { this.resultTheme = resultTheme; }

    public String getVisitType() { return visitType; }
    public void setVisitType(String visitType) { this.visitType = visitType; }

    public String getVisitStartDate() { return visitStartDate; }
    public void setVisitStartDate(String visitStartDate) { this.visitStartDate = visitStartDate; }

    public String getVisitEndDate() { return visitEndDate; }
    public void setVisitEndDate(String visitEndDate) { this.visitEndDate = visitEndDate; }

    public int getTodayEntryCount() { return todayEntryCount; }
    public void setTodayEntryCount(int todayEntryCount) { this.todayEntryCount = todayEntryCount; }
}



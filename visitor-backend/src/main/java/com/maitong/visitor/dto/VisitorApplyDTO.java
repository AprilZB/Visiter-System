package com.maitong.visitor.dto;

import lombok.Data;

@Data
public class VisitorApplyDTO {
    private String scenario; // A 或 B
    private String visitorName;
    private String idCard;
    private String phone;
    private Long hostUserId;
    private String visitPurpose;
    private String visitTime; // YYYY-MM-DD HH:mm:ss
    private String visitDate;
    private String visitStartTime;
    private String visitEndTime;


    public String getScenario() { return scenario; }
    public void setScenario(String scenario) { this.scenario = scenario; }

    public String getVisitorName() { return visitorName; }
    public void setVisitorName(String visitorName) { this.visitorName = visitorName; }

    public String getIdCard() { return idCard; }
    public void setIdCard(String idCard) { this.idCard = idCard; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Long getHostUserId() { return hostUserId; }
    public void setHostUserId(Long hostUserId) { this.hostUserId = hostUserId; }

    public String getVisitPurpose() { return visitPurpose; }
    public void setVisitPurpose(String visitPurpose) { this.visitPurpose = visitPurpose; }

    public String getVisitTime() { return visitTime; }
    public void setVisitTime(String visitTime) { this.visitTime = visitTime; }

    public String getVisitDate() { return visitDate; }
    public void setVisitDate(String visitDate) { this.visitDate = visitDate; }

    public String getVisitStartTime() { return visitStartTime; }
    public void setVisitStartTime(String visitStartTime) { this.visitStartTime = visitStartTime; }

    private String visitType; // SINGLE, MULTI
    private String visitStartDate;
    private String visitEndDate;

    public String getVisitType() { return visitType; }
    public void setVisitType(String visitType) { this.visitType = visitType; }

    public String getVisitStartDate() { return visitStartDate; }
    public void setVisitStartDate(String visitStartDate) { this.visitStartDate = visitStartDate; }

    public String getVisitEndDate() { return visitEndDate; }
    public void setVisitEndDate(String visitEndDate) { this.visitEndDate = visitEndDate; }
}



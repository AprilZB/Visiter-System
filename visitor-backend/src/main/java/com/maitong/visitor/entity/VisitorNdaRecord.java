package com.maitong.visitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("visitor_nda_records")
public class VisitorNdaRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long visitorRecordId;
    private String visitNo;
    private String visitorName;
    private String idCardEncrypted;
    private LocalDateTime signedAt;
    private String clientIp;
    private String deviceFingerprint;
    private String signatureBase64;
    private String ndaVersion;

    private String hashChain;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getVisitorRecordId() { return visitorRecordId; }
    public void setVisitorRecordId(Long visitorRecordId) { this.visitorRecordId = visitorRecordId; }

    public String getVisitNo() { return visitNo; }
    public void setVisitNo(String visitNo) { this.visitNo = visitNo; }

    public String getVisitorName() { return visitorName; }
    public void setVisitorName(String visitorName) { this.visitorName = visitorName; }

    public String getIdCardEncrypted() { return idCardEncrypted; }
    public void setIdCardEncrypted(String idCardEncrypted) { this.idCardEncrypted = idCardEncrypted; }

    public LocalDateTime getSignedAt() { return signedAt; }
    public void setSignedAt(LocalDateTime signedAt) { this.signedAt = signedAt; }

    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }

    public String getDeviceFingerprint() { return deviceFingerprint; }
    public void setDeviceFingerprint(String deviceFingerprint) { this.deviceFingerprint = deviceFingerprint; }

    public String getSignatureBase64() { return signatureBase64; }
    public void setSignatureBase64(String signatureBase64) { this.signatureBase64 = signatureBase64; }

    public String getNdaVersion() { return ndaVersion; }
    public void setNdaVersion(String ndaVersion) { this.ndaVersion = ndaVersion; }

    public String getHashChain() { return hashChain; }
    public void setHashChain(String hashChain) { this.hashChain = hashChain; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}


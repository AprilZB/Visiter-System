package com.maitong.visitor.dto;

import lombok.Data;

@Data
public class NdaSignDTO {
    private String visitNo;
    private String ndaVersion;
    private String clientIp;
    private String deviceFingerprint;
    private String signatureBase64;

    public String getVisitNo() { return visitNo; }
    public void setVisitNo(String visitNo) { this.visitNo = visitNo; }

    public String getNdaVersion() { return ndaVersion; }
    public void setNdaVersion(String ndaVersion) { this.ndaVersion = ndaVersion; }

    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }

    public String getDeviceFingerprint() { return deviceFingerprint; }
    public void setDeviceFingerprint(String deviceFingerprint) { this.deviceFingerprint = deviceFingerprint; }

    public String getSignatureBase64() { return signatureBase64; }
    public void setSignatureBase64(String signatureBase64) { this.signatureBase64 = signatureBase64; }
}



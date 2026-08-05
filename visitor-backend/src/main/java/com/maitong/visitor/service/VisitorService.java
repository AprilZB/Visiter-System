package com.maitong.visitor.service;

import com.maitong.visitor.dto.SecurityScanDTO;
import com.maitong.visitor.dto.VisitorApplyDTO;
import com.maitong.visitor.entity.VisitorRecord;

import java.util.List;
import java.util.Map;

public interface VisitorService {
    VisitorRecord applyVisit(VisitorApplyDTO dto);
    boolean approveVisit(Long recordId, boolean agree, String approverName);
    String getPassCodeToken(String visitNo);
    Map<String, Object> getLatestPassTokenByPhone(String phone);
    SecurityScanDTO securityScanVerify(String passToken);
    boolean confirmEntry(String visitNo, String securityName);
    VisitorRecord getByVisitNo(String visitNo);
    List<VisitorRecord> getHostPendingApprovals(Long hostUserId);
    List<VisitorRecord> getHostAllRecords(Long hostUserId);
    List<VisitorRecord> getAllRecords();
}

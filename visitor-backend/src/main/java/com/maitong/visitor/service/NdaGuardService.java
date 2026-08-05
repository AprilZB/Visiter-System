package com.maitong.visitor.service;

import com.maitong.visitor.dto.NdaSignDTO;
import com.maitong.visitor.entity.SysNdaTemplate;
import com.maitong.visitor.entity.VisitorNdaRecord;

public interface NdaGuardService {
    SysNdaTemplate getActiveNdaTemplate();
    boolean signNda(NdaSignDTO dto);
    VisitorNdaRecord getSignRecordByVisitNo(String visitNo);
    boolean checkNdaEnforcement(Long visitorRecordId);
    java.util.List<SysNdaTemplate> getAllTemplates();
    boolean publishNewTemplate(SysNdaTemplate template);
    boolean activateTemplate(Long id);
}



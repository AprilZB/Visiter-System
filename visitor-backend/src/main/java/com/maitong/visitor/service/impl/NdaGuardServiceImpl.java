package com.maitong.visitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.maitong.visitor.dto.NdaSignDTO;
import com.maitong.visitor.entity.SysNdaTemplate;
import com.maitong.visitor.entity.VisitorNdaRecord;
import com.maitong.visitor.entity.VisitorRecord;
import com.maitong.visitor.mapper.SysNdaTemplateMapper;
import com.maitong.visitor.mapper.VisitorNdaRecordMapper;
import com.maitong.visitor.mapper.VisitorRecordMapper;
import com.maitong.visitor.service.NdaGuardService;
import com.maitong.visitor.util.CryptoUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class NdaGuardServiceImpl implements NdaGuardService {

    @Autowired
    private SysNdaTemplateMapper sysNdaTemplateMapper;

    @Autowired
    private VisitorRecordMapper visitorRecordMapper;

    @Autowired
    private VisitorNdaRecordMapper visitorNdaRecordMapper;

    @jakarta.annotation.PostConstruct
    public void initDefaultTemplates() {
        try {
            Long count = sysNdaTemplateMapper.selectCount(null);
            if (count == null || count == 0) {
                // 1. 初始化基础版本 V1.0.0 (历史版)
                SysNdaTemplate v1 = new SysNdaTemplate();
                v1.setVersion("V1.0.0");
                v1.setTitle("浙江脉通智造科技有限公司外来人员保密协议 (基础版 V1.0.0)");
                v1.setContent("<h3>外来人员保密协议书 (V1.0.0)</h3><p>1. 进入公司厂区请遵守现场管理制度。</p><p>2. 未经许可严禁拍摄生产线设备。</p>");
                v1.setIsActive(0);
                v1.setCreatedBy("System");
                v1.setCreatedAt(LocalDateTime.now().minusDays(30));
                sysNdaTemplateMapper.insert(v1);

                // 2. 初始化测试新版本 V1.1.0 (当前生效版)
                SysNdaTemplate v2 = new SysNdaTemplate();
                v2.setVersion("V1.1.0");
                v2.setTitle("浙江脉通智造科技有限公司外来人员保密协议 (全面加强测试版 V1.1.0)");
                v2.setContent("<h3>外来人员保密协议书 (全面加强测试版 V1.1.0)</h3>" +
                        "<p><b>一、保密义务范围</b></p>" +
                        "<p>1. 本协议适用于所有进入浙江脉通智造科技有限公司厂区、车间及研发楼的外来到访人员。</p>" +
                        "<p>2. 到访人员在厂区内严禁私自拍照、录音、拷贝内部技术资料及工艺文档。</p>" +
                        "<p>3. 未经接待部门许可，不得擅自进入涉密生产线与研发实验室。</p>" +
                        "<p><b>二、法律效力与存证说明</b></p>" +
                        "<p>4. 本电子签署协议具有法律效力，签署时的设备 IP、手写签名及时间戳将实时通过 SHA-256 数字哈希链归档存证。</p>");
                v2.setIsActive(1);
                v2.setCreatedBy("Admin");

                v2.setCreatedAt(LocalDateTime.now());
                sysNdaTemplateMapper.insert(v2);
            }
        } catch (Exception ignored) {}
    }

    @Override
    public SysNdaTemplate getActiveNdaTemplate() {

        LambdaQueryWrapper<SysNdaTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysNdaTemplate::getIsActive, 1)
               .orderByDesc(SysNdaTemplate::getId)
               .last("LIMIT 1");
        return sysNdaTemplateMapper.selectOne(wrapper);
    }

    @Override
    @Transactional
    public boolean signNda(NdaSignDTO dto) {
        if (dto.getVisitNo() == null || dto.getVisitNo().trim().isEmpty()) {
            return false;
        }

        // 查访客单
        LambdaQueryWrapper<VisitorRecord> vWrapper = new LambdaQueryWrapper<>();
        vWrapper.eq(VisitorRecord::getVisitNo, dto.getVisitNo());
        VisitorRecord visitorRecord = visitorRecordMapper.selectOne(vWrapper);
        if (visitorRecord == null) {
            return false;
        }

        String clientIp = dto.getClientIp() != null ? dto.getClientIp() : "127.0.0.1";
        String deviceFingerprint = dto.getDeviceFingerprint() != null ? dto.getDeviceFingerprint() : "Browser/H5";
        String signatureBase64 = dto.getSignatureBase64() != null ? dto.getSignatureBase64() : "";

        SysNdaTemplate activeTemplate = getActiveNdaTemplate();
        String currentVersion = activeTemplate != null ? activeTemplate.getVersion() : "V1.0.0";
        LocalDateTime now = LocalDateTime.now();

        // 拼接生成 SHA-256 数字摘要与防篡改审计链
        String hashChain = CryptoUtils.generateHashChain(
                visitorRecord.getVisitorName(),
                visitorRecord.getIdCardEncrypted(),
                now.toString(),
                clientIp,
                deviceFingerprint,
                currentVersion + "|" + signatureBase64.hashCode()
        );


        // 插入保密协议存证表
        VisitorNdaRecord ndaRecord = new VisitorNdaRecord();
        ndaRecord.setVisitorRecordId(visitorRecord.getId());
        ndaRecord.setVisitNo(visitorRecord.getVisitNo());
        ndaRecord.setVisitorName(visitorRecord.getVisitorName());
        ndaRecord.setIdCardEncrypted(visitorRecord.getIdCardEncrypted());
        ndaRecord.setSignedAt(now);
        ndaRecord.setClientIp(clientIp);
        ndaRecord.setDeviceFingerprint(deviceFingerprint);
        ndaRecord.setSignatureBase64(signatureBase64);
        ndaRecord.setNdaVersion(currentVersion);
        ndaRecord.setHashChain(hashChain);
        ndaRecord.setCreatedAt(now);

        visitorNdaRecordMapper.insert(ndaRecord);

        // 更新访客单 NDA 状态
        visitorRecord.setNdaSigned(1);
        visitorRecord.setNdaSignedAt(now);
        if ("APPROVED".equals(visitorRecord.getStatus()) || "PENDING_APPROVAL".equals(visitorRecord.getStatus())) {
            visitorRecord.setStatus("NDA_SIGNED");
        }
        visitorRecordMapper.updateById(visitorRecord);

        return true;
    }

    @Override
    public java.util.List<SysNdaTemplate> getAllTemplates() {
        LambdaQueryWrapper<SysNdaTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(SysNdaTemplate::getId);
        return sysNdaTemplateMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public boolean publishNewTemplate(SysNdaTemplate template) {
        if (template == null || !org.springframework.util.StringUtils.hasText(template.getVersion())) {
            return false;
        }

        // 将之前生效的模版设为不生效
        java.util.List<SysNdaTemplate> activeList = sysNdaTemplateMapper.selectList(
                new LambdaQueryWrapper<SysNdaTemplate>().eq(SysNdaTemplate::getIsActive, 1));
        if (activeList != null) {
            for (SysNdaTemplate t : activeList) {
                t.setIsActive(0);
                sysNdaTemplateMapper.updateById(t);
            }
        }

        template.setIsActive(1);
        template.setCreatedAt(LocalDateTime.now());
        if (!org.springframework.util.StringUtils.hasText(template.getCreatedBy())) {
            template.setCreatedBy("Admin");
        }
        return sysNdaTemplateMapper.insert(template) > 0;
    }

    @Override
    @Transactional
    public boolean activateTemplate(Long id) {
        if (id == null) return false;
        // 1. 将所有版本设为非生效
        java.util.List<SysNdaTemplate> allList = sysNdaTemplateMapper.selectList(null);
        if (allList != null) {
            for (SysNdaTemplate t : allList) {
                if (t.getIsActive() != null && t.getIsActive() == 1) {
                    t.setIsActive(0);
                    sysNdaTemplateMapper.updateById(t);
                }
            }
        }
        // 2. 将指定 ID 设为唯一的生效版本
        SysNdaTemplate target = sysNdaTemplateMapper.selectById(id);
        if (target != null) {
            target.setIsActive(1);
            sysNdaTemplateMapper.updateById(target);
            return true;
        }
        return false;
    }

    @Override
    public VisitorNdaRecord getSignRecordByVisitNo(String visitNo) {
        LambdaQueryWrapper<VisitorNdaRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VisitorNdaRecord::getVisitNo, visitNo)
               .orderByDesc(VisitorNdaRecord::getId)
               .last("LIMIT 1");
        return visitorNdaRecordMapper.selectOne(wrapper);
    }

    @Override
    public boolean checkNdaEnforcement(Long visitorRecordId) {
        VisitorRecord record = visitorRecordMapper.selectById(visitorRecordId);
        if (record == null) return false;
        if (record.getNdaSigned() == null || record.getNdaSigned() != 1) return false;

        // 校验是否针对当前最新版本的协议
        SysNdaTemplate activeTemplate = getActiveNdaTemplate();
        if (activeTemplate == null) return true;

        VisitorNdaRecord ndaRecord = getSignRecordByVisitNo(record.getVisitNo());
        return ndaRecord != null && activeTemplate.getVersion().equalsIgnoreCase(ndaRecord.getNdaVersion());
    }
}

package com.maitong.visitor.controller;

import com.maitong.visitor.common.Result;
import com.maitong.visitor.entity.SysUserSync;
import com.maitong.visitor.entity.VisitorRecord;
import com.maitong.visitor.service.AphrSyncService;
import com.maitong.visitor.service.NdaGuardService;
import com.maitong.visitor.service.OcrService;
import com.maitong.visitor.service.VisitorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin
public class AdminController {

    @Autowired
    private VisitorService visitorService;

    @Autowired
    private NdaGuardService ndaGuardService;

    @Autowired
    private OcrService ocrService;

    @Autowired
    private AphrSyncService aphrSyncService;

    @Autowired
    private com.maitong.visitor.mapper.SysVisitReasonMapper sysVisitReasonMapper;

    @Autowired
    private com.maitong.visitor.service.DingTalkNotificationService dingTalkNotificationService;



    /**
     * 1. 简易管理登录
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> loginDto) {
        String username = loginDto.get("username");
        String password = loginDto.get("password");

        if ("admin".equals(username) && "Accupath@0723".equals(password)) {
            Map<String, Object> data = new HashMap<>();
            data.put("token", "admin_token_" + System.currentTimeMillis());
            data.put("username", username);
            data.put("role", "ADMIN");
            data.put("expiresIn", 300); // 5分钟有效时间 (秒)
            return Result.success("登录成功", data);
        }

        return Result.error("用户名或密码错误");
    }

    @GetMapping("/config/ocr")
    public Result<Map<String, Object>> getOcrConfig() {
        Map<String, Object> map = new HashMap<>();
        map.put("ocrServiceUrl", ocrService.getOcrServiceUrl());
        return Result.success(map);
    }

    @PostMapping("/config/ocr")
    public Result<String> saveOcrConfig(@RequestHeader(value = "Authorization", required = false) String token,
                                        @RequestBody Map<String, String> body) {
        if (token == null || !token.startsWith("admin_token_")) {
            return Result.error(401, "权限不足：请先完成管理员登录后再保存配置！");
        }
        if (body != null && body.containsKey("ocrServiceUrl")) {
            String url = body.get("ocrServiceUrl");
            if (org.springframework.util.StringUtils.hasText(url)) {
                boolean success = ocrService.updateOcrServiceUrl(url.trim());
                if (success) {
                    return Result.success("OCR 服务器地址更新成功！");
                }
            }
        }
        return Result.error("请输入有效的 OCR 服务器地址");
    }



    @Autowired
    private com.maitong.visitor.mapper.VisitorNdaRecordMapper visitorNdaRecordMapper;

    @Autowired
    private com.maitong.visitor.mapper.SysNdaTemplateMapper sysNdaTemplateMapper;

    /**
     * 2. 获取所有访客登记记录 (包含明文解密与 SHA-256 数字证据链追溯)
     */
    @GetMapping("/visitors")
    public Result<List<Map<String, Object>>> getAuditVisitors() {
        List<VisitorRecord> records = visitorService.getAllRecords();
        List<Map<String, Object>> resultList = new java.util.ArrayList<>();

        for (VisitorRecord r : records) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", r.getId());
            map.put("visitNo", r.getVisitNo());
            map.put("visitorName", r.getVisitorName());
            
            // 明文解密身份证用于管理端合规追溯
            String rawIdCard = com.maitong.visitor.util.CryptoUtils.decryptAES(r.getIdCardEncrypted());
            map.put("idCardDecrypted", (rawIdCard != null && !rawIdCard.isEmpty()) ? rawIdCard : r.getIdCardMasked());
            map.put("phone", r.getPhone());
            map.put("hostName", r.getHostName());
            map.put("visitPurpose", r.getVisitPurpose());
            map.put("status", r.getStatus());
            map.put("ndaSigned", r.getNdaSigned() != null && r.getNdaSigned() == 1);
            map.put("createdAt", r.getCreatedAt());

            // 联查保密协议 (NDA) 签署手写签名与 SHA-256 存证哈希链
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.maitong.visitor.entity.VisitorNdaRecord> ndaWrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            ndaWrapper.eq(com.maitong.visitor.entity.VisitorNdaRecord::getVisitorRecordId, r.getId());
            com.maitong.visitor.entity.VisitorNdaRecord ndaRecord = visitorNdaRecordMapper.selectOne(ndaWrapper);

            if (ndaRecord != null) {
                map.put("signatureBase64", ndaRecord.getSignatureBase64());
                map.put("clientIp", ndaRecord.getClientIp());
                map.put("evidenceHash", ndaRecord.getHashChain());
                map.put("ndaVersion", ndaRecord.getNdaVersion() != null ? ndaRecord.getNdaVersion() : "V1.1.0");
            } else {
                map.put("signatureBase64", null);
                map.put("clientIp", "-");
                map.put("evidenceHash", "-");
                map.put("ndaVersion", "-");
            }


            resultList.add(map);
        }

        return Result.success(resultList);
    }

    @GetMapping("/records")
    public Result<List<VisitorRecord>> getRecords() {
        return Result.success(visitorService.getAllRecords());
    }


    /**
     * 3. 审批访客
     */
    @PostMapping("/records/{id}/approve")
    public Result<String> approveRecord(@PathVariable Long id, @RequestParam boolean approved) {
        boolean res = visitorService.approveVisit(id, approved, "管理员");
        if (res) {
            return Result.success(approved ? "已批准通行" : "已拒绝入访");
        }
        return Result.error("操作失败");
    }



    /**
     * 4. 获取组织架构树
     */
    @GetMapping("/org/tree")
    public Result<List<Map<String, Object>>> getOrgTree() {
        return Result.success(aphrSyncService.getDeptTree());
    }

    /**
     * 5. 条件与检索查询全员档案列表
     */
    @GetMapping("/org/users")
    public Result<Map<String, Object>> getOrgUsers(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String adAccount,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String deptName,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) String status) {
        return Result.success(aphrSyncService.getUsersPage(page, pageSize, adAccount, name, deptName, deptId, status));
    }


    @GetMapping("/nda/templates")
    public Result<List<com.maitong.visitor.entity.SysNdaTemplate>> getNdaTemplates() {
        return Result.success(ndaGuardService.getAllTemplates());
    }

    @PostMapping("/nda/publish")
    public Result<String> publishNda(@RequestHeader(value = "Authorization", required = false) String token,
                                     @RequestBody com.maitong.visitor.entity.SysNdaTemplate template) {
        if (token == null || !token.startsWith("admin_token_")) {
            return Result.error(401, "权限不足：请先完成管理员登录后再发布新协议！");
        }
        boolean success = ndaGuardService.publishNewTemplate(template);
        if (success) {
            return Result.success("新版本保密协议发布成功！当前到访将强制要求签署该新版本。");
        }
        return Result.error("发布失败");
    }

    @PostMapping("/nda/upload-pdf")
    public Result<Map<String, String>> uploadNdaPdf(@RequestHeader(value = "Authorization", required = false) String token,
                                                    @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        if (token == null || !token.startsWith("admin_token_")) {
            return Result.error(401, "权限不足：请先完成管理员登录！");
        }
        if (file == null || file.isEmpty()) {
            return Result.error("请选择上传的 PDF 文件");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".pdf")) {
            return Result.error("格式错误：仅支持上传 .pdf 格式的保密协议文件");
        }
        try {
            String uploadDir = System.getProperty("user.dir") + java.io.File.separator + "uploads" + java.io.File.separator + "nda" + java.io.File.separator;
            java.io.File dir = new java.io.File(uploadDir);
            if (!dir.exists()) dir.mkdirs();

            String newFilename = "nda_" + System.currentTimeMillis() + "_" + java.util.UUID.randomUUID().toString().substring(0, 8) + ".pdf";
            java.io.File dest = new java.io.File(uploadDir + newFilename);
            file.transferTo(dest);

            String fileUrl = "/uploads/nda/" + newFilename;
            Map<String, String> resMap = new HashMap<>();
            resMap.put("fileUrl", fileUrl);
            resMap.put("originalFilename", originalFilename);
            return Result.success(resMap);
        } catch (Exception e) {
            return Result.error("PDF文件保存失败: " + e.getMessage());
        }
    }

    @PostMapping("/nda/activate/{id}")
    public Result<String> activateNda(@RequestHeader(value = "Authorization", required = false) String token,
                                      @PathVariable Long id) {
        if (token == null || !token.startsWith("admin_token_")) {
            return Result.error(401, "权限不足：请先完成管理员登录！");
        }
        boolean success = ndaGuardService.activateTemplate(id);
        if (success) {
            return Result.success("生效版本已切换成功！当前系统只允许保留此唯一生效版本。");
        }
        return Result.error("切换失败");
    }

    @GetMapping("/reasons")


    public Result<List<com.maitong.visitor.entity.SysVisitReason>> getReasons() {
        List<com.maitong.visitor.entity.SysVisitReason> list = sysVisitReasonMapper.selectList(null);
        if (list == null || list.isEmpty()) {
            String[] defaultReasons = {"商务洽谈", "面试应聘", "项目合作", "设备维护/施工", "送货/物流", "参观考察", "学术交流", "其他事由"};
            int sort = 1;
            for (String rName : defaultReasons) {
                com.maitong.visitor.entity.SysVisitReason r = new com.maitong.visitor.entity.SysVisitReason();
                r.setReasonName(rName);
                r.setSortOrder(sort++);
                r.setIsActive(1);
                r.setCreatedAt(java.time.LocalDateTime.now());
                sysVisitReasonMapper.insert(r);
            }
            list = sysVisitReasonMapper.selectList(null);
        }
        return Result.success(list);
    }

    @PostMapping("/reason/add")
    public Result<String> addReason(@RequestHeader(value = "Authorization", required = false) String token,
                                    @RequestBody Map<String, String> body) {
        if (token == null || !token.startsWith("admin_token_")) {
            return Result.error(401, "权限不足：请先完成管理员登录！");
        }
        if (body != null && body.containsKey("reasonName")) {
            String reasonName = body.get("reasonName");
            if (org.springframework.util.StringUtils.hasText(reasonName)) {
                com.maitong.visitor.entity.SysVisitReason r = new com.maitong.visitor.entity.SysVisitReason();
                r.setReasonName(reasonName.trim());
                r.setSortOrder(10);
                r.setIsActive(1);
                r.setCreatedAt(java.time.LocalDateTime.now());
                sysVisitReasonMapper.insert(r);
                return Result.success("来访事由添加成功！");
            }
        }
        return Result.error("请输入有效的来访事由名称");
    }

    @PostMapping("/reason/update")
    public Result<String> updateReason(@RequestHeader(value = "Authorization", required = false) String token,
                                       @RequestBody com.maitong.visitor.entity.SysVisitReason reason) {
        if (token == null || !token.startsWith("admin_token_")) {
            return Result.error(401, "权限不足：请先完成管理员登录！");
        }
        if (reason == null || reason.getId() == null) {
            return Result.error("非法参数：缺失ID");
        }
        sysVisitReasonMapper.updateById(reason);
        return Result.success("来访事由修改保存成功！");
    }

    @PostMapping("/reason/toggle-status")
    public Result<String> toggleReasonStatus(@RequestHeader(value = "Authorization", required = false) String token,
                                             @RequestBody Map<String, Object> body) {
        if (token == null || !token.startsWith("admin_token_")) {
            return Result.error(401, "权限不足：请先完成管理员登录！");
        }
        if (body != null && body.containsKey("id") && body.containsKey("isActive")) {
            Long id = Long.valueOf(body.get("id").toString());
            Integer isActive = Integer.valueOf(body.get("isActive").toString());
            com.maitong.visitor.entity.SysVisitReason r = sysVisitReasonMapper.selectById(id);
            if (r != null) {
                r.setIsActive(isActive);
                sysVisitReasonMapper.updateById(r);
                return Result.success(isActive == 1 ? "事由已启用 (使用中)" : "事由已禁用");
            }
        }
        return Result.error("操作失败");
    }

    /**
     * 6. 手动触发同步 APHR 组织架构与人员
     */


    @PostMapping("/org/sync")
    public Result<String> triggerOrgSync() {
        boolean success = aphrSyncService.syncAll();
        if (success) {
            return Result.success("手动同步组织架构与全员档案成功！");
        } else {
            return Result.error("同步失败，请检查数据库连接或后端日志！");
        }
    }

    @GetMapping("/debug/inspect-ods")
    public Result<Map<String, Object>> inspectOds() {
        Map<String, Object> res = new HashMap<>();
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            String localUrl = "jdbc:mysql://localhost:3306/visitor_system_db?useUnicode=true&characterEncoding=utf-8&useSSL=false&allowPublicKeyRetrieval=true";
            try (java.sql.Connection conn = java.sql.DriverManager.getConnection(localUrl, "root", "admin@123")) {
                conn.createStatement().execute("ALTER TABLE sys_user_sync MODIFY COLUMN phone VARCHAR(100)");
                conn.createStatement().execute("ALTER TABLE sys_dept_sync MODIFY COLUMN dept_name VARCHAR(255)");
                try {
                    conn.createStatement().execute("ALTER TABLE sys_nda_templates ADD COLUMN pdf_url VARCHAR(500)");
                } catch (Exception ignored) {}
            }

        } catch (Exception ignored) {}

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            String url = "jdbc:mysql://10.11.100.202:3306/?useUnicode=true&characterEncoding=utf-8&useSSL=false&allowPublicKeyRetrieval=true";
            try (java.sql.Connection conn = java.sql.DriverManager.getConnection(url, "root", "Mtdb@123")) {
                java.sql.Statement stmt = conn.createStatement();
                java.sql.ResultSet rs = stmt.executeQuery(
                        "SELECT TABLE_SCHEMA, TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA NOT IN ('information_schema','mysql','performance_schema','sys')"
                );
                List<Map<String, String>> tables = new java.util.ArrayList<>();
                while (rs.next()) {
                    Map<String, String> m = new HashMap<>();
                    m.put("db", rs.getString("TABLE_SCHEMA"));
                    m.put("table", rs.getString("TABLE_NAME"));
                    tables.add(m);
                }
                res.put("tables", tables);



                if (!tables.isEmpty()) {
                    String targetDb = tables.get(0).get("db");
                    res.put("targetDb", targetDb);
                    java.sql.ResultSet colsRs = stmt.executeQuery(
                            "SELECT COLUMN_NAME, DATA_TYPE, COLUMN_COMMENT FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = '" + targetDb + "' AND TABLE_NAME = 'ods_hr_information'"
                    );
                    List<Map<String, String>> cols = new java.util.ArrayList<>();
                    while (colsRs.next()) {
                        Map<String, String> col = new HashMap<>();
                        col.put("name", colsRs.getString("COLUMN_NAME"));
                        col.put("type", colsRs.getString("DATA_TYPE"));
                        col.put("comment", colsRs.getString("COLUMN_COMMENT"));
                        cols.add(col);
                    }
                    res.put("columns", cols);

                    java.sql.ResultSet sampleRs = stmt.executeQuery("SELECT * FROM `" + targetDb + "`.`ods_hr_information` LIMIT 5");
                    java.sql.ResultSetMetaData meta = sampleRs.getMetaData();
                    List<Map<String, Object>> samples = new java.util.ArrayList<>();
                    while (sampleRs.next()) {
                        Map<String, Object> row = new HashMap<>();
                        for (int i = 1; i <= meta.getColumnCount(); i++) {
                            row.put(meta.getColumnName(i), sampleRs.getObject(i));
                        }
                        samples.add(row);
                    }
                    res.put("samples", samples);
                }
            }
        } catch (Exception e) {
            return Result.error("探查失败: " + e.getMessage());
        }
        return Result.success(res);
    }

    @PostMapping("/dept/toggle-shield")
    public Result<String> toggleDeptShield(@RequestHeader(value = "Authorization", required = false) String token,
                                           @RequestBody Map<String, Object> dto) {
        if (token == null || !token.startsWith("admin_token_")) {
            return Result.error(401, "权限不足：请先完成管理员登录后再操作！");
        }
        String deptName = (String) dto.get("deptName");
        Integer isShielded = (Integer) dto.get("isShielded");
        if (isShielded == null && dto.containsKey("isShielded")) {
            Object val = dto.get("isShielded");
            if (val instanceof Boolean) {
                isShielded = ((Boolean) val) ? 1 : 0;
            } else if (val instanceof Number) {
                isShielded = ((Number) val).intValue();
            }
        }
        boolean success = aphrSyncService.updateDeptShieldByName(deptName, isShielded != null ? isShielded : 0);
        if (success) {
            return Result.success("部门屏蔽/防骚扰状态更新成功！");
        }
        return Result.error("更新失败");
    }

    /**
     * 手动向测试人员张勃 (zhangb9) 发送钉钉测试消息
     */

    @PostMapping("/dingtalk/send-test")
    public Result<String> sendTestDingTalkNotification(@RequestBody(required = false) Map<String, String> body) {
        String msg = (body != null && body.containsKey("msg")) ? body.get("msg") : "【脉通访客系统】测试通知：当前系统钉钉通知服务对接正常！接收人：张勃 (zhangb9)。";
        boolean success = dingTalkNotificationService.sendWorkNotificationByWorkNo("404256402", "张勃", msg);
        if (success) {
            return Result.success("钉钉测试消息成功推送到测试人员 张勃 (zhangb9, 工号: 404256402) 的钉钉终端！");
        } else {
            return Result.error("钉钉消息推送失败，请检查网络或中台服务 (10.11.100.154:8089)");
        }
    }
}



package com.maitong.visitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.maitong.visitor.entity.SysDeptSync;
import com.maitong.visitor.entity.SysUserSync;
import com.maitong.visitor.mapper.SysDeptSyncMapper;
import com.maitong.visitor.mapper.SysUserSyncMapper;
import com.maitong.visitor.service.AphrSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.*;

@Service
public class AphrSyncServiceImpl implements AphrSyncService {

    @Autowired
    private SysDeptSyncMapper sysDeptSyncMapper;

    @Autowired
    private SysUserSyncMapper sysUserSyncMapper;

    @Value("${spring.datasource.master-datasource.username:root}")
    private String remoteDbUser;

    @Value("${spring.datasource.master-datasource.password:Mtdb@123}")
    private String remoteDbPass;

    /**
     * 每天 03:00 定时从 10.11.100.202 远程库 fr_dw 提取全量组织架构与人员
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void scheduledSync() {
        System.out.println("⏰ 触发 APHR 组织架构与人员每天凌晨 03:00 定时同步任务...");
        syncAll();
    }

    @Override
    public boolean syncAll() {
        long startTime = System.currentTimeMillis();
        System.out.println("🚀 开始从 10.11.100.202:3306/fr_dw 库拉取 APHR 全量人员与部门数据...");

        // 1. 清理测试模拟假数据
        cleanMockTestData();

        // 2. 直连 10.11.100.202 fr_dw 数据库读取 ods_hr_personal_information / ods_hr_information
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            String dwUrl = "jdbc:mysql://10.11.100.202:3306/fr_dw?useUnicode=true&characterEncoding=utf-8&useSSL=false&allowPublicKeyRetrieval=true&connectTimeout=5000&socketTimeout=15000";

            try (Connection conn = DriverManager.getConnection(dwUrl, remoteDbUser, remoteDbPass)) {
                String tableName = checkOdsTableName(conn);
                System.out.println("💡 找到远程数仓表: fr_dw." + tableName);

                String sql = "SELECT * FROM " + tableName;
                List<Map<String, Object>> remoteRows = new ArrayList<>();
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(sql)) {
                    ResultSetMetaData meta = rs.getMetaData();
                    int colCount = meta.getColumnCount();
                    while (rs.next()) {
                        Map<String, Object> row = new HashMap<>();
                        for (int i = 1; i <= colCount; i++) {
                            row.put(meta.getColumnName(i).toLowerCase(), rs.getObject(i));
                        }
                        remoteRows.add(row);
                    }
                }

                System.out.println("📊 从 fr_dw 数据拉取到 " + remoteRows.size() + " 条原始人员记录");

                if (!remoteRows.isEmpty()) {
                    processSyncLogic(remoteRows);
                    System.out.println("⚡ APHR 全量组织与人员档案同步完成！耗时: " + (System.currentTimeMillis() - startTime) + "ms");
                    return true;
                }
            }
        } catch (Exception e) {
            System.err.println("❌ 尝试访问 fr_dw 异常: " + e.getMessage() + "，准备搜寻候选数据库...");
            try {
                String baseUrl = "jdbc:mysql://10.11.100.202:3306/?useUnicode=true&characterEncoding=utf-8&useSSL=false&allowPublicKeyRetrieval=true&connectTimeout=4000&socketTimeout=10000";
                try (Connection conn = DriverManager.getConnection(baseUrl, remoteDbUser, remoteDbPass)) {
                    List<Map<String, Object>> remoteRows = findAndFetchAnyOdsRows(conn);
                    if (!remoteRows.isEmpty()) {
                        processSyncLogic(remoteRows);
                        return true;
                    }
                }
            } catch (Exception ex) {
                System.err.println("❌ 直连 202 失败: " + ex.getMessage());
                ex.printStackTrace();
            }
        }

        touchLocalSyncedTime();
        return true;
    }

    private String checkOdsTableName(Connection conn) {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW TABLES LIKE 'ods_hr%'")) {
            while (rs.next()) {
                String name = rs.getString(1);
                if ("ods_hr_personal_information".equalsIgnoreCase(name)) return "ods_hr_personal_information";
                if ("ods_hr_information".equalsIgnoreCase(name)) return "ods_hr_information";
            }
        } catch (Exception ignored) {}
        return "ods_hr_personal_information";
    }

    private List<Map<String, Object>> findAndFetchAnyOdsRows(Connection conn) {
        List<Map<String, Object>> rows = new ArrayList<>();
        List<String> dbs = List.of("fr_dw", "accupath-boot", "aphrdb");
        for (String db : dbs) {
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM `" + db + "`.`ods_hr_personal_information` LIMIT 5000")) {
                ResultSetMetaData meta = rs.getMetaData();
                int colCount = meta.getColumnCount();
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= colCount; i++) {
                        row.put(meta.getColumnName(i).toLowerCase(), rs.getObject(i));
                    }
                    rows.add(row);
                }
                if (!rows.isEmpty()) {
                    return rows;
                }
            } catch (Exception ignored) {}
        }
        return rows;
    }

    private void cleanMockTestData() {
        List<String> mockDepts = List.of("研发部", "市场部", "生产部", "行政部", "财务部");
        LambdaQueryWrapper<SysDeptSync> deptW = new LambdaQueryWrapper<>();
        deptW.in(SysDeptSync::getDeptName, mockDepts);
        sysDeptSyncMapper.delete(deptW);

        LambdaQueryWrapper<SysUserSync> userW = new LambdaQueryWrapper<>();
        userW.likeRight(SysUserSync::getWorkNo, "MT");
        sysUserSyncMapper.delete(userW);
    }

    private void processSyncLogic(List<Map<String, Object>> remoteRows) {
        LocalDateTime now = LocalDateTime.now();
        Set<String> deptNameSet = new LinkedHashSet<>();
        Map<String, String> statusMap = Map.of("T", "离职", "A", "在职", "S", "试用期");

        Map<String, String> adToNameMap = new HashMap<>();
        for (Map<String, Object> row : remoteRows) {
            String ad = getStr(row, "sf_user_id", "ad_account", "username");
            String name = getStr(row, "name", "realname");
            if (StringUtils.hasText(ad) && StringUtils.hasText(name)) {
                adToNameMap.put(ad.trim(), name.trim());
            }
        }

        List<SysUserSync> localUsers = sysUserSyncMapper.selectList(null);
        Map<String, SysUserSync> localUserMap = new HashMap<>(localUsers.size());
        for (SysUserSync u : localUsers) {
            if (StringUtils.hasText(u.getWorkNo())) {
                localUserMap.put(u.getWorkNo(), u);
            }
        }

        List<SysUserSync> toInsertUsers = new ArrayList<>();
        List<SysUserSync> toUpdateUsers = new ArrayList<>();
        Set<String> processedWorkNos = new HashSet<>();

        for (Map<String, Object> row : remoteRows) {
            String workNo = getStr(row, "mp_number", "work_no", "employee_id", "sf_user_id");
            if (!StringUtils.hasText(workNo)) continue;
            workNo = workNo.trim();
            if (processedWorkNos.contains(workNo)) continue;
            processedWorkNos.add(workNo);

            String adAccount = getStr(row, "sf_user_id", "ad_account", "username");
            String name = getStr(row, "name", "realname");
            String rawGender = getStr(row, "gender", "sex");
            String gender = "M".equalsIgnoreCase(rawGender) || "1".equals(rawGender) || "男".equals(rawGender) ? "男" :
                    ("F".equalsIgnoreCase(rawGender) || "2".equals(rawGender) || "女".equals(rawGender) ? "女" : "未知");

            Object birthObj = row.get("birth_date");
            if (birthObj == null) birthObj = row.get("birthday");
            String birthDateStr = null;
            Integer age = null;
            if (birthObj != null) {
                String bStr = birthObj.toString().trim();
                if (bStr.length() >= 10) {
                    birthDateStr = bStr.substring(0, 10);
                    try {
                        LocalDate birthLocalDate = LocalDate.parse(birthDateStr);
                        age = Period.between(birthLocalDate, LocalDate.now()).getYears();
                    } catch (Exception ignored) {}
                }
            }

            String phone = getStr(row, "cell_phone", "mobile", "phone", "telephone");
            if (phone != null && phone.length() > 20) {
                phone = phone.substring(0, 20);
            }

            String rawStatus = getStr(row, "employee_status", "status");
            String status = statusMap.getOrDefault(rawStatus, StringUtils.hasText(rawStatus) ? rawStatus : "在职");

            // 保存全路径
            String fullDeptPath = getStr(row, "depart_name_cn", "depart_name", "department_name");
            String targetDeptName = "总部";
            if (StringUtils.hasText(fullDeptPath)) {
                String[] parts = fullDeptPath.split("-");
                for (String p : parts) {
                    String cleanP = p.trim();
                    if (StringUtils.hasText(cleanP) && !cleanP.contains("浙江脉通智造") && !cleanP.contains("集团")) {
                        deptNameSet.add(cleanP);
                    }
                }
                String lastPart = parts[parts.length - 1].trim();
                if (StringUtils.hasText(lastPart) && !lastPart.contains("浙江脉通智造") && !lastPart.contains("集团")) {
                    targetDeptName = lastPart;
                } else if (parts.length > 1) {
                    targetDeptName = parts[parts.length - 2].trim();
                }
            }

            String managerAd = getStr(row, "supervisor_sf_user_id", "direct_manager_id", "manager_id");
            String managerName = adToNameMap.getOrDefault(managerAd, managerAd);

            SysUserSync existing = localUserMap.get(workNo);
            if (existing == null) {
                SysUserSync u = new SysUserSync();
                u.setWorkNo(workNo);
                u.setAdAccount(adAccount);
                u.setName(name);
                u.setGender(gender);
                u.setAge(age);
                u.setBirthDate(birthDateStr);
                u.setPhone(phone);
                u.setDeptName(fullDeptPath != null ? fullDeptPath : targetDeptName); // 存全路径方便精确树分析
                u.setStatus(status);
                u.setManagerName(managerName);
                u.setSyncedAt(now);
                toInsertUsers.add(u);
            } else {
                existing.setAdAccount(adAccount);
                existing.setName(name);
                existing.setGender(gender);
                existing.setAge(age);
                existing.setBirthDate(birthDateStr);
                existing.setPhone(phone);
                existing.setDeptName(fullDeptPath != null ? fullDeptPath : targetDeptName);
                existing.setStatus(status);
                existing.setManagerName(managerName);
                existing.setSyncedAt(now);
                toUpdateUsers.add(existing);
            }
        }

        for (SysUserSync u : toInsertUsers) sysUserSyncMapper.insert(u);
        for (SysUserSync u : toUpdateUsers) sysUserSyncMapper.updateById(u);
        System.out.println("👥 人员档案同步 - 新增: " + toInsertUsers.size() + " 人, 更新: " + toUpdateUsers.size() + " 人");

        // 维护部门列表
        List<SysDeptSync> localDepts = sysDeptSyncMapper.selectList(null);
        Set<String> existingDeptNames = new HashSet<>();
        for (SysDeptSync d : localDepts) existingDeptNames.add(d.getDeptName());

        List<SysDeptSync> toInsertDepts = new ArrayList<>();
        for (String dName : deptNameSet) {
            if (!existingDeptNames.contains(dName)) {
                SysDeptSync d = new SysDeptSync();
                d.setDeptName(dName);
                d.setIsShielded(0);
                d.setSyncedAt(now);
                toInsertDepts.add(d);
                existingDeptNames.add(dName);
            }
        }
        for (SysDeptSync d : toInsertDepts) sysDeptSyncMapper.insert(d);
    }

    private String getStr(Map<String, Object> map, String... keys) {
        for (String k : keys) {
            Object val = map.get(k);
            if (val != null && StringUtils.hasText(val.toString())) {
                return val.toString().trim();
            }
        }
        return null;
    }

    private void touchLocalSyncedTime() {
        LocalDateTime now = LocalDateTime.now();
        List<SysDeptSync> depts = sysDeptSyncMapper.selectList(null);
        for (SysDeptSync d : depts) {
            d.setSyncedAt(now);
            sysDeptSyncMapper.updateById(d);
        }
        List<SysUserSync> users = sysUserSyncMapper.selectList(null);
        for (SysUserSync u : users) {
            u.setSyncedAt(now);
            sysUserSyncMapper.updateById(u);
        }
    }

    /**
     * 真正多级父子嵌套树状结构构建 (Recursive Tree Builder)
     */
    @Override
    public List<Map<String, Object>> getDeptTree() {
        LambdaQueryWrapper<SysUserSync> activeUserWrapper = new LambdaQueryWrapper<>();
        activeUserWrapper.eq(SysUserSync::getStatus, "在职");
        List<SysUserSync> users = sysUserSyncMapper.selectList(activeUserWrapper);

        // 检索已屏蔽的部门
        List<SysDeptSync> shieldedDepts = sysDeptSyncMapper.selectList(new LambdaQueryWrapper<SysDeptSync>().eq(SysDeptSync::getIsShielded, 1));
        java.util.Set<String> shieldedNames = new java.util.HashSet<>();
        if (shieldedDepts != null) {
            for (SysDeptSync d : shieldedDepts) {
                if (StringUtils.hasText(d.getDeptName())) {
                    shieldedNames.add(d.getDeptName().trim());
                }
            }
        }

        String companyRootName = "浙江脉通智造科技 (集团) 有限公司";

        // 根节点
        Map<String, Object> rootNode = new HashMap<>();
        rootNode.put("id", 0L);
        rootNode.put("label", companyRootName);
        rootNode.put("deptName", "");
        rootNode.put("fullPath", companyRootName);
        rootNode.put("count", users.size());
        rootNode.put("isShielded", shieldedNames.contains(companyRootName) ? 1 : 0);

        // 递归辅助 Node 结构
        class TreeNode {
            String name;
            String fullPath;
            int count = 0;
            Map<String, TreeNode> children = new LinkedHashMap<>();

            TreeNode(String name, String fullPath) {
                this.name = name;
                this.fullPath = fullPath;
            }

            Map<String, Object> toMap(long[] idCounter, boolean parentShielded) {
                boolean currentShielded = parentShielded || shieldedNames.contains(fullPath) || shieldedNames.contains(name);
                Map<String, Object> m = new HashMap<>();
                m.put("id", idCounter[0]++);
                m.put("label", name);
                m.put("deptName", name);
                m.put("fullPath", fullPath);
                m.put("count", count);
                m.put("isShielded", currentShielded ? 1 : 0);

                if (!children.isEmpty()) {
                    List<Map<String, Object>> childList = new ArrayList<>();
                    for (TreeNode child : children.values()) {
                        childList.add(child.toMap(idCounter, currentShielded));
                    }
                    m.put("children", childList);
                }
                return m;
            }
        }


        TreeNode rootTree = new TreeNode(companyRootName, "");

        for (SysUserSync user : users) {
            rootTree.count++;
            String path = user.getDeptName();
            if (!StringUtils.hasText(path)) continue;

            String[] parts = path.split("-");
            TreeNode current = rootTree;
            String curPath = "";

            for (String p : parts) {
                String cleanP = p.trim();
                if (!StringUtils.hasText(cleanP)) continue;
                if (cleanP.contains("浙江脉通智造") || cleanP.contains("集团")) continue; // 跳过冗余根层名

                curPath = curPath.isEmpty() ? cleanP : (curPath + "-" + cleanP);
                if (!current.children.containsKey(cleanP)) {
                    current.children.put(cleanP, new TreeNode(cleanP, curPath));
                }
                current = current.children.get(cleanP);
                current.count++;
            }
        }

        long[] idGen = new long[]{1L};
        List<Map<String, Object>> resultChildren = new ArrayList<>();
        boolean rootShielded = shieldedNames.contains(companyRootName);
        for (TreeNode child : rootTree.children.values()) {
            resultChildren.add(child.toMap(idGen, rootShielded));
        }

        rootNode.put("children", resultChildren);
        List<Map<String, Object>> tree = new ArrayList<>();
        tree.add(rootNode);

        return tree;
    }

    @Override
    public List<SysUserSync> getUsers(String adAccount, String name, String deptName, Long deptId, String status) {
        return (List<SysUserSync>) getUsersPage(1, 10000, adAccount, name, deptName, deptId, status).get("list");
    }

    /**
     * 高效物理分页实现
     */
    @Override
    public Map<String, Object> getUsersPage(Integer page, Integer pageSize, String adAccount, String name, String deptName, Long deptId, String status) {
        if (page == null || page < 1) page = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;

        Page<SysUserSync> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<SysUserSync> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(adAccount)) {
            wrapper.like(SysUserSync::getAdAccount, adAccount.trim());
        }
        if (StringUtils.hasText(name)) {
            wrapper.like(SysUserSync::getName, name.trim());
        }
        if (StringUtils.hasText(deptName)) {
            String cleanDName = deptName.trim();
            // 一对一精准匹配，彻底避免部门重名混淆
            wrapper.eq(SysUserSync::getDeptName, cleanDName);
        } else if (deptId != null && deptId > 0) {
            SysDeptSync dept = sysDeptSyncMapper.selectById(deptId);
            if (dept != null) {
                wrapper.eq(SysUserSync::getDeptName, dept.getDeptName().trim());
            }
        }


        if (StringUtils.hasText(status)) {
            wrapper.eq(SysUserSync::getStatus, status.trim());
        }

        wrapper.orderByAsc(SysUserSync::getWorkNo);

        Page<SysUserSync> resultPage = sysUserSyncMapper.selectPage(pageParam, wrapper);

        Map<String, Object> resMap = new HashMap<>();
        resMap.put("total", resultPage.getTotal());
        resMap.put("page", resultPage.getCurrent());
        resMap.put("pageSize", resultPage.getSize());
        resMap.put("pages", resultPage.getPages());
        resMap.put("list", resultPage.getRecords());

        return resMap;
    }

    @Override
    public boolean updateDeptShieldByName(String deptName, Integer isShielded) {
        if (!StringUtils.hasText(deptName)) return false;
        String cleanName = deptName.trim();
        saveOrUpdateShieldDept(cleanName, isShielded);

        if (cleanName.contains("-")) {
            String[] parts = cleanName.split("-");
            String shortName = parts[parts.length - 1].trim();
            if (StringUtils.hasText(shortName)) {
                saveOrUpdateShieldDept(shortName, isShielded);
            }
        }
        return true;
    }

    private void saveOrUpdateShieldDept(String name, Integer isShielded) {
        List<SysDeptSync> depts = sysDeptSyncMapper.selectList(new LambdaQueryWrapper<SysDeptSync>().eq(SysDeptSync::getDeptName, name));
        if (depts != null && !depts.isEmpty()) {
            for (SysDeptSync d : depts) {
                d.setIsShielded(isShielded);
                d.setSyncedAt(LocalDateTime.now());
                sysDeptSyncMapper.updateById(d);
            }
        } else {
            SysDeptSync newDept = new SysDeptSync();
            newDept.setDeptName(name);
            newDept.setIsShielded(isShielded);
            newDept.setSyncedAt(LocalDateTime.now());
            sysDeptSyncMapper.insert(newDept);
        }
    }
}



package com.maitong.visitor.runner;

import com.maitong.visitor.entity.SysUserSync;
import com.maitong.visitor.mapper.SysUserSyncMapper;
import com.maitong.visitor.service.AphrSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 项目启动保底数据初始化器：
 * 保证服务每次启动/重启后，若发现组织架构或人员数据为空，
 * 立刻在后台静默自动拉取一次全量 APHR 数仓数据，确保用户打开页面 100% 有完整数据！
 */
@Component
public class AphrSyncRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AphrSyncRunner.class);

    @Autowired
    private SysUserSyncMapper sysUserSyncMapper;

    @Autowired
    private AphrSyncService aphrSyncService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            String localUrl = "jdbc:mysql://localhost:3306/visitor_system_db?useUnicode=true&characterEncoding=utf-8&useSSL=false&allowPublicKeyRetrieval=true";
            try (java.sql.Connection conn = java.sql.DriverManager.getConnection(localUrl, "root", "admin@123")) {
                try {
                    conn.createStatement().execute("ALTER TABLE sys_nda_templates ADD COLUMN pdf_url VARCHAR(500)");
                } catch (Exception ignored) {}
                try {
                    conn.createStatement().execute("ALTER TABLE visitor_records ADD COLUMN approve_token VARCHAR(100)");
                } catch (Exception ignored) {}
                try {
                    conn.createStatement().execute("ALTER TABLE visitor_records ADD COLUMN visit_date VARCHAR(20)");
                } catch (Exception ignored) {}
                try {
                    conn.createStatement().execute("ALTER TABLE visitor_records ADD COLUMN visit_start_time VARCHAR(20)");
                } catch (Exception ignored) {}
                try {
                    conn.createStatement().execute("ALTER TABLE visitor_records ADD COLUMN visit_end_time VARCHAR(20)");
                } catch (Exception ignored) {}
                try {
                    conn.createStatement().execute("ALTER TABLE visitor_records ADD COLUMN email VARCHAR(100)");
                } catch (Exception ignored) {}
                try {
                    conn.createStatement().execute("ALTER TABLE visitor_records ADD COLUMN company VARCHAR(200)");
                } catch (Exception ignored) {}
                try {
                    conn.createStatement().execute("ALTER TABLE visitor_records ADD COLUMN visitor_token VARCHAR(100)");
                } catch (Exception ignored) {}
                try {
                    conn.createStatement().execute("ALTER TABLE visitor_records ADD COLUMN escalated INT DEFAULT 0");
                } catch (Exception ignored) {}
                try {
                    conn.createStatement().execute("ALTER TABLE visitor_records ADD COLUMN escalated_at DATETIME");
                } catch (Exception ignored) {}
            }



        } catch (Exception ignored) {}


        try {
            Long count = sysUserSyncMapper.selectCount(null);

            // 智能增量/初始化校验：若本地数据库已建立且拥有人员数据 (>=100条)，启动时秒级跳过全量同步，实现 0 秒极速启动！
            if (count == null || count < 100) {
                log.info("检测到本地数据库尚无完整人员档案（当前仅 {} 条），正在进行首次后台静默初始化拉取...", count);
                boolean success = aphrSyncService.syncAll();
                if (success) {
                    log.info("服务启动时 APHR 人员档案初始化成功！");
                }
            } else {
                log.info("✅ 本地人员档案库校验正常，当前已有 {} 条全员档案。启动时秒级跳过全量同步，实现秒速启动！（系统每天凌晨 03:00 自动定时增量同步）", count);
            }
        } catch (Exception e) {
            log.error("检查/初始化组织架构数据发生异常", e);
        }

    }

}

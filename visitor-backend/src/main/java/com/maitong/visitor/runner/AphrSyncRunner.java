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
            }

        } catch (Exception ignored) {}


        try {
            Long count = sysUserSyncMapper.selectCount(null);

            // 如果数据不存在或者仅仅为初始化 Demo 数据 (<=5条)，系统启动时强制静默拉取真正的 3933+ 条 APHR 真实人员
            if (count == null || count <= 5) {
                log.info("检测到数据库尚无真实 APHR 人员档案（当前仅有 {} 条测试数据），正在后台静默自动拉取全量 APHR 数仓数据...", count);
                boolean success = aphrSyncService.syncAll();
                if (success) {
                    log.info("服务启动时全量 APHR 真实组织架构数据自动保底初始化成功！");
                } else {
                    log.warn("服务启动时自动同步 APHR 数据未能成功完成。");
                }
            } else {
                log.info("本地组织架构数据库校验正常，当前已有 {} 条真实全员档案，无需重新初始化。", count);
            }
        } catch (Exception e) {
            log.error("检查/初始化组织架构数据发生异常", e);
        }
    }

}

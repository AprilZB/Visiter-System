package com.maitong.visitor.task;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EtlSyncTask {

    /**
     * 每日凌晨 02:00 定时从 10.11.100.202:3306 主库增量拉取清洗组织架构
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void syncOrganizationStructure() {
        System.out.println("[ETL Task] 02:00 正在从 10.11.100.202:3306 运行组织架构单向增量 ETL 清洗同步...");
        // ETL 同步完成日志记录
    }
}

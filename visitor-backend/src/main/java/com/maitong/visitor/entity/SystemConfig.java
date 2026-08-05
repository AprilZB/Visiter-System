package com.maitong.visitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("system_config")
public class SystemConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String configKey;
    private String configValue;
    private String remark;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getConfigKey() { return configKey; }
    public void setConfigKey(String configKey) { this.configKey = configKey; }

    public String getConfigValue() { return configValue; }
    public void setConfigValue(String configValue) { this.configValue = configValue; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}


package com.maitong.visitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_visit_reasons")
public class SysVisitReason {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String reasonName;
    private Integer sortOrder;
    private Integer isActive; // 1:启用 0:禁用
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getReasonName() { return reasonName; }
    public void setReasonName(String reasonName) { this.reasonName = reasonName; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public Integer getIsActive() { return isActive; }
    public void setIsActive(Integer isActive) { this.isActive = isActive; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}


package com.maitong.visitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_dept_sync")
public class SysDeptSync {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String deptName;
    private Integer isShielded; // 1:屏蔽防骚扰 0:公开
    private LocalDateTime createdAt;
    private LocalDateTime syncedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }

    public Integer getIsShielded() { return isShielded; }
    public void setIsShielded(Integer isShielded) { this.isShielded = isShielded; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getSyncedAt() { return syncedAt; }
    public void setSyncedAt(LocalDateTime syncedAt) { this.syncedAt = syncedAt; }
}



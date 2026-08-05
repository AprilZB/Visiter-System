package com.maitong.visitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_nda_templates")
public class SysNdaTemplate {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String version;
    private String title;
    private String content;
    private String pdfUrl;
    private Integer isActive;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getPdfUrl() { return pdfUrl; }
    public void setPdfUrl(String pdfUrl) { this.pdfUrl = pdfUrl; }

    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Integer getIsActive() { return isActive; }
    public void setIsActive(Integer isActive) { this.isActive = isActive; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}


package com.maitong.visitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user_sync")
public class SysUserSync {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String workNo;
    private String adAccount;
    private String name;
    private String gender;
    private Integer age;
    private String birthDate;
    private String phone;
    private Long deptId;
    private String deptName;
    private Long managerId;
    private String managerName;
    private String status; // 在职, 离职, 试用期
    private String dingUserid;
    private LocalDateTime syncedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getWorkNo() { return workNo; }
    public void setWorkNo(String workNo) { this.workNo = workNo; }

    public String getAdAccount() { return adAccount; }
    public void setAdAccount(String adAccount) { this.adAccount = adAccount; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public String getBirthDate() { return birthDate; }
    public void setBirthDate(String birthDate) { this.birthDate = birthDate; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }

    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }

    public Long getManagerId() { return managerId; }
    public void setManagerId(Long managerId) { this.managerId = managerId; }

    public String getManagerName() { return managerName; }
    public void setManagerName(String managerName) { this.managerName = managerName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDingUserid() { return dingUserid; }
    public void setDingUserid(String dingUserid) { this.dingUserid = dingUserid; }

    public LocalDateTime getSyncedAt() { return syncedAt; }
    public void setSyncedAt(LocalDateTime syncedAt) { this.syncedAt = syncedAt; }
}



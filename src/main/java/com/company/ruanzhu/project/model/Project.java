package com.company.ruanzhu.project.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.ruanzhu.common.model.BaseEntity;
import com.company.ruanzhu.project.enums.ProjectStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("project")
public class Project extends BaseEntity {
    private String name;
    private String customerName;
    private ProjectStatus status;
    private Long createdBy;
}

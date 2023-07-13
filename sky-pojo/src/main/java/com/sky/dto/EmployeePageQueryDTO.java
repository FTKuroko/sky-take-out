package com.sky.dto;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(description = "员工信息分页")
public class EmployeePageQueryDTO implements Serializable {

    @ApiModelProperty(value = "员工姓名")
    private String name;

    @ApiModelProperty(value = "页码")
    private int page;

    @ApiModelProperty(value = "每页显示记录数")
    private int pageSize;

}

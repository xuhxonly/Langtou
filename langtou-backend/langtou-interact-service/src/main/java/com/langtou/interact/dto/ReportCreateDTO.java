package com.langtou.interact.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReportCreateDTO {

    @NotBlank(message = "举报原因不能为空")
    private String reason;

    /**
     * 举报类型: spam-垃圾信息, illegal-违法违规, harassment-骚扰攻击, copyright-侵权, other-其他
     */
    @NotBlank(message = "举报类型不能为空")
    private String reportType;
}

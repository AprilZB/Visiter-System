package com.maitong.visitor.controller;

import com.maitong.visitor.common.Result;
import com.maitong.visitor.dto.SecurityScanDTO;
import com.maitong.visitor.service.VisitorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/security")
@CrossOrigin
public class SecurityController {

    @Autowired
    private VisitorService visitorService;

    /**
     * 1. 扫码核验接口 (强脱敏展示中间4位: 3301021234****1234)
     */
    @GetMapping("/scan")
    public Result<SecurityScanDTO> scanVerify(@RequestParam("token") String token) {
        SecurityScanDTO dto = visitorService.securityScanVerify(token);
        return Result.success(dto);
    }

    /**
     * 2. 保安核对物理证件一致后点击【确认放行】
     */
    @PostMapping("/confirm-entry")
    public Result<Boolean> confirmEntry(@RequestBody Map<String, String> body) {
        String visitNo = body.get("visitNo");
        String securityName = body.getOrDefault("securityName", "门岗保安");

        if (visitNo == null || visitNo.trim().isEmpty()) {
            return Result.error("缺少访客单号");
        }

        boolean ok = visitorService.confirmEntry(visitNo, securityName);
        if (ok) {
            return Result.success("放行核销成功，动态通行码已作废", true);
        }
        return Result.error("放行核销失败");
    }
}

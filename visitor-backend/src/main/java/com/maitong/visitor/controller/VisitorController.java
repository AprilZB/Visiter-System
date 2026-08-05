package com.maitong.visitor.controller;

import com.maitong.visitor.common.Result;
import com.maitong.visitor.dto.NdaSignDTO;
import com.maitong.visitor.dto.OcrResultDTO;
import com.maitong.visitor.dto.VisitorApplyDTO;
import com.maitong.visitor.entity.SysNdaTemplate;
import com.maitong.visitor.entity.VisitorNdaRecord;
import com.maitong.visitor.entity.VisitorRecord;
import com.maitong.visitor.service.NdaGuardService;
import com.maitong.visitor.service.OcrService;
import com.maitong.visitor.service.VisitorService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/visitor")
@CrossOrigin
public class VisitorController {

    @Autowired
    private OcrService ocrService;

    @Autowired
    private VisitorService visitorService;

    @Autowired
    private NdaGuardService ndaGuardService;

    /**
     * 1. 身份证照片 OCR 识别
     */
    @PostMapping("/ocr")
    public Result<OcrResultDTO> ocrRecognize(@RequestParam("file") MultipartFile file) {
        OcrResultDTO result = ocrService.recognizeIdCard(file);
        if (!result.isSuccess()) {
            return Result.error(result.getErrorMessage());
        }
        return Result.success(result);
    }

    /**
     * 2. 提交访客申请
     */
    @PostMapping("/apply")
    public Result<VisitorRecord> apply(@RequestBody VisitorApplyDTO dto) {
        if (dto.getVisitorName() == null || dto.getIdCard() == null || dto.getPhone() == null) {
            return Result.error("请完整填写访客姓名、身份证号与手机号");
        }
        VisitorRecord record = visitorService.applyVisit(dto);
        return Result.success("申请已提交", record);
    }

    /**
     * 3. 获取当前生效的保密协议条款正文
     */
    @GetMapping("/nda-template")
    public Result<SysNdaTemplate> getNdaTemplate() {
        SysNdaTemplate template = ndaGuardService.getActiveNdaTemplate();
        return Result.success(template);
    }

    /**
     * 4. 访客在线阅读并勾选签署保密协议
     */
    @PostMapping("/sign-nda")
    public Result<Boolean> signNda(@RequestBody NdaSignDTO dto, HttpServletRequest request) {
        if (dto.getClientIp() == null || dto.getClientIp().trim().isEmpty()) {
            dto.setClientIp(request.getRemoteAddr());
        }
        boolean ok = ndaGuardService.signNda(dto);
        if (ok) {
            return Result.success("保密协议签署成功并已完成加密备案", true);
        }
        return Result.error("协议签署失败，请检查访客单信息");
    }

    /**
     * 5. 获取限时动态通行码 Token (带保密协议强拦截)
     */
    @GetMapping("/pass-token")
    public Result<Map<String, Object>> getPassToken(@RequestParam("visitNo") String visitNo) {
        VisitorRecord record = visitorService.getByVisitNo(visitNo);
        if (record == null) {
            return Result.error("找不到访客单信息");
        }

        // 强校验拦截！
        if (record.getNdaSigned() == null || record.getNdaSigned() != 1) {
            return Result.error(403, "【拦截】必须先完成保密协议签署，方可获取通行二维码！");
        }

        try {
            String token = visitorService.getPassCodeToken(visitNo);
            Map<String, Object> map = new HashMap<>();
            map.put("passToken", token);
            map.put("status", record.getStatus());
            map.put("visitorName", record.getVisitorName());
            map.put("ndaSigned", true);
            return Result.success(map);
        } catch (IllegalStateException e) {
            return Result.error(403, e.getMessage());
        }
    }

    /**
     * 6. 查询访客单当前状态
     */
    @GetMapping("/detail")
    public Result<Map<String, Object>> getDetail(@RequestParam("visitNo") String visitNo) {
        VisitorRecord record = visitorService.getByVisitNo(visitNo);
        if (record == null) return Result.error("记录不存在");

        VisitorNdaRecord ndaRecord = ndaGuardService.getSignRecordByVisitNo(visitNo);

        Map<String, Object> map = new HashMap<>();
        map.put("record", record);
        map.put("ndaSigned", record.getNdaSigned() == 1);
        map.put("ndaRecord", ndaRecord);
        return Result.success(map);
    }

    /**
     * 7. 访客凭手机号找回/查询最近一次审批通过且签署协议的通行二维码 (防误关闭)
     */

    @GetMapping("/latest-pass-token")
    public Result<Map<String, Object>> getLatestPassTokenByPhone(@RequestParam("phone") String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return Result.error("请输入手机号码");
        }
        Map<String, Object> data = visitorService.getLatestPassTokenByPhone(phone.trim());
        if (data == null) {
            return Result.error("未查询到您近期已审批通过且签署保密协议的有效通行凭证");
        }
        return Result.success("查询通行凭证成功", data);
    }
}


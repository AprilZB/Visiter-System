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
     * 2. 保安核对物理证件一致后点击【确认放行】(带门岗岗位防越权校验)
     */
    @PostMapping("/confirm-entry")
    public Result<Boolean> confirmEntry(@RequestBody Map<String, String> body) {
        String visitNo = body.get("visitNo");
        String securityName = body.getOrDefault("securityName", "门岗保安");
        String securityKey = body.get("securityKey");

        // 安全防越权机制: 只有持有门岗授权口令(默认123456)的保安手机才能点击放行销号
        if (securityKey == null || !"123456".equals(securityKey.trim())) {
            return Result.error("【越权阻断】非授权门岗保安设备，无权执行放行销号！请将手机出示给正门保安。");
        }

        if (visitNo == null || visitNo.trim().isEmpty()) {
            return Result.error("缺少访客单号");
        }

        boolean ok = visitorService.confirmEntry(visitNo, securityName);
        if (ok) {
            return Result.success("放行核销成功，动态通行码已作废", true);
        }
        return Result.error("放行核销失败");
    }


    @Autowired
    private com.maitong.visitor.service.OcrService ocrService;

    /**
     * 3. 门岗拍照/图片上传服务端 PaddleOCR + ZXing 智能解算接口 (完美调用 10.11.100.238:8081/ocr)
     */
    @PostMapping("/scan-image")
    public Result<SecurityScanDTO> scanImageVerify(@RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.error("未收到上传的照片");
        }
        
        String detectedToken = null;

        // 第一重: 调用 10.11.100.238:8081/ocr 深度学习 OCR 服务识别照片中的 8 位短码或手机号
        try {
            com.maitong.visitor.dto.OcrResultDTO ocrResult = ocrService.recognizeIdCard(file);
            if (ocrResult != null && ocrResult.isSuccess() && ocrResult.getRawText() != null) {
                String rawText = ocrResult.getRawText().toUpperCase();
                System.out.println("[Security OCR Scan] 识别到照片全量文本: " + rawText);
                
                // 优先模式 1: 提取 6 位纯数字放行短码 (深度学习 PaddleOCR 识别率接近 100%)
                java.util.regex.Matcher numCodeMatcher = java.util.regex.Pattern.compile("\\b(\\d{6})\\b").matcher(rawText);
                if (numCodeMatcher.find()) {
                    detectedToken = numCodeMatcher.group(1);
                    System.out.println("[Security OCR Scan] 命中 6 位纯数字放行短码: " + detectedToken);
                }

                // 优先模式 2: 查找 【短码】 标识后面的 8 位大写字母/数字短码 (如 EB83C3F8)
                if (detectedToken == null) {
                    java.util.regex.Matcher tagMatcher = java.util.regex.Pattern.compile("(?:短码|放行码|通行码|CODE)[：:：\\s]*([A-Z0-9]{8})").matcher(rawText);
                    if (tagMatcher.find()) {
                        detectedToken = tagMatcher.group(1);
                        System.out.println("[Security OCR Scan] 命中短码标签正则: " + detectedToken);
                    }
                }


                // 优先模式 2: 全局匹配包含字母与数字组合的 8 位短码 (如 EB83C3F8)
                if (detectedToken == null) {
                    java.util.regex.Matcher codeMatcher = java.util.regex.Pattern.compile("\\b([A-Z0-9]{8})\\b").matcher(rawText);
                    while (codeMatcher.find()) {
                        String candidate = codeMatcher.group(1);
                        // 必须同时包含大写字母与数字
                        if (candidate.matches(".*[A-Z].*") && candidate.matches(".*[0-9].*")) {
                            detectedToken = candidate;
                            System.out.println("[Security OCR Scan] 命中8位混合短码正则: " + detectedToken);
                            break;
                        }
                    }
                }

                // 优先模式 3: 提取 11 位手机号
                if (detectedToken == null) {
                    java.util.regex.Matcher phoneMatcher = java.util.regex.Pattern.compile("1[3-9]\\d{9}").matcher(rawText);
                    if (phoneMatcher.find()) {
                        detectedToken = phoneMatcher.group();
                        System.out.println("[Security OCR Scan] 命中手机号正则: " + detectedToken);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("调用 10.11.100.238:8081/ocr 识别失败: " + e.getMessage());
        }


        // 第二重: 若 OCR 服务未识别到文字短码，降级使用 ZXing 图像二值化多尺度切片解算二维码图形
        if (detectedToken == null) {
            try {
                java.awt.image.BufferedImage originalImage = javax.imageio.ImageIO.read(file.getInputStream());
                if (originalImage != null) {
                    detectedToken = decodeQrFromBufferedImage(originalImage);
                }
            } catch (Exception e) {
                // 忽略图片读取异常
            }
        }

        if (detectedToken == null || detectedToken.trim().isEmpty()) {
            return Result.error("照片中未识别到有效二维码或 8 位短码，请拍照包含下方大字短码或直接输入手机号核验");
        }

        // 一键触发门岗身份核销与放行比对
        return scanVerify(detectedToken);
    }


    private String decodeQrFromBufferedImage(java.awt.image.BufferedImage img) {
        // 尝试 1: 全图 HybridBinarizer
        String token = tryDecodeZxing(img, true);
        if (token != null) return token;

        // 尝试 2: 全图 GlobalHistogramBinarizer
        token = tryDecodeZxing(img, false);
        if (token != null) return token;

        // 尝试 3: 提取中心 70% 区域切片 (去除外部背景干扰噪点)
        int w = img.getWidth();
        int h = img.getHeight();
        int cropW = (int) (w * 0.7);
        int cropH = (int) (h * 0.7);
        int cropX = (w - cropW) / 2;
        int cropY = (h - cropH) / 2;

        if (cropW > 50 && cropH > 50) {
            java.awt.image.BufferedImage croppedImg = img.getSubimage(cropX, cropY, cropW, cropH);
            token = tryDecodeZxing(croppedImg, true);
            if (token != null) return token;
            token = tryDecodeZxing(croppedImg, false);
            if (token != null) return token;
        }

        return null;
    }

    private String tryDecodeZxing(java.awt.image.BufferedImage img, boolean useHybrid) {
        try {
            com.google.zxing.LuminanceSource source = new com.google.zxing.client.j2se.BufferedImageLuminanceSource(img);
            com.google.zxing.Binarizer binarizer = useHybrid ?
                new com.google.zxing.common.HybridBinarizer(source) :
                new com.google.zxing.common.GlobalHistogramBinarizer(source);

            com.google.zxing.BinaryBitmap bitmap = new com.google.zxing.BinaryBitmap(binarizer);
            Map<com.google.zxing.DecodeHintType, Object> hints = new java.util.HashMap<>();
            hints.put(com.google.zxing.DecodeHintType.TRY_HARDER, Boolean.TRUE);
            hints.put(com.google.zxing.DecodeHintType.CHARACTER_SET, "UTF-8");

            com.google.zxing.Result result = new com.google.zxing.MultiFormatReader().decode(bitmap, hints);
            if (result != null && result.getText() != null) {
                String text = result.getText();
                if (text.contains("token=")) {
                    java.util.regex.Matcher m = java.util.regex.Pattern.compile("token=([^&]+)").matcher(text);
                    if (m.find()) return m.group(1);
                }
                return text;
            }
        } catch (Exception e) {
            // 忽略失败继续下一步尝试
        }
        return null;
    }
}


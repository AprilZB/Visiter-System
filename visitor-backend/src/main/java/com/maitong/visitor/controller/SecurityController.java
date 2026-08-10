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

    /**
     * 3. 门岗拍照/图片上传服务端高阶 CV 多尺度解算识别接口 (解决手机拍照屏幕水纹与背景噪点)
     */
    @PostMapping("/scan-image")
    public Result<SecurityScanDTO> scanImageVerify(@RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.error("未收到上传的照片");
        }
        try {
            java.awt.image.BufferedImage originalImage = javax.imageio.ImageIO.read(file.getInputStream());
            if (originalImage == null) {
                return Result.error("无法读取照片图像数据");
            }

            // 多尺度切片识别算法 (Original, 70% Center Crop, HybridBinarizer & GlobalHistogramBinarizer)
            String decodedToken = decodeQrFromBufferedImage(originalImage);

            if (decodedToken == null || decodedToken.trim().isEmpty()) {
                return Result.error("未能在此照片中定位识别到二维码，请贴近二维码拍摄");
            }

            // 提取 token 后直接进行身份核销与全量数据比对
            return scanVerify(decodedToken);
        } catch (Exception e) {
            return Result.error("照片识别算法处理失败: " + e.getMessage());
        }
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


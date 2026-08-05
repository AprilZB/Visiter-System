package com.maitong.visitor.service;

import com.maitong.visitor.dto.OcrResultDTO;
import org.springframework.web.multipart.MultipartFile;

public interface OcrService {
    OcrResultDTO recognizeIdCard(MultipartFile file);
    String getOcrServiceUrl();
    boolean updateOcrServiceUrl(String url);
}

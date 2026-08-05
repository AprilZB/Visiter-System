package com.maitong.visitor.dto;

import lombok.Data;

@Data
public class OcrResultDTO {
    private boolean success;
    private String name;
    private String idCard;
    private String gender;
    private String address;
    private String rawText;
    private String errorMessage;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIdCard() { return idCard; }
    public void setIdCard(String idCard) { this.idCard = idCard; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getRawText() { return rawText; }
    public void setRawText(String rawText) { this.rawText = rawText; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}


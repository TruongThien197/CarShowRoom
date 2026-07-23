package com.hsf302.carshowroom.dto.PayOS;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class PayOSWebhookRequest {
    private String code;
    private String desc;
    private Boolean success;
    private JsonNode data;
    private String signature;
}

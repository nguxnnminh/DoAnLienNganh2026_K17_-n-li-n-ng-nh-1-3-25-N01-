package com.shop.clothingstore.service.ai;

import java.io.IOException;

public class AiRequestException extends IOException {

    private final int statusCode;
    private final String responseBody;

    public AiRequestException(int statusCode, String message, String responseBody) {
        super(message);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }
}


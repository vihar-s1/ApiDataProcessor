package com.apiDataProcessor.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor
public class InternalHttpResponse<T> {
    private Boolean success;
    private T data;
}
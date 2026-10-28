package com.smartheritage.template.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** DTO mẫu có @Valid: sai dữ liệu -> 400 {success:false, message:"name: ..."} (GlobalExceptionHandler). */
public record SampleItemRequest(
        @NotBlank(message = "không được để trống")
        @Size(max = 100, message = "tối đa 100 ký tự")
        String name) {
}

package com.smartheritage.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
public class RecordVisitRequest {
    @NotNull
    @Positive
    private Long artifactId;

    /** null = không đo được thời gian dừng (không ghi 0 để không làm sai thống kê). */
    @Min(0)
    @Max(86400)
    private Integer durationSeconds;

    @NotBlank
    @Pattern(regexp = "(?i)BEACON|MAP|MANUAL")
    private String source = "MANUAL";

    @NotBlank
    @Pattern(regexp = "(?i)[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")
    private String clientEventId;

    /** ISO-8601 timestamp sent by mobile; absent means server time. */
    private OffsetDateTime detectedAt;
}

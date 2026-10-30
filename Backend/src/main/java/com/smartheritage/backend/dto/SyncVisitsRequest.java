package com.smartheritage.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class SyncVisitsRequest {
    @NotEmpty
    @Size(max = 100)
    private List<@Valid RecordVisitRequest> events = new ArrayList<>();
}

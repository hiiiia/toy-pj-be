package com.yh.toy_pj.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TriageRequest(@NotBlank @Size(max = 100) String title, @NotBlank @Size(max = 2000) String description) {
}

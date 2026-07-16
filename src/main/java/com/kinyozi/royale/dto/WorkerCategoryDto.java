package com.kinyozi.royale.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public class WorkerCategoryDto {

    public static class Request {
        @NotBlank public String name;
        public String description;
    }

    public static class Response {
        public UUID id;
        public String name;
        public String description;
        public long workerCount;
        public long serviceCount;
    }
}

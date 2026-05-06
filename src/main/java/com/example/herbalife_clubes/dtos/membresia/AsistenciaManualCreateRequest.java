package com.example.herbalife_clubes.dtos.membresia;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AsistenciaManualCreateRequest {
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fecha;
    private String nota;
}

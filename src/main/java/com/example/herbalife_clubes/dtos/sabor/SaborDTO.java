package com.example.herbalife_clubes.dtos.sabor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaborDTO {
    private Integer id;
    private Integer hubId;
    private String nombre;
    private Boolean activo;
}

package com.example.herbalife_clubes.dtos.fotoclub;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FotoClubDTO {
    private Integer id;
    private Integer clubId;
    private String clubNombre;
    private String urlFoto;
    private String tipo;
}


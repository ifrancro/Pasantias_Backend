package com.example.herbalife_clubes.dtos.qr;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QRValidacionRequest {
    private String qr;
    private Integer clubId;
}


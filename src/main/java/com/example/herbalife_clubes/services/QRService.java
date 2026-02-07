package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.dtos.qr.QRValidacionRequest;
import com.example.herbalife_clubes.dtos.qr.QRValidacionResponse;

public interface QRService {
    QRValidacionResponse validarSocio(QRValidacionRequest request);
}


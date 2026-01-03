package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.entities.MembresiaLogro;

import java.util.List;

public interface MembresiaLogroService {
    void evaluarLogrosAutomaticamente(Integer membresiaId);
    List<MembresiaLogro> listarLogrosByMembresia(Integer membresiaId);
}


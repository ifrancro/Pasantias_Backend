package com.example.herbalife_clubes.serviceimpls;

import com.example.herbalife_clubes.entities.*;
import com.example.herbalife_clubes.exceptions.ResourceNotFoundException;
import com.example.herbalife_clubes.repositories.*;
import com.example.herbalife_clubes.services.MembresiaLogroService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class MembresiaLogroServiceImpl implements MembresiaLogroService {
    @Autowired
    private MembresiaLogroRepository membresiaLogroRepository;
    @Autowired
    private MembresiaRepository membresiaRepository;
    @Autowired
    private LogroRepository logroRepository;
    @Autowired
    private AsistenciaRepository asistenciaRepository;
    @Autowired
    private ConsumoRepository consumoRepository;

    @Override
    @Transactional
    public void evaluarLogrosAutomaticamente(Integer membresiaId) {
        Membresia membresia = membresiaRepository.findById(membresiaId)
                .orElseThrow(() -> new ResourceNotFoundException("Membresía no encontrada con id: " + membresiaId));
        
        List<Logro> logros = logroRepository.findAll();
        
        for (Logro logro : logros) {
            // Verificar si el logro ya fue obtenido
            Optional<MembresiaLogro> logroExistente = membresiaLogroRepository
                    .findByMembresiaIdAndLogroId(membresiaId, logro.getId());
            
            if (logroExistente.isPresent()) {
                continue; // Ya tiene este logro
            }
            
            boolean cumpleRequisito = false;
            
            // Evaluar logros por asistencias
            // tipo_requisito ahora es INT y representa la cantidad de asistencias requeridas
            if (logro.getTipoRequisito() != null) {
                // Usar puntos_acumulados que ya es el conteo total de asistencias
                Integer puntosAcumulados = membresia.getPuntosAcumulados() != null ? membresia.getPuntosAcumulados() : 0;
                Integer umbralRequerido = logro.getTipoRequisito();
                
                if (puntosAcumulados >= umbralRequerido) {
                    cumpleRequisito = true;
                }
            }
            
            if (cumpleRequisito) {
                MembresiaLogro membresiaLogro = new MembresiaLogro();
                membresiaLogro.setMembresia(membresia);
                membresiaLogro.setLogro(logro);
                membresiaLogroRepository.save(membresiaLogro);
            }
        }
    }

    @Override
    public List<MembresiaLogro> listarLogrosByMembresia(Integer membresiaId) {
        return membresiaLogroRepository.findByMembresiaId(membresiaId);
    }
}


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
    private LogroMetricaCalculator logroMetricaCalculator;

    @Override
    @Transactional
    public void evaluarLogrosAutomaticamente(Integer membresiaId) {
        Membresia membresia = membresiaRepository.findById(membresiaId)
                .orElseThrow(() -> new ResourceNotFoundException("Membresía no encontrada con id: " + membresiaId));

        List<Logro> logros = logroRepository.findAllWithRequisitos();

        for (Logro logro : logros) {
            Optional<MembresiaLogro> logroExistente = membresiaLogroRepository
                    .findByMembresiaIdAndLogroId(membresiaId, logro.getId());

            if (logroExistente.isPresent()) {
                continue;
            }

            if (!logroMetricaCalculator.aplicaAMembresia(logro, membresia)) {
                continue;
            }

            if (logroMetricaCalculator.cumpleTodosRequisitos(membresia, logro)) {
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

package com.example.herbalife_clubes.controllers;

import com.example.herbalife_clubes.dtos.club.ClubDTO;
import com.example.herbalife_clubes.services.ClubService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clubes")
@CrossOrigin("*")
public class ClubController {
    @Autowired
    private ClubService clubService;

    @PostMapping
    public ResponseEntity<ClubDTO> createClub(@RequestBody ClubDTO clubDTO,
                                               @RequestParam Integer hubId,
                                               @RequestParam Integer anfitrionId) {
        ClubDTO savedClubDTO = clubService.createClub(clubDTO, hubId, anfitrionId);
        return new ResponseEntity<>(savedClubDTO, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<ClubDTO>> getAllClubes(@RequestParam(required = false) Integer hubId) {
        List<ClubDTO> clubes;
        if (hubId != null) {
            clubes = clubService.getClubesByHub(hubId);
        } else {
            clubes = clubService.getAllClubes();
        }
        return ResponseEntity.ok(clubes);
    }

    @GetMapping("{id}")
    public ResponseEntity<ClubDTO> getClub(@PathVariable Integer id) {
        ClubDTO clubDTO = clubService.getClub(id);
        return ResponseEntity.ok(clubDTO);
    }

    @PutMapping("{id}")
    public ResponseEntity<ClubDTO> updateClub(@PathVariable Integer id, @RequestBody ClubDTO clubDTO) {
        ClubDTO updatedClubDTO = clubService.updateClub(id, clubDTO);
        return ResponseEntity.ok(updatedClubDTO);
    }

    @PatchMapping("{id}/aprobar")
    public ResponseEntity<ClubDTO> aprobarClub(@PathVariable Integer id) {
        ClubDTO clubDTO = clubService.aprobarClub(id);
        return ResponseEntity.ok(clubDTO);
    }

    @PatchMapping("{id}/rechazar")
    public ResponseEntity<ClubDTO> rechazarClub(@PathVariable Integer id) {
        ClubDTO clubDTO = clubService.rechazarClub(id);
        return ResponseEntity.ok(clubDTO);
    }

    @PatchMapping("{id}/activar")
    public ResponseEntity<ClubDTO> activarClub(@PathVariable Integer id) {
        ClubDTO clubDTO = clubService.activarClub(id);
        return ResponseEntity.ok(clubDTO);
    }

    @PatchMapping("{id}/desactivar")
    public ResponseEntity<ClubDTO> desactivarClub(@PathVariable Integer id) {
        ClubDTO clubDTO = clubService.desactivarClub(id);
        return ResponseEntity.ok(clubDTO);
    }
}


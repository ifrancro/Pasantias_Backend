package com.example.herbalife_clubes.membresias;

import com.example.herbalife_clubes.exceptions.PointsRecalculationRejectedException;
import org.springframework.http.HttpStatus;

/**
 * Códigos estables POINTS-ORDER-001 para recálculo destructivo de puntos por asistencias.
 */
public final class PointsRecalculationRejections {

    public static final String POINTS_RECALCULATION_UNSUPPORTED = "POINTS_RECALCULATION_UNSUPPORTED";

    private static final String MSG_UNSUPPORTED =
            "Los puntos se calculan a partir de compras entregadas y no pueden recalcularse por asistencias.";

    private PointsRecalculationRejections() {
    }

    public static PointsRecalculationRejectedException unsupported() {
        return new PointsRecalculationRejectedException(
                POINTS_RECALCULATION_UNSUPPORTED, MSG_UNSUPPORTED, HttpStatus.CONFLICT);
    }

    public static void throwUnsupported() {
        throw unsupported();
    }
}

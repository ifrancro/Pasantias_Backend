package com.example.herbalife_clubes.pedidos;

import com.example.herbalife_clubes.exceptions.OrderCreationRejectedException;
import org.springframework.http.HttpStatus;

/**
 * Códigos estables ORD-SYNC-002 para rechazos de creación de pedidos socio.
 */
public final class OrderCreationRejections {

    public static final String MEMBERSHIP_INACTIVE = "MEMBERSHIP_INACTIVE";
    public static final String MEMBERSHIP_UNAVAILABLE = "MEMBERSHIP_UNAVAILABLE";
    public static final String CLUB_INACTIVE = "CLUB_INACTIVE";
    public static final String CLUB_UNAVAILABLE = "CLUB_UNAVAILABLE";
    public static final String ORDER_PRODUCT_UNAVAILABLE = "ORDER_PRODUCT_UNAVAILABLE";
    public static final String ORDER_COMBO_UNAVAILABLE = "ORDER_COMBO_UNAVAILABLE";
    public static final String ORDER_OPTION_INVALID = "ORDER_OPTION_INVALID";
    public static final String ORDER_INVALID_QUANTITY = "ORDER_INVALID_QUANTITY";
    public static final String ORDER_INVALID_REQUEST = "ORDER_INVALID_REQUEST";
    public static final String ORDER_CLIENT_ID_CONFLICT = "ORDER_CLIENT_ID_CONFLICT";

    private OrderCreationRejections() {
    }

    public static OrderCreationRejectedException membershipInactive(String message) {
        return new OrderCreationRejectedException(MEMBERSHIP_INACTIVE, message, HttpStatus.BAD_REQUEST);
    }

    public static OrderCreationRejectedException membershipUnavailable(String message) {
        return new OrderCreationRejectedException(MEMBERSHIP_UNAVAILABLE, message, HttpStatus.NOT_FOUND);
    }

    public static OrderCreationRejectedException clubInactive(String message) {
        return new OrderCreationRejectedException(CLUB_INACTIVE, message, HttpStatus.BAD_REQUEST);
    }

    public static OrderCreationRejectedException clubUnavailable(String message) {
        return new OrderCreationRejectedException(CLUB_UNAVAILABLE, message, HttpStatus.NOT_FOUND);
    }

    public static OrderCreationRejectedException productUnavailable(String message) {
        return productUnavailable(message, HttpStatus.BAD_REQUEST);
    }

    public static OrderCreationRejectedException productUnavailable(String message, HttpStatus status) {
        return new OrderCreationRejectedException(ORDER_PRODUCT_UNAVAILABLE, message, status);
    }

    public static OrderCreationRejectedException comboUnavailable(String message) {
        return comboUnavailable(message, HttpStatus.BAD_REQUEST);
    }

    public static OrderCreationRejectedException comboUnavailable(String message, HttpStatus status) {
        return new OrderCreationRejectedException(ORDER_COMBO_UNAVAILABLE, message, status);
    }

    public static OrderCreationRejectedException optionInvalid(String message) {
        return new OrderCreationRejectedException(ORDER_OPTION_INVALID, message, HttpStatus.BAD_REQUEST);
    }

    public static OrderCreationRejectedException invalidQuantity(String message) {
        return new OrderCreationRejectedException(ORDER_INVALID_QUANTITY, message, HttpStatus.BAD_REQUEST);
    }

    public static OrderCreationRejectedException invalidRequest(String message) {
        return new OrderCreationRejectedException(ORDER_INVALID_REQUEST, message, HttpStatus.BAD_REQUEST);
    }

    public static OrderCreationRejectedException clientOrderIdConflict(String message) {
        return new OrderCreationRejectedException(ORDER_CLIENT_ID_CONFLICT, message, HttpStatus.CONFLICT);
    }

    public static void throwMembershipInactive(String message) {
        throw membershipInactive(message);
    }

    public static void throwMembershipUnavailable(String message) {
        throw membershipUnavailable(message);
    }

    public static void throwClubInactive(String message) {
        throw clubInactive(message);
    }

    public static void throwClubUnavailable(String message) {
        throw clubUnavailable(message);
    }

    public static void throwProductUnavailable(String message) {
        throw productUnavailable(message);
    }

    public static void throwProductUnavailable(String message, HttpStatus status) {
        throw productUnavailable(message, status);
    }

    public static void throwComboUnavailable(String message) {
        throw comboUnavailable(message);
    }

    public static void throwComboUnavailable(String message, HttpStatus status) {
        throw comboUnavailable(message, status);
    }

    public static void throwOptionInvalid(String message) {
        throw optionInvalid(message);
    }

    public static void throwInvalidQuantity(String message) {
        throw invalidQuantity(message);
    }

    public static void throwInvalidRequest(String message) {
        throw invalidRequest(message);
    }

    public static void throwClientOrderIdConflict(String message) {
        throw clientOrderIdConflict(message);
    }
}

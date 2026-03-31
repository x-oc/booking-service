package ru.vova.airbnb.entity;

import java.util.Set;

public enum UserRole {
    GUEST(Set.of(
            Privilege.BOOKING_CREATE,
            Privilege.BOOKING_UPDATE,
            Privilege.BOOKING_DELETE,
            Privilege.BOOKING_PAY,
            Privilege.BOOKING_SUPPORT_REQUEST,
            Privilege.BOOKING_VIEW_OWN,
            Privilege.BOOKING_LIST_GUEST,
            Privilege.PROPERTY_VIEW
    )),
    HOST(Set.of(
            Privilege.BOOKING_CONFIRM,
            Privilege.BOOKING_REJECT,
            Privilege.BOOKING_SUPPORT_REQUEST,
            Privilege.BOOKING_VIEW_OWN,
            Privilege.BOOKING_LIST_HOST,
            Privilege.PROPERTY_CREATE,
            Privilege.PROPERTY_VIEW,
            Privilege.PROPERTY_VIEW_HOST_LIST
    )),
    ADMIN(Set.of(
            Privilege.BOOKING_FORCE_STATUS,
            Privilege.BOOKING_SUPPORT_PROCESS,
            Privilege.BOOKING_VIEW_ANY,
            Privilege.BOOKING_LIST_HOST,
            Privilege.BOOKING_LIST_GUEST,
            Privilege.PROPERTY_VIEW,
            Privilege.PROPERTY_VIEW_HOST_LIST
    ));

    private final Set<Privilege> privileges;

    UserRole(Set<Privilege> privileges) {
        this.privileges = privileges;
    }

    public Set<Privilege> getPrivileges() {
        return privileges;
    }
}
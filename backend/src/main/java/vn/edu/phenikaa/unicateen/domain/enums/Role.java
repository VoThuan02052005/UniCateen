package vn.edu.phenikaa.unicateen.domain.enums;

/** Vai tro nguoi dung; moi tai khoan dung mot vai tro (BR42). */
public enum Role {
    ADMIN, MANAGER, CASHIER, INVENTORY_STAFF, KITCHEN_STAFF, STUDENT, FACULTY;

    /** BR23, BR45: chi sinh vien, giang vien co ma the. */
    public boolean isCardHolder() {
        return this == STUDENT || this == FACULTY;
    }
}

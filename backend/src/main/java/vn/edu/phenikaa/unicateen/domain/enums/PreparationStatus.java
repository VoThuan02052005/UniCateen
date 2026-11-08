package vn.edu.phenikaa.unicateen.domain.enums;

/** Trang thai chuan bi mon (GD8). */
public enum PreparationStatus {
    NOT_STARTED, PREPARING, READY, SERVED;

    /** BR39: chi di tien theo thu tu. */
    public PreparationStatus next() {
        return switch (this) {
            case NOT_STARTED -> PREPARING;
            case PREPARING -> READY;
            case READY -> SERVED;
            case SERVED -> null;
        };
    }
}

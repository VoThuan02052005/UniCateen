package vn.edu.phenikaa.unicateen.api.common;

import jakarta.validation.constraints.NotBlank;

/** Doi trang thai dang chuoi (ACTIVE, INACTIVE, ON_SALE ...). */
public record StatusRequest(@NotBlank String status) {
}

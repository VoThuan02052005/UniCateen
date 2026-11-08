package vn.edu.phenikaa.unicateen.application.port;

import java.math.BigDecimal;

/** Tong luong theo nguyen lieu (ket qua truy van gop). */
public record IngredientQty(Long ingredientId, BigDecimal quantity) {
}

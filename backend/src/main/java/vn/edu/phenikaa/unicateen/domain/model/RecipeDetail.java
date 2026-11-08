package vn.edu.phenikaa.unicateen.domain.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

/** Dinh muc mot nguyen lieu cho mot phan mon (BR08). */
@Entity
@Table(name = "recipe_details")
public class RecipeDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_id")
    private Recipe recipe;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredient_id")
    private Ingredient ingredient;
    @Column(name = "qty_per_portion", nullable = false, precision = 10, scale = 3)
    private BigDecimal qtyPerPortion;

    protected RecipeDetail() {
    }

    RecipeDetail(Recipe recipe, Ingredient ingredient, BigDecimal qtyPerPortion) {
        this.recipe = recipe;
        this.ingredient = ingredient;
        this.qtyPerPortion = qtyPerPortion;
    }

    public Long getId() { return id; }
    public Recipe getRecipe() { return recipe; }
    public Ingredient getIngredient() { return ingredient; }
    public BigDecimal getQtyPerPortion() { return qtyPerPortion; }
}

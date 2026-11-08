package vn.edu.phenikaa.unicateen.api.kitchen;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.edu.phenikaa.unicateen.application.common.CurrentUser;
import vn.edu.phenikaa.unicateen.application.kitchen.KitchenService;
import vn.edu.phenikaa.unicateen.application.kitchen.KitchenService.PreparationView;
import vn.edu.phenikaa.unicateen.domain.enums.PreparationStatus;

/** UC4.5 Theo doi chuan bi mon (KITCHEN_STAFF). */
@RestController
@RequestMapping("/api/kitchen")
public class KitchenController {
    private final KitchenService kitchen;

    public KitchenController(KitchenService kitchen) {
        this.kitchen = kitchen;
    }

    public record AdvanceRequest(@NotNull PreparationStatus status) {
    }

    @GetMapping("/preparations")
    public List<PreparationView> board() {
        return kitchen.board();
    }

    @PostMapping("/preparations/{id}/status")
    public PreparationView advance(@PathVariable Long id, @Valid @RequestBody AdvanceRequest r,
                                   @AuthenticationPrincipal CurrentUser me) {
        return kitchen.advance(id, r.status(), me);
    }
}

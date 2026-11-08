package vn.edu.phenikaa.unicateen.application.kitchen;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.phenikaa.unicateen.application.common.Actors;
import vn.edu.phenikaa.unicateen.application.common.CurrentUser;
import vn.edu.phenikaa.unicateen.application.common.NotFoundException;
import vn.edu.phenikaa.unicateen.application.port.PreparationRepository;
import vn.edu.phenikaa.unicateen.domain.enums.PreparationStatus;
import vn.edu.phenikaa.unicateen.domain.model.Preparation;

/** UC4.5 Theo doi chuan bi mon theo don (GD8, BR39). */
@Service
@Transactional
public class KitchenService {
    private final PreparationRepository preparations;
    private final Actors actors;

    public KitchenService(PreparationRepository preparations, Actors actors) {
        this.preparations = preparations;
        this.actors = actors;
    }

    public record Line(String menuItemName, String unit, int quantity) {
    }

    public record PreparationView(Long id, String orderCode, PreparationStatus status, LocalDateTime paidAt,
                                  LocalDateTime startedAt, LocalDateTime readyAt, String customerName,
                                  List<Line> lines) {
        static PreparationView of(Preparation p) {
            var o = p.getOrder();
            return new PreparationView(p.getId(), o.getCode(), p.getStatus(), o.getPaidAt(), p.getStartedAt(),
                    p.getReadyAt(), o.getCustomer() == null ? null : o.getCustomer().getFullName(),
                    o.getDetails().stream().map(d -> new Line(d.getMenuItem().getName(), d.getMenuItem().getUnit(),
                            d.getQuantity())).toList());
        }
    }

    /** Danh sach don da thanh toan chua giao, theo thu tu thanh toan. */
    @Transactional(readOnly = true)
    public List<PreparationView> board() {
        return preparations.findByStatusInOrderByOrderPaidAtAsc(
                        EnumSet.of(PreparationStatus.NOT_STARTED, PreparationStatus.PREPARING, PreparationStatus.READY))
                .stream().map(PreparationView::of).toList();
    }

    /** BR39: chi chuyen sang trang thai ke tiep; khong doi trang thai don hay thanh toan. */
    public PreparationView advance(Long id, PreparationStatus target, CurrentUser actor) {
        Preparation p = preparations.findById(id).orElseThrow(() -> new NotFoundException("trạng thái chuẩn bị", id));
        p.advance(target, actors.load(actor));
        return PreparationView.of(p);
    }
}

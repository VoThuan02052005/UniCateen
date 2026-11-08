package vn.edu.phenikaa.unicateen.api.payment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.edu.phenikaa.unicateen.application.common.CurrentUser;
import vn.edu.phenikaa.unicateen.application.payment.PaymentService;
import vn.edu.phenikaa.unicateen.application.payment.PaymentView;
import vn.edu.phenikaa.unicateen.domain.Clock;
import vn.edu.phenikaa.unicateen.domain.enums.PaymentMethod;
import vn.edu.phenikaa.unicateen.domain.enums.PaymentStatus;

/**
 * Nhom 3: thanh toan (UC3.1-UC3.5). Moi yeu cau tao giao dich mang header Idempotency-Key do giao dien sinh
 * mot lan cho moi lan bam (NFR06).
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService payments;

    public PaymentController(PaymentService payments) {
        this.payments = payments;
    }

    public record CashRequest(@NotBlank String orderCode, @NotNull @PositiveOrZero Long receivedAmount,
                              @Size(max = 20) String cardCode) {
    }

    public record CardRequest(@NotBlank String orderCode, @NotBlank @Size(max = 20) String cardCode) {
    }

    public record TransferRequest(@NotBlank String orderCode, @Size(max = 20) String cardCode) {
    }

    public record FailRequest(@NotBlank @Size(max = 255) String reason) {
    }

    @PostMapping("/cash")
    public PaymentView cash(@RequestHeader("Idempotency-Key") @Size(max = 64) String key,
                            @Valid @RequestBody CashRequest r, @AuthenticationPrincipal CurrentUser me) {
        return payments.payCash(r.orderCode(), r.receivedAmount(), r.cardCode(), key, me);
    }

    @PostMapping("/card")
    public PaymentView card(@RequestHeader("Idempotency-Key") @Size(max = 64) String key,
                            @Valid @RequestBody CardRequest r, @AuthenticationPrincipal CurrentUser me) {
        return payments.payCard(r.orderCode(), r.cardCode(), key, me);
    }

    @PostMapping("/transfer")
    public PaymentView transfer(@RequestHeader("Idempotency-Key") @Size(max = 64) String key,
                                @Valid @RequestBody TransferRequest r, @AuthenticationPrincipal CurrentUser me) {
        return payments.startTransfer(r.orderCode(), r.cardCode(), key, me);
    }

    @GetMapping("/transfers/pending")
    public List<PaymentView> pendingTransfers() {
        return payments.pendingTransfers();
    }

    @PostMapping("/{code}/confirm")
    public PaymentView confirm(@PathVariable String code, @AuthenticationPrincipal CurrentUser me) {
        return payments.confirmTransfer(code, me);
    }

    @PostMapping("/{code}/fail")
    public PaymentView fail(@PathVariable String code, @Valid @RequestBody FailRequest r,
                            @AuthenticationPrincipal CurrentUser me) {
        return payments.failTransfer(code, r.reason(), me);
    }

    @GetMapping
    public List<PaymentView> search(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                    LocalDate from,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                    LocalDate to,
                                    @RequestParam(required = false) PaymentMethod method,
                                    @RequestParam(required = false) PaymentStatus status) {
        LocalDate t = to == null ? Clock.today() : to;
        return payments.search(from == null ? t : from, t, method, status);
    }

    @GetMapping("/by-order/{orderCode}")
    public List<PaymentView> byOrder(@PathVariable String orderCode, @AuthenticationPrincipal CurrentUser me) {
        return payments.byOrder(orderCode, me);
    }
}

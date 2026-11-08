package vn.edu.phenikaa.unicateen.infrastructure.card;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import vn.edu.phenikaa.unicateen.application.port.ICardPaymentGateway;

/**
 * Dich vu the mo phong (GD2). Quy uoc ma the thu de kiem thu:
 * <ul>
 *   <li>ma ket thuc bang "DECLINE" -> tu choi;</li>
 *   <li>ma ket thuc bang "SLOW" -> phan hoi cham hon thoi gian cho (kiem thu BR27);</li>
 *   <li>con lai -> chap nhan.</li>
 * </ul>
 * Cung mot idempotencyKey luon tra cung ket qua (mo phong dich vu that co chong lap).
 */
@Component
public class SimulatedCardGateway implements ICardPaymentGateway {
    private final Duration timeout;

    public SimulatedCardGateway(@Value("${unicateen.card.timeout-seconds:10}") long timeoutSeconds) {
        this.timeout = Duration.ofSeconds(timeoutSeconds);
    }

    @Override
    public CardResult charge(String cardCode, long amount, String orderCode, String idempotencyKey) {
        CompletableFuture<CardResult> call = CompletableFuture.supplyAsync(() -> simulate(cardCode, idempotencyKey));
        try {
            return call.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            call.cancel(true);
            throw new CardGatewayException("Dịch vụ thẻ không phản hồi trong " + timeout.toSeconds() + " giây", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CardGatewayException("Gọi dịch vụ thẻ bị gián đoạn", e);
        } catch (ExecutionException e) {
            throw new CardGatewayException("Lỗi dịch vụ thẻ", e.getCause());
        }
    }

    private CardResult simulate(String cardCode, String idempotencyKey) {
        String code = cardCode.toUpperCase();
        if (code.endsWith("SLOW")) {
            sleep(timeout.plusSeconds(1));
        }
        if (code.endsWith("DECLINE")) {
            return new CardResult(false, null, "Thẻ bị từ chối");
        }
        String ref = "SIM-" + UUID.nameUUIDFromBytes(idempotencyKey.getBytes()).toString().substring(0, 8).toUpperCase();
        return new CardResult(true, ref, "Chấp nhận");
    }

    private static void sleep(Duration d) {
        try {
            Thread.sleep(d.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

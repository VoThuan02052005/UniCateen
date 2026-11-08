package vn.edu.phenikaa.unicateen.application.port;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import vn.edu.phenikaa.unicateen.domain.model.ItemPrice;

public interface ItemPriceRepository {
    /** Gia co hieu luc tai thoi diem t (BR05). */
    Optional<ItemPrice> findEffectiveAt(Long menuItemId, LocalDateTime t);

    /** Cac gia chua bi huy, chua ket thuc truoc thoi diem t; dung de dong gia cu khi doi gia. */
    List<ItemPrice> findOpenFrom(Long menuItemId, LocalDateTime t);

    List<ItemPrice> findByMenuItemIdOrderByEffectiveFromDesc(Long menuItemId);

    List<ItemPrice> findDueScheduled(LocalDateTime t);

    <S extends ItemPrice> S save(S price);
}

package vn.edu.phenikaa.unicateen.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.phenikaa.unicateen.application.port.ItemPriceRepository;
import vn.edu.phenikaa.unicateen.domain.model.ItemPrice;

public interface JpaItemPriceRepository extends JpaRepository<ItemPrice, Long>, ItemPriceRepository {
    @Override
    @Query("""
            select p from ItemPrice p
            where p.menuItem.id = :itemId and p.status <> vn.edu.phenikaa.unicateen.domain.enums.PriceStatus.CANCELLED
              and p.effectiveFrom <= :t and (p.effectiveTo is null or p.effectiveTo > :t)""")
    Optional<ItemPrice> findEffectiveAt(@Param("itemId") Long menuItemId, @Param("t") LocalDateTime t);

    @Override
    @Query("""
            select p from ItemPrice p
            where p.menuItem.id = :itemId and p.status <> vn.edu.phenikaa.unicateen.domain.enums.PriceStatus.CANCELLED
              and (p.effectiveTo is null or p.effectiveTo > :t)""")
    List<ItemPrice> findOpenFrom(@Param("itemId") Long menuItemId, @Param("t") LocalDateTime t);

    @Override
    @Query("""
            select p from ItemPrice p
            where p.status = vn.edu.phenikaa.unicateen.domain.enums.PriceStatus.SCHEDULED and p.effectiveFrom <= :t""")
    List<ItemPrice> findDueScheduled(@Param("t") LocalDateTime t);
}

package vn.edu.phenikaa.unicateen.application.port;

import java.util.List;
import java.util.Optional;
import vn.edu.phenikaa.unicateen.domain.enums.Role;
import vn.edu.phenikaa.unicateen.domain.enums.UserStatus;
import vn.edu.phenikaa.unicateen.domain.model.User;

/** Port luu tru tai khoan; infrastructure.persistence cai dat bang Spring Data JPA. */
public interface UserRepository {
    Optional<User> findById(Long id);

    Optional<User> findByEmail(String email);

    Optional<User> findByCardCode(String cardCode);

    boolean existsByEmail(String email);

    boolean existsByCardCode(String cardCode);

    long countByRoleAndStatus(Role role, UserStatus status);

    List<User> findAllByOrderByIdAsc();

    <S extends User> S save(S user);
}

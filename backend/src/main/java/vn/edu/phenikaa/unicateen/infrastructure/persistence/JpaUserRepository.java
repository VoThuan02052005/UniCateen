package vn.edu.phenikaa.unicateen.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.phenikaa.unicateen.application.port.UserRepository;
import vn.edu.phenikaa.unicateen.domain.model.User;

public interface JpaUserRepository extends JpaRepository<User, Long>, UserRepository {
}

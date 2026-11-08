package vn.edu.phenikaa.unicateen.application.common;

import org.springframework.stereotype.Component;
import vn.edu.phenikaa.unicateen.application.port.UserRepository;
import vn.edu.phenikaa.unicateen.domain.model.User;

/** Nap entity User cua nguoi dang thao tac. */
@Component
public class Actors {
    private final UserRepository users;

    public Actors(UserRepository users) {
        this.users = users;
    }

    public User load(CurrentUser actor) {
        return users.findById(actor.id()).orElseThrow(() -> new NotFoundException("tài khoản", actor.id()));
    }
}

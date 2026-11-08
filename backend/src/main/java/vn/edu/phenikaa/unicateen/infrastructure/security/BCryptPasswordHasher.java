package vn.edu.phenikaa.unicateen.infrastructure.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import vn.edu.phenikaa.unicateen.application.port.IPasswordHasher;

/** Bam mat khau bang bcrypt, co muoi (BR41, NFR03). */
@Component
public class BCryptPasswordHasher implements IPasswordHasher {
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);

    @Override
    public String hash(String raw) {
        return encoder.encode(raw);
    }

    @Override
    public boolean matches(String raw, String hash) {
        return raw != null && hash != null && encoder.matches(raw, hash);
    }
}

package vn.edu.phenikaa.unicateen.application.port;

/** Bam mat khau co muoi (BR41). */
public interface IPasswordHasher {
    String hash(String raw);

    boolean matches(String raw, String hash);
}

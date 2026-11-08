package vn.edu.phenikaa.unicateen.domain;

/**
 * Vi pham quy tac nghiep vu. {@code rule} la ma quy tac trong bao cao (vi du "BR20"),
 * tang api chuyen thanh HTTP 409.
 */
public class BusinessRuleException extends RuntimeException {
    private final String rule;

    public BusinessRuleException(String rule, String message) {
        super(message);
        this.rule = rule;
    }

    public String getRule() {
        return rule;
    }
}

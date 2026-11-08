package vn.edu.phenikaa.unicateen.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import vn.edu.phenikaa.unicateen.domain.Clock;

/** Nhat ky kiem toan (NFR09). Bang chi them, khong sua, khong xoa. */
@Entity
@Table(name = "audit_logs")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_id")
    private User actor;
    @Column(nullable = false, length = 50)
    private String action;
    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;
    @Column(name = "entity_id", nullable = false)
    private Long entityId;
    @Column(name = "before_value", columnDefinition = "JSON")
    private String beforeValue;
    @Column(name = "after_value", columnDefinition = "JSON")
    private String afterValue;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected AuditLog() {
    }

    public AuditLog(User actor, String action, String entityType, Long entityId, String beforeJson, String afterJson) {
        this.actor = actor;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.beforeValue = beforeJson;
        this.afterValue = afterJson;
        this.createdAt = Clock.now();
    }

    public Long getId() { return id; }
    public User getActor() { return actor; }
    public String getAction() { return action; }
    public String getEntityType() { return entityType; }
    public Long getEntityId() { return entityId; }
    public String getBeforeValue() { return beforeValue; }
    public String getAfterValue() { return afterValue; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}

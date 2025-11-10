package model;

import java.io.Serializable;
import java.util.UUID;

public abstract class BaseEntity implements Serializable {
    private final UUID id = UUID.randomUUID();

    public UUID getId() { return id; }
}


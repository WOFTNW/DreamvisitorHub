package org.woftnw.DreamvisitorHub.data.type;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.util.UUID;

@Entity
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID uuid;
    private String mcUsername;
    private String dcUsername;
    private Long snowflakeId;

    public UUID getUuid() {
        return uuid;
    }

    public Long getSnowflakeId() {
        return snowflakeId;
    }

    public String getDcUsername() {
        return dcUsername;
    }

    public String getMcUsername() {
        return mcUsername;
    }

    public void setDcUsername(String dcUsername) {
        this.dcUsername = dcUsername;
    }

    public void setMcUsername(String mcUsername) {
        this.mcUsername = mcUsername;
    }

    public void setSnowflakeId(Long snowflakeId) {
        this.snowflakeId = snowflakeId;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }
}


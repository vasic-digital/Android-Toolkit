package com.redelf.commons.model;

import androidx.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

public abstract class HasId {

    @JsonProperty("id")
    @SerializedName("id")
    private Long id;

    @JsonCreator
    public HasId() {

        setId(initializeId());
    }

    @Nullable
    public Long getId() {

        return id;
    }

    public void setId(final long id) {

        this.id = id;
    }

    public boolean hasValidId() {

        return id != null && id != 0;
    }

    protected long initializeId() {

        return 0;
    }
}

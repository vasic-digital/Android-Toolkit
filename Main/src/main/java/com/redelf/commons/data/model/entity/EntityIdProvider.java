package com.redelf.commons.data.model.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import com.redelf.commons.data.Storage;
import com.redelf.commons.execution.Executor;
import com.redelf.commons.obtain.suspendable.Obtain;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class EntityIdProvider implements EntityIdProvide {

    @JsonProperty("lastid")
    @SerializedName("lastid")
    private final static ConcurrentHashMap<String, AtomicLong> lastId;

    @JsonProperty("kind")
    @SerializedName("kind")
    private final Obtain<String> kind;

    static {

        lastId = new ConcurrentHashMap<>();
    }

    public EntityIdProvider(final Obtain<String> kind) {

        this.kind = kind;

        Executor.MAIN.execute(() -> {

            final String idKey = getIdKey(kind);

            AtomicLong lastIdVal = lastId.get(idKey);

            if (lastIdVal == null) {

                lastIdVal = new AtomicLong();
                lastIdVal.set(Storage.get(idKey, 0L));
                lastId.put(idKey, lastIdVal);
            }
        });
    }


    @Override
    public long generateNewId() {

        final String idKey = getIdKey(kind);
        final AtomicLong newId = new AtomicLong(-1);
        final AtomicLong id = lastId.get(idKey);

        if (id != null) {

            if (id.get() == Long.MIN_VALUE) {

                id.set(0);
            }

            newId.set(id.decrementAndGet());
        }

        Storage.put(idKey, newId.get());

        return newId.get();
    }

    private String getIdKey(final Obtain<String> kind) {

        return "id_" + kind.obtain();
    }
}

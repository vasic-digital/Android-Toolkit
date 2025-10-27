/*
 * Copyright (c) 2025 MeTube Share
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */


package com.redelf.commons.data.model.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import com.redelf.commons.data.MemoryStorage;
import com.redelf.commons.execution.Executor;
import com.redelf.commons.obtain.Obtain;

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

                lastIdVal.set(

                        MemoryStorage.INSTANCE.get(idKey, 0L)
                );

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

        MemoryStorage.INSTANCE.put(idKey, newId.get());

        return newId.get();
    }

    private String getIdKey(final Obtain<String> kind) {

        return "id_" + kind.obtain();
    }
}

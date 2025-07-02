package com.redelf.commons.persistance.base

import com.redelf.commons.contain.Contain
import com.redelf.commons.destruction.delete.Deletion
import com.redelf.commons.destruction.delete.DeletionAsync
import com.redelf.commons.direction.Pull
import com.redelf.commons.direction.Push

interface PersistenceAsync<K> : Pull<K>, Push<K>, DeletionAsync<K>, Contain<K> {

    companion object {

        const val TAG = "PERSISTENCE ::"
    }
}
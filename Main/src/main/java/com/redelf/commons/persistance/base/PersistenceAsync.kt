package com.redelf.commons.persistance.base

import com.redelf.commons.contain.Contain
import com.redelf.commons.contain.ContainAsync
import com.redelf.commons.destruction.delete.Deletion
import com.redelf.commons.destruction.delete.DeletionAsync
import com.redelf.commons.direction.Pull
import com.redelf.commons.direction.PullAsync
import com.redelf.commons.direction.Push

interface PersistenceAsync<K> : PullAsync<K>, Push<K>, DeletionAsync<K>, ContainAsync<K> {

    companion object {

        const val TAG = "PERSISTENCE :: ASYNC ::"
    }
}
package com.redelf.commons.persistance.base

import com.redelf.commons.contain.ContainAsync
import com.redelf.commons.destruction.delete.DeletionAsync
import com.redelf.commons.direction.PullAsync
import com.redelf.commons.direction.PushWithConditionCheck

interface PersistenceAsync<K> :

    PullAsync<K>,
    ContainAsync<K>,
    DeletionAsync<K>,
    PushWithConditionCheck<K>

{

    companion object {

        const val TAG = "PERSISTENCE :: ASYNC ::"
    }
}
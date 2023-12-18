package com.redelf.commons.persistance

import com.redelf.commons.delete.Deletion
import com.redelf.commons.direction.Pull
import com.redelf.commons.direction.Push

interface Persistence<K> : Pull<K>, Push<K>, Deletion<K>
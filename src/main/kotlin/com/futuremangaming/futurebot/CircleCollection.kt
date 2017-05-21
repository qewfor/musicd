/*
 *     Copyright 2014-2017 FuturemanGaming
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.futuremangaming.futurebot

import java.util.Enumeration

class CircleCollection<T>(coll: Collection<T>): Enumeration<T> {
    private val list = ArrayList<T>(coll)
    private var index = 0

    constructor() : this(emptyList())
    constructor(initialCapacity: Int) : this(ArrayList<T>(initialCapacity))

    fun add(item: T) = list.add(item)
    fun addAll(items: Collection<T>) = list.addAll(items)
    fun remove(item: T) = list.remove(item)
    fun removeIf(test: (T) -> Boolean) = list.removeIf(test)

    fun next(): T = list[nextIndex()]
    override fun nextElement() = next()
    override fun hasMoreElements()= list.isNotEmpty()

    private fun nextIndex(): Int {
        if (list.isEmpty())
            throw NoSuchElementException()
        if (index == 0 && list.size == 1)
            return index
        if (index + 1 >= list.size)
            index = -1
        return ++index
    }
}

/*
 * Copyright 2024 T Jake Luciani
 *
 * The Jlama Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.github.tjake.jlama.util;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class UnsafeDirectByteBuffer {
    private static final VarHandle VAR_HANDLE_ADDRESS;
    public static final int CACHE_LINE_SIZE = 64;

    static {
        try {
            VAR_HANDLE_ADDRESS = MethodHandles
                    .privateLookupIn(ByteBuffer.class, MethodHandles.lookup()).findVarHandle(ByteBuffer.class,
                            "address", long.class);
        } catch (RuntimeException | IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public static long getAddress(ByteBuffer buffy) {
        if (VAR_HANDLE_ADDRESS != null) {
            return (Long) VAR_HANDLE_ADDRESS.get(buffy);
        }
        throw new IllegalArgumentException("unreachable");
    }


    public static ByteBuffer allocateAlignedByteBuffer(int capacity, long align) {
        if (Long.bitCount(align) != 1) {
            throw new IllegalArgumentException("Alignment must be a power of 2");
        }
        // We over allocate by the alignment so we know we can have a large
        // enough aligned block of memory to use.
        ByteBuffer buffy = ByteBuffer.allocateDirect((int) (capacity + align));
        long address = getAddress(buffy);
        if ((address & (align - 1)) == 0) {
            // limit to the capacity specified
            buffy.limit(capacity);
            // set order to native while we are here.
            ByteBuffer slice = buffy.slice().order(ByteOrder.nativeOrder());
            // the slice is now an aligned buffer of the required capacity
            return slice;
        } else {
            int newPosition = (int) (align - (address & (align - 1)));
            buffy.position(newPosition);
            int newLimit = newPosition + capacity;
            // limit to the capacity specified
            buffy.limit(newLimit);
            return buffy.slice().order(ByteOrder.nativeOrder());
        }
    }
}

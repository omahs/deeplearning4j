/*
 *  ******************************************************************************
 *  *
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Apache License, Version 2.0 which is available at
 *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *
 *  *  See the NOTICE file distributed with this work for additional
 *  *  information regarding copyright ownership.
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  * License for the specific language governing permissions and limitations
 *  * under the License.
 *  *
 *  * SPDX-License-Identifier: Apache-2.0
 *  *****************************************************************************
 */

package org.nd4j.linalg.cpu.nativecpu.buffer;

import lombok.NonNull;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.FloatPointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.indexer.DoubleIndexer;
import org.bytedeco.javacpp.indexer.FloatIndexer;
import org.bytedeco.javacpp.indexer.Indexer;
import org.bytedeco.javacpp.indexer.IntIndexer;
import org.nd4j.linalg.api.buffer.*;
import org.nd4j.linalg.api.buffer.factory.DataBufferFactory;
import org.nd4j.linalg.api.memory.MemoryWorkspace;
import org.nd4j.common.util.ArrayUtil;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class DefaultDataBufferFactory implements DataBufferFactory {
    protected DataBuffer.AllocationMode allocationMode;


    @Override
    public void setAllocationMode(DataBuffer.AllocationMode allocationMode) {
        this.allocationMode = allocationMode;
    }

    @Override
    public DataBuffer.AllocationMode allocationMode() {
        if (allocationMode == null) {
            String otherAlloc = System.getProperty("alloc");
            if (otherAlloc.equals("heap"))
                setAllocationMode(DataBuffer.AllocationMode.HEAP);
            else if (otherAlloc.equals("direct"))
                setAllocationMode(DataBuffer.AllocationMode.DIRECT);
            else if (otherAlloc.equals("javacpp"))
                setAllocationMode(DataBuffer.AllocationMode.JAVACPP);
        }
        return allocationMode;
    }

    @Override
    public DataBuffer create(DataBuffer underlyingBuffer, long offset, long length) {
        if (underlyingBuffer.dataType() == DataType.DOUBLE) {
            return new DoubleBuffer(underlyingBuffer, length, offset);
        } else if (underlyingBuffer.dataType() == DataType.FLOAT) {
            return new FloatBuffer(underlyingBuffer, length, offset);
        } else if (underlyingBuffer.dataType() == DataType.INT) {
            return new IntBuffer(underlyingBuffer, length, offset);
        } else if (underlyingBuffer.dataType() == DataType.LONG) {
            return new LongBuffer(underlyingBuffer, length, offset);
        } else if (underlyingBuffer.dataType() == DataType.BOOL) {
            return new BoolBuffer(underlyingBuffer, length, offset);
        } else if (underlyingBuffer.dataType() == DataType.SHORT) {
            return new Int16Buffer(underlyingBuffer, length, offset);
        } else if (underlyingBuffer.dataType() == DataType.BYTE) {
            return new Int8Buffer(underlyingBuffer, length, offset);
        } else if (underlyingBuffer.dataType() == DataType.UBYTE) {
            return new UInt8Buffer(underlyingBuffer, length, offset);
        } else if (underlyingBuffer.dataType() == DataType.UINT16) {
            return new UInt16Buffer(underlyingBuffer, length, offset);
        } else if (underlyingBuffer.dataType() == DataType.UINT32) {
            return new UInt32Buffer(underlyingBuffer, length, offset);
        } else if (underlyingBuffer.dataType() == DataType.UINT64) {
            return new UInt64Buffer(underlyingBuffer, length, offset);
        } else if (underlyingBuffer.dataType() == DataType.BFLOAT16) {
            return new BFloat16Buffer(underlyingBuffer, length, offset);
        } else if (underlyingBuffer.dataType() == DataType.HALF) {
            return new HalfBuffer(underlyingBuffer, length, offset);
        } else if (underlyingBuffer.dataType() == DataType.UTF8) {
            return new Utf8Buffer(underlyingBuffer, length, offset);
        }
        return null;
    }


    @Override
    public DataBuffer createDouble(long offset, int length) {
        return new DoubleBuffer(length, 8, offset);
    }

    @Override
    public DataBuffer createFloat(long offset, int length) {
        return new FloatBuffer(length, 4, offset);
    }

    @Override
    public DataBuffer createInt(long offset, int length) {
        return new IntBuffer(length, 4, offset);
    }

    @Override
    public DataBuffer createDouble(long offset, int[] data) {
        return createDouble(offset, data, true);
    }

    @Override
    public DataBuffer createFloat(long offset, int[] data) {
        FloatBuffer ret = new FloatBuffer(ArrayUtil.toFloats(data), true, offset);
        return ret;
    }

    @Override
    public DataBuffer createInt(long offset, int[] data) {
        return new IntBuffer(data, true, offset);
    }

    @Override
    public DataBuffer createDouble(long offset, double[] data) {
        return new DoubleBuffer(data, true, offset);
    }

    @Override
    public DataBuffer createDouble(long offset, double[] data, MemoryWorkspace workspace) {
        return new DoubleBuffer(data, true, offset, workspace);
    }

    @Override
    public DataBuffer createDouble(long offset, byte[] data, int length) {
        return createDouble(offset, ArrayUtil.toDoubleArray(data), true);
    }

    @Override
    public DataBuffer createFloat(long offset, byte[] data, int length) {
        return createFloat(offset, ArrayUtil.toFloatArray(data), true);
    }

    @Override
    public DataBuffer createFloat(long offset, double[] data) {
        return new FloatBuffer(ArrayUtil.toFloats(data), true, offset);
    }

    @Override
    public DataBuffer createInt(long offset, double[] data) {
        return new IntBuffer(ArrayUtil.toInts(data), true, offset);
    }

    @Override
    public DataBuffer createDouble(long offset, float[] data) {
        return new DoubleBuffer(ArrayUtil.toDoubles(data), true, offset);
    }

    @Override
    public DataBuffer createFloat(long offset, float[] data) {
        return new FloatBuffer(data, true, offset);
    }

    @Override
    public DataBuffer createFloat(long offset, float[] data, MemoryWorkspace workspace) {
        return new FloatBuffer(data, true, offset, workspace);
    }

    @Override
    public DataBuffer createInt(long offset, float[] data) {
        return new IntBuffer(ArrayUtil.toInts(data), true, offset);
    }

    @Override
    public DataBuffer createDouble(long offset, int[] data, boolean copy) {
        return new DoubleBuffer(ArrayUtil.toDoubles(data), true, offset);
    }

    @Override
    public DataBuffer createFloat(long offset, int[] data, boolean copy) {
        return new FloatBuffer(ArrayUtil.toFloats(data), copy, offset);
    }

    @Override
    public DataBuffer createInt(long offset, int[] data, boolean copy) {
        return new IntBuffer(data, copy, offset);
    }

    @Override
    public DataBuffer createDouble(long offset, double[] data, boolean copy) {
        return new DoubleBuffer(data, copy, offset);
    }

    @Override
    public DataBuffer createFloat(long offset, double[] data, boolean copy) {
        return new FloatBuffer(ArrayUtil.toFloats(data), copy, offset);
    }

    @Override
    public DataBuffer createInt(long offset, double[] data, boolean copy) {
        return new IntBuffer(ArrayUtil.toInts(data), copy, offset);
    }

    @Override
    public DataBuffer createDouble(long offset, float[] data, boolean copy) {
        return new DoubleBuffer(ArrayUtil.toDoubles(data), copy, offset);
    }



    @Override
    public DataBuffer createFloat(long offset, float[] data, boolean copy) {
        return new FloatBuffer(data, copy, offset);
    }

    @Override
    public DataBuffer createInt(long offset, float[] data, boolean copy) {
        return new IntBuffer(ArrayUtil.toInts(data), copy, offset);
    }


    @Override
    public DataBuffer createDouble(long length) {
        return new DoubleBuffer(length);
    }

    @Override
    public DataBuffer createDouble(long length, boolean initialize) {
        return new DoubleBuffer(length, initialize);
    }

    @Override
    public DataBuffer createFloat(long length) {
        return new FloatBuffer(length);
    }

    @Override
    public DataBuffer createFloat(long length, boolean initialize) {
        return new FloatBuffer(length, initialize);
    }

    @Override
    public DataBuffer createFloat(long length, boolean initialize, MemoryWorkspace workspace) {
        return new FloatBuffer(length, initialize, workspace);
    }

    @Override
    public DataBuffer create(ByteBuffer underlyingBuffer, DataType dataType, long length, long offset) {
        switch (dataType) {
            case DOUBLE:
                return new DoubleBuffer(underlyingBuffer, dataType, length, offset);
            case FLOAT:
                return new FloatBuffer(underlyingBuffer, dataType, length, offset);
            case HALF:
                return new HalfBuffer(underlyingBuffer, dataType, length, offset);
            case BFLOAT16:
                return new BFloat16Buffer(underlyingBuffer, dataType, length, offset);
            case LONG:
                return new LongBuffer(underlyingBuffer, dataType, length, offset);
            case INT:
                return new IntBuffer(underlyingBuffer, dataType, length, offset);
            case SHORT:
                return new Int16Buffer(underlyingBuffer, dataType, length, offset);
            case UBYTE:
                return new UInt8Buffer(underlyingBuffer, dataType, length, offset);
            case UINT16:
                return new UInt16Buffer(underlyingBuffer, dataType, length, offset);
            case UINT32:
                return new UInt32Buffer(underlyingBuffer, dataType, length, offset);
            case UINT64:
                return new UInt64Buffer(underlyingBuffer, dataType, length, offset);
            case BYTE:
                return new Int8Buffer(underlyingBuffer, dataType, length, offset);
            case BOOL:
                return new BoolBuffer(underlyingBuffer, dataType, length, offset);
            case UTF8:
                return new Utf8Buffer(underlyingBuffer, dataType, length, offset);
            default:
                throw new IllegalStateException("Unknown datatype used: [" + dataType + "]");
        }
    }

    @Override
    public DataBuffer create(@NonNull DataType dataType, long length, boolean initialize) {
        switch (dataType) {
            case DOUBLE:
                return new DoubleBuffer(length, initialize);
            case FLOAT:
                return new FloatBuffer(length, initialize);
            case HALF:
                return new HalfBuffer(length, initialize);
            case BFLOAT16:
                return new BFloat16Buffer(length, initialize);
            case LONG:
                return new LongBuffer(length, initialize);
            case INT:
                return new IntBuffer(length, initialize);
            case SHORT:
                return new Int16Buffer(length, initialize);
            case UBYTE:
                return new UInt8Buffer(length, initialize);
            case UINT16:
                return new UInt16Buffer(length, initialize);
            case UINT32:
                return new UInt32Buffer(length, initialize);
            case UINT64:
                return new UInt64Buffer(length, initialize);
            case BYTE:
                return new Int8Buffer(length, initialize);
            case BOOL:
                return new BoolBuffer(length, initialize);
            case UTF8:
                return new Utf8Buffer(length, true);
            default:
                throw new IllegalStateException("Unknown datatype used: [" + dataType + "]");
        }
    }

    @Override
    public DataBuffer create(DataType dataType, long length, boolean initialize, MemoryWorkspace workspace) {
        switch (dataType) {
            case DOUBLE:
                return new DoubleBuffer(length, initialize, workspace);
            case FLOAT:
                return new FloatBuffer(length, initialize, workspace);
            case BFLOAT16:
                return new BFloat16Buffer(length, initialize, workspace);
            case HALF:
                return new HalfBuffer(length, initialize, workspace);
            case LONG:
                return new LongBuffer(length, initialize, workspace);
            case INT:
                return new IntBuffer(length, initialize, workspace);
            case SHORT:
                return new Int16Buffer(length, initialize, workspace);
            case UBYTE:
                return new UInt8Buffer(length, initialize, workspace);
            case UINT16:
                return new UInt16Buffer(length, initialize, workspace);
            case UINT32:
                return new UInt32Buffer(length, initialize, workspace);
            case UINT64:
                return new UInt64Buffer(length, initialize, workspace);
            case BYTE:
                return new Int8Buffer(length, initialize, workspace);
            case BOOL:
                return new BoolBuffer(length, initialize, workspace);
            default:
                throw new IllegalStateException("Unknown datatype used: [" + dataType + "]");
        }
    }

    @Override
    public DataBuffer createInt(long length) {
        return new IntBuffer(length);
    }

    @Override
    public DataBuffer createBFloat16(long length) {
        return new BFloat16Buffer(length);
    }

    @Override
    public DataBuffer createUInt(long length) {
        return new UInt32Buffer(length);
    }

    @Override
    public DataBuffer createUShort(long length) {
        return new UInt16Buffer(length);
    }

    @Override
    public DataBuffer createUByte(long length) {
        return new UInt8Buffer(length);
    }

    @Override
    public DataBuffer createULong(long length) {
        return new UInt64Buffer(length);
    }

    @Override
    public DataBuffer createBool(long length) {
        return new BoolBuffer(length);
    }

    @Override
    public DataBuffer createShort(long length) {
        return new Int16Buffer(length);
    }

    @Override
    public DataBuffer createByte(long length) {
        return new Int8Buffer(length);
    }

    @Override
    public DataBuffer createBFloat16(long length, boolean initialize) {
        return new BFloat16Buffer(length, initialize);
    }

    @Override
    public DataBuffer createUInt(long length, boolean initialize) {
        return new UInt32Buffer(length, initialize);
    }

    @Override
    public DataBuffer createUShort(long length, boolean initialize) {
        return new UInt16Buffer(length, initialize);
    }

    @Override
    public DataBuffer createUByte(long length, boolean initialize) {
        return new UInt8Buffer(length, initialize);
    }

    @Override
    public DataBuffer createULong(long length, boolean initialize) {
        return new UInt64Buffer(length, initialize);
    }

    @Override
    public DataBuffer createBool(long length, boolean initialize) {
        return new BoolBuffer(length, initialize);
    }

    @Override
    public DataBuffer createShort(long length, boolean initialize) {
        return new Int16Buffer(length, initialize);
    }

    @Override
    public DataBuffer createByte(long length, boolean initialize) {
        return new Int8Buffer(length, initialize);
    }

    @Override
    public DataBuffer createInt(long length, boolean initialize) {
        return new IntBuffer(length, initialize);
    }

    @Override
    public DataBuffer createBFloat16(long length, boolean initialize,  MemoryWorkspace workspace) {
        return new BFloat16Buffer(length, initialize, workspace);
    }

    @Override
    public DataBuffer createUInt(long length, boolean initialize,  MemoryWorkspace workspace) {
        return new UInt32Buffer(length, initialize, workspace);
    }

    @Override
    public DataBuffer createUShort(long length, boolean initialize,  MemoryWorkspace workspace) {
        return new UInt16Buffer(length, initialize, workspace);
    }

    @Override
    public DataBuffer createUByte(long length, boolean initialize,  MemoryWorkspace workspace) {
        return new UInt8Buffer(length, initialize, workspace);
    }

    @Override
    public DataBuffer createULong(long length, boolean initialize,  MemoryWorkspace workspace) {
        return new UInt64Buffer(length, initialize, workspace);
    }

    @Override
    public DataBuffer createBool(long length, boolean initialize,  MemoryWorkspace workspace) {
        return new BoolBuffer(length, initialize, workspace);
    }

    @Override
    public DataBuffer createShort(long length, boolean initialize,  MemoryWorkspace workspace) {
        return new Int16Buffer(length, initialize, workspace);
    }

    @Override
    public DataBuffer createByte(long length, boolean initialize,  MemoryWorkspace workspace) {
        return new Int8Buffer(length, initialize, workspace);
    }


    @Override
    public DataBuffer createInt(long length, boolean initialize, MemoryWorkspace workspace) {
        return new IntBuffer(length, initialize, workspace);
    }

    /**
     * This method will create new DataBuffer of the same dataType & same length
     *
     * @param buffer
     * @return
     */
    @Override
    public DataBuffer createSame(DataBuffer buffer, boolean init) {
        return create(buffer.dataType(), buffer.length(), init);
    }

    /**
     * This method will create new DataBuffer of the same dataType & same length
     *
     * @param buffer
     * @param workspace
     * @return
     */
    @Override
    public DataBuffer createSame(DataBuffer buffer, boolean init, MemoryWorkspace workspace) {
        return create(buffer.dataType(), buffer.length(), init, workspace);
    }

    @Override
    public DataBuffer createBuffer(String[] data) {
        return  new Utf8Buffer(Arrays.asList(data));
    }

    @Override
    public DataBuffer createTypedBuffer(String[] data, DataType dataType) {
        switch(dataType) {
            case UTF8:
                return new Utf8Buffer(Arrays.asList(data));
            case UTF16:
                return new Utf16Buffer(Arrays.asList(data));
            case UTF32:
                return new Utf32Buffer(Arrays.asList(data));
            default:
                throw new UnsupportedOperationException("Cannot create buffer of type " + dataType);
        }
    }

    @Override
    public DataBuffer createDouble(int[] data) {
        return createDouble(data, true);
    }

    @Override
    public DataBuffer createFloat(int[] data) {
        return createFloat(data, true);
    }

    @Override
    public DataBuffer createInt(int[] data) {
        return createInt(data, true);
    }

    @Override
    public DataBuffer createInt(int[] data, MemoryWorkspace workspace) {
        return createInt(data, true, workspace);
    }

    @Override
    public DataBuffer createInt(int[] data, boolean copy, MemoryWorkspace workspace) {
        return new IntBuffer(data, copy, workspace);
    }

    @Override
    public DataBuffer createDouble(double[] data) {
        return createDouble(data, true);
    }

    @Override
    public DataBuffer createFloat(double[] data) {
        return createFloat(data, true);
    }

    @Override
    public DataBuffer createInt(double[] data) {
        return createInt(data, true);
    }

    @Override
    public DataBuffer createDouble(float[] data) {
        return createDouble(data, true);
    }

    @Override
    public DataBuffer createFloat(float[] data) {
        return createFloat(data, true);
    }

    @Override
    public DataBuffer createFloat(float[] data, MemoryWorkspace workspace) {
        return createFloat(data, true, workspace);
    }

    @Override
    public DataBuffer createInt(float[] data) {
        return createInt(data, true);
    }

    @Override
    public DataBuffer createDouble(int[] data, boolean copy) {
        return new DoubleBuffer(ArrayUtil.toDoubles(data), copy);
    }

    @Override
    public DataBuffer createFloat(int[] data, boolean copy) {
        return new FloatBuffer(ArrayUtil.toFloats(data), copy);
    }

    @Override
    public DataBuffer createInt(int[] data, boolean copy) {
        return new IntBuffer(data, copy);
    }

    @Override
    public DataBuffer createLong(int[] data, boolean copy) {
        return new LongBuffer(ArrayUtil.toLongArray(data), copy);
    }

    @Override
    public DataBuffer createDouble(long[] data, boolean copy) {
        return new DoubleBuffer(ArrayUtil.toDouble(data), copy);
    }

    @Override
    public DataBuffer createFloat(long[] data, boolean copy) {
        return new FloatBuffer(ArrayUtil.toFloats(data), copy);
    }

    @Override
    public DataBuffer createInt(long[] data, boolean copy) {
        return new IntBuffer(ArrayUtil.toInts(data), copy);
    }

    @Override
    public DataBuffer createLong(long[] data) {
        return createLong(data, true);
    }

    @Override
    public DataBuffer createLong(long[] data, boolean copy) {
        return new LongBuffer(data, copy);
    }

    @Override
    public DataBuffer createLong(long[] data, MemoryWorkspace workspace) {
        return new LongBuffer(data, true, workspace);
    }

    @Override
    public DataBuffer createLong(long length) {
        return new LongBuffer(length);
    }

    @Override
    public DataBuffer createLong(long length, boolean initialize) {
        return new LongBuffer(length, initialize);
    }

    @Override
    public DataBuffer createLong(long length, boolean initialize, MemoryWorkspace workspace) {
        return new LongBuffer(length, initialize, workspace);
    }

    @Override
    public DataBuffer createDouble(double[] data, boolean copy) {
        return new DoubleBuffer(data, copy);
    }

    @Override
    public DataBuffer createDouble(double[] data, MemoryWorkspace workspace) {
        return createDouble(data, true, workspace);
    }

    @Override
    public DataBuffer createDouble(double[] data, boolean copy, MemoryWorkspace workspace) {
        return new DoubleBuffer(data, copy, workspace);
    }

    @Override
    public DataBuffer createDouble(long length, boolean initialize, MemoryWorkspace workspace) {
        return new DoubleBuffer(length, initialize, workspace);
    }

    @Override
    public DataBuffer createFloat(double[] data, boolean copy) {
        return new FloatBuffer(ArrayUtil.toFloats(data), copy);
    }

    @Override
    public DataBuffer createInt(double[] data, boolean copy) {
        return new IntBuffer(ArrayUtil.toInts(data), copy);
    }

    @Override
    public DataBuffer createDouble(float[] data, boolean copy) {
        return new DoubleBuffer(data, copy);
    }

    @Override
    public DataBuffer createFloat(float[] data, boolean copy) {
        return new FloatBuffer(data, copy);
    }

    @Override
    public DataBuffer createFloat(float[] data, boolean copy, MemoryWorkspace workspace) {
        return new FloatBuffer(data, copy, workspace);
    }

    @Override
    public DataBuffer createInt(float[] data, boolean copy) {
        return new IntBuffer(ArrayUtil.toInts(data), copy);
    }

    /**
     * Create a data buffer based on the
     * given pointer, data buffer opType,
     * and length of the buffer
     *
     * @param pointer the pointer to use
     * @param type    the opType of buffer
     * @param length  the length of the buffer
     * @param indexer the indexer for the pointer
     * @return the data buffer
     * backed by this pointer with the given
     * opType and length.
     */
    @Override
    public DataBuffer create(Pointer pointer, DataType type, long length, @NonNull Indexer indexer) {
        switch (type) {
            case BOOL:
                return new BoolBuffer(pointer, indexer, length);
            case BYTE:
                return new Int8Buffer(pointer, indexer, length);
            case UBYTE:
                return new UInt8Buffer(pointer, indexer, length);
            case UINT16:
                return new UInt16Buffer(pointer, indexer, length);
            case UINT32:
                return new UInt32Buffer(pointer, indexer, length);
            case UINT64:
                return new UInt64Buffer(pointer, indexer, length);
            case SHORT:
                return new Int16Buffer(pointer, indexer, length);
            case INT:
                return new IntBuffer(pointer, indexer, length);
            case LONG:
                return new LongBuffer(pointer, indexer, length);
            case HALF:
                return new HalfBuffer(pointer, indexer, length);
            case BFLOAT16:
                return new BFloat16Buffer(pointer, indexer, length);
            case FLOAT:
                return new FloatBuffer(pointer, indexer, length);
            case DOUBLE:
                return new DoubleBuffer(pointer, indexer, length);
        }
        throw new IllegalArgumentException("Invalid opType " + type);
    }

    @Override
    public DataBuffer create(Pointer pointer, Pointer specialPointer, DataType type, long length, @NonNull Indexer indexer) {
        return create(pointer, type, length, indexer);
    }

    /**
     * @param doublePointer
     * @param length
     * @return
     */
    @Override
    public DataBuffer create(DoublePointer doublePointer, long length) {
        doublePointer.capacity(length);
        doublePointer.limit(length);
        doublePointer.position(0);
        return new DoubleBuffer(doublePointer, DoubleIndexer.create(doublePointer), length);
    }

    /**
     * @param intPointer
     * @param length
     * @return
     */
    @Override
    public DataBuffer create(IntPointer intPointer, long length) {
        intPointer.capacity(length);
        intPointer.limit(length);
        intPointer.position(0);
        return new IntBuffer(intPointer, IntIndexer.create(intPointer), length);
    }

    /**
     * @param floatPointer
     * @param length
     * @return
     */
    @Override
    public DataBuffer create(FloatPointer floatPointer, long length) {
        floatPointer.capacity(length);
        floatPointer.limit(length);
        floatPointer.position(0);
        return new FloatBuffer(floatPointer, FloatIndexer.create(floatPointer), length);
    }


    @Override
    public DataBuffer createHalf(long length) {
        return new HalfBuffer(length);
    }

    @Override
    public DataBuffer createHalf(long length, boolean initialize) {
        return new HalfBuffer(length, initialize);
    }

    /**
     * Creates a half-precision data buffer
     *
     * @param data the data to create the buffer from
     * @param copy
     * @return the new buffer
     */
    @Override
    public DataBuffer createHalf(float[] data, boolean copy) {
        throw new UnsupportedOperationException("FP16 isn't supported for CPU yet");
    }

    /**
     * Creates a half-precision data buffer
     *
     * @param data the data to create the buffer from
     * @param copy
     * @return the new buffer
     */
    @Override
    public DataBuffer createHalf(double[] data, boolean copy) {
        throw new UnsupportedOperationException("FP16 isn't supported for CPU yet");
    }

    /**
     * Creates a half-precision data buffer
     *
     * @param offset
     * @param data   the data to create the buffer from
     * @param copy
     * @return the new buffer
     */
    @Override
    public DataBuffer createHalf(long offset, double[] data, boolean copy) {
        throw new UnsupportedOperationException("FP16 isn't supported for CPU yet");
    }

    /**
     * Creates a half-precision data buffer
     *
     * @param offset
     * @param data   the data to create the buffer from
     * @param copy
     * @return the new buffer
     */
    @Override
    public DataBuffer createHalf(long offset, float[] data, boolean copy) {
        throw new UnsupportedOperationException("FP16 isn't supported for CPU yet");
    }

    /**
     * Creates a half-precision data buffer
     *
     * @param offset
     * @param data   the data to create the buffer from
     * @param copy
     * @return the new buffer
     */
    @Override
    public DataBuffer createHalf(long offset, int[] data, boolean copy) {
        throw new UnsupportedOperationException("FP16 isn't supported for CPU yet");
    }

    /**
     * Creates a half-precision data buffer
     *
     * @param offset
     * @param data   the data to create the buffer from
     * @return the new buffer
     */
    @Override
    public DataBuffer createHalf(long offset, double[] data) {
        throw new UnsupportedOperationException("FP16 isn't supported for CPU yet");
    }

    /**
     * Creates a half-precision data buffer
     *
     * @param offset
     * @param data   the data to create the buffer from
     * @return the new buffer
     */
    @Override
    public DataBuffer createHalf(long offset, float[] data) {
        throw new UnsupportedOperationException("FP16 isn't supported for CPU yet");
    }

    @Override
    public DataBuffer createHalf(long offset, float[] data, MemoryWorkspace workspace) {
        throw new UnsupportedOperationException("FP16 isn't supported for CPU yet");
    }

    /**
     * Creates a half-precision data buffer
     *
     * @param offset
     * @param data   the data to create the buffer from
     * @return the new buffer
     */
    @Override
    public DataBuffer createHalf(long offset, int[] data) {
        throw new UnsupportedOperationException("FP16 isn't supported for CPU yet");
    }

    /**
     * Creates a half-precision data buffer
     *
     * @param offset
     * @param data   the data to create the buffer from
     * @param copy
     * @return the new buffer
     */
    @Override
    public DataBuffer createHalf(long offset, byte[] data, boolean copy) {
        throw new UnsupportedOperationException("FP16 isn't supported for CPU yet");
    }

    /**
     * Creates a half-precision data buffer
     *
     * @param data the data to create the buffer from
     * @param copy
     * @return the new buffer
     */
    @Override
    public DataBuffer createHalf(int[] data, boolean copy) {
        throw new UnsupportedOperationException("FP16 isn't supported for CPU yet");
    }

    /**
     * Creates a half-precision data buffer
     *
     * @param data the data to create the buffer from
     * @return the new buffer
     */
    @Override
    public DataBuffer createHalf(float[] data) {
        throw new UnsupportedOperationException("FP16 isn't supported for CPU yet");
    }

    /**
     * Creates a half-precision data buffer
     *
     * @param data the data to create the buffer from
     * @return the new buffer
     */
    @Override
    public DataBuffer createHalf(double[] data) {
        throw new UnsupportedOperationException("FP16 isn't supported for CPU yet");
    }

    /**
     * Creates a half-precision data buffer
     *
     * @param data the data to create the buffer from
     * @return the new buffer
     */
    @Override
    public DataBuffer createHalf(int[] data) {
        throw new UnsupportedOperationException("FP16 isn't supported for CPU yet");
    }


    /**
     * Creates a half-precision data buffer
     *
     * @param offset
     * @param length
     * @return the new buffer
     */
    @Override
    public DataBuffer createHalf(long offset, int length) {
        throw new UnsupportedOperationException("FP16 isn't supported for CPU yet");
    }

    @Override
    public DataBuffer createHalf(long length, boolean initialize, MemoryWorkspace workspace) {
        throw new UnsupportedOperationException("FP16 isn't supported for CPU yet");
    }

    @Override
    public DataBuffer createHalf(float[] data, MemoryWorkspace workspace) {
        throw new UnsupportedOperationException("FP16 isn't supported for CPU yet");
    }

    @Override
    public DataBuffer createHalf(float[] data, boolean copy, MemoryWorkspace workspace) {
        throw new UnsupportedOperationException("FP16 isn't supported for CPU yet");
    }

    @Override
    public Class<? extends DataBuffer> intBufferClass() {
        return IntBuffer.class;
    }

    @Override
    public Class<? extends DataBuffer> longBufferClass() {
        return LongBuffer.class;
    }

    @Override
    public Class<? extends DataBuffer> halfBufferClass() {
        return HalfBuffer.class;    //Not yet supported
    }

    @Override
    public Class<? extends DataBuffer> floatBufferClass() {
        return FloatBuffer.class;
    }

    @Override
    public Class<? extends DataBuffer> doubleBufferClass() {
        return DoubleBuffer.class;
    }

    @Override
    public DataBuffer createUtf8Buffer(byte[] data, long product) {
        return new Utf8Buffer(data, product);
    }
}

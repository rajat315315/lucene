/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.lucene.internal.vectorization;

import static jdk.incubator.vector.VectorOperators.ZERO_EXTEND_B2L;
import static jdk.incubator.vector.VectorOperators.ZERO_EXTEND_I2L;
import static jdk.incubator.vector.VectorOperators.ZERO_EXTEND_S2L;

import java.nio.ByteOrder;
import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.LongVector;
import jdk.incubator.vector.ShortVector;
import jdk.incubator.vector.VectorSpecies;

/** Panama Vector API implementation of {@link DocValuesBulkDecodeSupport}. */
final class PanamaDocValuesBulkDecodeSupport implements DocValuesBulkDecodeSupport {

  static final PanamaDocValuesBulkDecodeSupport INSTANCE = new PanamaDocValuesBulkDecodeSupport();

  private static final VectorSpecies<Long> LONG_SPECIES = LongVector.SPECIES_PREFERRED;
  private static final VectorSpecies<Byte> BYTE_SPECIES = ByteVector.SPECIES_PREFERRED;

  private PanamaDocValuesBulkDecodeSupport() {}

  @Override
  public void decodeByteAligned(
      byte[] bytes, int bytesOffset, int bitsPerValue, long[] values, int valuesOffset, int count) {
    if (ByteOrder.nativeOrder() != ByteOrder.LITTLE_ENDIAN
        || BYTE_SPECIES.vectorByteSize() < 32) {
      DefaultDocValuesBulkDecodeSupport.INSTANCE.decodeByteAligned(
          bytes, bytesOffset, bitsPerValue, values, valuesOffset, count);
      return;
    }

    switch (bitsPerValue) {
      case Byte.SIZE -> decode8(bytes, bytesOffset, values, valuesOffset, count);
      case Short.SIZE -> decode16(bytes, bytesOffset, values, valuesOffset, count);
      case Integer.SIZE -> decode32(bytes, bytesOffset, values, valuesOffset, count);
      case Long.SIZE -> decode64(bytes, bytesOffset, values, valuesOffset, count);
      default -> DefaultDocValuesBulkDecodeSupport.INSTANCE.decodeByteAligned(
          bytes, bytesOffset, bitsPerValue, values, valuesOffset, count);
    }
  }

  private static void decode8(
      byte[] bytes, int bytesOffset, long[] values, int valuesOffset, int count) {
    final int L = LONG_SPECIES.length();
    final int bytesLimit = bytes.length - BYTE_SPECIES.length();
    final int loopBound = Math.min(count - count % L, bytesLimit - bytesOffset);
    int i = 0;
    if (loopBound > 0) {
      for (; i < loopBound; i += L) {
        ByteVector bv = ByteVector.fromArray(BYTE_SPECIES, bytes, bytesOffset + i);
        LongVector lv = (LongVector) bv.convertShape(ZERO_EXTEND_B2L, LONG_SPECIES, 0);
        lv.intoArray(values, valuesOffset + i);
      }
    }
    if (i < count) {
      DefaultDocValuesBulkDecodeSupport.INSTANCE.decodeByteAligned(
          bytes, bytesOffset + i, Byte.SIZE, values, valuesOffset + i, count - i);
    }
  }

  private static void decode16(
      byte[] bytes, int bytesOffset, long[] values, int valuesOffset, int count) {
    final int L = LONG_SPECIES.length();
    final int bytesLimit = bytes.length - BYTE_SPECIES.length();
    final int loopBound = Math.min(count - count % L, (bytesLimit - bytesOffset) / 2);
    int i = 0;
    if (loopBound > 0) {
      for (; i < loopBound; i += L) {
        ByteVector bv = ByteVector.fromArray(BYTE_SPECIES, bytes, bytesOffset + i * 2);
        ShortVector sv = bv.reinterpretAsShorts();
        LongVector lv = (LongVector) sv.convertShape(ZERO_EXTEND_S2L, LONG_SPECIES, 0);
        lv.intoArray(values, valuesOffset + i);
      }
    }
    if (i < count) {
      DefaultDocValuesBulkDecodeSupport.INSTANCE.decodeByteAligned(
          bytes, bytesOffset + i * 2, Short.SIZE, values, valuesOffset + i, count - i);
    }
  }

  private static void decode32(
      byte[] bytes, int bytesOffset, long[] values, int valuesOffset, int count) {
    final int L = LONG_SPECIES.length();
    final int bytesLimit = bytes.length - BYTE_SPECIES.length();
    final int loopBound = Math.min(count - count % L, (bytesLimit - bytesOffset) / 4);
    int i = 0;
    if (loopBound > 0) {
      for (; i < loopBound; i += L) {
        ByteVector bv = ByteVector.fromArray(BYTE_SPECIES, bytes, bytesOffset + i * 4);
        IntVector iv = bv.reinterpretAsInts();
        LongVector lv = (LongVector) iv.convertShape(ZERO_EXTEND_I2L, LONG_SPECIES, 0);
        lv.intoArray(values, valuesOffset + i);
      }
    }
    if (i < count) {
      DefaultDocValuesBulkDecodeSupport.INSTANCE.decodeByteAligned(
          bytes, bytesOffset + i * 4, Integer.SIZE, values, valuesOffset + i, count - i);
    }
  }

  private static void decode64(
      byte[] bytes, int bytesOffset, long[] values, int valuesOffset, int count) {
    final int valuesPerVector = BYTE_SPECIES.vectorByteSize() / Long.BYTES;
    final int loopBound = count - count % valuesPerVector;
    int i = 0;
    for (; i < loopBound; i += valuesPerVector) {
      ByteVector.fromArray(BYTE_SPECIES, bytes, bytesOffset + i * Long.BYTES)
          .reinterpretAsLongs()
          .intoArray(values, valuesOffset + i);
    }
    if (i < count) {
      DefaultDocValuesBulkDecodeSupport.INSTANCE.decodeByteAligned(
          bytes, bytesOffset + i * Long.BYTES, Long.SIZE, values, valuesOffset + i, count - i);
    }
  }
}

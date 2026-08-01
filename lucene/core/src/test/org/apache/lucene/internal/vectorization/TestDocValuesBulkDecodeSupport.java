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

import org.apache.lucene.tests.util.LuceneTestCase;
import org.apache.lucene.tests.util.TestUtil;

public class TestDocValuesBulkDecodeSupport extends LuceneTestCase {

  public void testDuelDecodeByteAligned() throws Exception {
    VectorizationProvider vectorizationProvider = VectorizationProvider.lookup(true);
    DocValuesBulkDecodeSupport optimizedSupport = vectorizationProvider.getDocValuesBulkDecodeSupport();
    DocValuesBulkDecodeSupport defaultSupport = DefaultDocValuesBulkDecodeSupport.INSTANCE;

    final int iterations = atLeast(100);
    for (int iter = 0; iter < iterations; ++iter) {
      int bitsPerValue = random().nextBoolean() ? (random().nextBoolean() ? 8 : 16) : (random().nextBoolean() ? 32 : 64);
      int bytesPerValue = bitsPerValue / 8;
      int count = TestUtil.nextInt(random(), 1, 1000);
      int bytesOffset = random().nextInt(16);
      int valuesOffset = random().nextInt(16);

      byte[] bytes = new byte[bytesOffset + count * bytesPerValue + random().nextInt(16)];
      random().nextBytes(bytes);

      long[] expectedValues = new long[valuesOffset + count + random().nextInt(16)];
      long[] actualValues = new long[expectedValues.length];

      for (int i = 0; i < expectedValues.length; i++) {
        long val = random().nextLong();
        expectedValues[i] = val;
        actualValues[i] = val;
      }

      defaultSupport.decodeByteAligned(bytes, bytesOffset, bitsPerValue, expectedValues, valuesOffset, count);
      optimizedSupport.decodeByteAligned(bytes, bytesOffset, bitsPerValue, actualValues, valuesOffset, count);

      assertArrayEquals("bitsPerValue=" + bitsPerValue + " count=" + count, expectedValues, actualValues);
    }
  }
}

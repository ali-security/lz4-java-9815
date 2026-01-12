package net.jpountz.lz4;

/*
 * Copyright 2025 Jonas Konrad and the lz4-java contributors.
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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import java.util.stream.IntStream;
import java.io.ByteArrayOutputStream;

import java.util.Arrays;
import java.util.Collection;

import static net.jpountz.lz4.LZ4Constants.MIN_MATCH;
import java.util.stream.Stream;
import static org.junit.Assert.assertArrayEquals;

import java.util.ArrayList;
import java.util.List;

// Simple interface to unify fast and safe decompressors
interface FallibleDecompressor {
    void decompress(byte[] src, byte[] dest);
}

@RunWith(Parameterized.class)
public class OutOfBoundsTest {
    @Parameterized.Parameters
    public static Collection<Object[]> parameters() {
        return Arrays.asList(
                new Object[]{LZ4Factory.fastestInstance().fastDecompressor()},
                new Object[]{LZ4Factory.fastestJavaInstance().fastDecompressor()},
                new Object[]{LZ4Factory.nativeInstance().fastDecompressor()},
                new Object[]{LZ4Factory.safeInstance().fastDecompressor()},
                new Object[]{LZ4Factory.unsafeInstance().fastDecompressor()}
        );
    }

    @Parameterized.Parameter
    public LZ4FastDecompressor fastDecompressor;

    @Test
    public void test() {
        byte[] output = new byte[2055];
        for (int i = 0; i < 1000000; i++) {
            try {
                fastDecompressor.decompress(new byte[]{
                        (byte) 0xf0,
                        -1, -1, -1, -1, -1, -1, -1, -1, 0
                }, output);
            } catch (ArrayIndexOutOfBoundsException ignored) {
            }
        }
    }
    static Stream<FallibleDecompressor> allDecompressors() {
        return Stream.of(
            (FallibleDecompressor) LZ4Factory.safeInstance().fastDecompressor()::decompress,
            (FallibleDecompressor) LZ4Factory.safeInstance().safeDecompressor()::decompress,
            (FallibleDecompressor) LZ4Factory.unsafeInstance().fastDecompressor()::decompress,
            (FallibleDecompressor) LZ4Factory.unsafeInstance().safeDecompressor()::decompress
        );
    }

    @Test
    public void copyBeyondOutput() {
        FallibleDecompressor[] decompressors = new FallibleDecompressor[]{
            LZ4Factory.safeInstance().fastDecompressor()::decompress,
            LZ4Factory.safeInstance().safeDecompressor()::decompress,
            LZ4Factory.unsafeInstance().fastDecompressor()::decompress,
            LZ4Factory.unsafeInstance().safeDecompressor()::decompress
        };

        for (FallibleDecompressor decompressor : decompressors) {
            for (int dec = 0; dec < 14; dec++) {
                for (int len = dec; len < 14; len++) {

                    byte[] compressed = {
                        (byte) 0xe0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        (byte) len, (byte) dec, 0,
                        (byte) 0xc0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
                    };

                    byte[] output = new byte[14 + MIN_MATCH + len + MIN_MATCH + 12];
                    Arrays.fill(output, (byte) 0x77);

                    decompressor.decompress(compressed, output);

                    assertArrayEquals(
                        "dec=" + dec + " len=" + len + " decompressor=" + decompressor,
                        new byte[output.length],
                        output);
                }
            }
        }
    }
} 


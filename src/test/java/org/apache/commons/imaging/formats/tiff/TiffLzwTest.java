/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.imaging.formats.tiff;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.imaging.common.ByteOrder;
import org.apache.commons.imaging.common.mylzw.MyLzwCompressor;
import org.apache.commons.imaging.common.mylzw.MyLzwDecompressor;
import org.apache.commons.imaging.util.Debug;

public class TiffLzwTest extends TiffBaseTest {

    public void testTrivial() throws Exception {
        final byte bytes[] = { 0, };
        compressRoundtripAndValidate(bytes);
    }

    public void testMedium() throws Exception {
        final int LENGTH = 1024 * 32;
        final byte bytes[] = new byte[LENGTH];
        for (int modulator = 1; modulator < 255; modulator += 3) {
            for (int i = 0; i < LENGTH; i++) {
                bytes[i] = (byte) (0xff & (i % modulator));
            }

            compressRoundtripAndValidate(bytes);
        }
    }

    // public void testTiffImageData() throws IOException, ImageReadException,
    // ImageWriteException
    // {
    // List images = getTiffImages();
    // for (int i = 0; i < images.size(); i++)
    // {
    // if (i % 10 == 0)
    // Debug.purgeMemory();
    //
    // File imageFile = (File) images.get(i);
    // Debug.debug("imageFile", imageFile);
    //
    // ByteSource byteSource = new ByteSourceFile(imageFile);
    // Map params = new HashMap();
    // List data = new TiffImageParser().collectRawImageData(byteSource,
    // params);
    //
    // for (int j = 0; j < data.size(); j++)
    // {
    // byte bytes[] = (byte[]) data.get(j);
    // decompressRoundtripAndValidate(bytes);
    // }
    // }
    // }

    private void compressRoundtripAndValidate(final byte src[]) throws IOException {
        final boolean DEBUG = false;

        if (DEBUG) {
            Debug.debug();
            Debug.debug("roundtripAndValidate: " + src.length);
            Debug.debug();
        }

        final int LZW_MINIMUM_CODE_SIZE = 8;
        final List<Integer> codes = new ArrayList<Integer>();
        final MyLzwCompressor.Listener compressionListener = new MyLzwCompressor.Listener() {
            public void dataCode(final int code) {
                codes.add(code);
            }

            public void eoiCode(final int code) {
                codes.add(code);
            }

            public void clearCode(final int code) {
                codes.add(code);
            }

            public void init(final int clearCode, final int eoiCode) {
            }
        };

        final MyLzwCompressor compressor = new MyLzwCompressor(LZW_MINIMUM_CODE_SIZE,
                ByteOrder.MOTOROLA, true, compressionListener);
        final byte compressed[] = compressor.compress(src);

        final MyLzwDecompressor.Listener decompressionListener = new MyLzwDecompressor.Listener() {

            int index = 0;
            int clearCode, eoiCode;

            public void code(final int code) {
                if (DEBUG) {
                    if (code == clearCode) {
                        Debug.debug("clearCode: " + index + "/" + codes.size());
                        Debug.debug();
                    }
                    if (code == eoiCode) {
                        Debug.debug("eoiCode: " + index + "/" + codes.size());
                        Debug.debug();
                    }
                }
                final Integer expectedCode = codes.get(index++);
                if (code != expectedCode) {
                    Debug.debug("bad code: " + index + "/" + codes.size());
                    Debug.debug("code: " + code + " (0x"
                            + Integer.toHexString(code) + ") "
                            + Integer.toBinaryString(code));
                    Debug.debug("expected: " + expectedCode + " (0x"
                            + Integer.toHexString(expectedCode)
                            + ") "
                            + Integer.toBinaryString(expectedCode));
                    Debug.debug("clearCode: " + clearCode + " (0x"
                            + Integer.toHexString(clearCode) + ") "
                            + Integer.toBinaryString(clearCode));
                    Debug.debug("eoiCode: " + eoiCode + " (0x"
                            + Integer.toHexString(eoiCode) + ") "
                            + Integer.toBinaryString(eoiCode));
                    Debug.debug();
                }
            }

            public void init(final int clearCode, final int eoiCode) {
                this.clearCode = clearCode;
                this.eoiCode = eoiCode;
            }

        };
        final InputStream is = new ByteArrayInputStream(compressed);
        final MyLzwDecompressor decompressor = new MyLzwDecompressor(
                LZW_MINIMUM_CODE_SIZE, ByteOrder.NETWORK,
                decompressionListener);
        decompressor.setTiffLZWMode();
        final byte decompressed[] = decompressor.decompress(is, src.length);

        assertEquals(src.length, decompressed.length);
        for (int i = 0; i < src.length; i++) {
            assertEquals(src[i], decompressed[i]);
        }
    }

    private void decompressRoundtripAndValidate(final byte src[]) throws IOException {
        Debug.debug();
        Debug.debug("roundtripAndValidate: " + src.length);
        Debug.debug();

        final int LZW_MINIMUM_CODE_SIZE = 8;
        final List<Integer> codes = new ArrayList<Integer>();

        final MyLzwDecompressor.Listener decompressionListener = new MyLzwDecompressor.Listener() {

            public void code(final int code) {
                Debug.debug("listener code: " + code + " (0x"
                        + Integer.toHexString(code) + ") "
                        + Integer.toBinaryString(code) + ", index: "
                        + codes.size());
                codes.add(code);
            }

            public void init(final int clearCode, final int eoiCode) {
            }

        };
        final InputStream is = new ByteArrayInputStream(src);
        final MyLzwDecompressor decompressor = new MyLzwDecompressor(
                LZW_MINIMUM_CODE_SIZE, ByteOrder.NETWORK,
                decompressionListener);
        decompressor.setTiffLZWMode();
        final byte decompressed[] = decompressor.decompress(is, src.length);

        final MyLzwCompressor.Listener compressionListener = new MyLzwCompressor.Listener() {

            int clearCode, eoiCode;

            public void init(final int clearCode, final int eoiCode) {
                this.clearCode = clearCode;
                this.eoiCode = eoiCode;
            }

            int index = 0;

            private void code(final int code) {

                if (code == clearCode) {
                    Debug.debug("clearCode: " + index + "/" + codes.size());
                    Debug.debug();
                }
                if (code == eoiCode) {
                    Debug.debug("eoiCode: " + index + "/" + codes.size());
                    Debug.debug();
                }
                final Integer expectedCode = codes.get(index++);
                if (code != expectedCode) {
                    Debug.debug("bad code: " + index + "/" + codes.size());
                    Debug.debug("code: " + code + " (0x"
                            + Integer.toHexString(code) + ") "
                            + Integer.toBinaryString(code));
                    Debug.debug("expected: " + expectedCode + " (0x"
                            + Integer.toHexString(expectedCode)
                            + ") "
                            + Integer.toBinaryString(expectedCode));
                    Debug.debug("clearCode: " + clearCode + " (0x"
                            + Integer.toHexString(clearCode) + ") "
                            + Integer.toBinaryString(clearCode));
                    Debug.debug("eoiCode: " + eoiCode + " (0x"
                            + Integer.toHexString(eoiCode) + ") "
                            + Integer.toBinaryString(eoiCode));
                    Debug.debug();
                }
            }

            public void dataCode(final int code) {
                code(code);
            }

            public void eoiCode(final int code) {
                code(code);
            }

            public void clearCode(final int code) {
                code(code);
            }

        };

        final MyLzwCompressor compressor = new MyLzwCompressor(LZW_MINIMUM_CODE_SIZE,
                ByteOrder.MOTOROLA, true, compressionListener);
        final byte compressed[] = compressor.compress(decompressed);

        assertEquals(src.length, compressed.length);
        for (int i = 0; i < src.length; i++) {
            assertEquals(src[i], compressed[i]);
        }
    }

}

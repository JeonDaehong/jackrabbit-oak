/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.jackrabbit.oak.index.indexer.document.flatfile.pipelined;

import java.util.ArrayList;
import java.util.function.Function;

import static org.apache.jackrabbit.oak.index.indexer.document.flatfile.pipelined.PipelinedStrategy.FLATFILESTORE_CHARSET;

final class NodeStateHolderFactory implements Function<byte[], NodeStateHolder> {
    private final static byte PIPE = (byte) '|';
    private final static byte PATH_SEPARATOR = (byte) '/';

    // In UTF-8, the ASCII characters have similar encoding as in ASCII, so we can search for them without decoding the
    // stream. And characters encoded as multibyte will not have any of their bytes matching an ASCII character, because
    // the bytes in a multibyte encoding all start with 1, while ASCII characters start with 0.
    // https://en.wikipedia.org/wiki/UTF-8#Encoding
    private static int indexOf(byte[] ffsLine, byte ch, int from, int to) {
        for (int i = from; i < to; i++) {
            if (ffsLine[i] == ch) {
                return i;
            }
        }
        return -1;
    }


    private static int indexOf(byte[] ffsLine, byte ch) {
        return indexOf(ffsLine, ch, 0, ffsLine.length);
    }

    private static boolean isAbsolutePath(String path) {
        return !path.isEmpty() && path.charAt(0) == '/';
    }

    private final ArrayList<String> partsBuffer = new ArrayList<>(16);

    public NodeStateHolder apply(byte[] ffsLine) {
        return ffsLine == null ? null : new NodeStateHolder(ffsLine, parts(ffsLine));
    }

    // We are only interested in the path section of the line, we do not need to convert to String the full line. So
    // we search for the pipe and then convert only this section a String.
    private String[] parts(byte[] ffsLine) {
        partsBuffer.clear();
        int pipeIdx = indexOf(ffsLine, PIPE);
        if (pipeIdx < 0) {
            throw new IllegalStateException("Line does not contain a pipe: " + new String(ffsLine, FLATFILESTORE_CHARSET));
        }
        String path = new String(ffsLine, 0, pipeIdx, FLATFILESTORE_CHARSET);

        int pos = isAbsolutePath(path) ? 1 : 0;
        while (true) {
            if (pos >= path.length()) {
                return partsBuffer.toArray(new String[0]);
            }
            int i = path.indexOf(PATH_SEPARATOR, pos);
            if (i < 0) {
                // Add last part and exit
                partsBuffer.add(path.substring(pos));
                pos = path.length();
            } else {
                partsBuffer.add(path.substring(pos, i));
                pos = i + 1;
            }
        }
    }
}

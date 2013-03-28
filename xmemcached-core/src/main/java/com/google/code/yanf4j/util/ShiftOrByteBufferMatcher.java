/**
 *Copyright [2009-2010] [dennis zhuang(killme2008@gmail.com)]
 *Licensed under the Apache License, Version 2.0 (the "License");
 *you may not use this file except in compliance with the License. 
 *You may obtain a copy of the License at 
 *             http://www.apache.org/licenses/LICENSE-2.0 
 *Unless required by applicable law or agreed to in writing, 
 *software distributed under the License is distributed on an "AS IS" BASIS, 
 *WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 *either express or implied. See the License for the specific language governing permissions and limitations under the License
 */
/**
 *Copyright [2009-2010] [dennis zhuang(killme2008@gmail.com)]
 *Licensed under the Apache License, Version 2.0 (the "License");
 *you may not use this file except in compliance with the License.
 *You may obtain a copy of the License at
 *             http://www.apache.org/licenses/LICENSE-2.0
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an "AS IS" BASIS,
 *WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 *either express or implied. See the License for the specific language governing permissions and limitations under the License
 */
package com.google.code.yanf4j.util;

import java.util.ArrayList;
import java.util.List;

import com.google.code.yanf4j.buffer.IoBuffer;




/**
 * ByteBuffer matcher based on shift-or algorithm
 * 
 * @author dennis
 * 
 */
public class ShiftOrByteBufferMatcher implements ByteBufferMatcher {

    private int[] b;
    private int lim;

    private int patternLen;


    public ShiftOrByteBufferMatcher(IoBuffer pat) {
        if (pat == null || pat.remaining() == 0) {
            throw new IllegalArgumentException("blank buffer");
        }
        this.patternLen = pat.remaining();
        preprocess(pat);
    }


    /**
     * Ԥ����
     * 
     * @param pat
     */
    private void preprocess(IoBuffer pat) {
        this.b = new int[256];
        this.lim = 0;
        for (int i = 0; i < 256; i++) {
            this.b[i] = ~0;

        }
        for (int i = 0, j = 1; i < this.patternLen; i++, j <<= 1) {
            this.b[ByteBufferUtils.uByte(pat.get(i))] &= ~j;
            this.lim |= j;
        }
        this.lim = ~(this.lim >> 1);

    }


    public final List<Integer> matchAll(IoBuffer buffer) {
        List<Integer> matches = new ArrayList<Integer>();
        int bufferLimit = buffer.limit();
        int state = ~0;
        for (int pos = buffer.position(); pos < bufferLimit; pos++) {
            state <<= 1;
            state |= this.b[ByteBufferUtils.uByte(buffer.get(pos))];
            if (state < this.lim) {
                matches.add(pos - this.patternLen + 1);
            }
        }
        return matches;
    }


    public final int matchFirst(IoBuffer buffer) {
        if (buffer == null) {
            return -1;
        }
        int bufferLimit = buffer.limit();
        int state = ~0;
        for (int pos = buffer.position(); pos < bufferLimit; pos++) {
            state = (state <<= 1) | this.b[ByteBufferUtils.uByte(buffer.get(pos))];
            if (state < this.lim) {
                return pos - this.patternLen + 1;
            }
        }
        return -1;
    }

}

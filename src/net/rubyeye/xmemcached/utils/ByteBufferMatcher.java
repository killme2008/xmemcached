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
package net.rubyeye.xmemcached.utils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * ByteBuffer匹配的BM算法，匹配模式类，进行预处理
 * 
 * @author dennis
 * 
 */
public class ByteBufferMatcher {

    private ByteBuffer pattern;
    private int remaining;
    private final int occ[] = new int[256];
    private int[] f;
    private int[] s;

    public ByteBufferMatcher(ByteBuffer buffer) {
        if (buffer == null || buffer.remaining() == 0) {
            throw new IllegalArgumentException("blank buffer");
        }
        this.pattern = buffer;
        this.remaining = buffer.remaining();
        f = new int[remaining + 1];
        s = new int[remaining + 1];
        bmPreprocess();
    }

    /**
     * 模式预处理
     */
    public final void bmPreprocess() {
        badBytePreProcess();
        goodBytePreProcess1();
        goodBytePreProcess2();
    }

    /**
     * 坏字符预处理
     *
     * @param p
     * @return
     */
    private final void badBytePreProcess() {
        int j;
        for (int i = 0; i < 255; i++) {
            occ[i] = -1;
        }
        for (j = this.pattern.position(); j < this.pattern.limit(); j++) {
            byte b = this.pattern.get(j);
            occ[unsigned(b)] = j;
        }
    }

    /**
     * 好字符预处理情况1
     */
    private final void goodBytePreProcess1() {
        int i = remaining, j = remaining + 1;
        f[i] = j;
        while (i > 0) {
            while (j <= remaining && pattern.get(i - 1) != pattern.get(j - 1)) {
                if (s[j] == 0) {
                    s[j] = j - i;
                }
                j = f[j];
            }
            i--;
            j--;
            f[i] = j;
        }
    }

    /**
     * 好字符预处理情况2
     */
    private final void goodBytePreProcess2() {
        int i, j;
        j = f[0];
        for (i = 0; i <= remaining; i++) {
            if (s[i] == 0) {
                s[i] = j;
            }
            if (i == j) {
                j = f[j];
            }
        }
    }

    public final int matchFirst(ByteBuffer buffer) {
        if (buffer == null || buffer.remaining() == 0) {
            return -1;
        }
        int n = buffer.limit();
        int i = buffer.position(), j;
        while (i <= n - remaining) {
            j = remaining - 1;
            while (j >= 0 && pattern.get(j) == buffer.get(i + j)) {
                j--;
            }
            if (j <= 0) {
                return i;
            // i += s[0]; 继续搜索
            } else {
                i += max(s[j + 1], j - occ[unsigned(buffer.get(i + j))]);
            }
        }
        return -1;
    }

    public final List<Integer> matchAll(final ByteBuffer buffer) {
        if (buffer == null || buffer.remaining() == 0) {
            return null;
        }
        int n = buffer.limit();
        int i = buffer.position(), j;
        List<Integer> result = new ArrayList<Integer>();
        while (i <= n - remaining) {
            j = remaining - 1;
            while (j >= 0 && pattern.get(j) == buffer.get(i + j)) {
                j--;
            }
            if (j <= 0) {
                result.add(i);
                i += s[0];
            } else {
                i += max(s[j + 1], j - occ[unsigned(buffer.get(i + j))]);
            }
        }
        return result;
    }

    private static final int max(int i, int j) {
        // if (i > 0 && j < 0)
        // return i;
        // else if (i < 0 && j > 0)
        // return j;
        //		else
        return i > j ? i : j;
    }

    public static final int unsigned(byte b) {
        return b & 0xFF;
    }
}

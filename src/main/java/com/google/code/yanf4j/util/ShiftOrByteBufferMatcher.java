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

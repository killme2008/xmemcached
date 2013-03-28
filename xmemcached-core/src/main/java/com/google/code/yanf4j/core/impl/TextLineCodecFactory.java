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
package com.google.code.yanf4j.core.impl;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

import com.google.code.yanf4j.buffer.IoBuffer;
import com.google.code.yanf4j.core.CodecFactory;
import com.google.code.yanf4j.core.Session;
import com.google.code.yanf4j.util.ByteBufferMatcher;
import com.google.code.yanf4j.util.ShiftAndByteBufferMatcher;




/**
 * Text line codec factory
 * @author dennis
 *
 */
public class TextLineCodecFactory implements CodecFactory {

    public static final IoBuffer SPLIT = IoBuffer.wrap("\r\n".getBytes());

    private static final ByteBufferMatcher SPLIT_PATTERN = new ShiftAndByteBufferMatcher(SPLIT);

    public static final String DEFAULT_CHARSET_NAME = "utf-8";

    private Charset charset;


    public TextLineCodecFactory() {
        this.charset = Charset.forName(DEFAULT_CHARSET_NAME);
    }


    public TextLineCodecFactory(String charsetName) {
        this.charset = Charset.forName(charsetName);
    }

    class StringDecoder implements Decoder {
        public Object decode(IoBuffer buffer, Session session) {
            String result = null;
            int index = SPLIT_PATTERN.matchFirst(buffer);
            if (index >= 0) {
                int limit = buffer.limit();
                buffer.limit(index);
                CharBuffer charBuffer = TextLineCodecFactory.this.charset.decode(buffer.buf());
                result = charBuffer.toString();
                buffer.limit(limit);
                buffer.position(index + SPLIT.remaining());

            }
            return result;
        }
    }

    private Decoder decoder = new StringDecoder();


    public Decoder getDecoder() {
        return this.decoder;

    }

    class StringEncoder implements Encoder {
        public IoBuffer encode(Object msg, Session session) {
            if (msg == null) {
                return null;
            }
            String message = (String) msg;
            ByteBuffer buff = TextLineCodecFactory.this.charset.encode(message);
            IoBuffer resultBuffer = IoBuffer.allocate(buff.remaining() + SPLIT.remaining());
            resultBuffer.put(buff);
            resultBuffer.put(SPLIT.slice());
            resultBuffer.flip();
            return resultBuffer;
        }
    }

    private Encoder encoder = new StringEncoder();


    public Encoder getEncoder() {
        return this.encoder;
    }

}

package com.google.code.yanf4j.nio;

/**
 *Copyright [2008-2009] [dennis zhuang]
 *Licensed under the Apache License, Version 2.0 (the "License");
 *you may not use this file except in compliance with the License. 
 *You may obtain a copy of the License at 
 *             http://www.apache.org/licenses/LICENSE-2.0 
 *Unless required by applicable law or agreed to in writing, 
 *software distributed under the License is distributed on an "AS IS" BASIS, 
 *WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 *either express or implied. See the License for the specific language governing permissions and limitations under the License
 */
import java.io.IOException;

import com.google.code.yanf4j.config.Configuration;
import com.google.code.yanf4j.core.CodecFactory;
import com.google.code.yanf4j.core.Handler;
import com.google.code.yanf4j.core.ServerController;
import com.google.code.yanf4j.nio.impl.DatagramChannelController;




/**
 * Controller for udp server
 * 
 * @author dennis
 * 
 */
public class UDPController extends DatagramChannelController implements ServerController {

    public UDPController(Configuration configuration) {
        super(configuration, null, null);
        setMaxDatagramPacketLength(configuration.getSessionReadBufferSize() > 9216 ? 4096 : configuration
            .getSessionReadBufferSize());
    }


    public UDPController() {
        super();
    }


    public UDPController(Configuration configuration, CodecFactory codecFactory) {
        super(configuration, null, codecFactory);
        setMaxDatagramPacketLength(configuration.getSessionReadBufferSize() > 9216 ? 4096 : configuration
            .getSessionReadBufferSize());
    }


    public void unbind() throws IOException {
        stop();
    }


    public UDPController(Configuration configuration, Handler handler, CodecFactory codecFactory) {
        super(configuration, handler, codecFactory);
        setMaxDatagramPacketLength(configuration.getSessionReadBufferSize() > 9216 ? 4096 : configuration
            .getSessionReadBufferSize());
    }
}

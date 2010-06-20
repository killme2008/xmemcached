package com.google.code.yanf4j.test.unittest.nio;

import com.google.code.yanf4j.config.Configuration;
import com.google.code.yanf4j.core.impl.HandlerAdapter;
import com.google.code.yanf4j.nio.UDPController;
import com.google.code.yanf4j.test.unittest.nio.impl.NioServerControllerUnitTest;


/**
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-25 ионГ11:21:45
 */

public class UDPControllerUnitTest extends NioServerControllerUnitTest {

    @Override
    public void newServer() {
        this.controller = new UDPController(new Configuration());
        this.controller.setHandler(new HandlerAdapter());
    }

}

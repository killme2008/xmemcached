package com.google.code.yanf4j.test.unittest.nio;

import com.google.code.yanf4j.config.Configuration;
import com.google.code.yanf4j.core.impl.HandlerAdapter;
import com.google.code.yanf4j.nio.TCPController;
import com.google.code.yanf4j.test.unittest.nio.impl.NioServerControllerUnitTest;


/**
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-24 ÏÂÎç03:56:48
 */

public class TCPControllerUnitTest extends NioServerControllerUnitTest {

    @Override
    public void newServer() {
        this.controller = new TCPController(new Configuration());
        this.controller.setHandler(new HandlerAdapter());
    }


    // TODO
    public void testFunction() {

    }

}

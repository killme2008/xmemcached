package com.google.code.yanf4j.test.unittest.nio.impl;

import java.net.InetSocketAddress;

import org.junit.Assert;
import org.junit.Test;

import com.google.code.yanf4j.core.ServerController;
import com.google.code.yanf4j.nio.impl.NioController;
import com.google.code.yanf4j.test.unittest.core.impl.AbstractControllerUnitTest;


/**
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-25 ÉÏÎç11:09:06
 */

public abstract class NioServerControllerUnitTest extends AbstractControllerUnitTest {

    public abstract void newServer();


    @Test
    public void testNioControllerConfig() throws Exception {
        newServer();
        ((NioController) controller).setSelectorPoolSize(5);
        Assert.assertEquals(5, ((NioController) controller).getSelectorPoolSize());

        ((ServerController) controller).bind(8080);
        Assert.assertEquals(5, ((NioController) controller).getSelectorManager().getSelectorCount());
    }


    @Test
    public void testBindUnBind() throws Exception {
        newServer();
        try {
            controller.bind(null);
            Assert.fail();
        }
        catch (IllegalArgumentException e) {
            Assert.assertEquals("Null inetSocketAddress", e.getMessage());
        }
        ((ServerController) controller).bind(8080);
        InetSocketAddress localAddress = controller.getLocalSocketAddress();
        Assert.assertEquals(8080, localAddress.getPort());
        Assert.assertTrue(controller.isStarted());

        ((ServerController) controller).unbind();
        Assert.assertFalse(controller.isStarted());
    }


    @Test
    public void testBindDuplicated() throws Exception {
        newServer();
        ((ServerController) controller).bind(8080);
        try {
            ((ServerController) controller).bind(8080);
            Assert.fail();
        }
        catch (IllegalStateException e) {
            Assert.assertTrue(true);
        }
    }

}

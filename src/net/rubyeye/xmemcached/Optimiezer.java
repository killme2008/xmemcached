/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package net.rubyeye.xmemcached;

import com.google.code.yanf4j.util.Queue;
import java.util.concurrent.BlockingQueue;
import net.rubyeye.xmemcached.command.Command;

/**
 *
 * @author dennis
 */
public interface Optimiezer {

    Command optimieze(final Command currentCommand, final Queue writeQueue, final BlockingQueue<Command> executingCmds, int sendBufferSize);

    void setMergeFactor(int mergeFactor);

    void setOptimiezeGet(boolean optimiezeGet);

    void setOptimiezeMergeBuffer(boolean optimiezeMergeBuffer);

}

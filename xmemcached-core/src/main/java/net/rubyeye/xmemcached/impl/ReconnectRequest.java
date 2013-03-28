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
package net.rubyeye.xmemcached.impl;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import net.rubyeye.xmemcached.utils.InetSocketAddressWrapper;


/**
 * A auto reconnect request,associating a socket address for reconnecting
 * 
 * @author dennis
 * 
 */
public final class ReconnectRequest implements Delayed {

    private InetSocketAddressWrapper inetSocketAddressWrapper;
    private int tries;

    private static final long MIN_RECONNECT_INTERVAL = 1000;

    private static final long MAX_RECONNECT_INTERVAL = 60 * 1000;

    private volatile long nextReconnectTimestamp;


    public ReconnectRequest(InetSocketAddressWrapper inetSocketAddressWrapper, int tries, long reconnectInterval) {
        super();
        this.setInetSocketAddressWrapper(inetSocketAddressWrapper);
        this.setTries(tries); // record reconnect times
        reconnectInterval = this.normalInterval(reconnectInterval);
        this.nextReconnectTimestamp = System.currentTimeMillis() + reconnectInterval;
    }


    private long normalInterval(long reconnectInterval) {
        if (reconnectInterval < MIN_RECONNECT_INTERVAL) {
            reconnectInterval = MIN_RECONNECT_INTERVAL;
        }
        if (reconnectInterval > MAX_RECONNECT_INTERVAL) {
            reconnectInterval = MAX_RECONNECT_INTERVAL;
        }
        return reconnectInterval;
    }


    public long getDelay(TimeUnit unit) {
        return unit.convert(this.nextReconnectTimestamp - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }


    public int compareTo(Delayed o) {
        ReconnectRequest other = (ReconnectRequest) o;
        if (this.nextReconnectTimestamp > other.nextReconnectTimestamp) {
            return 1;
        }
        else {
            return -1;
        }
    }


    /**
     * Returns a reconnect socket address wrapper
     * 
     * @see InetSocketAddressWrapper
     * @return
     */
    public final InetSocketAddressWrapper getInetSocketAddressWrapper() {
        return this.inetSocketAddressWrapper;
    }


    public void updateNextReconnectTimeStamp(long interval) {
        interval = this.normalInterval(interval);
        this.nextReconnectTimestamp = System.currentTimeMillis() + interval;
    }


    public final void setInetSocketAddressWrapper(InetSocketAddressWrapper inetSocketAddressWrapper) {
        this.inetSocketAddressWrapper = inetSocketAddressWrapper;
    }


    public final void setTries(int tries) {
        this.tries = tries;
    }


    /**
     * Returns retry times
     * 
     * @return retry times,it is zero if it does not retry to connect
     */
    public final int getTries() {
        return this.tries;
    }

}
/**
 * Copyright [2009-2010] [dennis zhuang(killme2008@gmail.com)] Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance with the License. You
 * may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by
 * applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License
 */
/**
 * Copyright [2009-2010] [dennis zhuang(killme2008@gmail.com)] Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance with the License. You
 * may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by
 * applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License
 */
package com.google.code.yanf4j.core;

/**
 * 
 * Controller state listener
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-16 ����05:59:44
 */
public interface ControllerStateListener {

  /**
   * When controller is started
   * 
   * @param controller
   */
  public void onStarted(final Controller controller);

  /**
   * When controller is ready
   * 
   * @param controller
   */
  public void onReady(final Controller controller);

  /**
   * When all connections are closed
   * 
   * @param controller
   */
  public void onAllSessionClosed(final Controller controller);

  /**
   * When controller has been stopped
   * 
   * @param controller
   */
  public void onStopped(final Controller controller);

  public void onException(final Controller controller, Throwable t);
}

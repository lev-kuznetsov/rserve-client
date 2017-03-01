/*
 * The MIT License (MIT)
 * Copyright (c) 2017 lev.v.kuznetsov@gmail.com
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package us.levk.rserve.client.mocks;

import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.net.URI;
import java.util.Set;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.DeploymentException;
import javax.websocket.Extension;
import javax.websocket.RemoteEndpoint.Basic;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import us.levk.rserve.client.websocket.Endpoint;

public class WebSocketContainerMock implements WebSocketContainer {

  private final Basic target;

  public WebSocketContainerMock (Basic t) {
    target = t;
  }

  @Override
  public Session connectToServer (Object e, URI arg1) throws DeploymentException, IOException {
    Session s = mock (Session.class, i -> {
      return (i.getMethod ().getName ().equals ("getBasicRemote")) ? target : null;
    });
    ((Endpoint) e).connect (s);
    return s;
  }

  @Override
  public Session connectToServer (Class <?> arg0, URI arg1) throws DeploymentException, IOException {
    throw new UnsupportedOperationException ();
  }

  @Override
  public long getDefaultAsyncSendTimeout () {
    throw new UnsupportedOperationException ();
  }

  @Override
  public int getDefaultMaxBinaryMessageBufferSize () {
    throw new UnsupportedOperationException ();
  }

  @Override
  public long getDefaultMaxSessionIdleTimeout () {
    throw new UnsupportedOperationException ();
  }

  @Override
  public int getDefaultMaxTextMessageBufferSize () {
    throw new UnsupportedOperationException ();
  }

  @Override
  public Set <Extension> getInstalledExtensions () {
    throw new UnsupportedOperationException ();
  }

  @Override
  public void setAsyncSendTimeout (long arg0) {
    throw new UnsupportedOperationException ();
  }

  @Override
  public void setDefaultMaxBinaryMessageBufferSize (int arg0) {
    throw new UnsupportedOperationException ();
  }

  @Override
  public void setDefaultMaxSessionIdleTimeout (long arg0) {
    throw new UnsupportedOperationException ();
  }

  @Override
  public void setDefaultMaxTextMessageBufferSize (int arg0) {
    throw new UnsupportedOperationException ();
  }

  @Override
  public Session connectToServer (javax.websocket.Endpoint arg0, ClientEndpointConfig arg1, URI arg2)
      throws DeploymentException, IOException {
    throw new UnsupportedOperationException ();
  }

  @Override
  public Session connectToServer (Class <? extends javax.websocket.Endpoint> arg0, ClientEndpointConfig arg1, URI arg2)
      throws DeploymentException, IOException {
    throw new UnsupportedOperationException ();
  }
}

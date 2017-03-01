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
package us.levk.rserve.client;

import static java.util.concurrent.Executors.newWorkStealingPool;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import javax.websocket.DeploymentException;

import org.junit.Test;

import us.levk.jackson.rserve.RserveMapper;
import us.levk.rserve.client.mocks.MatchingRemoteMock;
import us.levk.rserve.client.mocks.WebSocketContainerMock;
import us.levk.rserve.client.websocket.Endpoint;

public class ClientTest implements Streams {

  private Endpoint w (MatchingRemoteMock r) throws DeploymentException, IOException {
    Endpoint w =
        (Endpoint) Client.rserve ().with (newWorkStealingPool ()).with (new RserveMapper ()).websocket (new WebSocketContainerMock (r)).connect ("");
    w.handshake ("Rsrv0103QAP1  --------------  ");
    return w;
  }

  private <T> T command (Function <Client, CompletableFuture <T>> n, String b, String r) throws Exception {
    MatchingRemoteMock x = new MatchingRemoteMock ();
    Endpoint w = w (x);
    CompletableFuture <T> f = n.apply (w);
    Thread.yield ();
    x.matches (loadb64 (b).array ());
    System.out.println (w + ":" + r);
    try {
      w.receive (loadb64 (r));
    } catch (NullPointerException e) {
      e.printStackTrace ();
      throw e;
    }
    return f.get ();
  }

  @Test
  public void wsAssignFoo () throws Exception {
    assertNull (command (c -> c.assign ("foobar", "foobar"), "/assignStringFoobar.b64", "/emptyPacket.b64"));
  }

  @Test
  public void wsEvaluateFooToBar () throws Exception {
    assertNull (command (c -> c.evaluate ("foo<-'bar'"), "/evaluateFooToBar.b64", "/emptyPacket.b64"));
  }
}

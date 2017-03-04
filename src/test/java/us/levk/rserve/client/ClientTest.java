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

import static java.util.concurrent.Executors.*;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.websocket.DeploymentException;
import javax.websocket.RemoteEndpoint.Basic;

import org.junit.Test;

import us.levk.jackson.rserve.RserveMapper;
import us.levk.rserve.client.mocks.StreamingMatchingRemoteMock;
import us.levk.rserve.client.mocks.WebSocketContainerMock;
import us.levk.rserve.client.websocket.Endpoint;

public class ClientTest implements Streams {

  private Endpoint w (Basic r) throws DeploymentException, IOException {
    Endpoint w =
        (Endpoint) Client.rserve ().with (newSingleThreadExecutor ()).with (new RserveMapper ()).websocket (new WebSocketContainerMock (r)).connect ("");
    w.handshake ("Rsrv0103QAP1  --------------  ");
    return w;
  }

  private <T> T command (Function <Client, CompletableFuture <T>> n, String b, String r) throws Exception {
    StreamingMatchingRemoteMock y = new StreamingMatchingRemoteMock ();
    Endpoint w = w (y);
    y.add (loadb64 (b).array (), () -> {
      w.receive (loadb64 (r));
      return null;
    });
    return n.apply (w).get (10, TimeUnit.SECONDS);
  }

  @Test
  public void wsAssignFoo () throws Exception {
    assertNull (command (c -> c.assign ("foobar", "foobar"), "/assignStringFoobar.b64", "/emptyPacket.b64"));
  }

  @Test
  public void wsEvaluateFooToBar () throws Exception {
    assertNull (command (c -> c.evaluate ("foo<-'bar'"), "/evaluateFooToBar.b64", "/emptyPacket.b64"));
  }

  @Test
  public void wsResolveRTo89 () throws Exception {
    assertThat (command (c -> c.resolve ("r", Integer.class), "/resolveR.b64", "/int89Packet.b64"), is (89));
  }

  @Test
  public void wsBatchFib () throws Exception {
    StreamingMatchingRemoteMock b = new StreamingMatchingRemoteMock ();
    Endpoint w = w (b);
    b.add (loadb64 ("/assignN11.b64").array (), () -> {
      w.receive (loadb64 ("/emptyPacket.b64"));
      return null;
    }).add (loadb64 ("/evalFibonacci.b64").array (), () -> {
      w.receive (loadb64 ("/emptyPacket.b64"));
      return null;
    }).add (loadb64 ("/resolveR.b64").array (), () -> {
      w.receive (loadb64 ("/int89packet.b64"));
      return null;
    });
    w.batch (new Fib (11)).thenAccept (f -> assertThat (f.r, is (89)));
  }

  @Test
  public void wsPushDataTsv () throws Exception {
    StreamingMatchingRemoteMock b = new StreamingMatchingRemoteMock ();
    Endpoint w = w (b);
    b.add (loadb64 ("/createDataTsv.b64").array (), () -> {
      w.receive (loadb64 ("/emptyPacket.b64"));
      return null;
    }).add (loadb64 ("/writeDataTsv.b64").array (), () -> {
      w.receive (loadb64 ("/emptyPacket.b64"));
      return null;
    }).add (loadb64 ("/close.b64").array (), () -> {
      w.receive (loadb64 ("/emptyPacket.b64"));
      return null;
    });
    w.push (new File ("src/test/resources/data.tsv")).get (10, TimeUnit.SECONDS);
  }

//  @Test
//  public void wsPullDataTsv () throws Exception {
//    Path d = Files.createTempDirectory ("");
//    try {
//      StreamingMatchingRemoteMock b = new StreamingMatchingRemoteMock ();
//      Endpoint w = w (b);
//      b.add (loadb64 ("/openDataTsv.b64").array (), () -> {
//        System.out.println ("opening");
//        w.receive (loadb64 ("/emptyPacket.b64"));
//        System.out.println ("opened");
//        return null;
//      }).add (loadb64 ("/read1M.b64").array (), () -> {
//        System.out.println ("reading");
//        w.receive (loadb64 ("/dataTsvStreamPacket.b64"));
//        System.out.println ("read");
//        return null;
//      }).add (loadb64 ("/close.b64").array (), () -> {
//        System.out.println ("closing");
//        w.receive (loadb64 ("/emptyPacket.b64"));
//        System.out.println ("closed");
//        return null;
//      });
//      w.pull (new File (d.toFile (), "data.tsv")).get (10, TimeUnit.SECONDS);
//    } finally {
//      Files.walkFileTree (d, new SimpleFileVisitor <Path> () {
//        public FileVisitResult visitFile (Path f, BasicFileAttributes attrs) throws IOException {
//          Files.delete (f);
//          return FileVisitResult.CONTINUE;
//        }
//
//        public FileVisitResult postVisitDirectory (Path d, IOException exc) throws IOException {
//          Files.delete (d);
//          return FileVisitResult.CONTINUE;
//        }
//      });
//    }
//  }
}

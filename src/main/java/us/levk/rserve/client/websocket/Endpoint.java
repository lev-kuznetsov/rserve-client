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
package us.levk.rserve.client.websocket;

import static java.lang.Math.min;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.util.Optional.ofNullable;
import static java.util.stream.IntStream.range;
import static javax.websocket.CloseReason.CloseCodes.NORMAL_CLOSURE;
import static us.levk.rserve.client.protocol.Qap.packet;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

import com.fasterxml.jackson.databind.ObjectMapper;

import us.levk.rserve.client.Client;
import us.levk.rserve.client.protocol.commands.Command;

/**
 * Websocket client endpoint
 * 
 * @author levk
 */
@ClientEndpoint
public class Endpoint implements Client {

  /**
   * Mapper
   */
  private final ObjectMapper mapper;
  /**
   * Async provider
   */
  private final ExecutorService executor;
  /**
   * Websocket session
   */
  private Session session;
  /**
   * Current command
   */
  private Command <?> current;
  /**
   * Receiver for the current command result
   */
  private final AtomicReference <CompletableFuture <?>> receiver =
      new AtomicReference <> (new CompletableFuture <Void> ());
  /**
   * Command queue
   */
  private final AtomicReference <CompletableFuture <?>> queue = new AtomicReference <> (receiver.get ());

  /**
   * @param m
   *          mapper
   * @param e
   *          executor
   */
  public Endpoint (ObjectMapper m, ExecutorService e) {
    mapper = m;
    executor = e;
  }

  /**
   * @param s
   *          websocket session
   */
  @OnOpen
  public void connect (Session s) {
    session = s;
  }

  /**
   * @param w
   *          welcome wagon
   * @throws IOException
   *           on unsupported welcome wagon
   */
  @OnMessage
  public void handshake (String w) throws IOException {
    if (!HANDSHAKE_PATTERN.matcher (w).matches ()) throw new IOException ("Unsupported handshake wagon " + w);
    receiver.getAndSet (null).complete (null);
  }

  /**
   * @param i
   *          input
   * @throws IOException
   *           on read or decoding failure
   */
  @SuppressWarnings ("unchecked")
  @OnMessage
  public void receive (ByteBuffer i) throws IOException {
    Object v = current.decode (packet (i.order (LITTLE_ENDIAN)), mapper);
    ((CompletableFuture <Object>) receiver.getAndSet (null)).complete (v);
  }

  /**
   * @param e
   *          error
   */
  @OnError
  public void handle (Throwable e) {
    ofNullable (receiver.getAndSet (null)).ifPresent (r -> r.completeExceptionally (e));
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.io.Closeable#close()
   */
  @Override
  public void close () throws IOException {
    if (session.isOpen ()) session.close ();
  }

  /**
   * @param r
   *          close reason
   */
  @OnClose
  public void disconnect (CloseReason r) {
    if (r.getCloseCode () != NORMAL_CLOSURE)
      handle (new IOException ("Unable to fulfil request: " + r.getReasonPhrase ()));
  }

  /*
   * (non-Javadoc)
   * 
   * @see us.levk.rserve.client2.Client#execute(us.levk.rserve.client2.protocol.
   * commands.Command)
   */
  @Override
  public <T> CompletableFuture <T> execute (Command <T> c) {
    CompletableFuture <T> r = new CompletableFuture <> ();

    queue.getAndUpdate (q -> q.thenRunAsync ( () -> {
      try {
        receiver.set (r);
        for (Iterator <ByteBuffer> i = (current = c).encode (mapper).flatMap (b -> {
          int a = b.position (), z = b.limit (), s = 1 << 18;
          return range (0, 1 + (z - a) / s).map (p -> a + s * p).mapToObj (p -> {
            b.position (p).limit (min (p + s, z));
            return b.slice ();
          });
        }).iterator ();i.hasNext ();) {
          ByteBuffer b = i.next ();
          session.getBasicRemote ().sendBinary (b, !i.hasNext ());
        }
      } catch (Exception e) {
        current = null;
        receiver.getAndSet (null).completeExceptionally (e);
      }
    }, executor));

    return r;
  }
}

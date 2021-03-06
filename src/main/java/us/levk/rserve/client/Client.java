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

import static java.lang.Math.min;
import static java.net.URI.create;
import static java.nio.channels.FileChannel.MapMode.READ_ONLY;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.Executors.newWorkStealingPool;
import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.MULTILINE;
import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.joining;
import static java.util.stream.LongStream.range;
import static java.util.stream.Stream.of;
import static javax.websocket.ContainerProvider.getWebSocketContainer;
import static us.levk.rserve.client.tools.reflect.Classes.base;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.lang.reflect.Type;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;

import javax.websocket.DeploymentException;
import javax.websocket.WebSocketContainer;

import com.fasterxml.jackson.databind.ObjectMapper;

import us.levk.jackson.rserve.RserveMapper;
import us.levk.rserve.client.protocol.commands.Assign;
import us.levk.rserve.client.protocol.commands.Close;
import us.levk.rserve.client.protocol.commands.Command;
import us.levk.rserve.client.protocol.commands.Create;
import us.levk.rserve.client.protocol.commands.Evaluate;
import us.levk.rserve.client.protocol.commands.Resolve;
import us.levk.rserve.client.protocol.commands.Write;
import us.levk.rserve.client.websocket.Endpoint;

/**
 * Rserve client
 * 
 * @author levk
 */
public interface Client extends Closeable {

  /**
   * Welcome wagon regex
   */
  static final Pattern HANDSHAKE_PATTERN = compile ("Rsrv0103QAP1.*--------------.*", MULTILINE | DOTALL);
  /**
   * File IO operations buffer size
   */
  static final int FILE_COMMAND_BUFFER_SIZE = 1 << 18;

  /**
   * @param c
   *          command
   * @param <T>
   *          of promise
   * @return promise
   */
  <T> CompletableFuture <T> execute (Command <T> c);

  /**
   * @param n
   *          name
   * @param v
   *          value
   * @return promise
   */
  default CompletableFuture <Void> assign (String n, Object v) {
    return execute (new Assign (n, v));
  }

  /**
   * @param n
   *          name
   * @param t
   *          type
   * @param <T>
   *          type
   * @return promise
   */
  default <T> CompletableFuture <T> resolve (String n, Type t) {
    return execute (new Resolve <> (n, t));
  }

  /**
   * @param c
   *          code
   * @return promise
   */
  default CompletableFuture <Void> evaluate (String c) {
    return execute (new Evaluate (c));
  }

  /**
   * @param f
   *          file
   * @return promise
   */
  default CompletableFuture <Void> push (File f) {
    CompletableFuture <Void> p = new CompletableFuture <> ();

    try {
      long l = f.length ();
      RandomAccessFile s = new RandomAccessFile (f, "r");
      range (0, 1 + l / FILE_COMMAND_BUFFER_SIZE).map (q -> q * FILE_COMMAND_BUFFER_SIZE).mapToObj (q -> {
        try {
          return s.getChannel ().map (READ_ONLY, q, min (FILE_COMMAND_BUFFER_SIZE, l - q));
        } catch (IOException e) {
          throw new UndeclaredThrowableException (e);
        }
      }).reduce (execute (new Create (f.getName ())), (c, b) -> c.thenCompose (x -> {
        return execute (new Write (b));
      }), (x, y) -> {
        throw new UnsupportedOperationException ();
      }).thenCompose (x -> execute (new Close ())).whenComplete ( (x, e) -> {
        try {
          s.close ();
        } catch (Exception i) {
          if (e != null) e.addSuppressed (i);
          else e = i;
        }
        if (e != null) p.completeExceptionally (e);
        else p.complete (null);
      });
    } catch (UndeclaredThrowableException e) {
      p.completeExceptionally (e.getCause ());
    } catch (Exception e) {
      p.completeExceptionally (e);
    }

    return p;
  }

  /**
   * @param j
   *          job
   * @param <T>
   *          of job
   * @return promise
   */
  default <T> CompletableFuture <T> batch (T j) {
    CompletableFuture <Void> v = completedFuture (null);

    // push
    v = base (j.getClass ()).reduce (v, (w, c) -> {
      // fields
      w = of (c.getDeclaredFields ()).reduce (w, (u, f) -> {
        Push l = f.getAnnotation (Push.class);
        return l == null ? u : u.thenCompose (x -> {
          CompletableFuture <Void> p = new CompletableFuture <> ();

          try {
            if (!f.isAccessible ()) f.setAccessible (true);
            push ((File) f.get (j)).whenComplete ( (y, e) -> {
              if (e != null) p.completeExceptionally (e);
              else p.complete (null);
            });
          } catch (Exception e) {
            p.completeExceptionally (e);
          }

          return p;
        });
      }, (x, y) -> {
        throw new UnsupportedOperationException ();
      });

      // methods
      w = of (c.getDeclaredMethods ()).reduce (w, (u, m) -> {
        Push l = m.getAnnotation (Push.class);
        return l == null ? u : u.thenCompose (x -> {
          CompletableFuture <Void> p = new CompletableFuture <> ();

          try {
            if (!m.isAccessible ()) m.setAccessible (true);
            push ((File) m.invoke (j)).whenComplete ( (y, e) -> {
              if (e != null) p.completeExceptionally (e);
              else p.complete (null);
            });
          } catch (Exception e) {
            p.completeExceptionally (e);
          }

          return p;
        });
      }, (x, y) -> {
        throw new UnsupportedOperationException ();
      });

      return w;
    }, (x, y) -> {
      throw new UnsupportedOperationException ();
    });

    // assign
    v = base (j.getClass ()).reduce (v, (w, c) -> {
      // fields
      w = of (c.getDeclaredFields ()).reduce (w, (u, f) -> {
        us.levk.rserve.client.Assign a = f.getAnnotation (us.levk.rserve.client.Assign.class);
        return a == null ? u : u.thenCompose (x -> {
          CompletableFuture <Void> p = new CompletableFuture <> ();

          try {
            if (!f.isAccessible ()) f.setAccessible (true);
            assign (a.value ().equals ("") ? f.getName () : a.value (), f.get (j)).whenComplete ( (y, e) -> {
              if (e != null) p.completeExceptionally (e);
              else p.complete (null);
            });
          } catch (Exception e) {
            p.completeExceptionally (e);
          }

          return p;
        });
      }, (x, y) -> {
        throw new UnsupportedOperationException ();
      });

      // methods
      w = of (c.getDeclaredMethods ()).reduce (w, (u, m) -> {
        us.levk.rserve.client.Assign a = m.getAnnotation (us.levk.rserve.client.Assign.class);
        return a == null ? u : u.thenCompose (x -> {
          CompletableFuture <Void> p = new CompletableFuture <> ();

          try {
            if (!m.isAccessible ()) m.setAccessible (true);
            assign (a.value ().equals ("") ? m.getName () : a.value (), m.invoke (j)).whenComplete ( (y, e) -> {
              if (e != null) p.completeExceptionally (e);
              else p.complete (null);
            });
          } catch (Exception e) {
            p.completeExceptionally (e);
          }

          return p;
        });
      }, (x, y) -> {
        throw new UnsupportedOperationException ();
      });

      return w;
    }, (x, y) -> {
      throw new UnsupportedOperationException ();
    });

    // evaluate
    v = base (j.getClass ()).reduce (v, (w, c) -> {
      // script
      Rscript s = c.getAnnotation (Rscript.class);
      if (s != null) w = w.thenCompose (x -> {
        CompletableFuture <Void> p = new CompletableFuture <> ();

        try (BufferedReader q =
            new BufferedReader (new InputStreamReader (j.getClass ().getResourceAsStream (s.value ())))) {
          return evaluate (q.lines ().collect (joining ("\n"))).thenRun ( () -> p.complete (null));
        } catch (Exception e) {
          p.completeExceptionally (e);
        }

        return p;
      });

      // code
      R r = c.getAnnotation (R.class);
      if (r != null) w = w.thenCompose (x -> evaluate (r.value ()));

      return w;
    }, (x, y) -> {
      throw new UnsupportedOperationException ();
    });

    // resolve
    v = base (j.getClass ()).reduce (v, (w, c) -> {
      // fields
      w = of (c.getDeclaredFields ()).reduce (w, (u, f) -> {
        us.levk.rserve.client.Resolve r = f.getAnnotation (us.levk.rserve.client.Resolve.class);
        return r == null ? u : u.thenCompose (x -> {
          CompletableFuture <Void> p = new CompletableFuture <> ();

          String n = r.value ().equals ("") ? f.getName () : r.value ();
          resolve (n, f.getGenericType ()).thenAccept (t -> {
            try {
              if (!f.isAccessible ()) f.setAccessible (true);
              f.set (j, t);
              p.complete (null);
            } catch (Exception e) {
              p.completeExceptionally (e);
            }
          });

          return p;
        });
      }, (x, y) -> {
        throw new UnsupportedOperationException ();
      });

      // methods
      w = of (c.getDeclaredMethods ()).reduce (w, (u, m) -> {
        us.levk.rserve.client.Resolve r = m.getAnnotation (us.levk.rserve.client.Resolve.class);
        return r == null ? u : u.thenCompose (x -> {
          CompletableFuture <Void> p = new CompletableFuture <> ();

          String n = r.value ().equals ("") ? m.getName () : r.value ();
          resolve (n, m.getGenericParameterTypes ()[0]).thenAccept (t -> {
            try {
              if (!m.isAccessible ()) m.setAccessible (true);
              m.invoke (j, t);
              p.complete (null);
            } catch (Exception e) {
              p.completeExceptionally (e);
            }
          });

          return p;
        });
      }, (x, y) -> {
        throw new UnsupportedOperationException ();
      });

      return w;
    }, (x, y) -> {
      throw new UnsupportedOperationException ();
    });

    return v.thenApply (x -> j);
  }

  /**
   * Client builder
   * 
   * @author levk
   */
  static class Builder {
    /**
     * Object mapper
     */
    private final ObjectMapper mapper;
    /**
     * Async provider
     */
    private final ExecutorService executor;

    /**
     * @param m
     *          mapper
     * @param e
     *          executor
     */
    private Builder (ObjectMapper m, ExecutorService e) {
      mapper = m;
      executor = e;
    }

    /**
     * @param m
     *          mapper
     * @return copy of this builder with the specified mapper
     */
    public Builder with (RserveMapper m) {
      return new Builder (m, executor);
    }

    /**
     * @param e
     *          executor
     * @return copy of this builder with the executor specified
     */
    public Builder with (ExecutorService e) {
      return new Builder (mapper, e);
    }

    /**
     * @return websocket client builder
     */
    public WsBuilder websocket () {
      return websocket (getWebSocketContainer ());
    }

    /**
     * @param c
     *          container
     * @return websocket client builder
     */
    public WsBuilder websocket (WebSocketContainer c) {
      return new WsBuilder (c);
    }

    /**
     * Websocket client builder
     * 
     * @author levk
     */
    public class WsBuilder {
      /**
       * Container
       */
      private final WebSocketContainer container;

      /**
       * @param c
       *          container
       */
      private WsBuilder (WebSocketContainer c) {
        container = c;
      }

      /**
       * @param c
       *          container
       * @return copy of this builder with container specified
       */
      public WsBuilder with (WebSocketContainer c) {
        return new WsBuilder (c);
      }

      /**
       * @param u
       *          endpoint URI
       * @return client
       * @throws DeploymentException
       *           on connect
       * @throws IOException
       *           on connect
       */
      public Client connect (String u) throws DeploymentException, IOException {
        return connect (create (u));
      }

      /**
       * @param u
       *          endpoint URI
       * @return client
       * @throws DeploymentException
       *           on connect
       * @throws IOException
       *           on connect
       */
      public Client connect (URI u) throws DeploymentException, IOException {
        Endpoint e = new Endpoint (mapper, executor);
        container.connectToServer (e, u);
        return e;
      }
    }
  }

  /**
   * @return builder
   */
  public static Builder rserve () {
    return rserve (new RserveMapper ());
  }

  /**
   * @param m
   *          mapper
   * @return builder using the specified mapper
   */
  public static Builder rserve (RserveMapper m) {
    return rserve (m, newWorkStealingPool ());
  }

  /**
   * @param e
   *          executor
   * @return builder using the executor specified
   */
  public static Builder rserve (ExecutorService e) {
    return rserve (new RserveMapper (), e);
  }

  /**
   * @param m
   *          mapper
   * @param e
   *          executor
   * @return builder using the specified mapper and executor
   */
  public static Builder rserve (RserveMapper m, ExecutorService e) {
    return new Builder (m, e);
  }
}

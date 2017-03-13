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

import static com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThat;
import static us.levk.rserve.client.Client.rserve;

import java.io.File;
import java.util.concurrent.CompletableFuture;

import org.junit.Test;

import us.levk.jackson.rserve.RserveMapper;

public class E2e {

  private final String RSERVE = "ws://localhost:8081";

  /// Programatic

  /**
   * All command methods return promises, compose promise chain like in this
   * example or call {@link CompletableFuture#get()} on each to block for
   * completion like in the following example
   */
  @Test
  public void factorial () throws Exception {
    try (Client c = rserve ().websocket ().connect (RSERVE)) {
      CompletableFuture <?> f = c.assign ("n", 5);
      f = f.thenCompose (x -> c.evaluate ("f <- function (n) if (n < 1) 1 else n * f (n - 1)"));
      f = f.thenCompose (x -> c.evaluate ("r <- f(n)"));
      f = f.thenCompose (x -> c.resolve ("r", Integer.class));
      f = f.thenAccept (r -> assertThat (r, is (120)));
      f.get (10, SECONDS);
    }
  }

  @Test
  public void push () throws Exception {
    try (Client c = rserve ().websocket ().connect (RSERVE)) {
      c.push (new File ("src/test/resources/data.tsv")).get (10, SECONDS);
      c.evaluate ("d <- read.table ('data.tsv')").get (10, SECONDS);
      c.evaluate ("n <- colnames (d)").get (10, SECONDS);
      c.resolve ("n", String[].class).thenAccept (n -> {
        assertArrayEquals (new String[] { "X1", "X2", "X3", "X4", "X5", "X6", "X7", "X8", "X9", "X10" }, (String[]) n);
      }).get (10, SECONDS);
    }
  }

  /// Declarative

  /**
   * @see Fib
   */
  @Test
  public void fibonacci () throws Exception {
    try (Client c = Client.rserve ().websocket ().connect (RSERVE)) {
      Fib f = c.batch (new Fib (11)).get (10, SECONDS);
      assertThat (f.r, is (89));
    }
  }

  /**
   * @see KMeans
   */
  @Test
  public void kMeans () throws Exception {
    RserveMapper m = new RserveMapper ();
    m.enable (ACCEPT_SINGLE_VALUE_AS_ARRAY);
    try (Client c = rserve (m).websocket ().connect (RSERVE)) {
      KMeans k = c.batch (new KMeans (new double[][] { new double[] { .1, .2, .3 }, new double[] { .1, .2, .4 },
                                                       new double[] { 22, 33, 44 } },
                                      2)).get (10, SECONDS);
      assertArrayEquals (new int[] { 1, 1, 2 }, k.r);
    }
  }

  @Test
  public void methodAssignResolve () throws Exception {
    try (Client c = Client.rserve ().websocket ().connect (RSERVE)) {
      c.batch (new MethodAssignResolve ()).get (10, SECONDS);
    }
  }

  @Test
  public void batchPush () throws Exception {
    try (Client c = Client.rserve ().websocket ().connect (RSERVE)) {
      c.batch (new BatchPush ()).get (10, SECONDS);
    }
  }
}

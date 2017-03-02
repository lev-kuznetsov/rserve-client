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
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThat;
import static us.levk.rserve.client.Client.rserve;

import java.util.concurrent.CompletableFuture;

import org.junit.Test;

import us.levk.jackson.rserve.RserveMapper;

public class E2e {

  private final String RSERVE = "ws://192.168.99.101:8081";

  /// Declarative

  // Fibonacci

  @Test
  public void fibonacci () throws Exception {
    try (Client c = Client.rserve ().websocket ().connect (RSERVE)) {
      Fib f = c.batch (new Fib (11)).get ();
      assertThat (f.r, is (89));
    }
  }

  // KMeans

  @Test
  public void kMeans () throws Exception {
    RserveMapper m = new RserveMapper ();
    m.enable (ACCEPT_SINGLE_VALUE_AS_ARRAY);
    try (Client c = rserve (m).websocket ().connect (RSERVE)) {
      KMeans k = c.batch (new KMeans (new double[][] { new double[] { .1, .2, .3 }, new double[] { .1, .2, .4 },
                                                       new double[] { 22, 33, 44 } },
                                      2)).get ();
      assertArrayEquals (new int[] { 1, 1, 2 }, k.r);
    }
  }

  /// Programatic

  @Test
  public void factorial () throws Exception {
    try (Client c = rserve ().websocket ().connect (RSERVE)) {
      CompletableFuture <?> f = c.assign ("n", 5);
      f = f.thenCompose (x -> c.evaluate ("f <- function (n) if (n < 1) 1 else n * f (n - 1)"));
      f = f.thenCompose (x -> c.evaluate ("r <- f(n)"));
      f = f.thenCompose (x -> c.resolve ("r", Integer.class));
      int r = (Integer) f.get ();
      assertThat (r, is (120));
    }
  }
}

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
package us.levk.rserve.client.protocol;

import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteBuffer.wrap;
import static java.util.stream.Stream.of;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.stream.Stream;

import org.rosuda.REngine.Rserve.protocol.RTalk;

/**
 * QAP1 protocol specifics
 * 
 * @author levk
 */
public interface Qap {

  /**
   * String type
   */
  static final int DT_STRING = RTalk.DT_STRING;
  /**
   * Flag for objects larger than 0xffffff
   */
  static final int DT_LARGE = RTalk.DT_LARGE;
  /**
   * SEXP type
   */
  static final int DT_SEXP = RTalk.DT_SEXP;

  /**
   * Assign command code
   */
  static final int CMD_assignSEXP = RTalk.CMD_assignSEXP;
  /**
   * Evaluate command code
   */
  static final int CMD_eval = RTalk.CMD_eval;
  /**
   * Evaluate without result command code
   */
  static final int CMD_voidEval = RTalk.CMD_voidEval;

  /**
   * @param c
   *          code
   * @param s
   *          size
   * @return header
   */
  static ByteBuffer header (int c, int s) {
    boolean l = s > 0xfffff0;
    byte[] b = new byte[l ? 8 : 4];
    int o = 0;
    b[o] = (byte) ((c & 255) | ((s > 0xfffff0) ? DT_LARGE : 0));
    o++;
    b[o] = (byte) (s & 255);
    o++;
    b[o] = (byte) ((s & 0xff00) >> 8);
    o++;
    b[o] = (byte) ((s & 0xff0000) >> 16);
    o++;
    if (l) {
      b[o] = (byte) ((s & 0xff000000) >> 24);
      o++;
      b[o] = 0;
      o++;
      b[o] = 0;
      o++;
      b[o] = 0;
      o++;
    }
    return wrap (b);
  }

  /**
   * @param s
   *          string
   * @return stream of encoded buffers
   */
  static Stream <ByteBuffer> string (String s) {
    int l = s.length () + 1;
    int p = 4 - (l & 3);
    return of (header (DT_STRING, l + p), wrap (s.getBytes ()), allocate (p + 1));
  }

  /**
   * @param c
   *          content
   * @return stream of encoded buffers
   */
  static Stream <ByteBuffer> sexp (byte[] c) {
    return of (header (DT_SEXP, c.length), wrap (c));
  }

  /**
   * @param i
   *          input
   * @return packet content (or null if no content)
   * @throws IOException
   *           on read or bad packet
   */
  static ByteBuffer packet (ByteBuffer i) throws IOException {
    int r = i.getInt ();
    if ((r & 0xf) == 1) {
      int s = i.getInt ();
      int h = i.position () + 8;
      i.position (h);
      if (s > 0) i.position (h + ((i.get () & DT_LARGE) > 0 ? 8 : 4));
      return i;
    } else throw new IOException ("Error response packet with error code " + ((r >> 24) & 127));
  }
}

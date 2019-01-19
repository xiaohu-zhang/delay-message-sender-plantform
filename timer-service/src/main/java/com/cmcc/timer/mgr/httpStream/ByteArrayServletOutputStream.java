package com.cmcc.timer.mgr.httpStream;

import java.io.ByteArrayOutputStream;

import javax.servlet.ServletOutputStream;

public class ByteArrayServletOutputStream extends ServletOutputStream {
    /**
     * Our buffer to hold the stream.
     */
    protected final ByteArrayOutputStream buf;


    /**
     * Construct a new ServletOutputStream.
     */
    public ByteArrayServletOutputStream() {
        buf = new ByteArrayOutputStream();
    }


    /**
     * @return the byte array.
     */
    public byte[] toByteArray() {
        return buf.toByteArray();
    }


    /**
     * Write to our buffer.
     *
     * @param b The parameter to write
     */
    @Override
    public void write(int b) {
        buf.write(b);
    }




}


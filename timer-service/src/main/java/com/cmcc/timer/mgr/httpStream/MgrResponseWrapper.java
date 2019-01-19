package com.cmcc.timer.mgr.httpStream;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

public class MgrResponseWrapper extends HttpServletResponseWrapper {



    ByteArrayServletOutputStream bos = new ByteArrayServletOutputStream();
    

    @Override
    public PrintWriter getWriter() throws IOException {
        // TODO Auto-generated method stub
        return new PrintWriter(bos);
    }



    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        // TODO Auto-generated method stub
        return bos;
    }
    
    

    public MgrResponseWrapper(HttpServletResponse response) {
        super(response);
        // TODO Auto-generated constructor stub
    }
    
    
    

}

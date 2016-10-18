/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cxf.jaxrs.nio;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.apache.cxf.transport.http.AbstractHTTPDestination.WrappingOutputStream;

@Provider
public class NioMessageBodyWriter implements MessageBodyWriter<NioWriteEntity> {
    @Context
    private HttpServletRequest request;
    
    public NioMessageBodyWriter() {
    }

    @Override
    public boolean isWriteable(Class<?> cls, Type type, Annotation[] anns, MediaType mt) {
        return NioWriteEntity.class.isAssignableFrom(cls);
    }
    
    @Override
    public void writeTo(NioWriteEntity p, Class<?> cls, Type t, Annotation[] anns,
            MediaType mt, MultivaluedMap<String, Object> headers, OutputStream os) 
                throws IOException, WebApplicationException {
        
        OutputStream out = os;
        if (out instanceof WrappingOutputStream) {
            final WrappingOutputStream wrappingStream = (WrappingOutputStream)out;
            if (wrappingStream.getWrappingOutputStream() != null) {
                out = wrappingStream.getWrappingOutputStream();
            }
        }
        
        if (out instanceof ServletOutputStream) {
            final ServletOutputStream servletOutputStream = (ServletOutputStream)out;
            
            if (!request.isAsyncStarted()) {
                request.startAsync();
            }
            
            final WriteListener listener = new NioWriterImpl(p.getWriter(), p.getError(), 
                request.getAsyncContext(), servletOutputStream);
            servletOutputStream.setWriteListener(listener);
        } else {
            final DelegatingNioOutputStream nio = new DelegatingNioOutputStream(out);
            try {
                while (p.getWriter().write(nio)) {
                    Thread.yield();
                }
            } catch (Throwable ex) {
                try {
                    p.getError().error(ex);
                } catch (IOException | WebApplicationException inner) {
                    throw inner;
                } catch (Throwable inner) {
                    throw new WebApplicationException(ex);
                }
            }
        }
    }
    
    @Override
    public long getSize(NioWriteEntity t, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType) {
        return -1;
    }
}

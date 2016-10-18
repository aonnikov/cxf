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

import javax.servlet.AsyncContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.ws.rs.core.NioErrorHandler;
import javax.ws.rs.core.NioWriterHandler;

public final class NioWriterImpl implements WriteListener {
    private final NioWriterHandler writer;
    private final NioErrorHandler error;
    private final AsyncContext async;
    private final DelegatingNioServletOutputStream out;

    NioWriterImpl(NioWriterHandler writer, AsyncContext async, ServletOutputStream out) {
       this(writer, (throwable) -> {
       }, async, out);
    }
    
    NioWriterImpl(NioWriterHandler writer, NioErrorHandler error, AsyncContext async, ServletOutputStream out) {
        this.writer = writer;
        this.error = error;
        this.async = async;
        this.out = new DelegatingNioServletOutputStream(out);
    }

    @Override
    public void onWritePossible() throws IOException {
        // while we are able to write without blocking
        while (out.isReady()) {
            if (!writer.write(out)) {
                async.complete();
                return;
            }
        }
    }

    @Override
    public void onError(Throwable t) {
        try {
            error.error(t);
        } catch (final Throwable ex) {
            // LOG exception here;
        } finally {
            async.complete();
        }
    }
}
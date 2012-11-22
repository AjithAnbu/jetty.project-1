//
//  ========================================================================
//  Copyright (c) 1995-2012 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.websocket.common.io;

import java.nio.ByteBuffer;

import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.websocket.api.extensions.Frame;

public class DataFrameBytes extends FrameBytes
{
    private static final Logger LOG = Log.getLogger(DataFrameBytes.class);
    private ByteBuffer buffer;

    public DataFrameBytes(AbstractWebSocketConnection connection, Frame frame)
    {
        super(connection,frame);
    }

    @Override
    public void succeeded()
    {
        if (LOG.isDebugEnabled())
        {
            LOG.debug("completed() - frame.remaining() = {}",frame.remaining());
        }

        connection.getBufferPool().release(buffer);

        if (frame.remaining() > 0)
        {
            LOG.debug("More to send");
            // We have written a partial frame per windowing size.
            // We need to keep the correct ordering of frames, to avoid that another
            // Data frame for the same stream is written before this one is finished.
            connection.getQueue().prepend(this);
            connection.complete(this);
        }
        else
        {
            LOG.debug("Send complete");
            super.succeeded();
        }
        connection.flush();
    }

    @Override
    public ByteBuffer getByteBuffer()
    {
        try
        {
            int windowSize = connection.getInputBufferSize();
            buffer = connection.getGenerator().generate(windowSize,frame);
            return buffer;
        }
        catch (Throwable x)
        {
            failed(x);
            return null;
        }
    }
}
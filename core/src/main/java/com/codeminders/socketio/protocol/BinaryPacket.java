package com.codeminders.socketio.protocol;

import java.io.InputStream;
import java.util.Collection;

/**
 * @author Alexander Sova (bird@codeminders.com)
 */
public interface BinaryPacket
{
    Collection<InputStream> getAttachments();

    /**
     * @return true when all expected attachment arrived, false otherwise
     */
    boolean isComplete();

    /**
     * This method to be called when new attachement arrives to the socket
     *
     * @param attachment new attachment
     */
    void addAttachment(InputStream attachment);

    SocketIOPacket.Type getType();

    Object[] getArgs();

    void setArgs(Object[] args);
}

package com.codeminders.socketio.protocol;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Alexander Sova (bird@codeminders.com)
 */
public class BinaryEventPacket extends EventPacket implements BinaryPacket
{
    private List<InputStream> attachments;
    private int               number_attachments_expected;

    /**
     * This constructor suppose to be called by parser when new packet arrived
     *
     * @param id packet id. Used for ACK
     * @param name event name
     * @param args event arguments as array of POJOs to be converted to
     *             JSON with {@link com.codeminders.socketio.util.JSON }.
     * @param number_attachments_expected number of binary attachment expected to be attached to this packed
     */
    BinaryEventPacket(int id, String name, Object[] args, int number_attachments_expected)
    {
        super(Type.BINARY_EVENT, id, name, args);

        this.number_attachments_expected = number_attachments_expected;
        this.attachments = new ArrayList<>(number_attachments_expected);
    }

    /**
     * This constructor suppose to be called by user by emit() call
     *
     * @param name event name
     * @param args event arguments as array of POJOs to be converted to
     *             JSON with {@link com.codeminders.socketio.util.JSON }.
     *             {@link java.io.InputStream} to be used for binary objects
     */
    public BinaryEventPacket(int id, String name, Object[] args)
    {
        super(Type.BINARY_EVENT, id, name, null);

        attachments = new LinkedList<>();

        // We know that extractBinaryObjects does not change the structure of the object,
        // so we can safely case it to Object[]
        setArgs( (Object[])SocketIOProtocol.extractBinaryObjects(args, attachments) );
    }

    public Collection<InputStream> getAttachments()
    {
        return attachments;
    }

    @Override
    protected String getPrefix()
    {
        return String.valueOf(attachments.size()) + SocketIOProtocol.ATTACHMENTS_DELIMITER;
    }

    /**
     * @return true when all expected attachment arrived, false otherwise
     */
    public boolean isComplete()
    {
        return number_attachments_expected == 0;
    }

    /**
     * This method to be called when new attachement arrives to the socket
     *
     * @param attachment new attachment
     */
    public void addAttachment(InputStream attachment)
    {
        attachments.add(attachment);
        number_attachments_expected -= 1;
    }
}

package com.codeminders.socketio.protocol;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Alexander Sova (bird@codeminders.com)
 */
public class BinaryACKPacket extends ACKPacket implements BinaryPacket
{
    //TODO: refactor to avoid code duplication. use delegate object
    private List<InputStream> attachments;
    private int               number_attachments_expected;

    public BinaryACKPacket(int id, Object[] args)
    {
        super(Type.BINARY_ACK, id, null);

        attachments = new LinkedList<>();

        setArgs((Object[])SocketIOProtocol.extractBinaryObjects(args, attachments));
    }

    public BinaryACKPacket(int id, Object[] args, int number_attachments_expected)
    {
        super(Type.BINARY_ACK, id, args);

        this.number_attachments_expected = number_attachments_expected;
        this.attachments = new ArrayList<>(number_attachments_expected);
    }

    @Override
    protected String getPrefix()
    {
        return String.valueOf(attachments.size()) + SocketIOProtocol.ATTACHMENTS_DELIMITER;
    }

    public Collection<InputStream> getAttachments()
    {
        return attachments;
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

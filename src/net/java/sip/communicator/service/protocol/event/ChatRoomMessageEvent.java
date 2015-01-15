package net.java.sip.communicator.service.protocol.event;

import net.java.sip.communicator.service.protocol.ChatRoom;
import net.java.sip.communicator.service.protocol.Message;

import java.util.EventObject;

public class ChatRoomMessageEvent
        extends EventObject
{

    private final Message message;

    public ChatRoomMessageEvent(ChatRoom source, Message message)
    {
        super(source);
        this.message = message;
    }

    public ChatRoom getSourceChatRoom()
    {
        return (ChatRoom) getSource();
    }

    public Message getMessage()
    {
        return message;
    }
}

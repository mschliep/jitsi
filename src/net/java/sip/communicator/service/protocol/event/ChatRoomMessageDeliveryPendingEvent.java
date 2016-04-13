/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import net.java.sip.communicator.service.protocol.ChatRoom;
import net.java.sip.communicator.service.protocol.Message;

/**
 * <tt>MessageDeliveryPendingEvent</tt>s are used to transform messages before
 * sending them to the room.
 *
 * @author Mike Schliep
 */
public class ChatRoomMessageDeliveryPendingEvent
    extends ChatRoomMessageEvent
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;


     /**
      * Creates a <tt>MessageDeliveryPendingEvent</tt> representing delivery of the
      * <tt>source</tt> message to the specified <tt>ChatRoom</tt>.
      *  @param source the <tt>ChatRoom</tt> which triggered this event.
      * @param message the message that triggered this event.
      */
     public ChatRoomMessageDeliveryPendingEvent(ChatRoom source,
                                                Message message)
     {
         super(source, message);
     }

     /**
      * Returns the <tt>ChatRoom</tt> that triggered this event.
      * @return the <tt>ChatRoom</tt> that triggered this event.
      */
     public ChatRoom getSourceChatRoom()
     {
         return (ChatRoom) getSource();
     }
}

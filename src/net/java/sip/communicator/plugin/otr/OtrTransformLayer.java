/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.otr;

import net.java.gotr4j.*;
import net.java.gotr4j.crypto.*;
import net.java.gotr4j.util.*;
import net.java.otr4j.*;
import net.java.otr4j.io.*;
import net.java.sip.communicator.plugin.otr.OtrContactManager.OtrContact;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.Logger;

import java.io.IOException;

/**
 * The Off-the-Record {@link TransformLayer} implementation.
 *
 * @author George Politis
 */
public class OtrTransformLayer
    implements TransformLayer
{

    private static final Logger logger =
            Logger.getLogger(OtrTransformLayer.class);

    private final ScGotrSessionManager gotrSessionManager;

    public OtrTransformLayer(ScGotrSessionManager gotrSessionManager)
    {
        this.gotrSessionManager = gotrSessionManager;
    }

    /*
     * Implements TransformLayer#messageDelivered(MessageDeliveredEvent).
     */
    public MessageDeliveredEvent messageDelivered(MessageDeliveredEvent evt)
    {
        if(logger.isDebugEnabled())
        {
            logger.debug(String.format("Delivered %s: %s",
                    evt.getDestinationContact().getDisplayName(),
                    evt.getSourceMessage().getContent()));
        }
        try
        {
            SessionID gotrSessionID = GotrUtil.getEncodedMessageSessionID(evt.getSourceMessage().getContent());
            if(gotrSessionID != null)
            {
                ScGotrSessionHost host = gotrSessionManager.getGotrSessionHost(evt.getProtocolProvider(), gotrSessionID);
                if(host != null &&
                        host.sentP2pMessage(
                                evt.getSourceMessage().getMessageUID()))
                {
                    return null;
                }
            }
        } catch (IOException e)
        {
        }
        Contact contact = evt.getDestinationContact();
        OtrContact otrContact =
            OtrContactManager.getOtrContact(contact, evt.getContactResource());

        OtrPolicy policy = OtrActivator.scOtrEngine.getContactPolicy(contact);
        ScSessionStatus sessionStatus =
            OtrActivator.scOtrEngine.getSessionStatus(otrContact);
        // If OTR is disabled and we are not over an encrypted session, don't
        // process anything.
        if (!policy.getEnableManual()
            && sessionStatus != ScSessionStatus.ENCRYPTED
            && sessionStatus != ScSessionStatus.FINISHED)
            return evt;

        if (OtrActivator.scOtrEngine.isMessageUIDInjected(evt
            .getSourceMessage().getMessageUID()))
            // If this is a message otr4j injected earlier, don't display it,
            // this may have to change when we add support for fragmentation..
            return null;
        else
            return evt;
    }

    /*
     * Implements
     * TransformLayer#messageDeliveryFailed(MessageDeliveryFailedEvent).
     */
    public MessageDeliveryFailedEvent messageDeliveryFailed(
        MessageDeliveryFailedEvent evt)
    {
        if(logger.isDebugEnabled())
        {
            logger.debug(String.format("Failed %s: %s",
                    evt.getDestinationContact().getDisplayName(),
                    evt.getSourceMessage().getContent()));
        }
        return evt;
    }

    /*
     * Implements TransformLayer#messageDeliveryPending(MessageDeliveredEvent).
     */
    public MessageDeliveryPendingEvent[] messageDeliveryPending(
        MessageDeliveryPendingEvent evt)
    {
        if(logger.isDebugEnabled())
        {
            logger.debug(String.format("Pending %s: %s",
                    evt.getDestinationContact().getDisplayName(),
                    evt.getSourceMessage().getContent()));
        }
        try
        {
            SessionID gotrSessionID = GotrUtil.getEncodedMessageSessionID(evt.getSourceMessage().getContent());
            if(gotrSessionID != null)
            {
                ScGotrSessionHost host = gotrSessionManager.getGotrSessionHost(evt.getProtocolProvider(), gotrSessionID);
                if(host != null &&
                        host.sentP2pMessage(
                                evt.getSourceMessage().getMessageUID()))
                {
                    evt.setMessageEncrypted(true);
                    return new MessageDeliveryPendingEvent[] {evt};
                }
            }
        } catch (IOException e)
        {
        }
        Contact contact = evt.getDestinationContact();
        OtrContact otrContact =
            OtrContactManager.getOtrContact(contact, evt.getContactResource());

        OtrPolicy policy = OtrActivator.scOtrEngine.getContactPolicy(contact);
        ScSessionStatus sessionStatus =
            OtrActivator.scOtrEngine.getSessionStatus(otrContact);
        // If OTR is disabled and we are not over an encrypted session, don't
        // process anything.
        if (!policy.getEnableManual()
            && sessionStatus != ScSessionStatus.ENCRYPTED
            && sessionStatus != ScSessionStatus.FINISHED)
            return new MessageDeliveryPendingEvent[] {evt};
        // If this is a message otr4j injected earlier, return the event as is.
        if (OtrActivator.scOtrEngine.isMessageUIDInjected(evt
            .getSourceMessage().getMessageUID()))
            return new MessageDeliveryPendingEvent[] {evt};

        // Process the outgoing message.
        String msgContent = evt.getSourceMessage().getContent();
        String[] processedMessageContent =
            OtrActivator.scOtrEngine.transformSending(otrContact, msgContent);

        if (processedMessageContent == null
            || processedMessageContent.length <= 0
            || processedMessageContent[0].length() < 1)
            return new MessageDeliveryPendingEvent[0];

        if (processedMessageContent.length == 1
            && processedMessageContent[0].equals(msgContent))
            return new MessageDeliveryPendingEvent[] {evt};

        final MessageDeliveryPendingEvent[] processedEvents =
            new MessageDeliveryPendingEvent[processedMessageContent.length];
        for (int i = 0; i < processedMessageContent.length; i++)
        {
            final String fragmentContent = processedMessageContent[i];
            // Forge a new message based on the new contents.
            OperationSetBasicInstantMessaging imOpSet =
                contact.getProtocolProvider().getOperationSet(
                    OperationSetBasicInstantMessaging.class);
            Message processedMessage =
                imOpSet.createMessage(fragmentContent, evt
                    .getSourceMessage().getContentType(), evt
                    .getSourceMessage().getEncoding(), evt.getSourceMessage()
                    .getSubject());

            // Create a new event and return.
            final MessageDeliveryPendingEvent processedEvent =
                new MessageDeliveryPendingEvent(processedMessage, contact,
                    evt.getTimestamp());

            if (processedMessage.getContent().contains(
                SerializationConstants.HEAD))
            {
                processedEvent.setMessageEncrypted(true);
            }

            processedEvents[i] = processedEvent;
        }

        return processedEvents;
    }

    /*
     * Implements TransformLayer#messageReceived(MessageReceivedEvent).
     */
    public MessageReceivedEvent messageReceived(MessageReceivedEvent evt)
    {
        if(logger.isDebugEnabled())
        {
            logger.debug(String.format("Received %s: %s",
                    evt.getSourceContact().getAddress(),
                    evt.getSourceMessage().getContent()));
        }

        try
        {
            SessionID gotrSessionID = GotrUtil.getEncodedMessageSessionID(
                    evt.getSourceMessage().getContent());
            if(gotrSessionID != null)
            {
                return gotrP2PMessageReceived(gotrSessionID, evt);
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        }

        Contact contact = evt.getSourceContact();
        OtrContact otrContact =
            OtrContactManager.getOtrContact(contact, evt.getContactResource());

        // Process the incoming message.
        String msgContent = evt.getSourceMessage().getContent();

        String processedMessageContent =
            OtrActivator.scOtrEngine.transformReceiving(otrContact, msgContent);

        if (processedMessageContent == null
            || processedMessageContent.length() < 1)
            return null;

        if (processedMessageContent.equals(msgContent))
            return evt;

        // Forge a new message based on the new contents.
        OperationSetBasicInstantMessaging imOpSet =
            contact.getProtocolProvider().getOperationSet(
                OperationSetBasicInstantMessaging.class);
        Message processedMessage =
            imOpSet.createMessageWithUID(
                processedMessageContent,
                evt.getSourceMessage().getContentType(),
                evt.getSourceMessage().getMessageUID());

        // Create a new event and return.
        MessageReceivedEvent processedEvent =
            new MessageReceivedEvent(processedMessage, contact, evt
                .getContactResource(), evt.getTimestamp(),
                evt.getCorrectedMessageUID());

        return processedEvent;
    }

    /**
     * Handle private messages for GOTR.
     *
     * @param gotrSessionID
     * @param evt
     * @return
     */
    private MessageReceivedEvent gotrP2PMessageReceived(SessionID gotrSessionID, MessageReceivedEvent evt)
    {
        if(logger.isDebugEnabled())
        {
            logger.debug(String.format("Received GOTR p2p message: %s: %s",
                    evt.getSourceContact().getAddress(),
                    evt.getSourceMessage().getContent()));
        }
        final ScGotrSessionHost host = gotrSessionManager.getGotrSessionHost(
                evt.getSourceContact().getProtocolProvider(), gotrSessionID);
        if(host == null)
        {
            logger.debug("host is null");
            return evt;
        }
        final GotrUser source = host.getUser(evt.getSourceContact());
        final GotrSessionManager session = host.getSession();
        try
        {
            logger.debug(String.format("handleing message %s: %s", source, session));
            session.handleMessage(source, evt.getSourceMessage().getContent());
            logger.debug("post handleing message");
        } catch (GotrException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public ChatRoomMessageReceivedEvent chatRoomMessageReceived
            (ChatRoomMessageReceivedEvent evt)
    {
        if(logger.isDebugEnabled())
        {
            logger.debug(String.format("Received %s:%s", evt.getSourceChatRoomMember().getName(), evt.getMessage().getContent()));
        }

        if(evt.getEventType() !=
                ChatRoomMessageReceivedEvent.CONVERSATION_MESSAGE_RECEIVED)
        {
            logger.debug(String.format("MessageType: %s, %s", evt.getEventType(), evt.getMessage().getContent()));
            return evt;
        }

        if(evt.isHistoryMessage()
                && GotrUtil.isGotrBroadcast(evt.getMessage().getContent()))
        {
            logger.debug("Received history gotr message.");
            return null;
        }

        final ChatRoom chatRoom = evt.getSourceChatRoom();
        final ScGotrSessionHost host = gotrSessionManager
                .getGotrSessionHost(chatRoom);

        if(host.receivedChatRoomMessage(evt.getMessage().getMessageUID()))
        {
            logger.debug("Received message with null host.");
            return evt;
        }

        final GotrUser source = host.getUser(evt.getSourceChatRoomMember());

        final GotrSessionManager session = host.getSession();

        if(source == null){
            logger.debug("Received message with null source.");
            if(GotrUtil.isGotrBroadcast(evt.getMessage().getContent())) {
                return null;
            }
            else{
                logger.debug("Received message with null source. not broadcast.");
                return evt;
            }
        }

        try
        {
            session.handleBroadcast(source, evt.getMessage().getContent());
        } catch (GotrException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public ChatRoomMessageDeliveryPendingEvent chatRoomMessageDeliveryPending(
            ChatRoomMessageDeliveryPendingEvent evt)
    {
        final ScGotrSessionHost host =
                gotrSessionManager.getGotrSessionHost(evt.getSourceChatRoom());

        if(host == null){
            logger.debug(String.format("Host is null, %s", evt));
            return evt;
        }

        if(logger.isDebugEnabled())
        {
            logger.debug(String.format("Delivery Pending %s:%s", host.getLocalUser(), evt.getMessage().getContent()));
        }


        if(host.sentChatRoomMessage(evt.getMessage().getMessageUID()))
        {
            return evt;
        }
        else
        {
            try
            {
                host.getSession().broadcastMessage(evt.getMessage().getContent());
            } catch (GotrException e)
            {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public ChatRoomMessageDeliveredEvent chatRoomMessageDelivered(
            ChatRoomMessageDeliveredEvent evt)
    {

        if(evt.getEventType() !=
                ChatRoomMessageDeliveredEvent.CONVERSATION_MESSAGE_DELIVERED)
        {
            return evt;
        }

        if(evt.isHistoryMessage()
                && GotrUtil.isGotrBroadcast( evt.getMessage().getContent()))
        {
            return null;
        }

        final ScGotrSessionHost host =
                gotrSessionManager.getGotrSessionHost(evt.getSourceChatRoom());

        if(logger.isDebugEnabled())
        {
            logger.debug(String.format("Delivered %s:%s", host.getLocalUser(), evt.getMessage().getContent()));
        }

        if(host.receivedChatRoomMessage(evt.getMessage().getMessageUID()))
        {
            return evt;
        }

        try
        {
            host.getSession().handleBroadcast(host.getLocalUser(),
                    evt.getMessage().getContent());
        } catch (GotrException e)
        {
            e.printStackTrace();
        }

        return null;
    }
}

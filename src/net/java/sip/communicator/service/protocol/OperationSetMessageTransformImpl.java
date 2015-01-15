/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import net.java.sip.communicator.service.protocol.event.*;

import java.util.*;

/**
 * @author George Politis
 */
public class OperationSetMessageTransformImpl
    implements OperationSetMessageTransform
{
    public final Map<Integer, Vector<TransformLayer>> transformLayers
        = new TreeMap<Integer, Vector<TransformLayer>>();

    private static final int defaultPriority = 1;

    public void addTransformLayer(TransformLayer transformLayer)
    {
        this.addTransformLayer(defaultPriority, transformLayer);
    }

    public void addTransformLayer(int priority, TransformLayer transformLayer)
    {
        synchronized (transformLayers)
        {
            if (!transformLayers.containsKey(defaultPriority))
                transformLayers.put(defaultPriority,
                    new Vector<TransformLayer>());

            transformLayers.get(defaultPriority).add(transformLayer);
        }
    }

    public boolean containsLayer(TransformLayer layer)
    {
        synchronized (transformLayers)
        {
            for (Map.Entry<Integer, Vector<TransformLayer>> entry
                    : transformLayers.entrySet())
            {
                if (entry.getValue().contains(layer))
                    return true;
            }

        }
        return false;
    }

    public void removeTransformLayer(TransformLayer transformLayer)
    {
        synchronized (transformLayers)
        {
            for (Map.Entry<Integer, Vector<TransformLayer>> entry
                    : transformLayers.entrySet())
            {
                entry.getValue().remove(transformLayer);
            }
        }
    }

    public MessageEvent[] transform(MessageEvent event)
    {
        LinkedList<MessageEvent> current = new LinkedList<MessageEvent>();
        current.add(event);
        final LinkedList<MessageEvent> next = new LinkedList<MessageEvent>();

        synchronized (transformLayers)
        {
            for(Map.Entry<Integer, Vector<TransformLayer>> entry:
                    transformLayers.entrySet())
            {
                for(TransformLayer transform: entry.getValue())
                {
                    next.clear();
                    for (MessageEvent currentEvent: current)
                    {
                        next.addAll(Arrays.asList(
                                transform(transform, currentEvent)));
                    }
                    current = new LinkedList<MessageEvent>(next);
                }
            }
        }
        return current.toArray(new MessageEvent[current.size()]);
    }

    private MessageEvent[] transform(TransformLayer transform, MessageEvent event)
    {
        if(event instanceof MessageDeliveredEvent)
        {
            MessageEvent transformed = transform.messageDelivered(
                    (MessageDeliveredEvent) event);
            if(transformed != null)
            {
                return new MessageEvent[] {transformed};
            }
            else
            {
                return new MessageEvent[0];
            }
        }
        else if(event instanceof MessageReceivedEvent)
        {
            MessageEvent transformed = transform.messageReceived(
                    (MessageReceivedEvent) event);
            if(transformed != null)
            {
                return new MessageEvent[] {transformed};
            }
            else
            {
                return new MessageEvent[0];
            }
        }
        else if(event instanceof MessageDeliveryFailedEvent)
        {
            MessageEvent transformed = transform.messageDeliveryFailed(
                    (MessageDeliveryFailedEvent) event);
            if(transformed != null)
            {
                return new MessageEvent[] {transformed};
            }
            else
            {
                return new MessageEvent[0];
            }
        }
        else if(event instanceof MessageDeliveryPendingEvent)
        {
            return transform.messageDeliveryPending(
                    (MessageDeliveryPendingEvent) event);
        }
        else
        {
            return new MessageEvent[] {event};
        }
    }

    public ChatRoomMessageEvent[] transformChatRoomMessage(ChatRoomMessageEvent event)
    {
        LinkedList<ChatRoomMessageEvent> current = new LinkedList<ChatRoomMessageEvent>();
        current.add(event);
        final LinkedList<ChatRoomMessageEvent> next = new LinkedList<ChatRoomMessageEvent>();

        synchronized (transformLayers)
        {
            for(Map.Entry<Integer, Vector<TransformLayer>> entry:
                    transformLayers.entrySet())
            {
                for(TransformLayer transform: entry.getValue())
                {
                    next.clear();
                    for (ChatRoomMessageEvent currentEvent: current)
                    {
                        next.addAll(Arrays.asList(
                                transformChatRoomMessage(transform, currentEvent)));
                    }
                    current = new LinkedList<ChatRoomMessageEvent>(next);
                }
            }
        }
        return current.toArray(new ChatRoomMessageEvent[current.size()]);
    }

    private ChatRoomMessageEvent[] transformChatRoomMessage(TransformLayer transform, ChatRoomMessageEvent event)
    {
        if(event instanceof ChatRoomMessageDeliveredEvent)
        {
            ChatRoomMessageEvent transformed = transform.chatRoomMessageDelivered(
                    (ChatRoomMessageDeliveredEvent) event);
            if(transformed != null)
            {
                return new ChatRoomMessageEvent[] {transformed};
            }
            else
            {
                return new ChatRoomMessageEvent[0];
            }
        }
        else if(event instanceof ChatRoomMessageReceivedEvent)
        {
            ChatRoomMessageEvent transformed = transform.chatRoomMessageReceived(
                    (ChatRoomMessageReceivedEvent) event);
            if(transformed != null)
            {
                return new ChatRoomMessageEvent[] {transformed};
            }
            else
            {
                return new ChatRoomMessageEvent[0];
            }
        }
        else if(event instanceof ChatRoomMessageDeliveryPendingEvent)
        {
            ChatRoomMessageEvent transformed = transform.chatRoomMessageDeliveryPending(
                    (ChatRoomMessageDeliveryPendingEvent) event);
            if(transformed != null)
            {
                return new ChatRoomMessageEvent[] {transformed};
            }
            else
            {
                return new ChatRoomMessageEvent[0];
            }
        }
        else
        {
            return new ChatRoomMessageEvent[] {event};
        }
    }

}

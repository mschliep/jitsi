package net.java.sip.communicator.plugin.otr;

import net.java.gotr4j.SessionID;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.Logger;

import java.util.*;

/**
 * Manages all the GOTR sessions for this client.
 */
public class ScGotrSessionManager
    implements LocalUserChatRoomPresenceListener
{

    private static final Logger logger = Logger.getLogger(ScGotrSessionManager.class);

    private final Map<ChatRoom, ScGotrSessionHost> chatRoom2SessionHost =
            new HashMap<ChatRoom, ScGotrSessionHost>();

    public ScGotrSessionHost getGotrSessionHost(ChatRoom chatRoom)
    {
        synchronized (chatRoom2SessionHost) {
            return chatRoom2SessionHost.get(chatRoom);
        }
    }

    public ScGotrSessionHost getGotrSessionHost(ProtocolProviderService provider,
                                                SessionID sessionID)
    {
        synchronized (chatRoom2SessionHost) {
            for (Map.Entry<ChatRoom, ScGotrSessionHost> entry :
                    chatRoom2SessionHost.entrySet()) {
                ScGotrSessionHost host = entry.getValue();
                if (host.getSessionID() != null
                        && host.getSessionID().equals(sessionID)
                        && host.getProtocolProvider().equals(provider)) {
                    return host;
                }
            }
            return null;
        }
    }

    /**
     * Handle when a new {@link ProtocolProviderService} is added.
     *
     * Adds itself to the {@link OperationSetMultiUserChat} as a
     * {@link LocalUserChatRoomPresenceListener}.
     *
     * Adds a {@link ScGotrSessionHost} for each joined {@link ChatRoom}.
     *
     * @param provider provider that was added
     */
    public void protocolProviderAdded(ProtocolProviderService provider)
    {
        OperationSetMultiUserChat opMUC = provider.getOperationSet(
                OperationSetMultiUserChat.class);
        if(opMUC != null)
        {
            opMUC.addPresenceListener(this);
            for(ChatRoom room: opMUC.getCurrentlyJoinedChatRooms()){
                chatRoomJoined(room);
            }
        }

    }

    /**
     * Handle when a {@link ProtocolProviderService} is removed.
     *
     * Removes itself from the {@link OperationSetMultiUserChat} as a
     * {@link LocalUserChatRoomPresenceListener}.
     *
     * @param provider provider that was removed
     */
    public void protocolProviderRemoved(ProtocolProviderService provider)
    {
        OperationSetMultiUserChat opMUC = provider.getOperationSet(
                OperationSetMultiUserChat.class);
        if(opMUC != null)
        {
            opMUC.removePresenceListener(this);
        }
    }

    /**
     * Creates a {@link ScGotrSessionHost} for newly joined {@link ChatRoom}s.
     *
     * @param chatRoom newly joined chat room
     */
    private void chatRoomJoined(ChatRoom chatRoom)
    {
        synchronized (chatRoom2SessionHost) {
            ScGotrSessionHost host = new ScGotrSessionHost(chatRoom);
            chatRoom2SessionHost.put(chatRoom, host);
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("%s chat rooms", chatRoom2SessionHost.size()));
            }
        }
    }

    /**
     * Closes the {@link ScGotrSessionHost} for closed {@link ChatRoom}s.
     *
     * @param chatRoom closed chat room
     */
    private void chatRoomClosed(ChatRoom chatRoom)
    {
        synchronized (chatRoom2SessionHost) {
            ScGotrSessionHost host = chatRoom2SessionHost.remove(chatRoom);
            if (host != null) {
                host.close();
            }
        }
    }

    @Override
    public void localUserPresenceChanged(LocalUserChatRoomPresenceChangeEvent
                                                     evt)
    {
        if(evt.getEventType().equals(
                LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_JOINED))
        {
            chatRoomJoined(evt.getChatRoom());
        }
        else if(evt.getEventType().equals(
                LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_DROPPED)
                || evt.getEventType().equals(
                LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_KICKED)
                || evt.getEventType().equals(
                LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_LEFT))
        {
            chatRoomClosed(evt.getChatRoom());
        }
    }
}

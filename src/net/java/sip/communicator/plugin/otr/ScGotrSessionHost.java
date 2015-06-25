package net.java.sip.communicator.plugin.otr;

import net.java.gotr4j.*;
import net.java.gotr4j.crypto.GotrException;
import net.java.sip.communicator.plugin.otr.authdialog.GotrMemberAuthDialogBackend;
import net.java.sip.communicator.plugin.otr.authdialog.SmpAuthenticateBuddyDialog;

import net.java.sip.communicator.plugin.otr.authdialog.SmpProgressDialog;
import net.java.sip.communicator.plugin.otr.gui.*;
import net.java.sip.communicator.service.gui.Chat;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.Logger;

import java.security.KeyPair;
import java.security.PublicKey;
import java.util.*;

public class ScGotrSessionHost
        implements GotrSessionHost,
        ChatRoomMemberPresenceListener
{

    private static final String FORGE_ME = "FORGE_ME";
    private static final String FORGE_OTHER = "FORGE_OTHER";

    private static final Logger logger = Logger.getLogger(ScGotrSessionHost.class);

    private final ChatRoom chatRoom;
    private final ProtocolProviderService protocolProvider;
    private final OperationSetBasicInstantMessaging imOpSet;

    private GotrSessionManager gotrSession;

    private GotrUser localUser;

    private boolean errorInSession = false;

    private final Map<GotrUser, Contact> userToContactMap =
            new HashMap<GotrUser, Contact>();

    private final Map<Contact, GotrUser> contactToUserMap =
            new HashMap<Contact, GotrUser>();

    private final Map<ChatRoomMember, GotrUser> memberToUserMap =
            new HashMap<ChatRoomMember, GotrUser>();

    private final Map<GotrUser, ChatRoomMember> userToMemberMap =
            new HashMap<GotrUser, ChatRoomMember>();

    private final Map<GotrUser, SmpProgressDialog> progressDialogMap = new HashMap<GotrUser, SmpProgressDialog>();

    private final Set<String> sentBroadcasts = new HashSet<String>();
    private final Set<String> receivedBroadcasts = new HashSet<String>();
    private final Set<String> sentP2pMessages = new HashSet<String>();

    private final List<ScGotrSessionListener> listeners =
            new ArrayList<ScGotrSessionListener>();

    public ScGotrSessionHost(ChatRoom chatRoom) {
        this.chatRoom = chatRoom;
        this.protocolProvider = chatRoom.getParentProvider();
        this.imOpSet = protocolProvider.getOperationSet(
                OperationSetBasicInstantMessaging.class);

        this.localUser = new GotrUser(chatRoom.getPrivateContactByNickname(
                chatRoom.getUserNickname()).getAddress());
        try {
            this.gotrSession = new GotrSessionManager(this, localUser, chatRoom.getName(), true);
        } catch (GotrException e) {
            this.gotrSession = null;
            errorInSession = true;
        }

        chatRoom.addMemberPresenceListener(this);

        for (ChatRoomMember member : chatRoom.getMembers())
        {
            if(!member.getName().equals(chatRoom.getUserNickname()))
            {
                memberAdded(member);
            }
        }
    }

    @Override
    public void sendMessage(GotrUser gotrUser, String message)
    {
        if(logger.isDebugEnabled())
        {
            logger.debug(String.format("Host: %s sending %s:%s.", localUser, gotrUser, message));
        }

        final Contact contact = userToContactMap.get(gotrUser);

        Message iMessage = imOpSet.createMessage(message);
        sentP2pMessages.add(iMessage.getMessageUID());
        imOpSet.sendInstantMessage(contact, iMessage);
    }

    @Override
    public void sendBroadcast(String broadcast)
    {
        if(logger.isDebugEnabled())
        {
            logger.debug(String.format("Sending %s: %s", localUser, broadcast));
            logger.debug(String.format("size %s: %d", localUser, gotrSession.getSize()));
        }
        Message message = chatRoom.createMessage(broadcast);
        sentBroadcasts.add(message.getMessageUID());
        try {
            chatRoom.sendMessage(message);
        } catch (OperationFailedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handleBroadcast(GotrUser source, String broadcast)
    {
        if(logger.isDebugEnabled()){
            logger.debug(String.format("%s: %s", source, broadcast));
            logger.debug(String.format("size %s: %d", localUser, gotrSession.getSize()));
        }

        if(broadcast.startsWith(FORGE_ME)){
            receivedUnsentMessage(localUser);
            return;
        }
        else if(broadcast.startsWith(FORGE_OTHER)){
            receivedUnsentMessage(source);
            return;
        }

        ChatRoomMessageEvent event;
        final Message message = chatRoom.createMessage(broadcast);
        receivedBroadcasts.add(message.getMessageUID());

        if(source.equals(localUser))
        {
            event = new ChatRoomMessageDeliveredEvent(chatRoom,
                    new Date(),
                    message,
                    ChatRoomMessageDeliveredEvent.CONVERSATION_MESSAGE_DELIVERED);
        }
        else
        {
            final ChatRoomMember member = userToMemberMap.get(source);
            event = new ChatRoomMessageReceivedEvent(chatRoom, member,
                    new Date(),
                    message,
                    ChatRoomMessageReceivedEvent.CONVERSATION_MESSAGE_RECEIVED);
        }
        chatRoom.fireMessageEvent(event);
    }

    @Override
    public KeyPair getLocalKeyPair()
    {
        AccountID accountID = protocolProvider.getAccountID();
        KeyPair keyPair =
                OtrActivator.scOtrKeyManager.loadKeyPair(accountID);
        if (keyPair == null)
            OtrActivator.scOtrKeyManager.generateKeyPair(accountID);

        return OtrActivator.scOtrKeyManager.loadKeyPair(accountID);
    }

    @Override
    public void verify(GotrUser user, String fingerprint) {
        ChatRoomMember member = userToMemberMap.get(user);
        OtrActivator.scOtrKeyManager.verify("", fingerprint);
        OtrActivator.gotrComponentService.update(chatRoom);

        SmpProgressDialog dialog = getSmpProgressDialog(user);
        dialog.setProgressSuccess();
        dialog.setVisible(true);
    }

    @Override
    public void stateChanged(GotrSessionState gotrSessionState)
    {
        GotrComponentServiceImpl componentService =
                OtrActivator.gotrComponentService;
        componentService.update(chatRoom);

        postStateMessageToChat(gotrSessionState);

        synchronized (listeners)
        {
            for(ScGotrSessionListener listener: listeners)
            {
                listener.statusChanged(this);
            }
        }
    }

    private void postStateMessageToChat(GotrSessionState state) {

        String message = null;
        String messageType = null;

        if(state == GotrSessionState.PLAINTEXT){
            return;
        }
        else if(state == GotrSessionState.SETUP){
            message = OtrActivator.resourceService
                    .getI18NString("plugin.otr.gotr.SETUP_STATE", new String[]{});
            messageType = Chat.SYSTEM_MESSAGE;
        }
        else if(state == GotrSessionState.SECURE){
            if(areAllAuthenticated()) {
                message = OtrActivator.resourceService
                        .getI18NString("plugin.otr.gotr.SECURE_AUTH_STATE", new String[]{});
                messageType = Chat.SYSTEM_MESSAGE;
            }
            else {
                message = OtrActivator.resourceService
                        .getI18NString("plugin.otr.gotr.SECURE_NO_AUTH_STATE", new String[]{});
                messageType = Chat.ERROR_MESSAGE;
            }
        }

        OtrActivator.uiService.getChat(chatRoom).addMessage(chatRoom.getName(), new Date(),
                messageType, message,
                OperationSetBasicInstantMessaging.DEFAULT_MIME_TYPE);


    }

    @Override
    public void askForSecret(GotrUser user, String question) {
        ChatRoomMember member = userToMemberMap.get(user);
        SmpAuthenticateBuddyDialog dialog =
                new SmpAuthenticateBuddyDialog(question, new GotrMemberAuthDialogBackend(this, member, user));
        dialog.setVisible(true);

        SmpProgressDialog progressDialog = getSmpProgressDialog(user);
        progressDialog.init();
        progressDialog.setVisible(true);
    }

    @Override
    public void smpAborted(GotrUser user) {
        SmpProgressDialog dialog = getSmpProgressDialog(user);
        dialog.setProgressFail();
        dialog.setVisible(true);
    }

    @Override
    public void smpError(GotrUser user) {
        SmpProgressDialog dialog = getSmpProgressDialog(user);
        dialog.setProgressFail();
        dialog.setVisible(true);
    }

    @Override
    public void unverify(GotrUser user, String fingerprint) {
        OtrActivator.scOtrKeyManager.unverify("", fingerprint);
        OtrActivator.gotrComponentService.update(chatRoom);

        SmpProgressDialog dialog = getSmpProgressDialog(user);
        dialog.setProgressFail();
        dialog.setVisible(true);
    }

    @Override
    public void sessionFinished(GotrUser user)
    {
        String finished = OtrActivator.resourceService
                .getI18NString("plugin.otr.gotr.FINISHED", new String[]{getName(user)});
        OtrActivator.uiService.getChat(chatRoom).addMessage(chatRoom.getName(), new Date(),
                Chat.ERROR_MESSAGE, finished,
                OperationSetBasicInstantMessaging.DEFAULT_MIME_TYPE);
    }

    @Override
    public void unrecoverableError(GotrUser user)
    {
        String finished = OtrActivator.resourceService
                .getI18NString("plugin.otr.gotr.UNRECOVERABLE_ERROR", new String[]{getName(user)});
        OtrActivator.uiService.getChat(chatRoom).addMessage(chatRoom.getName(), new Date(),
                Chat.ERROR_MESSAGE, finished,
                OperationSetBasicInstantMessaging.DEFAULT_MIME_TYPE);
    }

    @Override
    public void recoverableError(GotrUser user)
    {
        String finished = OtrActivator.resourceService
                .getI18NString("plugin.otr.gotr.RECOVERABLE_ERROR", new String[]{getName(user)});
        OtrActivator.uiService.getChat(chatRoom).addMessage(chatRoom.getName(), new Date(),
                Chat.ERROR_MESSAGE, finished,
                OperationSetBasicInstantMessaging.DEFAULT_MIME_TYPE);
    }

    @Override
    public void receivedUnsentMessage(GotrUser source) {
        String finished = OtrActivator.resourceService
                .getI18NString("plugin.otr.gotr.RECEIVED_UNSENT_MSG", new String[]{});
        OtrActivator.uiService.getChat(chatRoom).addMessage(chatRoom.getName(), new Date(),
                Chat.ERROR_MESSAGE, finished,
                OperationSetBasicInstantMessaging.DEFAULT_MIME_TYPE);
    }

    @Override
    public void broadcastToEmptySecureRoom(String message) {
        String finished = OtrActivator.resourceService
                .getI18NString("plugin.otr.gotr.EMPTY_SECURE_BROADCAST", new String[]{});
        OtrActivator.uiService.getChat(chatRoom).addMessage(chatRoom.getName(), new Date(),
                Chat.ERROR_MESSAGE, finished,
                OperationSetBasicInstantMessaging.DEFAULT_MIME_TYPE);
    }

    private String getName(GotrUser user)
    {
        if(user.equals(localUser))
        {
            return chatRoom.getUserNickname();
        }
        else{
            ChatRoomMember member = userToMemberMap.get(user);
            return member.getName();
        }
    }

    public GotrSessionManager getSession()
    {
        return gotrSession;
    }

    /**
     * Called when a new {@link ChatRoomMember} is added to the {@link ChatRoom}.
     *
     * @param member newly added member
     */
    private void memberAdded(ChatRoomMember member)
    {
        if(logger.isDebugEnabled())
        {
            logger.debug(String.format("%s added to %s, %s", member.getName(), chatRoom.getUserNickname(), member.getName().equals(chatRoom.getUserNickname())));
        }
        final Contact contact = chatRoom.getPrivateContactByNickname(
                member.getName());
        final GotrUser user = new GotrUser(contact.getAddress());

        userToContactMap.put(user, contact);
        contactToUserMap.put(contact, user);
        userToMemberMap.put(user, member);
        memberToUserMap.put(member, user);

        try
        {
            this.gotrSession.addUser(user);
        } catch (GotrException e)
        {
            errorInSession = true;
            e.printStackTrace();
        }
    }

    private void memberRemoved(ChatRoomMember member)
    {
        final GotrUser user = memberToUserMap.remove(member);
        final Contact contact = userToContactMap.remove(user);
        contactToUserMap.remove(contact);
        userToMemberMap.remove(user);

        if(logger.isDebugEnabled())
        {
            logger.debug(String.format("%s, %s removed.", user, chatRoom.getUserNickname()));
        }

        try
        {
            this.gotrSession.removeUser(user);
        } catch (GotrException e)
        {
            errorInSession = true;
            e.printStackTrace();
        }
    }

    public GotrUser getUser(ChatRoomMember member)
    {
        return memberToUserMap.get(member);
    }

    /**
     * Should be called when the user leaves the chat room.
     */
    public void close(){
        chatRoom.removeMemberPresenceListener(this);
    }

    @Override
    public void memberPresenceChanged(ChatRoomMemberPresenceChangeEvent evt)
    {
        if (evt.getEventType().equals(
                ChatRoomMemberPresenceChangeEvent.MEMBER_JOINED)
                && !evt.getChatRoomMember().getName()
                .equals(chatRoom.getUserNickname()))
        {
            memberAdded(evt.getChatRoomMember());
        } else if ((evt.getEventType().equals(
                    ChatRoomMemberPresenceChangeEvent.MEMBER_KICKED)
                || evt.getEventType().equals(
                    ChatRoomMemberPresenceChangeEvent.MEMBER_LEFT)
                || evt.getEventType().equals(
                    ChatRoomMemberPresenceChangeEvent.MEMBER_QUIT))
                && chatRoom.getUserNickname() != null
                && !evt.getChatRoomMember().getName().equals(
                    chatRoom.getUserNickname()))
        {
            memberRemoved(evt.getChatRoomMember());
        }
    }


    public GotrUser getUser(Contact contact)
    {
        return contactToUserMap.get(contact);
    }

    public SessionID getSessionID()
    {
        return gotrSession.getSessionID();
    }

    public ProtocolProviderService getProtocolProvider()
    {
        return protocolProvider;
    }

    public boolean sentChatRoomMessage(String messageUID)
    {
        return sentBroadcasts.contains(messageUID);
    }

    public GotrUser getLocalUser()
    {
        return localUser;
    }

    public boolean receivedChatRoomMessage(String messageUID)
    {
        return receivedBroadcasts.contains(messageUID);
    }

    public void addSessionListener(ScGotrSessionListener listener)
    {
        synchronized (listener)
        {
            if(!listeners.contains(listener))
            {
                listeners.add(listener);
            }
        }
    }

    public boolean areAllAuthenticated()
    {
        for(ChatRoomMember member: memberToUserMap.keySet())
        {
            if(!authenticated(member))
            {
                return false;
            }
        }
        return true;
    }

    public boolean sentP2pMessage(String messageUID)
    {
        return sentP2pMessages.contains(messageUID);
    }

    public boolean authenticated(ChatRoomMember member)
    {
        String fingerprint = getRemoteFingerprint(member);

        return OtrActivator.scOtrKeyManager.isVerified(fingerprint);

    }

    public List<ChatRoomMember> getChatRoomMembers() {
        return new ArrayList<ChatRoomMember>(memberToUserMap.keySet());
    }

    public String getRemoteFingerprint(ChatRoomMember member)
    {
        GotrUser user  = memberToUserMap.get(member);
        try
        {
            PublicKey remoteKey = gotrSession.getRemotePublicKey(user);
            if(remoteKey == null)
            {
                return null;
            }
            return OtrActivator.scOtrKeyManager.getFingerprintFromPublicKey(remoteKey);
        } catch (GotrException e)
        {
            logger.error("Unable to get remote users public key", e);
            return null;
        }
    }

    public void initSmp(ChatRoomMember member, String question, String secret)
    {
        GotrUser user = memberToUserMap.get(member);
        try
        {
            gotrSession.initSmp(user, question, secret);
            SmpProgressDialog dialog = getSmpProgressDialog(user);
            dialog.init();
            dialog.setVisible(true);
        } catch (GotrException e)
        {
            logger.error("Unable to init smp with remote user.", e);
        }
    }

    public SmpProgressDialog getSmpProgressDialog(GotrUser user){
        synchronized (progressDialogMap){
            SmpProgressDialog dialog = progressDialogMap.get(user);

            if(dialog == null){

                Contact contact = userToContactMap.get(user);

                dialog = new SmpProgressDialog(contact.getDisplayName());
                progressDialogMap.put(user, dialog);
            }

            return dialog;
        }
    }

    public void respondSmp(GotrUser user, String question, String text) {
        try {
            gotrSession.respondSmp(user, question, text);
            SmpProgressDialog dialog = getSmpProgressDialog(user);
            dialog.incrementProgress();
            dialog.setVisible(true);
        } catch (GotrException e) {
            e.printStackTrace();
        }
    }

    public void abortSmp(GotrUser user) {
        try {
            gotrSession.abortSmp(user);
            SmpProgressDialog dialog = getSmpProgressDialog(user);
            dialog.dispose();
        } catch (GotrException e) {
            e.printStackTrace();
        }
    }
}

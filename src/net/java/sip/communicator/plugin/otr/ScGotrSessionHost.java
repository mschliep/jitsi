package net.java.sip.communicator.plugin.otr;

import net.java.gotr4j.*;
import net.java.gotr4j.crypto.GotrException;
import net.java.hsm.HSMException;
import net.java.sip.communicator.plugin.otr.authdialog.GotrMemberAuthDialogBackend;
import net.java.sip.communicator.plugin.otr.authdialog.SmpAuthenticateBuddyDialog;

import net.java.sip.communicator.plugin.otr.authdialog.SmpProgressDialog;
import net.java.sip.communicator.plugin.otr.gui.*;
import net.java.sip.communicator.service.gui.Chat;
import net.java.sip.communicator.service.gui.ChatLinkClickedListener;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.Logger;

import javax.swing.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ScGotrSessionHost
        implements GotrSessionHost,
        ChatRoomMemberPresenceListener,
        ChatLinkClickedListener,
        ScOtrKeyManagerListener{

    private static final Logger logger = Logger.getLogger(ScGotrSessionHost.class);

    private final ChatRoom chatRoom;
    private final ProtocolProviderService protocolProvider;
    private final OperationSetBasicInstantMessaging imOpSet;

    private GotrSessionManager gotrSession;

    private GotrUser localUser;

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

    private boolean displaySecure = true;

    private volatile int outgoing = 0;

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
            logger.error(e);
        }

        chatRoom.addMemberPresenceListener(this);
        OtrActivator.scOtrKeyManager.addListener(this);

        for (ChatRoomMember member : chatRoom.getMembers()) {
            if (!member.getName().equals(chatRoom.getUserNickname())) {
                logger.debug(String.format("%s adding %s in construct.", chatRoom.getUserNickname(), member.getName()));
                memberAdded(member);
            }
        }

        registerChatLinkListener();
    }

    private void registerChatLinkListener() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    registerChatLinkListener();
                }
            });
            return;
        }

        final Chat chat = OtrActivator.uiService.getChat(chatRoom);
        chat.addChatLinkClickedListener(this);

    }

    @Override
    public void sendMessage(GotrUser gotrUser, String message) {
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Host: %s sending %s:%s.", localUser, gotrUser, message));
        }

        final Contact contact = userToContactMap.get(gotrUser);

        Message iMessage = imOpSet.createMessage(message);
        sentP2pMessages.add(iMessage.getMessageUID());
        imOpSet.sendInstantMessage(contact, iMessage);
    }

    @Override
    public void sendBroadcast(String broadcast) {
        if (logger.isDebugEnabled()) {
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
    public void handleBroadcast(GotrUser source, String broadcast) {
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("%s: %s", source, broadcast));
            logger.debug(String.format("size %s: %d", localUser, gotrSession.getSize()));
        }

        if(broadcast.equals("JUST A MAGIC STRING")){
            receivedUnsentMessage(source);
            return;
        }

        ChatRoomMessageEvent event;
        final Message message = chatRoom.createMessage(broadcast);
        receivedBroadcasts.add(message.getMessageUID());

        if (source.equals(localUser)) {
            event = new ChatRoomMessageDeliveredEvent(chatRoom,
                    new Date(),
                    message,
                    ChatRoomMessageDeliveredEvent.CONVERSATION_MESSAGE_DELIVERED);
            outgoing--;
            if(outgoing < 0){
                outgoing = 0;
            }
            for (ScGotrSessionListener listener : listeners) {
                listener.outgoingMessagesUpdated(this);
            }
        } else {
            final ChatRoomMember member = userToMemberMap.get(source);
            event = new ChatRoomMessageReceivedEvent(chatRoom, member,
                    new Date(),
                    message,
                    ChatRoomMessageReceivedEvent.CONVERSATION_MESSAGE_RECEIVED);
        }
        chatRoom.fireMessageEvent(event);
    }

    @Override
    public KeyPair getLocalKeyPair() {
        AccountID accountID = protocolProvider.getAccountID();
        KeyPair keyPair =
                OtrActivator.scOtrKeyManager.loadKeyPair(accountID);
        if (keyPair == null)
            OtrActivator.scOtrKeyManager.generateKeyPair(accountID);

        return OtrActivator.scOtrKeyManager.loadKeyPair(accountID);
    }

    @Override
    public void verify(GotrUser user, String fingerprint) {
        OtrActivator.scOtrKeyManager.verify("", fingerprint);
        OtrActivator.gotrComponentService.update(chatRoom);

        closeSmpProgressDialog(user);
    }

    @Override
    public void stateChanged(GotrSessionState gotrSessionState) {
        GotrComponentServiceImpl componentService =
                OtrActivator.gotrComponentService;
        componentService.update(chatRoom);

        postStateMessageToChat(gotrSessionState);

        outgoing=0;
        for (ScGotrSessionListener listener : listeners) {
            listener.outgoingMessagesUpdated(this);
        }
        synchronized (listeners) {
            for (ScGotrSessionListener listener : listeners) {
                listener.statusChanged(this);
            }
        }
    }

    private void postStateMessageToChat(final GotrSessionState state) {

        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    postStateMessageToChat(state);
                }
            });
            return;
        }

        String message = null;
        String messageType = null;

        if (state == GotrSessionState.PLAINTEXT) {
            return;
        } else if (state == GotrSessionState.AWAITING_USERS) {
            displaySecure = true;
            message = OtrActivator.resourceService
                    .getI18NString("plugin.otr.gotr.AWAITING_USERS_STATE", new String[]{});
            messageType = Chat.SYSTEM_MESSAGE;
        } else if (state == GotrSessionState.SETUP) {
            displaySecure = true;
            message = OtrActivator.resourceService
                    .getI18NString("plugin.otr.gotr.SETUP_STATE", new String[]{});
            messageType = Chat.SYSTEM_MESSAGE;
        } else if (state == GotrSessionState.SECURE && displaySecure) {
            displaySecure = false;
            if (areAllAuthenticated()) {
                displayAuthMessage();
                return;
            } else {
                displayNoAuthMessage();
                return;
            }
        }
        else{
            return;
        }

        final Chat chat = OtrActivator.uiService.getChat(chatRoom);

        chat.addMessage(chatRoom.getName(), new Date(),
                messageType, message,
                OperationSetBasicInstantMessaging.HTML_MIME_TYPE);
    }

    private void displayNoAuthMessage() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    displayNoAuthMessage();
                }
            });
            return;
        }
        String message = OtrActivator.resourceService
                .getI18NString("plugin.otr.gotr.SECURE_NO_AUTH_STATE", new String[]{
                        ScGotrSessionHost.class.getName(),
                        getFirstUnauthedUser().getName()});

        final Chat chat = OtrActivator.uiService.getChat(chatRoom);

        chat.addMessage(chatRoom.getName(), new Date(),
                Chat.ERROR_MESSAGE, message,
                OperationSetBasicInstantMessaging.HTML_MIME_TYPE);
    }

    private void displayAuthMessage() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    displayAuthMessage();
                }
            });
            return;
        }

        String imageURL = null;
        try {
            imageURL = OtrActivator.resourceService.getImageURL("plugin.otr.ENCRYPTED_ICON_16x16").toURI().toString();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        String message = OtrActivator.resourceService
                .getI18NString("plugin.otr.gotr.SECURE_AUTH_STATE", new String[]{imageURL});

        final Chat chat = OtrActivator.uiService.getChat(chatRoom);

        chat.addMessage(chatRoom.getName(), new Date(),
                Chat.SYSTEM_MESSAGE, message,
                OperationSetBasicInstantMessaging.HTML_MIME_TYPE);
    }

    @Override
    public void askForSecret(GotrUser user, String question) {
        ChatRoomMember member = userToMemberMap.get(user);
        SmpAuthenticateBuddyDialog dialog =
                new SmpAuthenticateBuddyDialog(question, new GotrMemberAuthDialogBackend(this, member, user));
        dialog.setVisible(true);
    }

    @Override
    public void smpAborted(GotrUser user) {

        logger.debug("Found an smp abort.");
        String message = OtrActivator.resourceService
                .getI18NString("plugin.otr.gotr.AUTH_FAILED", new String[]{user.getUsername()});

        final Chat chat = OtrActivator.uiService.getChat(chatRoom);

        chat.addMessage(chatRoom.getName(), new Date(),
                Chat.ERROR_MESSAGE, message,
                OperationSetBasicInstantMessaging.HTML_MIME_TYPE);

        closeSmpProgressDialog(user);
    }

    @Override
    public void smpError(GotrUser user) {
        logger.debug("Found an smp error.");
        String message = OtrActivator.resourceService
                .getI18NString("plugin.otr.gotr.AUTH_FAILED", new String[]{user.getUsername()});

        final Chat chat = OtrActivator.uiService.getChat(chatRoom);

        chat.addMessage(chatRoom.getName(), new Date(),
                Chat.ERROR_MESSAGE, message,
                OperationSetBasicInstantMessaging.HTML_MIME_TYPE);

        closeSmpProgressDialog(user);
    }

    @Override
    public void unverify(GotrUser user, String fingerprint) {
        logger.debug("Found an smp fail.");
        OtrActivator.scOtrKeyManager.unverify("", fingerprint);
        OtrActivator.gotrComponentService.update(chatRoom);

        String message = OtrActivator.resourceService
                .getI18NString("plugin.otr.gotr.AUTH_FAILED", new String[]{user.getUsername()});

        final Chat chat = OtrActivator.uiService.getChat(chatRoom);

        chat.addMessage(chatRoom.getName(), new Date(),
                Chat.ERROR_MESSAGE, message,
                OperationSetBasicInstantMessaging.HTML_MIME_TYPE);

        closeSmpProgressDialog(user);
    }

    @Override
    public void sessionFinished(GotrUser user) {
        String finished = OtrActivator.resourceService
                .getI18NString("plugin.otr.gotr.FINISHED", new String[]{getName(user)});
        OtrActivator.uiService.getChat(chatRoom).addMessage(chatRoom.getName(), new Date(),
                Chat.ERROR_MESSAGE, finished,
                OperationSetBasicInstantMessaging.DEFAULT_MIME_TYPE);
    }

    @Override
    public void unrecoverableError(GotrUser user) {
        String finished = OtrActivator.resourceService
                .getI18NString("plugin.otr.gotr.UNRECOVERABLE_ERROR", new String[]{getName(user)});
        OtrActivator.uiService.getChat(chatRoom).addMessage(chatRoom.getName(), new Date(),
                Chat.ERROR_MESSAGE, finished,
                OperationSetBasicInstantMessaging.DEFAULT_MIME_TYPE);
        outgoing=0;
        for (ScGotrSessionListener listener : listeners) {
            listener.outgoingMessagesUpdated(this);
        }
    }

    @Override
    public void recoverableError(GotrUser user) {
        String finished = OtrActivator.resourceService
                .getI18NString("plugin.otr.gotr.RECOVERABLE_ERROR", new String[]{getName(user)});
        OtrActivator.uiService.getChat(chatRoom).addMessage(chatRoom.getName(), new Date(),
                Chat.SYSTEM_MESSAGE, finished,
                OperationSetBasicInstantMessaging.DEFAULT_MIME_TYPE);
        outgoing=0;
        for (ScGotrSessionListener listener : listeners) {
            listener.outgoingMessagesUpdated(this);
        }
    }

    @Override
    public void receivedUnsentMessage(GotrUser source) {
        String finished = OtrActivator.resourceService
                .getI18NString("plugin.otr.gotr.INCONSISTENT_DIGEST", new String[]{});
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
        outgoing=0;
        for (ScGotrSessionListener listener : listeners) {
            listener.outgoingMessagesUpdated(this);
        }
    }

    private String getName(GotrUser user) {
        if (user.equals(localUser)) {
            return chatRoom.getUserNickname();
        } else {
            ChatRoomMember member = userToMemberMap.get(user);
            return member.getName();
        }
    }

    public GotrSessionManager getSession() {
        return gotrSession;
    }

    /**
     * Called when a new {@link ChatRoomMember} is added to the {@link ChatRoom}.
     *
     * @param member newly added member
     */
    private void memberAdded(ChatRoomMember member) {
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("%s added to %s, %s", member.getName(), chatRoom.getUserNickname(), member.getName().equals(chatRoom.getUserNickname())));
        }
        final Contact contact = chatRoom.getPrivateContactByNickname(
                member.getName());
        final GotrUser user = new GotrUser(contact.getAddress());

        userToContactMap.put(user, contact);
        contactToUserMap.put(contact, user);
        userToMemberMap.put(user, member);
        memberToUserMap.put(member, user);

        try {
            this.gotrSession.addUser(user);
        } catch (GotrException e) {
            e.printStackTrace();
        }
    }

    private void memberRemoved(ChatRoomMember member) {
        final GotrUser user = memberToUserMap.remove(member);
        final Contact contact = userToContactMap.remove(user);
        contactToUserMap.remove(contact);
        userToMemberMap.remove(user);

        if (logger.isDebugEnabled()) {
            logger.debug(String.format("%s, %s removed.", user, chatRoom.getUserNickname()));
        }

        try {
            this.gotrSession.removeUser(user);
        } catch (GotrException e) {
            e.printStackTrace();
        }
    }

    public GotrUser getUser(ChatRoomMember member) {
        return memberToUserMap.get(member);
    }

    /**
     * Should be called when the user leaves the chat room.
     */
    public void close() {
        gotrSession.shutdown();
        chatRoom.removeMemberPresenceListener(this);
        OtrActivator.scOtrKeyManager.removeListener(this);
    }

    @Override
    public void memberPresenceChanged(ChatRoomMemberPresenceChangeEvent evt) {
        if (evt.getEventType().equals(
                ChatRoomMemberPresenceChangeEvent.MEMBER_JOINED)
                && !evt.getChatRoomMember().getName()
                .equals(chatRoom.getUserNickname())) {
            memberAdded(evt.getChatRoomMember());
        } else if ((evt.getEventType().equals(
                ChatRoomMemberPresenceChangeEvent.MEMBER_KICKED)
                || evt.getEventType().equals(
                ChatRoomMemberPresenceChangeEvent.MEMBER_LEFT)
                || evt.getEventType().equals(
                ChatRoomMemberPresenceChangeEvent.MEMBER_QUIT))
                && chatRoom.getUserNickname() != null
                && !evt.getChatRoomMember().getName().equals(
                chatRoom.getUserNickname())) {
            memberRemoved(evt.getChatRoomMember());
        }
    }


    public GotrUser getUser(Contact contact) {
        return contactToUserMap.get(contact);
    }

    public SessionID getSessionID() {
        return gotrSession.getSessionID();
    }

    public ProtocolProviderService getProtocolProvider() {
        return protocolProvider;
    }

    public boolean sentChatRoomMessage(String messageUID) {
        return sentBroadcasts.contains(messageUID);
    }

    public GotrUser getLocalUser() {
        return localUser;
    }

    public boolean receivedChatRoomMessage(String messageUID) {
        return receivedBroadcasts.contains(messageUID);
    }

    public void addSessionListener(ScGotrSessionListener listener) {
        synchronized (listener) {
            if (!listeners.contains(listener)) {
                listeners.add(listener);
            }
        }
    }

    public boolean areAllAuthenticated() {
        for (ChatRoomMember member : memberToUserMap.keySet()) {
            if (!authenticated(member)) {
                return false;
            }
        }
        return true;
    }

    public boolean sentP2pMessage(String messageUID) {
        return sentP2pMessages.contains(messageUID);
    }

    public boolean authenticated(ChatRoomMember member) {
        String fingerprint = getRemoteFingerprint(member);

        return OtrActivator.scOtrKeyManager.isVerified(fingerprint);

    }

    public List<ChatRoomMember> getChatRoomMembers() {
        return new ArrayList<ChatRoomMember>(memberToUserMap.keySet());
    }

    public String getRemoteFingerprint(ChatRoomMember member) {
        GotrUser user = memberToUserMap.get(member);
        try {
            if(user == null){
                return null;
            }
            PublicKey remoteKey = gotrSession.getRemotePublicKey(user);
            if (remoteKey == null) {
                return null;
            }
            return OtrActivator.scOtrKeyManager.getFingerprintFromPublicKey(remoteKey);
        } catch (GotrException e) {
            logger.error("Unable to get remote users public key", e);
            return null;
        }
    }

    public void initSmp(ChatRoomMember member, String question, String secret) {
        GotrUser user = memberToUserMap.get(member);
        try {
            gotrSession.initSmp(user, question, secret);
            displaySmpProgressDialog(user);
        } catch (GotrException e) {
            logger.error("Unable to init smp with remote user.", e);
        }
    }

    public void displaySmpProgressDialog(GotrUser user) {
        synchronized (progressDialogMap) {
            SmpProgressDialog dialog = progressDialogMap.get(user);

            if (dialog == null) {

                Contact contact = userToContactMap.get(user);

                dialog = new SmpProgressDialog(contact.getDisplayName());
                progressDialogMap.put(user, dialog);
            }
            dialog.setVisible(true);
        }
    }

    public void closeSmpProgressDialog(GotrUser user){
        synchronized (progressDialogMap){
            SmpProgressDialog dialog = progressDialogMap.remove(user);
            if(dialog != null){
                dialog.dispose();
            }
        }
    }

    public void respondSmp(GotrUser user, String question, String text) {
        try {
            gotrSession.respondSmp(user, question, text);
        } catch (GotrException e) {
            e.printStackTrace();
        }
    }

    public void abortSmp(GotrUser user) {
        try {
            gotrSession.abortSmp(user);
        } catch (GotrException e) {
            e.printStackTrace();
        }
    }

    public boolean isGotrRequired() {
        return chatRoom.getName().contains("_gotr");
    }

    @Override
    public void chatLinkClicked(URI url) {
        final String action = url.getPath();
        if("/authenticate".equals(action)){
            final String displayName = url.getQuery();

            final ChatRoomMember member = getMemberWithName(displayName);

            if(member != null){
                SwingOtrActionHandler.openAuthDialog(this, member);
            }
        }
    }

    private ChatRoomMember getMemberWithName(String displayName) {
        for (ChatRoomMember member : chatRoom.getMembers()) {
            if (member.getName().equals(displayName)) {
                return member;
            }
        }
        return null;
    }

    public ChatRoomMember getFirstUnauthedUser() {
        final List<ChatRoomMember> memberList = chatRoom.getMembers();
        Collections.sort(memberList, new Comparator<ChatRoomMember>() {
            @Override
            public int compare(ChatRoomMember member, ChatRoomMember member2) {
                return member.getName().compareTo(member2.getName());
            }
        });

        for (ChatRoomMember member : memberList) {
            if (!member.getName().equals(chatRoom.getUserNickname())
                    && !authenticated(member)) {
                return member;
            }
        }
        return null;
    }

    @Override
    public void verificationStatusChanged(String fingerprint) {
        for(ChatRoomMember member: chatRoom.getMembers()){
            GotrUser gotrUser = getUser(member);
            if(gotrUser == null){
                continue;
            }
            try {
                PublicKey publicKey = gotrSession.getRemotePublicKey(gotrUser);
                if(publicKey == null){
                    continue;
                }

                String userFingerprint = OtrActivator.scOtrKeyManager.getFingerprintFromPublicKey(publicKey);
                boolean verified = OtrActivator.scOtrKeyManager.isVerified(userFingerprint);

                if(fingerprint.equals(userFingerprint)){
                    if(verified) {
                        String message = OtrActivator.resourceService
                                .getI18NString("plugin.otr.gotr.AUTH_SUCCESS", new String[]{member.getName()});

                        final Chat chat = OtrActivator.uiService.getChat(chatRoom);

                        chat.addMessage(chatRoom.getName(), new Date(),
                                Chat.SYSTEM_MESSAGE, message,
                                OperationSetBasicInstantMessaging.HTML_MIME_TYPE);

                        displaySecure = true;
                        postStateMessageToChat(gotrSession.getState());
                    }
                    else {
                        String message = OtrActivator.resourceService
                                .getI18NString("plugin.otr.gotr.USER_UNAUTHED", new String[]{
                                        ScGotrSessionHost.class.getName(),
                                        getFirstUnauthedUser().getName()});

                        final Chat chat = OtrActivator.uiService.getChat(chatRoom);

                        chat.addMessage(chatRoom.getName(), new Date(),
                                Chat.ERROR_MESSAGE, message,
                                OperationSetBasicInstantMessaging.HTML_MIME_TYPE);
                    }
                    return;
                }

            } catch (GotrException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean outgoingMessages(){
        return outgoing > 0;
    }

    public void handleUserBroadcast(String content) throws GotrException {
        outgoing++;
        for (ScGotrSessionListener listener : listeners) {
            listener.outgoingMessagesUpdated(this);
        }
        gotrSession.broadcastMessage(content);
    }

    @Override
    public void handleException(HSMException e) {
        logger.error("GOTR threw an exception.", e);
    }
}

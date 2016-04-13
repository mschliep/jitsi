package net.java.sip.communicator.plugin.otr.gui;

import net.java.gotr4j.*;
import net.java.sip.communicator.plugin.otr.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class GotrComponentServiceImpl
        implements GotrComponentService,
        ScOtrKeyManagerListener
{

    private static final Logger logger = Logger.getLogger(
            GotrComponentServiceImpl.class);

    private final WeakHashMap<ChatRoom, JList> memberLists = new WeakHashMap
            <ChatRoom, JList>();

    private final GotrMemberListRenderer renderer =
            new GotrMemberListRenderer();

    public GotrComponentServiceImpl()
    {
        OtrActivator.scOtrKeyManager.addListener(this);
    }

    public void update(ChatRoom room)
    {
        synchronized (memberLists)
        {
            JList list = memberLists.get(room);
            if(list != null)
            {
                list.repaint();
            }
        }
    }

    @Override
    public Component getChatRoomMemberRendererComponent(JList list, ChatRoom room,
                                                    ChatRoomMember member)
    {
        synchronized (memberLists)
        {
            if(!memberLists.containsKey(room))
            {
                memberLists.put(room, list);
            }
        }

        ScGotrSessionHost sessionHost =
                OtrActivator.gotrSessionManager.getGotrSessionHost(room);

        return renderer.getListCellRendererComponent(list, room, member,
                sessionHost);
    }

    @Override
    public JMenuItem getChatRoomMemberMenuItem(ChatRoom room, ChatRoomMember member) {
        ScGotrSessionHost sessionHost =
                OtrActivator.gotrSessionManager.getGotrSessionHost(room);

        if(sessionHost == null
                || sessionHost.getSession().getState().equals(
                GotrSessionState.PLAINTEXT)
                || sessionHost.authenticated(member))
        {
            return null;
        }

        String text = OtrActivator.resourceService.getI18NString(
                "plugin.otr.authbuddydialog.AUTHENTICATE_BUDDY");

        JMenuItem menuItem = new JMenuItem(text);
        menuItem.addActionListener(new MenuActionListener(room, member));
        return menuItem;
    }

    @Override
    public void verificationStatusChanged(String fingerprint)
    {
        logger.debug("verifcation status changed");
        synchronized (memberLists)
        {
            for(JList list: memberLists.values())
            {
                list.repaint();
            }
        }
    }

    private class MenuActionListener
            implements ActionListener
    {

        private final ChatRoom room;
        private final ChatRoomMember member;

        private MenuActionListener(ChatRoom room, ChatRoomMember member)
        {
            this.room = room;
            this.member = member;
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent)
        {
            ScGotrSessionHost sessionHost =
                    OtrActivator.gotrSessionManager.getGotrSessionHost(room);
            if(sessionHost != null)
            {
                SwingOtrActionHandler.openAuthDialog(sessionHost, member);
            }
        }
    }
}

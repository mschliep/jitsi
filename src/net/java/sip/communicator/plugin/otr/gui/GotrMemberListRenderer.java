package net.java.sip.communicator.plugin.otr.gui;

import net.java.gotr4j.*;
import net.java.sip.communicator.plugin.otr.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import javax.swing.*;
import java.awt.*;

public class GotrMemberListRenderer extends JLabel
{
    private static final Logger logger =
            Logger.getLogger(GotrMemberListRenderer.class);

    public GotrMemberListRenderer()
    {
        super();
        setHorizontalAlignment(SwingConstants.LEFT);
    }

    public Component getListCellRendererComponent(JList list, ChatRoom room,
                                                  ChatRoomMember member,
                                                  ScGotrSessionHost sessionHost)
    {

        if(member.getName().equals(room.getUserNickname())
            || sessionHost == null
            || sessionHost.getSession().getState().equals(GotrSessionState.PLAINTEXT))
        {
            setVisible(false);
            return this;
        }

        String fingerprint = sessionHost.getRemoteFingerprint(member);
        setText("");
        if(fingerprint != null)
        {
            String petname = OtrActivator.scOtrKeyManager.getPetname(fingerprint);
            if(petname != null && !petname.equals(member.getName()))
            {
                setText(petname);
            }
        }

        Icon icon;
        if(sessionHost.authenticated(member))
        {
            icon = OtrActivator.resourceService.getImage(
                    "plugin.otr.ENCRYPTED_ICON_16x16");
        }
        else
        {
            icon = OtrActivator.resourceService.getImage(
                    "plugin.otr.ENCRYPTED_UNVERIFIED_ICON_16x16");
        }
        setIcon(icon);
        setVisible(true);

        return this;
    }
}

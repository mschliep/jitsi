package net.java.sip.communicator.plugin.otr.gui;

import net.java.sip.communicator.service.protocol.*;

import javax.swing.*;
import java.awt.*;

public interface GotrComponentService {

    public Component getChatRoomMemberRendererComponent(JList list, ChatRoom room, ChatRoomMember member);

    public JMenuItem getChatRoomMemberMenuItem(ChatRoom room, ChatRoomMember member);

}

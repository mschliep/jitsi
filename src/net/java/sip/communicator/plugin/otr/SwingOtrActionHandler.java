/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.otr;

import java.awt.*;
import java.util.*;

import net.java.gotr4j.GotrUser;
import net.java.otr4j.session.InstanceTag;
import net.java.sip.communicator.plugin.otr.OtrContactManager.OtrContact;
import net.java.sip.communicator.plugin.otr.authdialog.*;
import net.java.sip.communicator.service.protocol.ChatRoomMember;

/**
 * Default OtrActionHandler implementation that opens SWING buddy authenticate
 * dialog.
 *
 * @author Daniel Perren
 * @author Pawel Domas
 */
public class SwingOtrActionHandler
    implements OtrActionHandler
{
    public void onAuthenticateLinkClicked(UUID uuid)
    {
        OtrContact otrContact = ScOtrEngineImpl.getOtrContact(
                    ScOtrEngineImpl.getScSessionForGuid(uuid).getSessionID());

        openAuthDialog(otrContact);
    }

    public static void openAuthDialog(OtrContact contact)
    {
        openAuthDialog(new OtrContactAuthDialogBackend(contact, null));
    }

    public static void openAuthDialog(ScGotrSessionHost sessionHost, ChatRoomMember member) {
        GotrUser user = sessionHost.getUser(member);
        openAuthDialog(new GotrMemberAuthDialogBackend(sessionHost, member, user));
    }

    public static void openAuthDialog(AuthenticationDialogBackend backend)
    {
        // Launch auth buddy dialog.
        OtrBuddyAuthenticationDialog authenticateBuddyDialog
                = new OtrBuddyAuthenticationDialog(backend);

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        authenticateBuddyDialog.setLocation(
                screenSize.width / 2
                        - authenticateBuddyDialog.getWidth() / 2,
                screenSize.height / 2
                        - authenticateBuddyDialog.getHeight() / 2);

        authenticateBuddyDialog.setVisible(true);
    }
}

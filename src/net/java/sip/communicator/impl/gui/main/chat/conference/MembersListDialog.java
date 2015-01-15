/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat.conference;

import net.java.sip.communicator.impl.gui.GuiActivator;
import net.java.sip.communicator.impl.gui.main.contactlist.InviteUIContact;
import net.java.sip.communicator.impl.gui.main.contactlist.contactsource.ProtocolContactSourceServiceImpl;
import net.java.sip.communicator.impl.gui.main.contactlist.contactsource.StringContactSourceServiceImpl;
import net.java.sip.communicator.impl.gui.utils.InviteDialog;
import net.java.sip.communicator.service.contactsource.ContactSourceService;
import net.java.sip.communicator.service.contactsource.SourceContact;
import net.java.sip.communicator.service.gui.UIContact;
import net.java.sip.communicator.service.gui.UIContactDetail;
import net.java.sip.communicator.service.gui.UIContactSource;
import net.java.sip.communicator.service.muc.ChatRoomWrapper;
import net.java.sip.communicator.service.protocol.OperationSetMultiUserChat;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * A dialog with room provider's contacts on the left and contacts
 * that has members role set for this chat room on the right.
 *
 * @author Damian Minkov
 */
public class MembersListDialog
    extends InviteDialog
    implements ActionListener
{
    /**
     * The chat room.
     */
    private final ChatRoomWrapper chatRoomWrapper;

    /**
     * The contact source used to be able to add simple string contact ids.
     * We can grant members role to users which are not part of our contact
     * list.
     */
    private StringContactSourceServiceImpl currentStringContactSource;

    /**
     * Constructs an <tt>MembersListDialog</tt>.
     *
     * @param chatRoomWrapper the room
     * @param title the title to show on the top of this dialog
     * @param enableReason
     */
    public MembersListDialog(
        ChatRoomWrapper chatRoomWrapper, String title, boolean enableReason)
    {
        super(title, enableReason);

        this.chatRoomWrapper = chatRoomWrapper;

        // change invite button text
        inviteButton.setText(
            GuiActivator.getResources().getI18NString("service.gui.SAVE"));
        // change description text
        infoTextArea.setText(GuiActivator.getResources().getI18NString(
            "service.gui.CHAT_ROOM_CONFIGURATION_MEMBERS_EDIT_DESCRIPTION"));
        infoTextArea.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));

        initContactListData(chatRoomWrapper.getParentProvider()
            .getProtocolProvider());

        this.addInviteButtonListener(this);

        this.addCancelButtonListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                dispose();
            }
        });
    }

    /**
     * When save button is pressed.
     * @param e the action event
     */
    public void actionPerformed(ActionEvent e)
    {
        Collection<UIContact> selectedContacts
            = destContactList.getContacts(null);

        List<String> selectedMembers = new ArrayList<String>();

        if (selectedContacts != null)
        {
            for(UIContact c : selectedContacts)
            {
                Iterator<UIContactDetail> contactDetailsIter = c
                    .getContactDetailsForOperationSet(
                        OperationSetMultiUserChat.class).iterator();

                if (contactDetailsIter.hasNext())
                {
                    UIContactDetail inviteDetail
                        = contactDetailsIter.next();
                    selectedMembers.add(inviteDetail.getAddress());
                }
            }

            // save
            chatRoomWrapper.getChatRoom()
                .setMembersWhiteList(selectedMembers);

            dispose();
        }
    }

    /**
     * Initializes the left contact list with the contacts that could be added
     * to the right.
     * @param protocolProvider the protocol provider from which to initialize
     * the contact list data
     */
    private void initContactListData(
        final ProtocolProviderService protocolProvider)
    {
        this.setCurrentProvider(protocolProvider);

        srcContactList.removeAllContactSources();

        ContactSourceService currentProviderContactSource
            = new ProtocolContactSourceServiceImpl(
                    protocolProvider,
                    OperationSetMultiUserChat.class);
        currentStringContactSource
            = new StringContactSourceServiceImpl(
                    protocolProvider,
                    OperationSetMultiUserChat.class);
        currentStringContactSource.setDisableDisplayDetails(false);

        srcContactList.addContactSource(currentProviderContactSource);
        srcContactList.addContactSource(currentStringContactSource);

        srcContactList.applyDefaultFilter();

        // load in new thread, obtaining white list maybe slow as it involves
        // network operations
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                List<String> membersWhiteList =
                    MembersListDialog.this.chatRoomWrapper
                        .getChatRoom().getMembersWhiteList();

                UIContactSource uiContactSource =
                    srcContactList.getContactSource(currentStringContactSource);

                for(String member : membersWhiteList)
                {
                    SourceContact newSourceContact =
                        currentStringContactSource.createSourceContact(member);

                    destContactList.addContact(
                        new InviteUIContact(
                            uiContactSource.createUIContact(newSourceContact),
                            protocolProvider),
                        null, false, false);
                }
            }
        }).start();
    }
}

/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.plugin.otr;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.gotr4j.*;
import net.java.gotr4j.crypto.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.Logger;

/**
 * @author George Politis
 * @author Lubomir Marinov
 */
public class OtrMetaContactMenu
    extends AbstractPluginComponent
    implements ActionListener,
               PopupMenuListener
{

    private static final Logger logger = Logger.getLogger(
            OtrMetaContactMenu.class);

    /**
     * The last known <tt>MetaContact</tt> to be currently selected and to be
     * depicted by this instance and the <tt>OtrContactMenu</tt>s it contains.
     */
    private MetaContact currentContact;

    private ScGotrSessionHost currentChatRoomSession;

    /**
     * The indicator which determines whether the <tt>JMenu</tt> of this
     * <tt>OtrMetaContactMenu</tt> is displayed in the Mac OS X screen menu bar
     * and thus should work around the known problem of PopupMenuListener not
     * being invoked.
     */
    private final boolean inMacOSXScreenMenuBar;

    /**
     * The <tt>JMenu</tt> which is the component of this plug-in.
     */
    private JMenu menu;

    private final ScGotrSessionManager gotrSessionManager;

    /**
     * The "What's this?" <tt>JMenuItem</tt> which launches help on the subject
     * of off-the-record messaging.
     */
    private JMenuItem whatsThis;

    public OtrMetaContactMenu(Container container,
                              PluginComponentFactory parentFactory,
                              ScGotrSessionManager gotrSessionManager)
    {
        super(container, parentFactory);
        this.gotrSessionManager = gotrSessionManager;

        inMacOSXScreenMenuBar =
            Container.CONTAINER_CHAT_MENU_BAR.equals(container)
                && OtrActivator.uiService.useMacOSXScreenMenuBar();
    }

    /*
     * Implements ActionListener#actionPerformed(ActionEvent). Handles the
     * invocation of the whatsThis menu item i.e. launches help on the subject
     * of off-the-record messaging.
     */
    public void actionPerformed(ActionEvent e)
    {
        OtrActivator.scOtrEngine.launchHelp();
    }

    /**
     * Creates an {@link OtrContactMenu} for each {@link Contact} contained in
     * the <tt>metaContact</tt>.
     *
     * @param metaContact The {@link MetaContact} this
     *            {@link OtrMetaContactMenu} refers to.
     */
    private void createOtrContactMenus(MetaContact metaContact)
    {
        JMenu menu = getMenu();

        // Remove any existing OtrContactMenu items.
        menu.removeAll();

        // Create the new OtrContactMenu items.
        if (metaContact != null)
        {
            Iterator<Contact> contacts = metaContact.getContacts();

            if (metaContact.getContactCount() == 1)
            {
                Contact contact = contacts.next();
                Collection<ContactResource> resources = contact.getResources();
                if (contact.supportResources() &&
                    resources != null &&
                    resources.size() > 0)
                {
                    for (ContactResource resource : resources)
                    {
                        new OtrContactMenu(
                            OtrContactManager.getOtrContact(contact, resource),
                            inMacOSXScreenMenuBar,
                            menu,
                            true);
                    }
                }
                else
                    new OtrContactMenu(
                        OtrContactManager.getOtrContact(contact, null),
                        inMacOSXScreenMenuBar,
                        menu,
                        false);
            }
            else
                while (contacts.hasNext())
                {
                    Contact contact = contacts.next();
                    Collection<ContactResource> resources =
                        contact.getResources();
                    if (contact.supportResources() &&
                        resources != null &&
                        resources.size() > 0)
                    {
                        for (ContactResource resource : resources)
                        {
                            new OtrContactMenu(
                                OtrContactManager.getOtrContact(
                                    contact, resource),
                                inMacOSXScreenMenuBar,
                                menu,
                                true);
                        }
                    }
                    else
                        new OtrContactMenu(
                            OtrContactManager.getOtrContact(contact, null),
                            inMacOSXScreenMenuBar,
                            menu,
                            true);
                }
        }
    }

    /*
     * Implements PluginComponent#getComponent(). Returns the JMenu which is the
     * component of this plug-in creating it first if it doesn't exist.
     */
    public Component getComponent()
    {
        return getMenu();
    }

    /**
     * Gets the <tt>JMenu</tt> which is the component of this plug-in. If it
     * still doesn't exist, it's created.
     *
     * @return the <tt>JMenu</tt> which is the component of this plug-in
     */
    private JMenu getMenu()
    {
        if (menu == null)
        {
            menu = new SIPCommMenu();
            menu.setText(getName());

            if (Container.CONTAINER_CONTACT_RIGHT_BUTTON_MENU
                .equals(getContainer()))
            {
                Icon icon =
                    OtrActivator.resourceService
                        .getImage("plugin.otr.MENU_ITEM_ICON_16x16");

                if (icon != null)
                    menu.setIcon(icon);
            }

            if (!inMacOSXScreenMenuBar)
                menu.getPopupMenu().addPopupMenuListener(this);
        }
        return menu;
    }

    /*
     * Implements PluginComponent#getName().
     */
    public String getName()
    {
        return OtrActivator.resourceService
            .getI18NString("plugin.otr.menu.TITLE");
    }

    /*
     * Implements PopupMenuListener#popupMenuCanceled(PopupMenuEvent).
     */
    public void popupMenuCanceled(PopupMenuEvent e)
    {
        menu.removeAll();
    }

    /*
     * Implements PopupMenuListener#popupMenuWillBecomeInvisible(
     * PopupMenuEvent).
     */
    public void popupMenuWillBecomeInvisible(PopupMenuEvent e)
    {
        popupMenuCanceled(e);
    }

    /*
     * Implements PopupMenuListener#popupMenuWillBecomeVisible(PopupMenuEvent).
     */
    public void popupMenuWillBecomeVisible(PopupMenuEvent e)
    {
        if(currentChatRoomSession != null)
        {
            createGotrMenu(currentChatRoomSession);
        }
        else if(currentContact != null)
        {
            createOtrContactMenus(currentContact);
        }

        JMenu menu = getMenu();

        menu.addSeparator();

        whatsThis = new JMenuItem();
        whatsThis.setIcon(
                OtrActivator.resourceService.getImage(
                        "plugin.otr.HELP_ICON_15x15"));
        whatsThis.setText(
                OtrActivator.resourceService.getI18NString(
                        "plugin.otr.menu.WHATS_THIS"));
        whatsThis.addActionListener(this);
        menu.add(whatsThis);
    }

    /*
     * Implements PluginComponent#setCurrentContact(MetaContact).
     */
    @Override
    public void setCurrentContact(MetaContact metaContact)
    {
        if(metaContact != null) {
            currentChatRoomSession = null;
        }
        if (this.currentContact != metaContact)
        {
            this.currentContact = metaContact;

            if (inMacOSXScreenMenuBar)
                popupMenuWillBecomeVisible(null);
            else if ((menu != null) && menu.isPopupMenuVisible())
                createOtrContactMenus(currentContact);
        }
    }

    @Override
    public void setCurrentChatRoom(ChatRoom chatRoom) {
        currentChatRoomSession = gotrSessionManager.getGotrSessionHost(chatRoom);
        currentContact = null;

        if(inMacOSXScreenMenuBar)
        {
            popupMenuWillBecomeVisible(null);
        }
        else if(menu != null && menu.isPopupMenuVisible())
        {
            createGotrMenu(currentChatRoomSession);
        }
    }

    private void createGotrMenu(final ScGotrSessionHost currentChatRoomSession) {
        final JMenu menu = getMenu();
        menu.removeAll();

        if(currentChatRoomSession.getSession().getState()
                .equals(GotrSessionState.PLAINTEXT))
        {
            JMenuItem startItem = new JMenuItem();
            startItem.setText(OtrActivator.resourceService
                    .getI18NString("plugin.otr.menu.START_OTR"));
            startItem.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent actionEvent)
                {
                    try
                    {
                        currentChatRoomSession.getSession().start();
                    } catch (GotrException e)
                    {
                        logger.error("Unable to start session", e);
                    }
                }
            });
            menu.add(startItem);
        }
        else
        {

            final JMenu authenticatedMenu = new JMenu();
            authenticatedMenu.setText("Authenticated");

            for (ChatRoomMember member : currentChatRoomSession.getChatRoomMembers())

            {
                authenticatedMenu.add(
                        new GotrUserMenuItem(currentChatRoomSession, member));
            }
            menu.add(authenticatedMenu);

            JMenuItem refreshKey = new JMenuItem();
            refreshKey.setText(OtrActivator.resourceService
                    .getI18NString("plugin.otr.gotr.menu.REFRESH_KEYS"));
            refreshKey.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent actionEvent)
                {
                    currentChatRoomSession.getSession().refreshKeys();
                }
            });
            menu.add(refreshKey);

            if(!currentChatRoomSession.isGotrRequired()) {
                JMenuItem finishItem = new JMenuItem();
                finishItem.setText(OtrActivator.resourceService
                        .getI18NString("plugin.otr.menu.END_OTR"));
                finishItem.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        try {
                            currentChatRoomSession.getSession().end();
                        } catch (GotrException e) {
                            logger.error("Unable to end session", e);
                        }
                    }
                });
                menu.add(finishItem);
            }
        }
    }

    private class GotrUserMenuItem
        extends JMenuItem
        implements ActionListener
    {

        private final ScGotrSessionHost sessionHost;
        private final ChatRoomMember member;

        private GotrUserMenuItem(ScGotrSessionHost sessionHost,
                                 ChatRoomMember member) {
            this.sessionHost = sessionHost;
            this.member = member;


            this.setText(member.getName());

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
            this.setIcon(icon);
            this.addActionListener(this);
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            SwingOtrActionHandler.openAuthDialog(sessionHost, member);
        }
    }
}

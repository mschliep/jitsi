/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.otr;

import net.java.gotr4j.GotrSessionState;
import net.java.gotr4j.crypto.GotrException;
import net.java.otr4j.OtrPolicy;
import net.java.sip.communicator.impl.gui.lookandfeel.SIPCommLabelUI;
import net.java.sip.communicator.plugin.desktoputil.AnimatedImage;
import net.java.sip.communicator.plugin.desktoputil.SIPCommButton;
import net.java.sip.communicator.plugin.otr.OtrContactManager.OtrContact;
import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.contactlist.MetaContactGroup;
import net.java.sip.communicator.service.gui.AbstractPluginComponent;
import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.service.gui.PluginComponent;
import net.java.sip.communicator.service.gui.PluginComponentFactory;
import net.java.sip.communicator.service.protocol.AccountID;
import net.java.sip.communicator.service.protocol.ChatRoom;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.ContactResource;
import net.java.sip.communicator.util.Logger;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.security.PublicKey;

/**
 * A {@link net.java.sip.communicator.service.gui.AbstractPluginComponent} that registers the Off-the-Record button in
 * the main chat toolbar.
 *
 * @author George Politis
 * @author Marin Dzhigarov
 */
public class GotrSendingLabel
    extends AbstractPluginComponent
    implements ScGotrSessionListener
{

    private final Logger logger = Logger.getLogger(GotrSendingLabel.class);

    private final JLabel label = new JLabel("Sending...");
    private ScGotrSessionHost currentHost = null;

    private final ScGotrSessionManager gotrSessionManager;

    /**
     * Initializes a new <code>AbstractPluginComponent</code> which is to be
     * added to a specific <code>Container</code>.
     *  @param container     the container in which the component of the new plug-in
     *                      is to be added
     * @param parentFactory the parent <tt>PluginComponentFactory</tt> that is
     * @param gotrSessionManager
     */
    protected GotrSendingLabel(Container container, PluginComponentFactory parentFactory,
                               ScGotrSessionManager gotrSessionManager) {
        super(container, parentFactory);
        this.gotrSessionManager = gotrSessionManager;
        label.setVisible(false);
    }

    @Override
    public void setCurrentContact(Contact contact) {
        currentHost = null;
        label.setVisible(false);
    }

    @Override
    public void setCurrentContact(Contact contact, String resourceName) {
        currentHost = null;
        label.setVisible(false);
    }

    @Override
    public void setCurrentContact(MetaContact metaContact) {
        currentHost = null;
        label.setVisible(false);
    }

    @Override
    public void setCurrentContactGroup(MetaContactGroup metaGroup) {
        currentHost = null;
        label.setVisible(false);
    }

    @Override
    public void setCurrentChatRoom(ChatRoom chatRoom) {
        currentHost = gotrSessionManager.getGotrSessionHost(chatRoom);
        if(currentHost != null){
            currentHost.addSessionListener(this);
            label.setVisible(currentHost.outgoingMessages());
        }
        else{
            label.setVisible(false);
        }
    }

    @Override
    public String getName() {
        return "OTR SENDING";
    }

    @Override
    public Object getComponent() {
        return label;
    }

    @Override
    public void statusChanged(ScGotrSessionHost host) {

    }

    @Override
    public void outgoingMessagesUpdated(ScGotrSessionHost host) {
        if(currentHost != null && currentHost.equals(host)){
            label.setVisible(host.outgoingMessages());
            label.repaint();
        }
    }
}

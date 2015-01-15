/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.otr.authdialog;

import net.java.gotr4j.GotrUser;
import net.java.gotr4j.crypto.GotrException;
import net.java.sip.communicator.plugin.desktoputil.SIPCommDialog;
import net.java.sip.communicator.plugin.desktoputil.TransparentPanel;
import net.java.sip.communicator.plugin.otr.OtrActivator;
import net.java.sip.communicator.plugin.otr.ScGotrSessionHost;
import net.java.sip.communicator.service.protocol.ChatRoomMember;
import net.java.sip.communicator.util.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * @author George Politis
 * @author Marin Dzhigarov
 */
@SuppressWarnings("serial")
public class GotrAuthenticationDialog
    extends SIPCommDialog
{

    private static final Logger logger = Logger.getLogger(
            GotrAuthenticationDialog.class);

    private final ScGotrSessionHost sessionHost;
    private final ChatRoomMember member;
    private final GotrUser user;

    /**
     * The {@link net.java.sip.communicator.plugin.otr.authdialog.GotrAuthenticationDialog} ctor.
     *  @param sessionHost
     * @param member
     * @param user
     */
    public GotrAuthenticationDialog(ScGotrSessionHost sessionHost, ChatRoomMember member, GotrUser user)
    {
        super(false);
        this.sessionHost = sessionHost;
        this.member = member;
        this.user = user;

        initComponents();
    }

    /**
     * Initializes the {@link net.java.sip.communicator.plugin.otr.authdialog.GotrAuthenticationDialog} components.
     */
    private void initComponents()
    {
        this.setTitle(OtrActivator.resourceService
            .getI18NString("plugin.otr.authbuddydialog.TITLE"));

        TransparentPanel mainPanel = new TransparentPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.setPreferredSize(new Dimension(350, 400));

        JTextArea generalInformation = new CustomTextArea();
        generalInformation.setText(OtrActivator.resourceService
            .getI18NString("plugin.otr.authbuddydialog.AUTHENTICATION_INFO"));
        mainPanel.add(generalInformation);

        mainPanel.add(Box.createVerticalStrut(10));

        // Add authentication method label and combo box.
        final String am[] = new String[]{
            OtrActivator.resourceService.getI18NString(
                "plugin.otr.authbuddydialog.AUTHENTICATION_METHOD_QUESTION"),
            OtrActivator.resourceService.getI18NString(
                "plugin.otr.authbuddydialog.AUTHENTICATION_METHOD_SECRET"),
            OtrActivator.resourceService.getI18NString(
                "plugin.otr.authbuddydialog.AUTHENTICATION_METHOD_FINGERPRINT")};
        final JComboBox authenticationMethodComboBox =
            new JComboBox(am);
        JTextArea authMethodLabel = new CustomTextArea();
        authMethodLabel.setText(
                OtrActivator.resourceService.getI18NString(
                    "plugin.otr.authbuddydialog.AUTHENTICATION_METHOD"));
        mainPanel.add(authMethodLabel);
        mainPanel.add(authenticationMethodComboBox);
        mainPanel.add(Box.createVerticalStrut(10));

        // Add authentication panels in a card layout so that the user can
        // use the combo box to switch between authentication methods. 
        final JPanel authenticationPanel =
            new TransparentPanel(new CardLayout());
        final SecretQuestionAuthenticationPanel secretQuestionPanel =
            new SecretQuestionAuthenticationPanel(member.getName());
        final SharedSecretAuthenticationPanel sharedSecretPanel =
            new SharedSecretAuthenticationPanel(member.getName());
        authenticationPanel.add(secretQuestionPanel, am[0]);
        authenticationPanel.add(sharedSecretPanel, am[1]);

        authenticationMethodComboBox.addItemListener(new ItemListener()
        {
            @Override
            public void itemStateChanged(ItemEvent e)
            {
                if (e.getStateChange() == ItemEvent.SELECTED)
                {
                    CardLayout cl =
                        (CardLayout) (authenticationPanel.getLayout());
                    cl.show(authenticationPanel, (String)e.getItem());
                }
            }
        });
        authenticationMethodComboBox.setSelectedIndex(0);
        mainPanel.add(authenticationPanel);

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.weightx = 1.0;
        c.gridwidth = 1;

        // Buttons panel.
        JPanel buttonPanel = new TransparentPanel(new GridBagLayout());
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));

        JButton helpButton =
            new JButton(OtrActivator.resourceService
                .getI18NString("plugin.otr.authbuddydialog.HELP"));
        helpButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent arg0)
            {
                OtrActivator.scOtrEngine.launchHelp();
            }
        });

        buttonPanel.add(helpButton, c);

        // Provide space between help and the other two button, not sure if this
        // is optimal..
        c.weightx = 1.0;
        buttonPanel.add(new JLabel(), c);
        c.weightx = 0.0;

        JButton cancelButton =
            new JButton(OtrActivator.resourceService
                .getI18NString("plugin.otr.authbuddydialog.CANCEL"));
        cancelButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                dispose();
            }
        });
        buttonPanel.add(cancelButton, c);

        JButton authenticateButton =
            new JButton(OtrActivator.resourceService
                .getI18NString("plugin.otr.authbuddydialog.AUTHENTICATE_BUDDY"));
        authenticateButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                String authenticationMethod =
                    (String)authenticationMethodComboBox.getSelectedItem();
                if (authenticationMethod.equals(am[0]))
                {
                    String secret = secretQuestionPanel.getSecret();
                    String question = secretQuestionPanel.getQuestion();

                    try {
                        sessionHost.getSession().initSmp(user, question, secret);
                    } catch (GotrException e1) {
                        e1.printStackTrace();
                    }
                    dispose();
                }
                else if (authenticationMethod.equals(am[1]))
                {
                    String secret = sharedSecretPanel.getSecret();
                    String question = null;

                    try {
                        sessionHost.getSession().initSmp(user, question, secret);
                    } catch (GotrException e1) {
                        e1.printStackTrace();
                    }
                    dispose();
                }
            }
        });
        buttonPanel.add(authenticateButton, c);

        this.getContentPane().add(mainPanel, BorderLayout.NORTH);
        this.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        this.pack();
    }
}

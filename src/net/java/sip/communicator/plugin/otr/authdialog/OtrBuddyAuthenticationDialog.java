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
package net.java.sip.communicator.plugin.otr.authdialog;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.plugin.otr.*;
import net.java.sip.communicator.plugin.otr.authdialog.FingerprintAuthenticationPanel.ActionComboBoxItem;


/**
 * @author George Politis
 * @author Marin Dzhigarov
 */
@SuppressWarnings("serial")
public class OtrBuddyAuthenticationDialog
    extends SIPCommDialog
{
    private final AuthenticationDialogBackend backend;

    public OtrBuddyAuthenticationDialog(AuthenticationDialogBackend backend)
    {
        super(false);
        this.backend = backend;

        initComponents();
    }

    /**
     * Initializes the {@link OtrBuddyAuthenticationDialog} components.
     */
    private void initComponents()
    {
        this.setTitle(OtrActivator.resourceService
            .getI18NString("plugin.otr.authbuddydialog.TITLE"));

        TransparentPanel mainPanel = new TransparentPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        //mainPanel.setPreferredSize(new Dimension(350, 400));

        AuthStepPanel authPanel = new AuthStepPanel();
        authPanel.initStep();

        mainPanel.add(authPanel);
        mainPanel.add(Box.createVerticalStrut(10));

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
        final FingerprintAuthenticationPanel fingerprintPanel =
            new FingerprintAuthenticationPanel(backend.getLocalName(),
                    backend.getLocalFingerprint(),
                    backend.getRemoteFingerprint(),
                    backend.getDefaultPetname());
        final SecretQuestionAuthenticationPanel secretQuestionPanel =
            new SecretQuestionAuthenticationPanel(backend.getDefaultPetname());
        final SharedSecretAuthenticationPanel sharedSecretPanel =
            new SharedSecretAuthenticationPanel(backend.getDefaultPetname());
        authenticationPanel.add(secretQuestionPanel, am[0]);
        authenticationPanel.add(sharedSecretPanel, am[1]);
        authenticationPanel.add(fingerprintPanel, am[2]);

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

        final JButton authenticateButton =
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
                    String petname = secretQuestionPanel.getPetname();
                    String secret = secretQuestionPanel.getSecret();
                    String question = secretQuestionPanel.getQuestion();

                    backend.initSmp(petname, question, secret);
                    dispose();
                }
                else if (authenticationMethod.equals(am[1]))
                {
                    String petname = sharedSecretPanel.getPetname();
                    String secret = sharedSecretPanel.getSecret();
                    String question = null;

                    backend.initSmp(petname, question, secret);
                    dispose();
                }
                else if (authenticationMethod.equals(am[2]))
                {
                    ActionComboBoxItem actionItem =
                        (ActionComboBoxItem) fingerprintPanel.
                            getCbAction().getSelectedItem();
                    String fingerprint = backend.getRemoteFingerprint();
                    switch (actionItem.action)
                    {
                    case I_HAVE:
                        OtrActivator.scOtrKeyManager.verify(
                            fingerprintPanel.getPetname(), fingerprint);
                        break;
                    case I_HAVE_NOT:
                        OtrActivator.scOtrKeyManager.unverify(
                            fingerprintPanel.getPetname(), fingerprint);
                        break;
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

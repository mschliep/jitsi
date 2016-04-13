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
import java.security.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.plugin.otr.*;
import net.java.sip.communicator.plugin.otr.OtrContactManager.OtrContact;

/**
 * @author George Politis
 * @author Marin Dzhigarov
 */
@SuppressWarnings("serial")
public class FingerprintAuthenticationPanel
    extends TransparentPanel
    implements DocumentListener
{

    private final JTextField petname = new JTextField();

    private final String accountName;
    private final String localFingerprint;
    private final String remoteFingerprint;
    private final String defaultPetname;

    private SIPCommTextField txtRemoteFingerprintComparison;

    /**
     * Our fingerprint.
     */
    private JTextArea txtLocalFingerprint;

    /**
     * The purported fingerprint of the remote party.
     */
    private JTextArea txtRemoteFingerprint;

    /**
     * The "I have" / "I have not" combo box.
     */
    private JComboBox cbAction;

    private ActionComboBoxItem actionIHave =
        new ActionComboBoxItem(ActionComboBoxItemIndex.I_HAVE);

    private ActionComboBoxItem actionIHaveNot =
        new ActionComboBoxItem(ActionComboBoxItemIndex.I_HAVE_NOT);

    private JTextArea txtAction;

    /**
     * Creates an instance FingerprintAuthenticationPanel
     * @param accountName
     * @param localFingerprint
     * @param remoteFingerprint
     * @param defaultPetname
     */
    FingerprintAuthenticationPanel(String accountName, String localFingerprint, String remoteFingerprint, String defaultPetname)
    {
        this.accountName = accountName;
        this.localFingerprint = localFingerprint;
        this.remoteFingerprint = remoteFingerprint;
        this.defaultPetname = defaultPetname;
        initComponents();
        loadContact();
        
    }

    /**
     * Initializes the {@link FingerprintAuthenticationPanel} components.
     */
    private void initComponents()
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setPreferredSize(new Dimension(350, 300));

        JTextArea generalInformation = new CustomTextArea();
        generalInformation.setText(OtrActivator.resourceService
            .getI18NString(
                "plugin.otr.authbuddydialog.AUTHENTICATION_FINGERPRINT"));
        this.add(generalInformation);

        add(Box.createVerticalStrut(10));

        txtLocalFingerprint = new CustomTextArea();
        this.add(txtLocalFingerprint);

        add(Box.createVerticalStrut(10));

        txtRemoteFingerprint = new CustomTextArea();
        this.add(txtRemoteFingerprint);

        add(Box.createVerticalStrut(10));

        // Action Panel (the panel that holds the I have/I have not dropdown)
        JPanel pnlAction = new JPanel(new GridBagLayout());
        pnlAction.setBorder(BorderFactory.createEtchedBorder());
        this.add(pnlAction);

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(5, 5, 5, 5);
        c.gridy = 0;
        c.gridx = 0;
        c.weightx = 1;

        JLabel petnameLabel =
                new JLabel(
                        OtrActivator.resourceService
                                .getI18NString(
                                        "plugin.otr.authbuddydialog.PETNAME"));
        pnlAction.add(petnameLabel, c);
        c.gridy = 1;
        c.insets = new Insets(0, 5, 5, 5);
        petname.setText(defaultPetname);
        pnlAction.add(petname, c);


        c.weightx = 0.0;
        c.gridy = 2;

        cbAction = new JComboBox();
        cbAction.addItem(actionIHave);
        cbAction.addItem(actionIHaveNot);

        cbAction.setSelectedItem(OtrActivator.scOtrKeyManager
            .isVerified(remoteFingerprint)
                ? actionIHave : actionIHaveNot);

        pnlAction.add(cbAction, c);

        txtAction = new CustomTextArea();
        c.weightx = 1.0;
        c.gridx = 1;
        pnlAction.add(txtAction, c);

        txtRemoteFingerprintComparison = new SIPCommTextField(
        OtrActivator.resourceService
        .getI18NString("plugin.otr.authbuddydialog.FINGERPRINT_CHECK",
                new String[]{defaultPetname}));
        txtRemoteFingerprintComparison.getDocument().addDocumentListener(this);

        c.gridwidth = 2;
        c.gridx = 0;
        c.gridy = 3;
        pnlAction.add(txtRemoteFingerprintComparison, c);
        c.gridwidth = 1;
        c.gridy = 0;
    }

    public JComboBox getCbAction()
    {
        return cbAction;
    }

    /**
     * Sets up the {@link OtrBuddyAuthenticationDialog} components so that they
     * reflect the remote contact.
     */
    private void loadContact()
    {
        txtLocalFingerprint.setText(OtrActivator.resourceService.getI18NString(
            "plugin.otr.authbuddydialog.LOCAL_FINGERPRINT", new String[]
            { accountName, localFingerprint }));

        txtRemoteFingerprint.setText(OtrActivator.resourceService
            .getI18NString("plugin.otr.authbuddydialog.REMOTE_FINGERPRINT",
                new String[]
                { defaultPetname, remoteFingerprint }));

        // Action
        txtAction.setText(OtrActivator.resourceService.getI18NString(
            "plugin.otr.authbuddydialog.VERIFY_ACTION", new String[]
            { defaultPetname }));
    }

    public void removeUpdate(DocumentEvent e)
    {
        compareFingerprints();
    }

    public void insertUpdate(DocumentEvent e)
    {
        compareFingerprints();
    }

    public void changedUpdate(DocumentEvent e)
    {
        compareFingerprints();
    }

    public void compareFingerprints()
    {
        if(txtRemoteFingerprintComparison.getText() == null
            || txtRemoteFingerprintComparison.getText().length() == 0)
        {
            txtRemoteFingerprintComparison.setBackground(Color.white);
            return;
        }
        if(txtRemoteFingerprintComparison.getText().toLowerCase().contains(
            remoteFingerprint.toLowerCase()))
        {
            txtRemoteFingerprintComparison.setBackground(Color.green);
            cbAction.setSelectedItem(actionIHave);
        }
        else
        {
            txtRemoteFingerprintComparison.setBackground(
                new Color(243, 72, 48));
            cbAction.setSelectedItem(actionIHaveNot);
        }
    }

    public String getPetname()
    {
        return petname.getText();
    }

    /**
     * A simple enumeration that is meant to be used with
     * {@link ActionComboBoxItem} to distinguish them (like an ID).
     *
     * @author George Politis
     */
    enum ActionComboBoxItemIndex
    {
        I_HAVE, I_HAVE_NOT
    }

    /**
     * A special {@link JComboBox}.
     *
     * @author George Politis
     */
    class ActionComboBoxItem
    {
        public ActionComboBoxItemIndex action;

        private String text;

        public ActionComboBoxItem(ActionComboBoxItemIndex actionIndex)
        {
            this.action = actionIndex;
            switch (action)
            {
            case I_HAVE:
                text =
                    OtrActivator.resourceService
                        .getI18NString("plugin.otr.authbuddydialog.I_HAVE");
                break;
            case I_HAVE_NOT:
                text =
                    OtrActivator.resourceService
                        .getI18NString("plugin.otr.authbuddydialog.I_HAVE_NOT");
                break;
            }
        }

        @Override
        public String toString()
        {
            return text;
        }
    }
}

/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.otr.authdialog;

import net.java.sip.communicator.plugin.desktoputil.SIPCommDialog;
import net.java.sip.communicator.plugin.desktoputil.TransparentPanel;
import net.java.sip.communicator.plugin.otr.OtrActivator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * The dialog that pops up when the remote party send us SMP
 * request. It contains detailed information for the user about
 * the authentication process and allows him to authenticate.
 * 
 * @author Marin Dzhigarov
 */
@SuppressWarnings("serial")
public class SmpAuthenticateBuddyDialog
    extends SIPCommDialog
{

    private final AuthenticationDialogBackend backend;

    private final String question;

    public SmpAuthenticateBuddyDialog(String question, AuthenticationDialogBackend backend)
    {
        this.backend = backend;
        this.question = question;
        initComponents();
    }

    private void initComponents()
    {
        this.setTitle(
            OtrActivator.resourceService
                .getI18NString(
                    "plugin.otr.authbuddydialog.TITLE"));

        // The main panel that contains all components.
        JPanel mainPanel = new TransparentPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(
            BorderFactory.createEmptyBorder(10, 10, 10, 10));
        //mainPanel.setPreferredSize(new Dimension(300, 350));

        AuthStepPanel authPanel = new AuthStepPanel();
        authPanel.awaitingStep();

        mainPanel.add(authPanel);
        mainPanel.add(Box.createVerticalStrut(10));

        // Add "authentication from contact" to the main panel.
        JTextArea authenticationFrom = new CustomTextArea();
        Font newFont =
            new Font(
                UIManager.getDefaults().getFont("TextArea.font").
                    getFontName()
                , Font.BOLD
                , 14);
        authenticationFrom.setFont(newFont);
        String authFromText =
            String.format(
                OtrActivator.resourceService
                    .getI18NString(
                        "plugin.otr.authbuddydialog.AUTHENTICATION_FROM",
                        new String[]
                            {backend.getDefaultPetname()}));
        authenticationFrom.setText(authFromText);
        mainPanel.add(authenticationFrom);

        // Add "general info" text to the main panel.
        JTextArea generalInfo = new CustomTextArea();
        generalInfo.setText(OtrActivator.resourceService
            .getI18NString(
                "plugin.otr.authbuddydialog.AUTHENTICATION_INFO"));
        mainPanel.add(generalInfo);

        // Add "authentication-by-secret" info text to the main panel.
        JTextArea authBySecretInfo = new CustomTextArea();
        newFont =
            new Font(
                UIManager.getDefaults().getFont("TextArea.font").
                    getFontName()
                , Font.ITALIC
                , 10);
        authBySecretInfo.setText(OtrActivator.resourceService
            .getI18NString(
                "plugin.otr.authbuddydialog.AUTH_BY_SECRET_INFO_RESPOND"));
        authBySecretInfo.setFont(newFont);
        mainPanel.add(authBySecretInfo);

        // Create a panel to add question/answer related components
        JPanel questionAnswerPanel = new JPanel(new GridBagLayout());
        questionAnswerPanel.setBorder(BorderFactory.createEtchedBorder());

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(5, 5, 0, 5);
        c.weightx = 0;

        //Add petname prompt
        JLabel petnameLabel =
                new JLabel(
                        OtrActivator.resourceService
                                .getI18NString(
                                        "plugin.otr.authbuddydialog.PETNAME"));
        questionAnswerPanel.add(petnameLabel, c);
        c.gridy = 1;
        c.insets = new Insets(0, 5, 5, 5);
        final JTextField petname= new JTextField();
        petname.setText(backend.getDefaultPetname());
        questionAnswerPanel.add(petname, c);

        if(question != null && !question.isEmpty()) {
            // Add question label.
            c.gridy = 2;
            c.insets = new Insets(5, 5, 0, 5);
            JLabel questionLabel =
                    new JLabel(
                            OtrActivator.resourceService
                                    .getI18NString(
                                            "plugin.otr.authbuddydialog.QUESTION_RESPOND"));
            questionAnswerPanel.add(questionLabel, c);

            // Add the question.
            c.insets = new Insets(0, 5, 5, 5);
            c.gridy = 3;
            JTextArea questionArea =
                    new CustomTextArea();
            newFont =
                    new Font(
                            UIManager.getDefaults().getFont("TextArea.font").
                                    getFontName()
                            , Font.BOLD
                            , UIManager.getDefaults().getFont("TextArea.font")
                            .getSize());
            questionArea.setFont(newFont);
            questionArea.setText(question);
            questionAnswerPanel.add(questionArea, c);

            // Add answer label.
            c.insets = new Insets(5, 5, 5, 5);
            c.gridy = 4;
            JLabel answerLabel =
                    new JLabel(OtrActivator.resourceService
                            .getI18NString("plugin.otr.authbuddydialog.ANSWER"));
            questionAnswerPanel.add(answerLabel, c);
        }
        else{
            // Add shared secret label.
            c.insets = new Insets(5, 5, 5, 5);
            c.gridy = 4;
            JLabel shareSecretLabel =
                    new JLabel(OtrActivator.resourceService
                            .getI18NString("plugin.otr.authbuddydialog.SHARED_SECRET"));
            questionAnswerPanel.add(shareSecretLabel, c);
        }

        // Add the answer text field.
        c.gridy = 5;
        final JTextField answerTextBox = new JTextField();
        questionAnswerPanel.add(answerTextBox, c);

        // Add the question/answer panel to the main panel.
        mainPanel.add(questionAnswerPanel);

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

        c.gridwidth = 1;
        c.gridy = 0;
        c.gridx = 0;
        c.weightx = 0;
        c.insets = new Insets(5, 5, 5, 20);
        buttonPanel.add(helpButton, c);

        JButton cancelButton =
            new JButton(OtrActivator.resourceService
                .getI18NString("plugin.otr.authbuddydialog.CANCEL"));
        cancelButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                backend.abortSmp();
                SmpAuthenticateBuddyDialog.this.dispose();
            }
        });
        c.insets = new Insets(5, 5, 5, 5);
        c.gridx = 1;
        buttonPanel.add(cancelButton, c);

        c.gridx = 2;
        JButton authenticateButton =
            new JButton(OtrActivator.resourceService
                .getI18NString(
                    "plugin.otr.authbuddydialog.AUTHENTICATE_BUDDY"));
        authenticateButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                backend.respondSmp(petname.getText(), question, answerTextBox.getText());
                SmpAuthenticateBuddyDialog.this.dispose();
            }
        });

        buttonPanel.add(authenticateButton, c);

        this.getContentPane().add(mainPanel, BorderLayout.NORTH);
        this.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        this.pack();
    }
}

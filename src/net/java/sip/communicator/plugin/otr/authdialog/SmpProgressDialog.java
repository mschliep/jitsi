/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.otr.authdialog;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

import javax.swing.*;
import javax.swing.border.Border;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.plugin.otr.*;
import net.java.sip.communicator.util.Logger;


/**
 * The dialog that pops up when SMP negotiation starts.
 * It contains a progress bar that indicates the status of the SMP
 * authentication process.
 * 
 * @author Marin Dzhigarov
 */
@SuppressWarnings("serial")
public class SmpProgressDialog
    extends SIPCommDialog
{

    private static final Logger logger = Logger.getLogger(SmpProgressDialog.class);

    private final JLabel iconLabel = new JLabel();

    private final AuthStepPanel authStepPanel = new AuthStepPanel();

    /**
     * Instantiates SmpProgressDialog.
     * 
     * @param name The contact that this dialog is associated with.
     */
    public SmpProgressDialog(String name)
    {
        setTitle(
            OtrActivator.resourceService.getI18NString(
                "plugin.otr.smpprogressdialog.TITLE"));

        JPanel mainPanel = new TransparentPanel();
        mainPanel.setLayout(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.setPreferredSize(new Dimension(500, 400));

        String authFromText =
            String.format(
                OtrActivator.resourceService
                    .getI18NString(
                        "plugin.otr.authbuddydialog.AUTHENTICATION_FROM",
                        new String[] {name}));

        JPanel labelsPanel = new TransparentPanel();
        labelsPanel.setLayout(new BoxLayout(labelsPanel, BoxLayout.LINE_AXIS));

        labelsPanel.add(iconLabel);
        labelsPanel.add(Box.createRigidArea(new Dimension(5,0)));
        labelsPanel.add(new JLabel(authFromText));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weighty = 0;

        mainPanel.add(labelsPanel, c);

        c.gridx=0;
        c.gridy=1;
        c.weighty=1;
        c.weightx=1;

        authStepPanel.awaitingStep();
        mainPanel.add(authStepPanel, c);

        this.getContentPane().add(mainPanel);
        this.pack();
        this.setPreferredSize(this.getPreferredSize());
    }
}

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

    private static final Color successColor = new Color(86, 140, 2);

    private static final Color inProgressColor = Color.YELLOW;

    private static final Color failColor = new Color(204, 0, 0);

    private static final Color waitColor = Color.GRAY;

    private final JLabel iconLabel = new JLabel();
    private final JLabel successLabel = new JLabel("Authenticated");

    private final AuthStepPanel firstAuthStepPanel = new AuthStepPanel("Initiate", 1, successColor);
    private final AuthStepPanel secondAuthStepPanel = new AuthStepPanel("Response", 2, waitColor);
    private final AuthStepPanel thirdAuthStepPanel = new AuthStepPanel("Authenticate", 3, waitColor);

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

        JPanel authPanel = new TransparentPanel();
        authPanel.setLayout(new GridLayout(1, 3));
        authPanel.add(firstAuthStepPanel);
        authPanel.add(secondAuthStepPanel);
        authPanel.add(thirdAuthStepPanel);

        c.gridx=0;
        c.gridy=1;
        c.weighty=1;

        mainPanel.add(authPanel, c);

        c.gridx = 0;
        c.gridy = 2;
        c.weighty = 0;
        mainPanel.add(successLabel, c);

        init();

        this.getContentPane().add(mainPanel);
        this.pack();
        this.setPreferredSize(this.getPreferredSize());
    }

    /**
     * Initializes the progress bar and sets it's progression to 1/3.
     */
    public void init()
    {
        firstAuthStepPanel.updateColor(successColor);
        secondAuthStepPanel.updateColor(inProgressColor);
        thirdAuthStepPanel.updateColor(waitColor);
        iconLabel.setIcon(
            OtrActivator.resourceService.getImage(
                "plugin.otr.ENCRYPTED_UNVERIFIED_ICON_22x22"));

        successLabel.setForeground(successColor);
        successLabel.setVisible(false);
    }

    /**
     * Sets the progress bar to 2/3 of completion.
     */
    public void incrementProgress()
    {
        firstAuthStepPanel.updateColor(successColor);
        secondAuthStepPanel.updateColor(successColor);
        thirdAuthStepPanel.updateColor(inProgressColor);
    }

    /**
     * Sets the progress bar to green.
     */
    public void setProgressSuccess()
    {
        firstAuthStepPanel.updateColor(successColor);
        secondAuthStepPanel.updateColor(successColor);
        thirdAuthStepPanel.updateColor(successColor);
        successLabel.setVisible(true);
        iconLabel.setIcon(
            OtrActivator.resourceService.getImage(
                "plugin.otr.ENCRYPTED_ICON_22x22"));
    }

    /**
     * Sets the progress bar to red.
     */
    public void setProgressFail()
    {
        firstAuthStepPanel.updateColor(successColor);
        secondAuthStepPanel.updateColor(successColor);
        thirdAuthStepPanel.updateColor(failColor);

        successLabel.setForeground(failColor);
        successLabel.setText("Authentication Failed");
        successLabel.setVisible(true);
    }

    private class ProgressCircle extends JPanel{

        private final static int STROKE_WIDTH = 10;
        private final static int PADDING = 5;
        private final static float FONT_SIZE = 38f;

        private final int stepNum;
        private Color color;

        private ProgressCircle(int stepNum, Color color) {
            this.stepNum = stepNum;
            this.color = color;
        }

        @Override
        public void paint(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setColor(color);
            g2d.setStroke(new BasicStroke(STROKE_WIDTH));
            Font font = g2d.getFont().deriveFont(FONT_SIZE);
            g2d.setFont(font);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Rectangle rect = g2d.getClipBounds();
            double size = Math.min(rect.width-STROKE_WIDTH-(2*PADDING), rect.height-STROKE_WIDTH-(2*PADDING));
            size = Math.max(0, size);
            double xOffset = (rect.width-size)/2;
            double yOffset = (rect.height-size)/2;
            logger.debug(String.format("X: %f, Y: %f.", xOffset, yOffset));
            Ellipse2D circle = new Ellipse2D.Double(xOffset, yOffset, size, size);
            g2d.draw(circle);
            centerString(g2d, String.valueOf(stepNum), ((size) / 2)+xOffset, ((size) / 2)+yOffset);
        }

        public void centerString(Graphics2D g2d, String string, double w, double h){
            FontMetrics fm = g2d.getFontMetrics();

            Rectangle2D bounds = fm.getStringBounds(string, g2d);

            double x = w - (bounds.getWidth() / 2);
            double y = h - ((bounds.getHeight() / 2)) + fm.getAscent();

            g2d.drawString(string, (float) x, (float) y);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(100, 100);
        }

        @Override
        public Dimension getMinimumSize() {
            return new Dimension(100, 100);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(100, 100);
        }

        public void updateColor(Color color){
            this.color = color;
        }
    }

    private class AuthStepPanel extends JPanel{

        private final ProgressCircle circle;
        private final JLabel label;

        private AuthStepPanel(String step, int stepNum, Color color) {
            super();

            this.setLayout(new GridBagLayout());
            this.setOpaque(false);

            circle = new ProgressCircle(stepNum, color);

            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.weighty = 1.0;
            c.anchor = GridBagConstraints.PAGE_END;

            this.add(circle, c);

            label = new JLabel(step, SwingConstants.CENTER);
            label.setForeground(color);
            c.gridx = 0;
            c.gridy = 1;
            c.weighty = 0.0;
            c.anchor = GridBagConstraints.PAGE_START;

            this.add(label, c);
        }

        public void updateColor(Color color){
            label.setForeground(color);
            circle.updateColor(color);
        }
    }
}

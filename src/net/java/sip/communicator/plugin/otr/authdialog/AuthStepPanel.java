package net.java.sip.communicator.plugin.otr.authdialog;

import net.java.sip.communicator.plugin.desktoputil.TransparentPanel;
import net.java.sip.communicator.plugin.otr.OtrActivator;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

public class AuthStepPanel extends  JPanel{

    private static final Color successColor = new Color(86, 140, 2);

    private static final Color inProgressColor = new Color(231, 160, 32);

    private static final Color failColor = new Color(204, 0, 0);

    private static final Color waitColor = Color.GRAY;

    private final AuthStep firstAuthStep = new AuthStep("Initiate", 1, inProgressColor);
    private final AuthStep secondAuthStep = new AuthStep("Response", 2, waitColor);
    private final AuthStep thirdAuthStep = new AuthStep("Authenticate", 3, waitColor);

    public AuthStepPanel(){
        this.setOpaque(false);

        this.setLayout(new GridLayout(1, 3));

        this.add(firstAuthStep);
        this.add(secondAuthStep);
        this.add(thirdAuthStep);
    }

    public void initStep(){
        firstAuthStep.updateColor(inProgressColor);
        secondAuthStep.updateColor(waitColor);
        thirdAuthStep.updateColor(waitColor);
    }

    public void awaitingStep(){
        firstAuthStep.updateColor(successColor);
        secondAuthStep.updateColor(inProgressColor);
        thirdAuthStep.updateColor(waitColor);
    }

    public void failStep() {
        firstAuthStep.updateColor(successColor);
        secondAuthStep.updateColor(successColor);
        thirdAuthStep.updateColor(failColor);
    }

    public void successStep() {
        firstAuthStep.updateColor(successColor);
        secondAuthStep.updateColor(successColor);
        thirdAuthStep.updateColor(successColor);
    }

    private class AuthStep extends JPanel {

        private final ProgressCircle circle;
        private final JLabel label;

        private AuthStep(String step, int stepNum, Color color) {
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

        public void updateColor(Color color){
            this.color = color;
        }
    }
}

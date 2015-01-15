/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.otr.authdialog;

import java.awt.*;
import java.util.*;
import java.util.List;

import javax.swing.table.*;

import net.java.sip.communicator.plugin.otr.*;
import net.java.sip.communicator.plugin.otr.OtrContactManager.OtrContact;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;

import org.osgi.framework.*;

/**
 * A special {@link Panel} for fingerprints display.
 *
 * @author George Politis
 * @author Yana Stamcheva
 */
public class KnownFingerprintsTableModel
    extends AbstractTableModel
    implements ScOtrKeyManagerListener
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    public static final int CONTACTNAME_INDEX = 0;

    public static final int VERIFIED_INDEX = 1;

    public static final int FINGERPRINT_INDEX = 2;

    public final List<String> fingerprints;

    public KnownFingerprintsTableModel()
    {
        fingerprints = OtrActivator.scOtrKeyManager.getAllRemoteFingerprints();
        OtrActivator.scOtrKeyManager.addListener(this);
    }

    /**
     * Implements AbstractTableModel#getColumnName(int).
     */
    @Override
    public String getColumnName(int column)
    {
        switch (column)
        {
        case CONTACTNAME_INDEX:
            return OtrActivator.resourceService
                .getI18NString(
                    "plugin.otr.configform.COLUMN_NAME_CONTACT");
        case VERIFIED_INDEX:
            return OtrActivator.resourceService
                .getI18NString(
                    "plugin.otr.configform.COLUMN_NAME_VERIFIED_STATUS");
        case FINGERPRINT_INDEX:
            return OtrActivator.resourceService
                .getI18NString(
                    "plugin.otr.configform.FINGERPRINT");
        default:
            return null;
        }
    }

    /**
     * Implements AbstractTableModel#getValueAt(int,int).
     */
    public Object getValueAt(int row, int column)
    {
        String petname = getPetnameFromRow(row);
        String fingerprint = getFingerprintFromRow(row);
        switch (column)
        {
        case CONTACTNAME_INDEX:
            return petname;
        case VERIFIED_INDEX:
            // TODO: Maybe use a CheckBoxColumn?
            return (OtrActivator.scOtrKeyManager.isVerified(fingerprint))
                ? OtrActivator.resourceService.getI18NString(
                    "plugin.otr.configform.COLUMN_VALUE_VERIFIED_TRUE")
                : OtrActivator.resourceService.getI18NString(
                    "plugin.otr.configform.COLUMN_VALUE_VERIFIED_FALSE");
        case FINGERPRINT_INDEX:
            return fingerprint;
        default:
            return null;
        }
    }

    String getPetnameFromRow(int row)
    {
        if (row < 0 || row >= getRowCount())
            return null;

        String fingerprint = getFingerprintFromRow(row);

        return OtrActivator.scOtrKeyManager.getPetname(fingerprint);
    }

    String getFingerprintFromRow(int row)
    {
        if (row < 0 || row >= getRowCount())
            return null;

        return fingerprints.get(row);
    }

    /**
     * Implements AbstractTableModel#getRowCount().
     */
    public int getRowCount()
    {
        return fingerprints.size();
    }

    /**
     * Implements AbstractTableModel#getColumnCount().
     */
    public int getColumnCount()
    {
        return 3;
    }

    @Override
    public void verificationStatusChanged(String fingerprint)
    {
        if(!fingerprints.contains(fingerprint))
        {
            fingerprints.add(fingerprint);
        }
    }
}

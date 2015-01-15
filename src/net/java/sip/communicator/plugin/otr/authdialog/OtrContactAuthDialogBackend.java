package net.java.sip.communicator.plugin.otr.authdialog;

import net.java.sip.communicator.plugin.otr.OtrActivator;
import net.java.sip.communicator.plugin.otr.OtrContactManager;

import java.security.PublicKey;

public class OtrContactAuthDialogBackend
    implements AuthenticationDialogBackend
{

    private final OtrContactManager.OtrContact contact;

    public OtrContactAuthDialogBackend(OtrContactManager.OtrContact contact)
    {
        this.contact = contact;
    }

    @Override
    public String getDefaultPetname()
    {
        return contact.contact.getDisplayName();
    }

    @Override
    public String getLocalName()
    {
        String account =
                contact.contact.getProtocolProvider().getAccountID().getDisplayName();

        return account;
    }

    @Override
    public String getLocalFingerprint()
    {
        String localFingerprint =
                OtrActivator.scOtrKeyManager.getLocalFingerprint(contact.contact
                        .getProtocolProvider().getAccountID());

        return localFingerprint;
    }

    @Override
    public String getRemoteFingerprint()
    {
        PublicKey remoteKey = OtrActivator.scOtrEngine.getRemotePublicKey(contact);
        return OtrActivator.scOtrKeyManager.getFingerprintFromPublicKey(remoteKey);
    }

    @Override
    public void initSmp(String petname, String question, String answer)
    {
        OtrActivator.scOtrKeyManager.saveFingerprint(petname, getRemoteFingerprint());
        OtrActivator.scOtrEngine.initSmp(contact, question, answer);
    }
}

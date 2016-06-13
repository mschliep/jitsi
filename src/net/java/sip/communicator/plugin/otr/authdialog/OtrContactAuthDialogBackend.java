package net.java.sip.communicator.plugin.otr.authdialog;

import net.java.otr4j.session.InstanceTag;
import net.java.sip.communicator.plugin.otr.OtrActivator;
import net.java.sip.communicator.plugin.otr.OtrContactManager;

import java.security.PublicKey;

public class OtrContactAuthDialogBackend
    implements AuthenticationDialogBackend
{

    private final OtrContactManager.OtrContact contact;
    private final InstanceTag receiverTag;

    public OtrContactAuthDialogBackend(OtrContactManager.OtrContact contact, InstanceTag receiverTag)
    {
        this.contact = contact;
        this.receiverTag = receiverTag;
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
                OtrActivator.scOtrKeyManager.getLocalOtrFingerprint(contact.contact
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

    @Override
    public void abortSmp() {
        OtrActivator.scOtrEngine.abortSmp(contact);
    }

    @Override
    public void respondSmp(String petname, String question, String secret) {
        PublicKey pubKey = OtrActivator.scOtrEngine.getRemotePublicKey(contact);
        String fingerprint = OtrActivator.scOtrKeyManager.getFingerprintFromPublicKey(pubKey);
        OtrActivator.scOtrKeyManager.saveFingerprint(petname, fingerprint);
        OtrActivator.scOtrEngine.respondSmp(contact, receiverTag, question, secret);
    }
}

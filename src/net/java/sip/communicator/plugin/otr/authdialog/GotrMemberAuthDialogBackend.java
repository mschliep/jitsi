package net.java.sip.communicator.plugin.otr.authdialog;

import net.java.sip.communicator.plugin.otr.OtrActivator;
import net.java.sip.communicator.plugin.otr.OtrContactManager;
import net.java.sip.communicator.plugin.otr.ScGotrSessionHost;
import net.java.sip.communicator.service.protocol.ChatRoomMember;

import java.security.PublicKey;

public class GotrMemberAuthDialogBackend
    implements AuthenticationDialogBackend
{

    private final ScGotrSessionHost host;
    private final ChatRoomMember member;


    public GotrMemberAuthDialogBackend(ScGotrSessionHost host, ChatRoomMember member)
    {
        this.host = host;
        this.member = member;
    }

    @Override
    public String getDefaultPetname()
    {
        return member.getName();
    }

    @Override
    public String getLocalName()
    {
        String account = host.getProtocolProvider().getAccountID().getDisplayName();
        return account;
    }

    @Override
    public String getLocalFingerprint()
    {
        String localFingerprint = OtrActivator.scOtrKeyManager
                .getLocalFingerprint(host.getProtocolProvider().getAccountID());

        return localFingerprint;
    }

    @Override
    public String getRemoteFingerprint()
    {
        return host.getRemoteFingerprint(member);
    }

    @Override
    public void initSmp(String petname, String question, String secret)
    {
        OtrActivator.scOtrKeyManager.saveFingerprint(petname, getRemoteFingerprint());
        host.initSmp(member, question, secret);
    }
}

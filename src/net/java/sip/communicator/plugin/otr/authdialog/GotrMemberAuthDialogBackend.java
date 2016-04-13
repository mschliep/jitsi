package net.java.sip.communicator.plugin.otr.authdialog;

import net.java.gotr4j.GotrUser;
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
    private final GotrUser user;


    public GotrMemberAuthDialogBackend(ScGotrSessionHost host, ChatRoomMember member, GotrUser user)
    {
        this.host = host;
        this.member = member;
        this.user = user;
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

    @Override
    public void abortSmp() {
        host.abortSmp(user);
    }

    @Override
    public void respondSmp(String petname, String question, String secret) {
        String fingerprint = host.getRemoteFingerprint(member);
        OtrActivator.scOtrKeyManager.saveFingerprint(petname, fingerprint);
        host.respondSmp(user, question, secret);
    }
}

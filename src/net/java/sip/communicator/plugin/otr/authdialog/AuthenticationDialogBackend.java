package net.java.sip.communicator.plugin.otr.authdialog;

public interface AuthenticationDialogBackend
{
    public String getDefaultPetname();

    public String getLocalName();

    public String getLocalFingerprint();

    public String getRemoteFingerprint();

    public void initSmp(String petname, String question, String secret);

}

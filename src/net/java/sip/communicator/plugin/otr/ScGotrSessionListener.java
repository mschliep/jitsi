package net.java.sip.communicator.plugin.otr;

public interface ScGotrSessionListener
{

    /**
     * Called when the status of the GOTR session has changed.
     *
     * @param host of the session that changed status
     */
    public void statusChanged(ScGotrSessionHost host);

}

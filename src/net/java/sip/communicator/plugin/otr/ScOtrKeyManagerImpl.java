/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.otr;

import net.java.gotr4j.crypto.GotrCrypto;
import net.java.gotr4j.crypto.GotrException;
import net.java.otr4j.crypto.OtrCryptoEngineImpl;
import net.java.otr4j.crypto.OtrCryptoException;
import net.java.sip.communicator.plugin.otr.OtrContactManager.OtrContact;
import net.java.sip.communicator.service.protocol.AccountID;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.util.Logger;

import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.List;
import java.util.Vector;

/**
 *
 * @author George Politis
 * @author Lyubomir Marinov
 */
public class ScOtrKeyManagerImpl
    implements ScOtrKeyManager
{
    private static final Logger logger = Logger.getLogger(ScOtrKeyManagerImpl.class);

    private final OtrConfigurator configurator = new OtrConfigurator();

    private final List<ScOtrKeyManagerListener> listeners =
        new Vector<ScOtrKeyManagerListener>();

    private GotrCrypto gotrCryptoEngine;

    public ScOtrKeyManagerImpl(){
        try {
            gotrCryptoEngine = new GotrCrypto();
        } catch (GotrException e) {
            logger.error("Unable to build Gotr crypto engine", e);
            gotrCryptoEngine = null;
        }
    }

    public void addListener(ScOtrKeyManagerListener l)
    {
        synchronized (listeners)
        {
            if (!listeners.contains(l))
                listeners.add(l);
        }
    }

    /**
     * Gets a copy of the list of <tt>ScOtrKeyManagerListener</tt>s registered
     * with this instance which may safely be iterated without the risk of a
     * <tt>ConcurrentModificationException</tt>.
     *
     * @return a copy of the list of <tt>ScOtrKeyManagerListener<tt>s registered
     * with this instance which may safely be iterated without the risk of a
     * <tt>ConcurrentModificationException</tt>
     */
    private ScOtrKeyManagerListener[] getListeners()
    {
        synchronized (listeners)
        {
            return
                listeners.toArray(
                        new ScOtrKeyManagerListener[listeners.size()]);
        }
    }

    public void removeListener(ScOtrKeyManagerListener l)
    {
        synchronized (listeners)
        {
            listeners.remove(l);
        }
    }

    public void verify(OtrContact otrContact, String fingerprint)
    {
        if ((fingerprint == null) || otrContact == null)
            return;

        String petname = otrContact.contact.getDisplayName();

        configurator.setVerified(petname, fingerprint, true);
    }

    public void unverify(OtrContact otrContact, String fingerprint)
    {
        if ((fingerprint == null) || otrContact == null)
            return;

        String petname = otrContact.contact.getDisplayName();

        configurator.setVerified(petname, fingerprint, false);
    }

    public boolean isVerified(Contact contact, String fingerprint)
    {
        if (fingerprint == null || contact == null)
            return false;

        boolean old =  this.configurator.getPropertyBoolean(
            contact.getAddress() + fingerprint
                + ".fingerprint.verified", false);

        if(old)
        {
            configurator.removeProperty(contact.getAddress() + fingerprint
                    + ".fingerprint.verified");


            String petname = contact.getDisplayName();
            configurator.setVerified(petname, fingerprint, true);
            return true;
        }
        else
        {
            return configurator.isVerified(null, fingerprint);
        }
    }

    public List<String> getAllRemoteFingerprints()
    {
        return configurator.getAllRemoteFingerprints();
    }

    public String getFingerprintFromPublicKey(PublicKey pubKey)
    {
        try
        {
            return new OtrCryptoEngineImpl().getFingerprint(pubKey);
        }
        catch (OtrCryptoException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    public String getLocalOtrFingerprint(AccountID account)
    {
        KeyPair keyPair = loadOtrKeyPair(account);

        if (keyPair == null)
            return null;

        PublicKey pubKey = keyPair.getPublic();

        try
        {
            return new OtrCryptoEngineImpl().getFingerprint(pubKey);
        }
        catch (OtrCryptoException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    public byte[] getLocalOtrFingerprintRaw(AccountID account)
    {
        KeyPair keyPair = loadOtrKeyPair(account);

        if (keyPair == null)
            return null;

        PublicKey pubKey = keyPair.getPublic();

        try
        {
            return new OtrCryptoEngineImpl().getFingerprintRaw(pubKey);
        }
        catch (OtrCryptoException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    public void saveFingerprint(String petname, String fingerprint)
    {
        if (petname == null)
            return;

        configurator.setVerified(petname, fingerprint, false);
    }

    public KeyPair loadOtrKeyPair(AccountID account)
    {
        KeyPair result = configurator.getOtrKeyPair(account);

        if(result == null)
        {
            return loadAndUpdateKeyPair(account);
        }
        else
        {
            logger.debug(String.format("%s was not null?", account.getAccountAddress()));
            return result;
        }
    }

    private KeyPair loadAndUpdateKeyPair(AccountID account)
    {
        logger.debug(String.format("Migrating %s.", account.getAccountAddress()));
        if (account == null)
            return null;

        String accountID = account.getAccountUniqueID();
        // Load Private Key.
        byte[] b64PrivKey =
            this.configurator.getPropertyBytes(accountID + ".privateKey");
        if (b64PrivKey == null)
            return null;

        PKCS8EncodedKeySpec privateKeySpec =
            new PKCS8EncodedKeySpec(b64PrivKey);

        // Load Public Key.
        byte[] b64PubKey =
            this.configurator.getPropertyBytes(accountID + ".publicKey");
        if (b64PubKey == null)
            return null;

        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(b64PubKey);

        PublicKey publicKey;
        PrivateKey privateKey;

        // Generate KeyPair.
        KeyFactory keyFactory;
        try
        {
            keyFactory = KeyFactory.getInstance("DSA");
            publicKey = keyFactory.generatePublic(publicKeySpec);
            privateKey = keyFactory.generatePrivate(privateKeySpec);
        }
        catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
            return null;
        }
        catch (InvalidKeySpecException e)
        {
            e.printStackTrace();
            return null;
        }

        KeyPair keyPair = new KeyPair(publicKey, privateKey);

        configurator.removeProperty(accountID + ".privateKey");
        configurator.removeProperty(accountID + ".publicKey");

        configurator.setOtrKeyPair(account, keyPair);

        return keyPair;
    }

    public void generateOtrKeyPair(AccountID account)
    {
        if (account == null)
            return;

        KeyPair keyPair;
        try
        {
            keyPair = KeyPairGenerator.getInstance("DSA").genKeyPair();
            configurator.setOtrKeyPair(account, keyPair);
        }
        catch (NoSuchAlgorithmException e)
        {
            logger.error("Failed to create KeyPair.", e);
        }
    }

    @Override
    public KeyPair loadGotrKeyPair(AccountID accountID) {
        return configurator.getGotrKeyPair(accountID);
    }

    @Override
    public void generateGotrKeyPair(AccountID account) {
        if (account == null)
            return;

        KeyPair keyPair;
        keyPair = gotrCryptoEngine.generateKeyPair();
        configurator.setGotrKeyPair(account, keyPair);
    }

    @Override
    public String getPetname(String fingerprint)
    {
        return configurator.getPetname(fingerprint);
    }

    @Override
    public boolean isVerified(String fingerprint)
    {
        return configurator.isVerified(null, fingerprint);
    }

    @Override
    public void verify(String petname, String fingerprint)
    {
        boolean old = configurator.isVerified(petname, fingerprint);
        configurator.setVerified(petname, fingerprint, true);

        if(!old) {
            for (ScOtrKeyManagerListener l : getListeners())
                l.verificationStatusChanged(fingerprint);
        }
    }

    @Override
    public void unverify(String petname, String fingerprint)
    {
        boolean old = configurator.isVerified(petname, fingerprint);
        configurator.setVerified(petname, fingerprint, false);

        if(old) {
            for (ScOtrKeyManagerListener l : getListeners())
                l.verificationStatusChanged(fingerprint);
        }
    }

    @Override
    public String getLocalGotrFingerprint(AccountID accountID) {
        KeyPair keyPair = loadGotrKeyPair(accountID);

        if (keyPair == null)
            return null;

        PublicKey pubKey = keyPair.getPublic();

        try {
            return gotrCryptoEngine.getFingerprint(pubKey);
        } catch (NoSuchAlgorithmException e) {
            logger.error(e);
            return null;
        }
    }
}

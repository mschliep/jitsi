/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.otr;

import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

import net.java.gotr4j.crypto.GotrCrypto;
import net.java.sip.communicator.service.protocol.AccountID;
import net.java.sip.communicator.util.Logger;
import org.bouncycastle.util.encoders.Base64; // disambiguation
import org.jitsi.service.configuration.*;

/**
 * A class that gets/sets the OTR configuration values. Introduced to assure our
 * configuration is properly written when <tt>XMLConfigurationStore</tt> is
 * used. Can be seen as a proxy between the {@link ConfigurationService} and the
 * OTR Plugin.
 *
 * @author George Politis
 */
public class OtrConfigurator
{
    private static final String KEY_ALGORITHM = "DSA";

    private static final String CONFIG_PREFIX =
            "net.java.sip.communicator.plugin.otr";

    private static final String ACCOUNT_PREFIX = CONFIG_PREFIX + ".account";

    private static final String OTR_INFIX = "otr";
    private static final String GOTR_INFIX = "gotr";

    private static final String UID_SUFFIX = "uid";
    private static final String PRIV_KEY_SUFFIX = "private";
    private static final String PUB_KEY_SUFFIX = "public";

    private static final String FINGERPRINT_PREFIX = CONFIG_PREFIX+".fingerprint";

    private static final String NAME_SUFFIX = "name";

    private static final String PETNAME_SUFFIX = "petname";

    private static final String FP_SUFFIX = "fingerprint";

    private static final String FP_VERIFIED_SUFFIX = "verified";

    private static final String UID_PREFIX = "uid";

    private static final Logger logger = Logger.getLogger(OtrConfigurator.class);

    /**
     * Gets an XML tag friendly {@link String} from a {@link String}.
     *
     * @param s a {@link String}
     * @return an XML friendly {@link String}
     */
    @Deprecated
    private String getXmlFriendlyString(String s)
    {
        if (s == null || s.length() < 1)
            return s;

        // XML Tags are not allowed to start with digits,
        // insert a dummy "p" char.
        if (Character.isDigit(s.charAt(0)))
            s = "p" + s;

        char[] cId = new char[s.length()];

        for (int i = 0; i < cId.length; i++)
        {
            char c = s.charAt(i);

            cId[i] = Character.isLetterOrDigit(c) ? c : '_';
        }

        return new String(cId);
    }

    /**
     * Puts a given property ID under the OTR namespace and makes sure it is XML
     * tag friendly.
     *
     * @param id the property ID.
     * @return the namespaced ID.
     */
    @Deprecated
    private String getID(String id)
    {
        return
            "net.java.sip.communicator.plugin.otr." + getXmlFriendlyString(id);
    }

    /**
     * Returns the value of the property with the specified name or null if no
     * such property exists ({@link ConfigurationService#getProperty(String)}
     * proxy).
     *
     * @param id of the property that is being queried.
     * @return the <tt>byte[]</tt> value of the property with the specified
     *         name.
     */
    @Deprecated
    public byte[] getPropertyBytes(String id)
    {
        String value = OtrActivator.configService.getString(getID(id));

        return (value == null) ? null : Base64.decode(value.getBytes());
    }

    /**
     * Gets the value of a specific property as a boolean (
     * {@link ConfigurationService#getBoolean(String, boolean)} proxy).
     *
     * @param id of the property that is being queried.
     * @param defaultValue the value to be returned if the specified property
     *            name is not associated with a value.
     * @return the <tt>Boolean</tt> value of the property with the specified
     *         name.
     */
    @Deprecated
    public boolean getPropertyBoolean(String id, boolean defaultValue)
    {
        OtrActivator.configService.getPropertyNamesByPrefix("oop", false);
        return
            OtrActivator.configService.getBoolean(getID(id), defaultValue);
    }

    /**
     * Sets the property with the specified name to the specified value (
     * {@link ConfigurationService#setProperty(String, Object)} proxy). The
     * value is Base64 encoded.
     *
     * @param id the name of the property to change.
     * @param value the new value of the specified property.
     */
    @Deprecated
    public void setProperty(String id, byte[] value)
    {
        String valueToStore = new String(Base64.encode(value));

        OtrActivator.configService.setProperty(getID(id), valueToStore);
    }

    /**
     * Sets the property with the specified name to the specified value (
     * {@link ConfigurationService#setProperty(String, Object)} proxy).
     *
     * @param id the name of the property to change.
     * @param value the new value of the specified property.
     */
    @Deprecated
    public void setProperty(String id, Object value)
    {
        OtrActivator.configService.setProperty(getID(id), value);
    }

    /**
     * Removes the property with the specified name (
     * {@link ConfigurationService#removeProperty(String)} proxy).
     *
     * @param id the name of the property to change.
     */
    @Deprecated
    public void removeProperty(String id)
    {
        OtrActivator.configService.removeProperty(getID(id));
    }

    /**
     * Gets the value of a specific property as a signed decimal integer.
     *
     * @param id the name of the property to change.
     * @param defaultValue the value to be returned if the specified property
     *            name is not associated with a value.
     * @return the <tt>int</tt> value of the property
     */
    @Deprecated
    public int getPropertyInt(String id, int defaultValue)
    {
        return OtrActivator.configService.getInt(getID(id), defaultValue);
    }

    /**
     * Appends <tt>value</tt> to the old value of the property with the
     * specified name. The two values will be comma separated.
     *
     * @param id the name of the property to append to
     * @param value the value to append
     */
    @Deprecated
    public void appendProperty(String id, Object value)
    {
        Object oldValue = OtrActivator.configService.getProperty(getID(id));

        String newValue =
            oldValue == null ? value.toString() : oldValue + "," + value;

        setProperty(id, newValue);
    }

    @Deprecated
    public List<String> getAppendedProperties(String id)
    {
        String listProperties =
           (String) OtrActivator.configService.getProperty(getID(id));

        if (listProperties == null) return new ArrayList<String>();

        return Arrays.asList(listProperties.split(","));
    }

    public void setVerified(String petname, String fingerprint, boolean verified)
    {

        String fpUID = getFingerprintUID(fingerprint);;

        if(fpUID == null)
        {
            addFingerprint(petname, fingerprint, verified);
        }
        else
        {
            if(petname != null && !petname.isEmpty())
            {
                String uidKey = String.format("%s.%s", FINGERPRINT_PREFIX, fpUID);
                String petnameKey = String.format("%s.%s", uidKey, PETNAME_SUFFIX);
                OtrActivator.configService.setProperty(petnameKey, petname);
            }
            setFingerprintVerified(fpUID, verified);
        }
    }

    public boolean isVerified(String petname, String fingerprint)
    {
        String fpUID = getFingerprintUID(fingerprint);
        if(fpUID == null)
        {
            return false;
        }

        String petnameKey = String.format("%s.%s.%s", FINGERPRINT_PREFIX, fpUID, PETNAME_SUFFIX);
        String verifiedKey = String.format("%s.%s.%s", FINGERPRINT_PREFIX, fpUID, FP_VERIFIED_SUFFIX);

        String petnameValue = OtrActivator.configService.getString(petnameKey);
        if(petname == null
                || (petnameValue != null && petnameValue.equals(petname)))
        {
            return OtrActivator.configService.getBoolean(verifiedKey, false);
        }
        else
        {
            return false;
        }
    }

    public String getPetname(String fingerprint)
    {

        String fpUID = getFingerprintUID(fingerprint);
        if(fpUID == null)
        {
            return null;
        }

        String petnameKey = String.format("%s.%s.%s", FINGERPRINT_PREFIX, fpUID, PETNAME_SUFFIX);
        return OtrActivator.configService.getString(petnameKey);
    }

    private void setFingerprintVerified(String fpUID, boolean verified)
    {
        String key = String.format("%s.%s.%s",
                FINGERPRINT_PREFIX, fpUID, FP_VERIFIED_SUFFIX);

        OtrActivator.configService.setProperty(key, verified);
    }

    private String addFingerprint(String petname, String fingerprint,
                                  boolean verified)
    {
        synchronized (FINGERPRINT_PREFIX)
        {
            int nextUID = OtrActivator.configService.getPropertyNamesByPrefix(
                    FINGERPRINT_PREFIX, true).size();

            String uid = String.format("%s%d", UID_PREFIX, nextUID);

            String uidKey = String.format("%s.%s", FINGERPRINT_PREFIX, uid);

            String fpKey = String.format("%s.%s", uidKey, FP_SUFFIX);

            String petnameKey = String.format("%s.%s", uidKey, PETNAME_SUFFIX);

            String verifiedKey = String.format("%s.%s", uidKey, FP_VERIFIED_SUFFIX);

            OtrActivator.configService.setProperty(uidKey, uid);
            OtrActivator.configService.setProperty(fpKey, fingerprint);
            OtrActivator.configService.setProperty(petnameKey, petname);
            OtrActivator.configService.setProperty(verifiedKey, verified);

            return uid;
        }
    }

    private String getFingerprintUID(String fingerprint)
    {
        List<String> fpUIDs = OtrActivator.configService
                .getPropertyNamesByPrefix(FINGERPRINT_PREFIX, true);


        for(String fpUID: fpUIDs)
        {
            String key = String.format("%s.%s", fpUID, FP_SUFFIX);

            String value = OtrActivator.configService.getString(key);

            if(value != null && value.equals(fingerprint))
            {
                return OtrActivator.configService.getString(fpUID);
            }
        }
        return null;
    }

    public KeyPair getOtrKeyPair(AccountID accountID)
    {
        String uid = getAccountUID(accountID);
        if(uid == null)
        {
            return null;
        }

        String privateKeyKey = String.format("%s.%s.%s.%s", ACCOUNT_PREFIX, uid, OTR_INFIX,
                PRIV_KEY_SUFFIX);
        String publicKeyKey = String.format("%s.%s.%s.%s", ACCOUNT_PREFIX, uid, OTR_INFIX,
                PUB_KEY_SUFFIX);

        String publicKeyEncoded = OtrActivator.configService.getString(publicKeyKey);
        String privateKeyEncoded = OtrActivator.configService.getString(privateKeyKey);
        if(publicKeyEncoded == null || privateKeyEncoded == null)
        {
            return null;
        }

        X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(Base64.decode(publicKeyEncoded));
        PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(Base64.decode(privateKeyEncoded));

        try
        {
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
            PrivateKey privateKey = keyFactory.generatePrivate(privKeySpec);
            PublicKey publicKey = keyFactory.generatePublic(pubKeySpec);
            return new KeyPair(publicKey, privateKey);
        } catch (NoSuchAlgorithmException e)
        {
            logger.error("KEY_ALGORITHM not found.", e);
            return null;
        } catch (InvalidKeySpecException e)
        {
            logger.error("Stored keys were invalid.", e);
            return null;
        }

    }

    public void setOtrKeyPair(AccountID accountID, KeyPair keyPair)
    {
        String uid = getAccountUID(accountID);
        if(uid == null)
        {
            uid = addAccount(accountID);
        }

        String privateKeyKey = String.format("%s.%s.%s.%s", ACCOUNT_PREFIX, uid, OTR_INFIX,
                PRIV_KEY_SUFFIX);
        String publicKeyKey = String.format("%s.%s.%s.%s", ACCOUNT_PREFIX, uid, OTR_INFIX,
                PUB_KEY_SUFFIX);

        byte[] publicKeyEncoded = Base64.encode(keyPair.getPublic().getEncoded());

        byte[] privateKeyEncoded = Base64.encode(keyPair.getPrivate().getEncoded());

        OtrActivator.configService.setProperty(privateKeyKey, new String(privateKeyEncoded));
        OtrActivator.configService.setProperty(publicKeyKey, new String(publicKeyEncoded));
    }

    private String addAccount(AccountID accountID)
    {
        synchronized (ACCOUNT_PREFIX)
        {
            int nextUID = OtrActivator.configService.getPropertyNamesByPrefix(
                    ACCOUNT_PREFIX, true).size();

            String uid = String.format("%s%d", UID_PREFIX, nextUID);

            String accountKey = String.format("%s.%s", ACCOUNT_PREFIX, uid);
            String nameKey = String.format("%s.%s", accountKey, NAME_SUFFIX);

            OtrActivator.configService.setProperty(accountKey, uid);
            OtrActivator.configService.setProperty(nameKey, accountID.getAccountUniqueID());

            return uid;
        }
    }

    private String getAccountUID(AccountID accountID)
    {
        List<String> accountUIDs = OtrActivator.configService.getPropertyNamesByPrefix(ACCOUNT_PREFIX, true);

        for(String accountUID: accountUIDs)
        {
            String nameKey = String.format("%s.%s", accountUID, NAME_SUFFIX);

            String name = OtrActivator.configService.getString(nameKey);
            if(name != null && name.equals(accountID.getAccountUniqueID()))
            {
                return OtrActivator.configService.getString(accountUID);
            }
        }
        return null;
    }

    public List<String> getAllRemoteFingerprints() {

        List<String> fpUIDs = OtrActivator.configService
                .getPropertyNamesByPrefix(FINGERPRINT_PREFIX, true);

        List<String> results = new ArrayList<>(fpUIDs.size());


        for(String fpUID: fpUIDs)
        {
            String key = String.format("%s.%s", fpUID, FP_SUFFIX);

            String value = OtrActivator.configService.getString(key);
            if(value != null){
                results.add(value);
            }
        }
        return results;
    }

    public void setGotrKeyPair(AccountID account, KeyPair keyPair) {
        String uid = getAccountUID(account);
        if(uid == null)
        {
            uid = addAccount(account);
        }

        String privateKeyKey = String.format("%s.%s.%s.%s", ACCOUNT_PREFIX, uid, GOTR_INFIX,
                PRIV_KEY_SUFFIX);
        String publicKeyKey = String.format("%s.%s.%s.%s", ACCOUNT_PREFIX, uid, GOTR_INFIX,
                PUB_KEY_SUFFIX);

        byte[] publicKeyEncoded = Base64.encode(keyPair.getPublic().getEncoded());

        byte[] privateKeyEncoded = Base64.encode(keyPair.getPrivate().getEncoded());

        OtrActivator.configService.setProperty(privateKeyKey, new String(privateKeyEncoded));
        OtrActivator.configService.setProperty(publicKeyKey, new String(publicKeyEncoded));
    }

    public KeyPair getGotrKeyPair(AccountID accountID) {
        String uid = getAccountUID(accountID);
        if(uid == null)
        {
            return null;
        }

        String privateKeyKey = String.format("%s.%s.%s.%s", ACCOUNT_PREFIX, uid, GOTR_INFIX,
                PRIV_KEY_SUFFIX);
        String publicKeyKey = String.format("%s.%s.%s.%s", ACCOUNT_PREFIX, uid, GOTR_INFIX,
                PUB_KEY_SUFFIX);

        String publicKeyEncoded = OtrActivator.configService.getString(publicKeyKey);
        String privateKeyEncoded = OtrActivator.configService.getString(privateKeyKey);
        if(publicKeyEncoded == null || privateKeyEncoded == null)
        {
            return null;
        }

        X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(Base64.decode(publicKeyEncoded));
        PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(Base64.decode(privateKeyEncoded));

        try
        {
            KeyFactory keyFactory = KeyFactory.getInstance(GotrCrypto.KEY_PAIR_ALGORITHM);
            PrivateKey privateKey = keyFactory.generatePrivate(privKeySpec);
            PublicKey publicKey = keyFactory.generatePublic(pubKeySpec);
            return new KeyPair(publicKey, privateKey);
        } catch (NoSuchAlgorithmException e)
        {
            logger.error("KEY_ALGORITHM not found.", e);
            return null;
        } catch (InvalidKeySpecException e)
        {
            logger.error("Stored keys were invalid.", e);
            return null;
        }
    }
}

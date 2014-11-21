/* 
 * Kosmo - Sistema Abierto de Información Geográfica
 * Kosmo - Open Geographical Information System
 *
 * http://www.saig.es
 * (C) 2006, SAIG S.L.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation;
 * version 2.1 of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 * For more information, contact:
 * 
 * Sistemas Abiertos de Información Geográfica, S.L.
 * Avnda. República Argentina, 28
 * Edificio Domocenter Planta 2ª Oficina 7
 * C.P.: 41930 - Bormujos (Sevilla)
 * España / Spain
 *
 * Teléfono / Phone Number
 * +34 954 788876
 * 
 * Correo electrónico / Email
 * info@saig.es
 *
 */
package org.saig.core.crypt;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import net.iharder.Base64;

import org.apache.log4j.Logger;

/**
 * Crypt manager that applies the PBE (Password-Based Encryption) algorithm as described in PKCS #5
 * <p>
 * This encryption algorithm needs a seed (<I>salt</I>) and an iteration counter (<I>iteration
 * count</I>) that allow to encrypt/decrypt the given object
 * </p>
 * 
 * @author Sergio Ba&ntilde;os Calvo - sbc@saig.es
 * @since 1.0
 */
public class PBEManager extends CryptManager {

    /** Log */
    public final static Logger LOGGER = Logger.getLogger(PBEManager.class);

    /** Seed */
    private final static byte[] SALT = {(byte) 0xc7, (byte) 0x73, (byte) 0x21, (byte) 0x8c,
        (byte) 0x7e, (byte) 0xc8, (byte) 0xee, (byte) 0x99};

    /** Iteration counter */
    private final static int COUNT = 20;

    /** Password */
    private final static char[] PASSWD_KEY = new char[]{'k', 'o', 's', 'm', 'o'};

    private PBEKeySpec pbeKeySpec;
    private PBEParameterSpec pbeParamSpec;
    private SecretKeyFactory keyFac;
    private SecretKey pbeKey;
    private Cipher pbeCipher;

    /**
     * Initializes the manager
     */
    public void initialize() {

        // Create PBE parameter set
        pbeParamSpec = new PBEParameterSpec(SALT, COUNT);

        pbeKeySpec = new PBEKeySpec(PASSWD_KEY);
        try {
            keyFac = SecretKeyFactory.getInstance("PBEWithMD5AndDES"); //$NON-NLS-1$
            pbeKey = keyFac.generateSecret(pbeKeySpec);

            // Create PBE Cipher
            pbeCipher = Cipher.getInstance("PBEWithMD5AndDES"); //$NON-NLS-1$

        } catch (Exception e) {
            LOGGER.error("Error inicializando PBEManager: " + e.getMessage()); //$NON-NLS-1$
        }

    }

    /**
     * Encrypt the given string
     * 
     * @param str String to encrypt
     * @return String
     * @throws Exception
     */
    public String encrypt( String str ) throws Exception {

        // Initialize PBE Cipher with key and parameters
        pbeCipher.init(Cipher.ENCRYPT_MODE, pbeKey, pbeParamSpec);

        byte raw[] = pbeCipher.doFinal(str.getBytes());
        String hash = Base64.encodeBytes(raw);

        return hash;
    }

    /**
     * Decrypt the given string
     * 
     * @param str String to decrypt
     * @return byte[]
     * @throws Exception
     */
    public String decrypt( String str ) throws Exception {

        // Initialize PBE Cipher with key and parameters
        pbeCipher.init(Cipher.DECRYPT_MODE, pbeKey, pbeParamSpec);

        byte decodedStr[] = Base64.decode(str);
        byte raw[] = pbeCipher.doFinal(decodedStr);
        String decrypted = new String(raw, "UTF-8"); //$NON-NLS-1$

        return decrypted;
    }
}
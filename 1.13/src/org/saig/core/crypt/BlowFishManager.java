/* 
 * Kosmo - Sistema Abierto de Información Geográfica
 * Kosmo - Open Geographical Information System
 *
 * http://www.saig.es
 * (C) 2007, SAIG S.L.
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
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.vividsolutions.jump.workbench.Logger;



/**
 * BlowFish algorithm crypt manager
 * <p>
 * </p>
 * 
 * @author Sergio Ba&ntilde;os Calvo - sbc@saig.es
 * @since 1.1.1
 */
public class BlowFishManager extends CryptManager {

    /** Log */


    private SecretKeySpec skeySpec;
    private SecretKey blowFishKey;
    private Cipher blowFishCipher;

    @Override
    public void initialize() {

        KeyGenerator kgen;
        try {
            kgen = KeyGenerator.getInstance("Blowfish"); //$NON-NLS-1$

            blowFishKey = kgen.generateKey();
            byte[] raw = blowFishKey.getEncoded();
            skeySpec = new SecretKeySpec(raw, "Blowfish"); //$NON-NLS-1$

            blowFishCipher = Cipher.getInstance("Blowfish"); //$NON-NLS-1$
            
        } catch (Exception e) {
            Logger.error(e);
        }

    }

    @Override
    public String encrypt( String str ) throws Exception {
        
        blowFishCipher.init(Cipher.ENCRYPT_MODE, skeySpec);        
        byte[] encrypted = blowFishCipher.doFinal(str.getBytes("UTF-8")); //$NON-NLS-1$
        
        return new String(encrypted);  
    }
    
    @Override
    public String decrypt( String str ) throws Exception {
        
        blowFishCipher.init(Cipher.DECRYPT_MODE, skeySpec);        
        byte[] decrypted = blowFishCipher.doFinal(str.getBytes("UTF-8")); //$NON-NLS-1$
        
        return new String(decrypted);        
    }
}
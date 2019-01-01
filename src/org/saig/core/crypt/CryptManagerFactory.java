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

import org.saig.jump.lang.I18N;

/**
 * Crypt manager factory
 * <p>
 * </p>
 * 
 * @author Sergio Ba&ntilde;os Calvo - sbc@saig.es
 * @since 1.0
 */
public class CryptManagerFactory {

    /** PBE encryption - Password based */
    public final static String PASSWORD_BASED_ENCRYPTION = "Password based encryption"; //$NON-NLS-1$

    /** BlowFish encryption */
    public final static String BLOWFISH_ENCRYPTION = "BlowFish"; //$NON-NLS-1$

    /**
     * Build the encryption manager for the given algorithm
     * 
     * @param algorithm Encryption algorithm to apply
     * @return CryptManager
     * @throws CryptManagerException
     */
    public static CryptManager getManager( String algorithm ) throws CryptManagerException {
        CryptManager manager = null;

        if (algorithm.equalsIgnoreCase(PASSWORD_BASED_ENCRYPTION)) {
            manager = new PBEManager();
        } else if (algorithm.equalsIgnoreCase(BLOWFISH_ENCRYPTION)) {
            manager = new BlowFishManager();
        } else {
            throw new CryptManagerException(I18N.getMessage("org.saig.core.crypt.CryptManagerFactory.a-{0}-algorithm-manager-can-not-be-found", //$NON-NLS-1$
                new Object[]{algorithm}));
        }

        manager.initialize();

        return manager;
    }
}
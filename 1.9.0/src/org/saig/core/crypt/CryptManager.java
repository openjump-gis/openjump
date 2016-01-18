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

/**
 * Crypt manager abstract class
 * <p>
 * </p>
 * 
 * @author Sergio Ba&ntilde;os Calvo - sbc@saig.es
 * @since 1.0
 */
public abstract class CryptManager {

    /**
     * Inicializa el controlador correpondiente
     */
    public abstract void initialize();

    /**
     * Permite encriptar la cadena aplicando el algoritmo pertinente
     * 
     * @param str Cadena que se quiere encriptar
     * @throws Exception
     */
    public abstract String encrypt( String str ) throws Exception;

    /**
     * Permite desencriptar la cadena indicada aplicando el algoritmo pertinente
     * 
     * @param str Cadena que se quiere desencriptar
     * @return String - Cadena desencriptada
     */
    public abstract String decrypt( String str ) throws Exception;

    /**
     * Permite probar el encriptador
     * 
     * @param args
     */
    public static void main( String[] args ) {

        try {

            CryptManager manager = CryptManagerFactory
                    .getManager(CryptManagerFactory.PASSWORD_BASED_ENCRYPTION);

            String toEncrypt = args[0];//$NON-NLS-1$
            String toEncrypt2 = "";//$NON-NLS-1$

            String encrypted = manager.encrypt(toEncrypt);
            String encrypted2 = manager.encrypt(toEncrypt2);

            System.out.println("Se ha encriptado la cadena " + toEncrypt + //$NON-NLS-1$
                    " y se recuperado la cadena <" + encrypted + ">"); //$NON-NLS-1$ //$NON-NLS-2$
            System.out.println("Se ha encriptado la cadena " + toEncrypt2 + //$NON-NLS-1$
                    " y se recuperado la cadena <" + encrypted2 + ">"); //$NON-NLS-1$ //$NON-NLS-2$

            String decrypted2 = manager.decrypt(encrypted2);
            String decrypted3 = manager.decrypt(encrypted);

            System.out.println("Se ha recuperado la cadena <" + decrypted2 + ">");//$NON-NLS-1$ //$NON-NLS-2$
            System.out.println("Se ha desencriptado la cadena " + encrypted + //$NON-NLS-1$
                    " de <" + decrypted3 + ">");//$NON-NLS-1$ //$NON-NLS-2$

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

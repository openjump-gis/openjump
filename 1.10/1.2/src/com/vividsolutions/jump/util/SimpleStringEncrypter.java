package com.vividsolutions.jump.util;

import java.util.*;

/**
 * Provides a simple encyrption/decryption mechanism for ASCII string values.
 * The algorithm does not provide strong encryption, but serves
 * as a way of obfuscating the value of short strings (such as passwords).
 * The code symbol set is drawn from the set of printable ASCII symbols.
 * The encrypted strings are longer than the clear text (roughly double in length).
 * A random element is used, so that different encryptions of the same clear
 * text will result in different encodings.
 */
public class SimpleStringEncrypter
{
  private static final String INVALID_CODE_STRING_MSG = "Invalid code string";

  private static String codeSymbols =
      "ABCDEFGHIJKLMNOP"
  +   "abcdefghijklmnop"
  +   "QRSTUVWXYZ012345"
  +   "qrstuvwxyz6789@$";

  /**
   * Creates a new encrypter
   */
  public SimpleStringEncrypter() {
  }

  /**
   * Encrypts a string.
   *
   * @param clearText the string to encrypt
   * @return the encryted code
   */
  public String encrypt(String clearText)
  {
    char[] code = new char[clearText.length() * 2];
    for (int i = 0; i < clearText.length(); i++) {
      char c = clearText.charAt(i);
      setEncryptedSymbol(c, code, 2 * i);
    }
    return new String(code);
  }

  public void setEncryptedSymbol(char c, char[] code, int i)
  {
    int charVal = c;
    int nibble0 = charVal & 0xf;
    int nibble1 = charVal >> 4;

    code[i] = encodeNibble(nibble0);
    code[i + 1] = encodeNibble(nibble1);
  }

  private char encodeNibble(int val)
  {
    int random4 = (int) (4 * Math.random());
    int randomOffset = 16 * random4;
    return codeSymbols.charAt(val + randomOffset);
  }

  /**
   * Decrypts a code string.
   *
   * @param codeText the code to decrypt
   * @return the clear text for the code
   *
   * @throws IllegalArgumentException if the code string is invalid
   */
  public String decrypt(String codeText)
  {
    if (codeText.length() % 2 != 0)
      throw new IllegalArgumentException(INVALID_CODE_STRING_MSG);
    char[] clear = new char[codeText.length() / 2];
    for (int i = 0; i < codeText.length() / 2; i++) {
      char symbol0 = codeText.charAt(2 * i);
      char symbol1 = codeText.charAt(2 * i + 1);
      clear[i] = decryptedChar(symbol0, symbol1);
    }
    return new String(clear);
  }

  private char decryptedChar(char symbol0, char symbol1)
  {
    int nibble0 = decodeNibble(symbol0);
    int nibble1 = decodeNibble(symbol1);
    int charVal = nibble1 << 4 | nibble0;
    return (char) charVal;
  }

  private int decodeNibble(int symbolValue)
  {
    int nibbleValue = codeSymbols.indexOf(symbolValue);
    if (nibbleValue < 0)
      throw new IllegalArgumentException(INVALID_CODE_STRING_MSG);
    return nibbleValue % 16;
  }
  
}
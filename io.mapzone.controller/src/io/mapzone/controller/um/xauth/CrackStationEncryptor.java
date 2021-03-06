/* 
 * mapzone.io
 * Copyright (C) 2013, Falko Bräutigam. All rights reserved.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3.0 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */
package io.mapzone.controller.um.xauth;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.polymap.core.runtime.Timer;

/**
 * Basic password encryptor based on PBKDF2 salted password hashing.
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
class CrackStationEncryptor
        extends PasswordEncryptor {

    private static Log log = LogFactory.getLog( CrackStationEncryptor.class );

    @Override
    public String encryptPassword( String password ) {
        return PasswordHash.createHash( password );
    }

    @Override
    public boolean checkPassword( String inputPassword, String encryptedPassword ) {
        return PasswordHash.validatePassword( inputPassword, encryptedPassword );
    }
    
    @Override
    public String createPassword( int length ) {
        return RandomStringUtils.random( length, true, true );
    }


    /**
     * PBKDF2 salted password hashing.
     * @author havoc AT defuse.ca (http://crackstation.net/hashing-security.htm)
     */
    static class PasswordHash {
        
        public static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA1";

        // The following constants may be changed without breaking existing hashes.
        public static final int SALT_BYTE_SIZE = 24;
        public static final int HASH_BYTE_SIZE = 24;
        public static final int PBKDF2_ITERATIONS = 500000;

        public static final int ITERATION_INDEX = 0;
        public static final int SALT_INDEX = 1;
        public static final int PBKDF2_INDEX = 2;


        /**
         * Returns a salted PBKDF2 hash of the password.
         * 
         * @param password the password to hash
         * @return a salted PBKDF2 hash of the password
         */
        public static String createHash( String password ) {
            return createHash( password.toCharArray() );
        }


        /**
         * Returns a salted PBKDF2 hash of the password.
         * 
         * @param password the password to hash
         * @return a salted PBKDF2 hash of the password
         */
        public static String createHash( char[] password ) {
            try {
                // Generate a random salt
                SecureRandom random = new SecureRandom();
                byte[] salt = new byte[SALT_BYTE_SIZE];
                random.nextBytes( salt );

                // Hash the password
                byte[] hash = pbkdf2( password, salt, PBKDF2_ITERATIONS, HASH_BYTE_SIZE );
                // format iterations:salt:hash
                return PBKDF2_ITERATIONS + ":" + toHex( salt ) + ":" + toHex( hash );
            }
            catch (Exception e) {
                throw new RuntimeException( e );
            }
        }


        /**
         * Validates a password using a hash.
         * 
         * @param password the password to check
         * @param correctHash the hash of the valid password
         * @return true if the password is correct, false if not
         */
        public static boolean validatePassword( String password, String correctHash ) {
            return validatePassword( password.toCharArray(), correctHash );
        }


        /**
         * Validates a password using a hash.
         * 
         * @param password the password to check
         * @param correctHash the hash of the valid password
         * @return true if the password is correct, false if not
         */
        public static boolean validatePassword( char[] password, String correctHash ) {
            Timer timer = new Timer();
            try {
                // Decode the hash into its parameters
                String[] params = correctHash.split( ":" );
                int iterations = Integer.parseInt( params[ITERATION_INDEX] );
                byte[] salt = fromHex( params[SALT_INDEX] );
                byte[] hash = fromHex( params[PBKDF2_INDEX] );
                // Compute the hash of the provided password, using the same salt,
                // iteration count, and hash length
                byte[] testHash = pbkdf2( password, salt, iterations, hash.length );
                // Compare the hashes in constant time. The password is correct if
                // both hashes match.
                return slowEquals( hash, testHash );
            }
            catch (Exception e) {
                throw new RuntimeException( e );
            }
            finally {
                log.info( "Password validated in " + timer.elapsedTime() + "ms" );
            }
        }


        /**
         * Compares two byte arrays in length-constant time. This comparison method
         * is used so that password hashes cannot be extracted from an on-line system
         * using a timing attack and then attacked off-line.
         * 
         * @param a the first byte array
         * @param b the second byte array
         * @return true if both byte arrays are the same, false if not
         */
        private static boolean slowEquals( byte[] a, byte[] b ) {
            int diff = a.length ^ b.length;
            for (int i = 0; i < a.length && i < b.length; i++)
                diff |= a[i] ^ b[i];
            return diff == 0;
        }


        /**
         * Computes the PBKDF2 hash of a password.
         * 
         * @param password the password to hash.
         * @param salt the salt
         * @param iterations the iteration count (slowness factor)
         * @param bytes the length of the hash to compute in bytes
         * @return the PBDKF2 hash of the password
         */
        private static byte[] pbkdf2( char[] password, byte[] salt, int iterations, int bytes )
                throws NoSuchAlgorithmException, InvalidKeySpecException {
            PBEKeySpec spec = new PBEKeySpec( password, salt, iterations, bytes * 8 );
            SecretKeyFactory skf = SecretKeyFactory.getInstance( PBKDF2_ALGORITHM );
            return skf.generateSecret( spec ).getEncoded();
        }


        /**
         * Converts a string of hexadecimal characters into a byte array.
         * 
         * @param hex the hex string
         * @return the hex string decoded into a byte array
         */
        private static byte[] fromHex( String hex ) {
            byte[] binary = new byte[hex.length() / 2];
            for (int i = 0; i < binary.length; i++) {
                binary[i] = (byte)Integer.parseInt( hex.substring( 2 * i, 2 * i + 2 ), 16 );
            }
            return binary;
        }


        /**
         * Converts a byte array into a hexadecimal string.
         * 
         * @param array the byte array to convert
         * @return a length*2 character string encoding the byte array
         */
        private static String toHex( byte[] array ) {
            BigInteger bi = new BigInteger( 1, array );
            String hex = bi.toString( 16 );
            int paddingLength = (array.length * 2) - hex.length();
            if (paddingLength > 0)
                return String.format( "%0" + paddingLength + "d", 0 ) + hex;
            else
                return hex;
        }
    }

    /**
     * Tests the basic functionality of the PasswordHash class
     */
    public static void main( String[] args ) throws Exception {
        // Print out 10 hashes
        Timer timer = new Timer();
        for (int i = 0; i < 10; i++) {
            System.out.println( "Hash: " + PasswordHash.createHash( "p\r\nassw0Rd!" ) );
        }
        System.out.println( "10 hashes created. (" + timer.elapsedTime() + "ms)" );

        // Test password validation
        timer.start();
        boolean failure = false;
        System.out.println( "Running tests..." );
        for (int i = 0; i < 10; i++) {
            String password = "" + i;
            String hash = PasswordHash.createHash( password );
            String secondHash = PasswordHash.createHash( password );
            if (hash.equals( secondHash )) {
                System.out.println( "FAILURE: TWO HASHES ARE EQUAL!" );
                failure = true;
            }
            String wrongPassword = "" + (i + 1);
            if (PasswordHash.validatePassword( wrongPassword, hash )) {
                System.out.println( "FAILURE: WRONG PASSWORD ACCEPTED!" );
                failure = true;
            }
            if (!PasswordHash.validatePassword( password, hash )) {
                System.out.println( "FAILURE: GOOD PASSWORD NOT ACCEPTED!" );
                failure = true;
            }
        }
        if (failure) {
            System.out.println( "TESTS FAILED!" );
        } else {
            System.out.println( "TESTS PASSED!" );
        }
        System.out.println( "10(x2) hashes checked. (" + timer.elapsedTime() + "ms)" );
    }

}

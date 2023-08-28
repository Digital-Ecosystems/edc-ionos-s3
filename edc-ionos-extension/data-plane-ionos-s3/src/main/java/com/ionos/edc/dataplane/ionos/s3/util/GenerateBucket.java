package com.ionos.edc.dataplane.ionos.s3.util;

import java.nio.charset.Charset;
import java.util.*;
public class GenerateBucket {

    public String genBucket() {
        String CHARS = "abcdefghijklmnopqrstuvxyz1234567890";
        StringBuilder charGen = new StringBuilder();
        Random rnd = new Random();
        while (charGen.length() < 10) { // length of the random string.
            int index = (int) (rnd.nextFloat() * CHARS.length());
            charGen.append(CHARS.charAt(index));
        }
        String charGenString = charGen.toString();
        return charGenString;

    }

}

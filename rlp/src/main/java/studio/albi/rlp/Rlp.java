package studio.albi.rlp;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Recursive Length Prefix encodes arbitrarily nested arrays of binary data. Used to serialize objects in Ethereum.
 */
public final class Rlp {
    public static String OFFSET_SHORT_STRING = "80";
    public static String OFFSET_STRING = "b7";
    public static String OFFSET_SHORT_LIST = "c0";
    public static String OFFSET_LIST = "f7";


    public static List<String> encode(Object item) {
        if (item == null || item.toString().isEmpty()) return List.of(OFFSET_SHORT_STRING);

        if (item instanceof String str) {
            return encodeTypeString(str);
        } else if (item instanceof Boolean b) {
            return encodeTypeBoolean(b);
        } else if (item instanceof List list) {
            return encodeTypeList(list);
        } else return null;
    }

    private static List<String> encodeTypeList(List<?> list) {
        var encodedInput = new ArrayList<String>();
        list.forEach(entry -> encodedInput.addAll(Rlp.encode(entry)));
        var lengthPrefix = encode_length(encodedInput.size(), OFFSET_SHORT_LIST);

        var prebuilt = new ArrayList<String>();
        prebuilt.addAll(lengthPrefix);
        prebuilt.addAll(encodedInput);
        return prebuilt;
    }

    private static List<String> encodeTypeBoolean(boolean b) {
        if (b) {
            return List.of("01");
        } else return List.of(OFFSET_SHORT_STRING);
    }

    private static List<String> encodeTypeString(String str) {
        var strLength = str.length();
        var bytes = str.getBytes(StandardCharsets.UTF_8);

        if (strLength == 1) {
            return List.of(Integer.toHexString(bytes[0]));
        } else if (strLength < 3 && Integer.parseInt(str, 16) < 128) {
            return List.of(str);
        } else if (strLength < 3 && Integer.parseInt(str, 16) > 127) {
            return List.of("81", str);
        } else {
            var lengthPrefixList = encode_length(strLength, OFFSET_SHORT_STRING);
            var convertedInput = new ArrayList<>(lengthPrefixList);
            for (byte b : bytes) {
                convertedInput.add(Integer.toHexString(b));
            }
            return convertedInput;
        }
    }

    /**
     * returns type that corresponds to type String/Array of strings/List of  strings
     */
    public static void decode() {

        /*
        *     1) According to the first byte of input, RLP decoding analyses data the type, the length of the actual data and offset.
              2) According to the type and the offset of data, decode data correspondingly.
              3) Continue to decode the rest of the input if still possible.
        * */
    }

    private static List<String> encode_length(int length, String offset) {
        if (length < 56) {
            return List.of(helperAdditionIntHex(length, offset));
        } else {
            var hexLength = toBinary(length);
            var lLength = hexLength.length() / 2;
            var firstByte = toBinary(Integer.parseInt(offset, 16) + 55 + lLength);
            return List.of(firstByte, hexLength);
        }
    }

    private static String helperAdditionIntHex(int number, String hex) {
        var intAdd = number + Integer.parseInt(hex, 16);
        return cutLeadingZeroes(HexFormat.of().toHexDigits(intAdd));
    }

    private static String cutLeadingZeroes(String s) {
        return s.substring(6, 8);
    }

    private static String toBinary(int num) {
        if (num == 0) {
            return "";
        } else {
            return toBinary(num / 256) + getChar(num % 256);
        }
    }

    private static String getChar(int i) {
        var temp = String.format("%x", i);
        if (temp.length() % 2 != 0) {
            return "0" + temp;
        }
        return temp;
    }
}

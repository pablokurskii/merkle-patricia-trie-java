package studio.albi.rlp;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

//https://www.utf8-chartable.de/
//https://codechain-io.github.io/rlp-debugger/
//https://medium.com/coinmonks/data-structure-in-ethereum-episode-1-recursive-length-prefix-rlp-encoding-decoding-d1016832f919
//https://eth.wiki/fundamentals/rlp
public class RlpTest {

    /**
     * 1. If input is a single byte in the [0x00, 0x7f] (0, 127) range, so itself is RLP encoding.
     */
    @ParameterizedTest
    @ValueSource(strings = {"00", "0f", "7f"})
    public void testSingleByte(String input) {
        var expected = List.of(input);
        var rlpEncoded = Rlp.encode(input);
        assertThat(rlpEncoded).hasSameElementsAs(expected);
    }

    /**
     * 2. If input is non-value (uint(0), string(“”), empty pointer …), RLP encoding is 0x80 (128).
     */
    @ParameterizedTest
    @NullSource
    public void testNullInt(Integer input) {

        var expected = List.of("80");
        var rlpEncoded = Rlp.encode(input);
        assertThat(rlpEncoded).hasSameElementsAs(expected);
    }

    @ParameterizedTest
    @EmptySource
    public void testEmptyString(String input) {
        var expected = List.of("80");
        var rlpEncoded = Rlp.encode(input);
        assertThat(input).isEqualTo("");
        assertThat(rlpEncoded).hasSameElementsAs(expected);
    }


    /**
     * 3. If input is a special byte in [0x80, 0xff] range, RLP encoding will concatenate 0x81 (129) with the byte, [0x81, the_byte].
     */
    @ParameterizedTest
    @ValueSource(strings = {"80", "ff"})
    public void testSpecialByte(String input) {
        var expected = List.of("81", input);
        var rlpEncoded = Rlp.encode(input);
        assertThat(rlpEncoded).hasSameElementsAs(expected);
    }

    /*passed*/

    /**
     * 4. If input is a string with 0–55 bytes long, RLP encoding consists of a single byte with value 0x80 plus the length of the string in bytes and then array of hex value of string.
     */
    @ParameterizedTest
    @ValueSource(strings = {"d"})
    public void testCharacter(String input) {
        var expected = List.of("64");
        var rlpEncoded = Rlp.encode(input);
        assertThat(rlpEncoded).hasSameElementsAs(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"hello world"})
    public void testShortString(String input) {
        var expected = List.of("8b", "68", "65", "6c", "6c", "6f", "20", "77", "6f", "72", "6c", "64");
        var rlpEncoded = Rlp.encode(input);
        assertThat(rlpEncoded).hasSameElementsAs(expected);
    }

    /**
     * 5. If input is a string with more than 55 bytes long, RLP encoding consists of 3 parts from the left to the right. The first part is a single byte with value 0xb7 plus the length in bytes of the second part. The second part is hex value of the length of the string. The last one is the string in bytes. The range of the first byte is [0xb8, 0xbf].
     */
    @ParameterizedTest
    @ValueSource(strings = {"Lorem ipsum dolor sit amet, consectetur adipiscing elit,"})
    public void testString(String input) {

        var expected = List.of("b8", "38", "4c", "6f", "72", "65", "6d", "20", "69", "70", "73", "75", "6d", "20", "64", "6f", "6c", "6f", "72", "20", "73", "69", "74", "20", "61", "6d", "65", "74", "2c", "20", "63", "6f", "6e", "73", "65", "63", "74", "65", "74", "75", "72", "20", "61", "64", "69", "70", "69", "73", "69", "63", "69", "6e", "67", "20", "65", "6c", "69", "74");
        var rlpEncoded = Rlp.encode(input);
        assertThat(rlpEncoded).hasSameElementsAs(expected);
    }

    /**
     * 6. If input is an empty array, RLP encoding is a single byte 0xc0.
     */
    @ParameterizedTest
    @MethodSource("emptyStringListProvider")
    public void testEmptyList(List<String> input) {

        var expected = List.of("c0");
        var rlpEncoded = Rlp.encode(input);
        assertThat(rlpEncoded).hasSameElementsAs(expected);
    }

    static Stream<Arguments> emptyStringListProvider() {
        return Stream.of(Arguments.of(List.of()));
    }

    /**
     * 7. If input is a list with total payload in 0–55 bytes long, RLP encoding consists of a single byte with value 0xc0 plus the length of the list and then the concatenation of RLP encodings of the items in list.
     */
    @ParameterizedTest
    @MethodSource("shortStringListProvider")
    public void testShortStringList(List<String> input) {

        var expected = List.of("c8", "83", "63", "61", "74", "83", "64", "6f", "67");
        var rlpEncoded = Rlp.encode(input);
        assertThat(rlpEncoded).hasSameElementsAs(expected);
    }

    static Stream<Arguments> shortStringListProvider() {
        return Stream.of(Arguments.of(List.of("cat", "dog")));
    }

    /**
     * 8. If input is a list with total payload more than 55 bytes long, RLP encoding includes 3 parts. The first one is a single byte with value 0xf7 plus the length in bytes of the second part. The second part is the length of total payload. The last part is the concatenation of RLP encodings of the items in list. The range of the first byte is [0xf8, 0xff].
     */
    @ParameterizedTest
    @MethodSource("stringListProvider")
    public void testStringList(String[] input) {

        var expected = List.of("c8", "83", "63", "61", "74", "83", "64", "6f", "67");
        var rlpEncoded = Rlp.encode(input);
        assertThat(rlpEncoded).hasSameElementsAs(expected);
    }

    static Stream<Arguments> stringListProvider() {
        return Stream.of(Arguments.of(Arrays.asList("Lorem ipsum dolor sit amet, ", "consectetur adipiscing elit,")));
    }


    /**
     * 9. If input is a boolean value of TRUE - RLP encoding 0x01 (1), FALSE - 0x80 (0).
     */
    @ParameterizedTest
    @ValueSource(booleans = {true})
    public void testBoolean(boolean input) {
        var expected = List.of("01");
        var rlpEncoded = Rlp.encode(input);
        assertThat(rlpEncoded).hasSameElementsAs(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Lorem ipsum dolor sit amet, consectetur adipisicing elit"})
    public void metaTest(String input) {
        var rlpEncoded = Rlp.encode(input);
        System.out.println(rlpEncoded);
        /*The integer 0 = [ 0x80 ]*/
        /*The encoded integer 1024 (’\x04\x00’) = [ 0x82, 0x04, 0x00 ]*/
    }

}

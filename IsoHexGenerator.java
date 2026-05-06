import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;
import org.jpos.iso.packager.ISO87APackager;

/**
 * Root-level helper to generate valid ISO HEX with correct field lengths.
 */
public class IsoHexGenerator {

    public static void main(String[] args) throws Exception {
        ISOMsg iso = new ISOMsg();
        iso.setPackager(new ISO87APackager());

        iso.setMTI("0600");

        iso.set(2, "3538210000000026");
        iso.set(3, "910000");
        iso.set(7, "0313112749");
        iso.set(11, "252035");
        iso.set(12, "112749");
        iso.set(13, "0313");
        iso.set(14, "3005");
        iso.set(18, "1111");
        iso.set(22, "000");
        iso.set(23, "001");
        iso.set(25, "16");
        iso.set(41, "01234567");
        iso.set(42, "123456789012345");
        iso.set(43, "abcdefghijklmnopqrstuvwxyz01234567890123");
        iso.set(100, "10011");
        iso.set(123, "153220372737300");

        String xml = "<InquiryOrUpdateData>"
                + "<Card>"
                + "<PAN>3538210000000026</PAN>"
                + "<ExpiryDate>3005</ExpiryDate>"
                + "<SeqNr>001</SeqNr>"
                // + "<Field Name=\"upi_limit\">90000</Field>"
                + "<Field Name=\"cash_limit\">140000</Field>"
                // + "<Field Name=\"tap_and_pay_limit\">15000</Field>"
                + "<Field Name=\"goods_limit\">100</Field>"
                // + "<Field Name=\"international_limit\">120000</Field>"
                // + "<Field Name=\"card_not_present_limit\">90000</Field>"
                // + "<Field Name=\"atm_limit\">30000</Field>"
                // + "<Field Name=\"recurring_limit\">18000</Field>"
                + "<Field Name=\"contactless_limit\">14000</Field>"
                + "</Card>"
                + "</InquiryOrUpdateData>";

        iso.set(127, xml);
        byte[] packed = iso.pack();
        String hex = ISOUtil.hexString(packed);

        System.out.println("Generated HEX:");
        System.out.println(hex);
    }
}

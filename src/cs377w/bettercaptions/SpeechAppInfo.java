package cs377w.bettercaptions;

public class SpeechAppInfo
{
    /**
     * The login parameters should be specified in the following manner:
     * 
     * public static final String SpeechKitServer = "ndev.server.name";
     * 
     * public static final int SpeechKitPort = 1000;
     * 
     * public static final String SpeechKitAppId = "ExampleSpeechKitSampleID";
     * 
     * public static final byte[] SpeechKitApplicationKey =
     * {
     *     (byte)0x38, (byte)0x32, (byte)0x0e, (byte)0x46, (byte)0x4e, (byte)0x46, (byte)0x12, (byte)0x5c, (byte)0x50, (byte)0x1d,
     *     (byte)0x4a, (byte)0x39, (byte)0x4f, (byte)0x12, (byte)0x48, (byte)0x53, (byte)0x3e, (byte)0x5b, (byte)0x31, (byte)0x22,
     *     (byte)0x5d, (byte)0x4b, (byte)0x22, (byte)0x09, (byte)0x13, (byte)0x46, (byte)0x61, (byte)0x19, (byte)0x1f, (byte)0x2d,
     *     (byte)0x13, (byte)0x47, (byte)0x3d, (byte)0x58, (byte)0x30, (byte)0x29, (byte)0x56, (byte)0x04, (byte)0x20, (byte)0x33,
     *     (byte)0x27, (byte)0x0f, (byte)0x57, (byte)0x45, (byte)0x61, (byte)0x5f, (byte)0x25, (byte)0x0d, (byte)0x48, (byte)0x21,
     *     (byte)0x2a, (byte)0x62, (byte)0x46, (byte)0x64, (byte)0x54, (byte)0x4a, (byte)0x10, (byte)0x36, (byte)0x4f, (byte)0x64
     * };
     * 
     * Please note that all the specified values are non-functional
     * and are provided solely as an illustrative example.
     * 
     */

    /* Please contact Nuance to receive the necessary connection and login parameters */
    public static final String SpeechKitServer = "sslsandbox.nmdp.nuancemobility.net";

    public static final int SpeechKitPort = 443;
    
    public static final boolean SpeechKitSsl = true;

    public static final String SpeechKitAppId = "NMDPTRIAL_liveinspired20140514062052";

    public static final byte[] SpeechKitApplicationKey = {
	(byte)0x5e, (byte)0x59, (byte)0xd9, (byte)0xa3, (byte)0xbc, (byte)0x3c, (byte)0xa0, (byte)0x6c,
	(byte)0x8f, (byte)0x37, (byte)0x6f, (byte)0xfc, (byte)0xff, (byte)0x01, (byte)0x6e, (byte)0x7b,
	(byte)0xa0, (byte)0x55, (byte)0x25, (byte)0x5e, (byte)0xb3, (byte)0x40, (byte)0x1b, (byte)0xcc,
	(byte)0x59, (byte)0x81, (byte)0x5b, (byte)0xca, (byte)0xba, (byte)0x50, (byte)0x64, (byte)0x18,
	(byte)0xaa, (byte)0x3d, (byte)0xbb, (byte)0x66, (byte)0x79, (byte)0x5b, (byte)0xd3, (byte)0x08,
	(byte)0x92, (byte)0x42, (byte)0x04, (byte)0x91, (byte)0x43, (byte)0x72, (byte)0x0d, (byte)0xe3,
	(byte)0xc8, (byte)0x32, (byte)0x41, (byte)0x7b, (byte)0x72, (byte)0x5c, (byte)0x47, (byte)0xcb,
	(byte)0xdd, (byte)0xa2, (byte)0x95, (byte)0x98, (byte)0xf3, (byte)0xf9, (byte)0xb0, (byte)0x9d
    };
}

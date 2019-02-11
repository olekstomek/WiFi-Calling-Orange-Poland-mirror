package pl.orangelabs.wificalling.util.flavoured;

/**
 * @author Cookie
 */

public class UUIDHelper
{
    public static String getCustomUUID(final String appVersion)
    {
        return "OWCAppSm"+appVersion+"_@";
    }
}

package pl.orangelabs.wificalling.util.flavoured;

import pl.orangelabs.wificalling.R;
import pl.orangelabs.wificalling.model.Dividers;

/**
 * @author F
 */

public final class DividersFlavouredDataSet
{
    public static Dividers.DividerData[] getData(final Dividers.DividerType type)
    {
        return new Dividers.DividerData[]
            {
                new Dividers.DividerData(0xff59b484, R.drawable.divider1),
                new Dividers.DividerData(0xff59b484, R.drawable.divider2),
                new Dividers.DividerData(0xff4eb2e1, R.drawable.divider3),
                new Dividers.DividerData(0xff4eb2e1, R.drawable.divider4),
                new Dividers.DividerData(0xffffd200, R.drawable.divider5),
                new Dividers.DividerData(0xffffd200, R.drawable.divider6),
                new Dividers.DividerData(0xffa885d8, R.drawable.divider7),
                new Dividers.DividerData(0xffa885d8, R.drawable.divider8),
                new Dividers.DividerData(0xffffb4e6, R.drawable.divider9),
                new Dividers.DividerData(0xffffb4e6, R.drawable.divider10)
            };
    }
}

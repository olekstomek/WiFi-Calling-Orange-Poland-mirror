package pl.orangelabs.wificalling.util.flavoured;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;

import pl.orangelabs.wificalling.R;

/**
 * @author Cookie
 */

public class DefaultCallerPhoto
{

    public static Drawable getDefaultPhoto(final Context context)
    {
        Drawable image = DrawableCompat.wrap(ContextCompat.getDrawable(context, R.drawable.ic_avatar_full));

        int color = ContextCompat.getColor(context, R.color.white);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            DrawableCompat.setTint(image, color);

        }
        else
        {
            image.mutate().setColorFilter(color, PorterDuff.Mode.SRC_IN);
        }
        return image;
    }
}

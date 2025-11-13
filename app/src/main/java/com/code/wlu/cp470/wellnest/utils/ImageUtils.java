package com.code.wlu.cp470.wellnest.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ImageUtils {

    // Downscale an image from a given URI to a maximum side length (e.g. 480px)
    public static Bitmap downscaleFromUri(Context ctx, Uri uri, int maxSide) throws IOException {
        ContentResolver cr = ctx.getContentResolver();
        InputStream in = cr.openInputStream(uri);

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(in, null, opts);
        if (in != null) in.close();

        int srcW = opts.outWidth, srcH = opts.outHeight;
        int sample = 1;
        while (Math.max(srcW / sample, srcH / sample) > maxSide) sample *= 2;

        opts = new BitmapFactory.Options();
        opts.inSampleSize = sample;
        in = cr.openInputStream(uri);
        Bitmap bmp = BitmapFactory.decodeStream(in, null, opts);
        if (in != null) in.close();

        if (bmp == null) throw new IOException("Decode failed");

        int w = bmp.getWidth(), h = bmp.getHeight();
        float scale = maxSide / (float) Math.max(w, h);
        if (scale < 1f) {
            int nw = Math.round(w * scale);
            int nh = Math.round(h * scale);
            bmp = Bitmap.createScaledBitmap(bmp, nw, nh, true);
        }
        return bmp;
    }

    // Save a temporary compressed version of a Bitmap to cache
    public static File saveTempCompressed(Context ctx, Bitmap bmp, String prefix) throws IOException {
        File f = File.createTempFile(prefix + "_", ".webp", ctx.getCacheDir());
        try (FileOutputStream out = new FileOutputStream(f)) {
            // Use WEBP_LOSSY for API 30+; fallback to WEBP for older devices
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                bmp.compress(Bitmap.CompressFormat.WEBP_LOSSY, 80, out);
            } else {
                bmp.compress(Bitmap.CompressFormat.WEBP, 80, out);
            }
            out.flush();
        }
        return f;
    }
}

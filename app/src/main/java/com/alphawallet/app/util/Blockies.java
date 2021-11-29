package com.alphawallet.app.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;

import java.util.Arrays;

public class Blockies {
    private static final int size = 8;
    private static final long[] randSeed = new long[4];

    public static Bitmap createIcon(String address) {
        return createIcon(address, 16);
    }

    public static Bitmap createIcon(String address, int scale) {
        seedRand(address);
        HSL color = createColor();
        HSL bgColor = createColor();
        HSL spotColor = createColor();
        return createCanvas(createImageData(), color, bgColor, spotColor, scale);
    }

    private static Bitmap createCanvas(double[] imgData, HSL color, HSL bgColor, HSL spotColor, int scale) {
        int width = (int) Math.sqrt(imgData.length);

        int w = width * scale;
        int h = width * scale;

        Bitmap.Config conf = Bitmap.Config.ARGB_8888;
        Bitmap bmp = Bitmap.createBitmap(w, h, conf);
        Canvas canvas = new Canvas(bmp);

        int background = toRGB((int) bgColor.h, (int) bgColor.s, (int) bgColor.l);

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(background);
        canvas.drawRect(0, 0, w, h, paint);

        int main = toRGB((int) color.h, (int) color.s, (int) color.l);
        int scolor = toRGB((int) spotColor.h, (int) spotColor.s, (int) spotColor.l);

        for (int i = 0; i < imgData.length; i++) {
            int row = (int) Math.floor(i / width);
            int col = i % width;
            paint = new Paint();

            paint.setColor((imgData[i] == 1.0d) ? main : scolor);

            if (imgData[i] > 0d) {
                canvas.drawRect(col * scale, row * scale, (col * scale) + scale, (row * scale) + scale, paint);
            }
        }
        return getCroppedBitmap(bmp);
    }

    private static double rand() {
        int t = (int) (randSeed[0] ^ (randSeed[0] << 11));
        randSeed[0] = randSeed[1];
        randSeed[1] = randSeed[2];
        randSeed[2] = randSeed[3];
        randSeed[3] = (randSeed[3] ^ (randSeed[3] >> 19) ^ t ^ (t >> 8));
        double t1 = Math.abs(randSeed[3]);
        return (t1 / Integer.MAX_VALUE);
    }

    private static HSL createColor() {
        double h = Math.floor(rand() * 360d);
        double s = ((rand() * 60d) + 40d);
        double l = ((rand() + rand() + rand() + rand()) * 25d);
        return new HSL(h, s, l);
    }

    private static double[] createImageData() {
        int width = size;
        int height = size;

        double dataWidth = Math.ceil(width / 2);
        double mirrorWidth = width - dataWidth;

        double[] data = new double[size * size];
        int dataCount = 0;
        for (int y = 0; y < height; y++) {
            double[] row = new double[(int) dataWidth];
            for (int x = 0; x < dataWidth; x++) {
                row[x] = Math.floor(rand() * 2.3d);

            }
            double[] r = Arrays.copyOfRange(row, 0, (int) mirrorWidth);
            r = reverse(r);
            row = concat(row, r);

            for (double v : row) {
                data[dataCount] = v;
                dataCount++;
            }
        }

        return data;
    }

    public static double[] concat(double[] a, double[] b) {
        int aLen = a.length;
        int bLen = b.length;
        double[] c = new double[aLen + bLen];
        System.arraycopy(a, 0, c, 0, aLen);
        System.arraycopy(b, 0, c, aLen, bLen);
        return c;
    }

    private static double[] reverse(double[] data) {
        for (int i = 0; i < data.length / 2; i++) {
            double temp = data[i];
            data[i] = data[data.length - i - 1];
            data[data.length - i - 1] = temp;
        }
        return data;
    }

    private static void seedRand(String seed) {
        for (int i = 0; i < Blockies.randSeed.length; i++) {
            Blockies.randSeed[i] = 0;
        }
        for (int i = 0; i < seed.length(); i++) {
            long test = Blockies.randSeed[i % 4] << 5;
            if (test > Integer.MAX_VALUE << 1 || test < Integer.MIN_VALUE << 1)
                test = (int) test;

            long test2 = test - Blockies.randSeed[i % 4];
            Blockies.randSeed[i % 4] = (test2 + Character.codePointAt(seed, i));
        }

        for (int i = 0; i < Blockies.randSeed.length; i++)
            Blockies.randSeed[i] = (int) Blockies.randSeed[i];
    }

    private static int toRGB(float h, float s, float l) {
        h = h % 360.0f;
        h /= 360f;
        s /= 100f;
        l /= 100f;

        float q = 0;

        if (l < 0.5)
            q = l * (1 + s);
        else
            q = (l + s) - (s * l);

        float p = 2 * l - q;

        float r = Math.max(0, hueToRGB(p, q, h + (1.0f / 3.0f)));
        float g = Math.max(0, hueToRGB(p, q, h));
        float b = Math.max(0, hueToRGB(p, q, h - (1.0f / 3.0f)));

        r = Math.min(r, 1.0f);
        g = Math.min(g, 1.0f);
        b = Math.min(b, 1.0f);

        int red = (int) (r * 255);
        int green = (int) (g * 255);
        int blue = (int) (b * 255);
        return Color.rgb(red, green, blue);
    }

    private static float hueToRGB(float p, float q, float h) {
        if (h < 0) h += 1;
        if (h > 1) h -= 1;
        if (6 * h < 1) {
            return p + ((q - p) * 6 * h);
        }
        if (2 * h < 1) {
            return q;
        }
        if (3 * h < 2) {
            return p + ((q - p) * 6 * ((2.0f / 3.0f) - h));
        }
        return p;
    }

    private static Bitmap getCroppedBitmap(Bitmap bitmap) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawCircle(bitmap.getWidth() / 2, bitmap.getHeight() / 2,
                bitmap.getWidth() / 2, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return output;
    }

    static class HSL {
        double h, s, l;

        HSL(double h, double s, double l) {
            this.h = h;
            this.s = s;
            this.l = l;
        }
    }
}
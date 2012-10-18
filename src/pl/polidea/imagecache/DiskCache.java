package pl.polidea.imagecache;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.jakewharton.DiskLruCache;

/**
 * The Class QwiltDiskLruCache.
 */
public class DiskCache {

    private static final String TAG = DiskCache.class.getSimpleName();
    private static final int APP_VERSION = 1;
    private static final int VALUE_COUNT = 1;

    private CompressFormat compressFormat;
    private int compressQuality;
    private DiskLruCache mDiskCache;

    /**
     * Creates disk cache in specified directory and size
     * 
     * @param path
     *            path of the cache directory
     * @param size
     *            cache size in bytes
     */
    public DiskCache(final String path, final long size, final CompressFormat compressFormat, final int compressQuality) {
        try {
            mDiskCache = openDiskLruCache(new File(path), APP_VERSION, VALUE_COUNT, size);
            this.compressFormat = compressFormat;
            this.compressQuality = compressQuality;
        } catch (final IOException e) {
            Log.e(TAG, "Opening disk cache error");
        }
    }

    private DiskLruCache openDiskLruCache(final File directory, final int appVersion, final int valueCount,
            final long size) throws IOException {
        return DiskLruCache.open(directory, appVersion, valueCount, size);
    }

    private boolean writeBitmapToFile(final Bitmap bitmap, final DiskLruCache.Editor editor) throws IOException {
        OutputStream out = null;
        try {
            out = new BufferedOutputStream(editor.newOutputStream(0), 1024);
            return bitmap.isRecycled() ? true : bitmap.compress(compressFormat, compressQuality, out);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    public void put(final String key, final Bitmap data) {
        if (data == null) {
            Log.d(TAG, "null ERROR on: image put on disk cache " + key);
            return;
        }
        DiskLruCache.Editor editor = null;
        try {
            editor = mDiskCache.edit(key);
            if (editor == null) {
                return;
            }

            if (writeBitmapToFile(data, editor)) {
                mDiskCache.flush();
                editor.commit();
                Log.d(TAG, "image put on disk cache " + key);
            } else {
                editor.abort();
                Log.d(TAG, "abort ERROR on: image put on disk cache " + key);
            }
        } catch (final IOException e) {
            Log.d(TAG, "IOException ERROR on: image put on disk cache " + key);
            try {
                if (editor != null) {
                    editor.abort();
                }
            } catch (final IOException ignored) {
                Log.e(TAG, "Editor abort error");
            }
        }

        Log.d(TAG, "Cache disk current size: " + mDiskCache.size());

    }

    public Bitmap getBitmap(final String key) {

        Bitmap bitmap = null;
        DiskLruCache.Snapshot snapshot = null;
        try {

            snapshot = mDiskCache.get(key);
            if (snapshot == null) {
                return null;
            }
            final InputStream in = snapshot.getInputStream(0);
            if (in != null) {
                final BufferedInputStream buffIn = new BufferedInputStream(in, 1024);
                try {
                    Log.i(TAG, "Loading bitmap from disk");
                    bitmap = BitmapFactory.decodeStream(buffIn);
                } catch (final OutOfMemoryError e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        } catch (final IOException e) {
            Log.e(TAG, "Loading bitmap from disk error.");
        } finally {
            if (snapshot != null) {
                snapshot.close();
            }
        }

        Log.d(TAG, bitmap == null ? "" : "image read from disk " + key);

        return bitmap;

    }

    public boolean containsKey(final String key) {

        boolean contained = false;
        DiskLruCache.Snapshot snapshot = null;
        try {
            snapshot = mDiskCache.get(key);
            contained = snapshot != null;
        } catch (final IOException e) {
            Log.e(TAG, "Reading disk cache error");
        } finally {
            if (snapshot != null) {
                snapshot.close();
            }
        }

        return contained;

    }

    public boolean remove(final String key) throws IOException {
        return mDiskCache.remove(key);
    }

    public void clearCache() {
        Log.d(TAG, "disk cache CLEARED");
        try {
            final File directory = getCacheDirectory();
            final long size = getCacheMaxSize();
            mDiskCache.delete();
            mDiskCache = openDiskLruCache(directory, APP_VERSION, VALUE_COUNT, size);
        } catch (final IOException e) {
            Log.e(TAG, "Clearing disk cache error.");
        }
    }

    private long getCacheMaxSize() {
        return mDiskCache.maxSize();
    }

    private File getCacheDirectory() {
        return mDiskCache.getDirectory();
    }

}
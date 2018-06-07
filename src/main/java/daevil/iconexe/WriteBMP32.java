package daevil.iconexe;

import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * For writing 32 bit BMP (imageio won't), credit to Ian McDonagh
 */
public class WriteBMP32 {

    /**
     * Creates a new instance of WriteBMP32
     */
    private WriteBMP32() {
    }

    /**
     * Encodes and writes BMP data the output file
     *
     * @param img  the image to encode
     * @param file the file to which encoded data will be written
     * @throws java.io.IOException if an error occurs
     */
    public static void write(BufferedImage img, java.io.File file)
            throws IOException {
        java.io.FileOutputStream fout = new java.io.FileOutputStream(file);
        try {
            BufferedOutputStream out = new BufferedOutputStream(fout);
            write(img, out);
            out.flush();
        } finally {
            try {
                fout.close();
            } catch (IOException ex) {
            }
        }
    }

    /**
     * Encodes and writes BMP data to the output
     *
     * @param img the image to encode
     * @param os  the output to which encoded data will be written
     * @throws java.io.IOException if an error occurs
     */
    public static void write(BufferedImage img, java.io.OutputStream os)
            throws IOException {

        // create info header

        InfoHeader ih = createInfoHeader(img);

        // Create colour map if the image uses an indexed colour model.
        // Images with colour depth of 8 bits or less use an indexed colour
        // model.

        int mapSize = 0;
        IndexColorModel icm = null;

        if (ih.sBitCount <= 8) {
            icm = (IndexColorModel) img.getColorModel();
            mapSize = icm.getMapSize();
        }

        // Calculate header size

        int headerSize = 14 // file header
                + ih.iSize // info header
                ;

        // Calculate map size

        int mapBytes = 4 * mapSize;

        // Calculate data offset

        int dataOffset = headerSize + mapBytes;

        // Calculate bytes per line

        int bytesPerLine = 0;

        switch (ih.sBitCount) {
            case 1:
                bytesPerLine = getBytesPerLine1(ih.iWidth);
                break;
            case 4:
                bytesPerLine = getBytesPerLine4(ih.iWidth);
                break;
            case 8:
                bytesPerLine = getBytesPerLine8(ih.iWidth);
                break;
            case 24:
                bytesPerLine = getBytesPerLine24(ih.iWidth);
                break;
            case 32:
                bytesPerLine = ih.iWidth * 4;
                break;
        }

        // calculate file size

        int fileSize = dataOffset + bytesPerLine * ih.iHeight;

        // output little endian byte order

        LittleEndianOutputStream out = new LittleEndianOutputStream(os);

        // write file header
        writeFileHeader(fileSize, dataOffset, out);

        // write info header
        ih.write(out);

        // write color map (bit count <= 8)
        if (ih.sBitCount <= 8) {
            writeColorMap(icm, out);
        }

        // write raster data
        switch (ih.sBitCount) {
            case 1:
                write1(img.getRaster(), out);
                break;
            case 4:
                write4(img.getRaster(), out);
                break;
            case 8:
                write8(img.getRaster(), out);
                break;
            case 24:
                write24(img.getRaster(), out);
                break;
            case 32:
                write32(img.getRaster(), img.getAlphaRaster(), out);
                break;
        }
    }

    /**
     * Creates an <tt>InfoHeader</tt> from the source image.
     *
     * @param img the source image
     * @return the resultant <tt>InfoHeader</tt> structure
     */
    public static InfoHeader createInfoHeader(BufferedImage img) {
        InfoHeader ret = new InfoHeader();
        ret.iColorsImportant = 0;
        ret.iColorsUsed = 0;
        ret.iCompression = 0;
        ret.iHeight = img.getHeight();
        ret.iWidth = img.getWidth();
        ret.sBitCount = (short) img.getColorModel().getPixelSize();
        ret.iNumColors = 1 << (ret.sBitCount == 32 ? 24 : ret.sBitCount);
        ret.iImageSize = 0;
        return ret;
    }

    /**
     * Writes the file header.
     *
     * @param fileSize   the calculated file size for the BMP data being written
     * @param dataOffset the calculated offset within the BMP data where the actual
     *                   bitmap begins
     * @param out        the output to which the file header will be written
     * @throws java.io.IOException if an error occurs
     */
    public static void writeFileHeader(int fileSize, int dataOffset,
                                       LittleEndianOutputStream out) throws IOException {
        // signature
        byte[] signature = "BM".getBytes("UTF-8");
        out.write(signature);
        // file size
        out.writeIntLE(fileSize);
        // reserved
        out.writeIntLE(0);
        // data offset
        out.writeIntLE(dataOffset);
    }

    /**
     * Writes the colour map resulting from the source <tt>IndexColorModel</tt>.
     *
     * @param icm the source <tt>IndexColorModel</tt>
     * @param out the output to which the colour map will be written
     * @throws java.io.IOException if an error occurs
     */
    public static void writeColorMap(IndexColorModel icm,
                                     LittleEndianOutputStream out) throws IOException {
        int mapSize = icm.getMapSize();
        for (int i = 0; i < mapSize; i++) {
            int rgb = icm.getRGB(i);
            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8) & 0xFF;
            int b = (rgb) & 0xFF;
            out.writeByte(b);
            out.writeByte(g);
            out.writeByte(r);
            out.writeByte(0);
        }
    }

    /**
     * Calculates the number of bytes per line required for the given width in
     * pixels, for a 1-bit bitmap. Lines are always padded to the next 4-byte
     * boundary.
     *
     * @param width the width in pixels
     * @return the number of bytes per line
     */
    public static int getBytesPerLine1(int width) {
        int ret = (int) width / 8;
        if (ret * 8 < width) {
            ret++;
        }
        if (ret % 4 != 0) {
            ret = (ret / 4 + 1) * 4;
        }
        return ret;
    }

    /**
     * Calculates the number of bytes per line required for the given with in
     * pixels, for a 4-bit bitmap. Lines are always padded to the next 4-byte
     * boundary.
     *
     * @param width the width in pixels
     * @return the number of bytes per line
     */
    public static int getBytesPerLine4(int width) {
        int ret = (int) width / 2;
        if (ret % 4 != 0) {
            ret = (ret / 4 + 1) * 4;
        }
        return ret;
    }

    /**
     * Calculates the number of bytes per line required for the given with in
     * pixels, for a 8-bit bitmap. Lines are always padded to the next 4-byte
     * boundary.
     *
     * @param width the width in pixels
     * @return the number of bytes per line
     */
    public static int getBytesPerLine8(int width) {
        int ret = width;
        if (ret % 4 != 0) {
            ret = (ret / 4 + 1) * 4;
        }
        return ret;
    }

    /**
     * Calculates the number of bytes per line required for the given with in
     * pixels, for a 24-bit bitmap. Lines are always padded to the next 4-byte
     * boundary.
     *
     * @param width the width in pixels
     * @return the number of bytes per line
     */
    public static int getBytesPerLine24(int width) {
        int ret = width * 3;
        if (ret % 4 != 0) {
            ret = (ret / 4 + 1) * 4;
        }
        return ret;
    }

    /**
     * Calculates the size in bytes of a bitmap with the specified size and
     * colour depth.
     *
     * @param w   the width in pixels
     * @param h   the height in pixels
     * @param bpp the colour depth (bits per pixel)
     * @return the size of the bitmap in bytes
     */
    public static int getBitmapSize(int w, int h, int bpp) {
        int bytesPerLine = 0;
        switch (bpp) {
            case 1:
                bytesPerLine = getBytesPerLine1(w);
                break;
            case 4:
                bytesPerLine = getBytesPerLine4(w);
                break;
            case 8:
                bytesPerLine = getBytesPerLine8(w);
                break;
            case 24:
                bytesPerLine = getBytesPerLine24(w);
                break;
            case 32:
                bytesPerLine = w * 4;
                break;
        }
        int ret = bytesPerLine * h;
        return ret;
    }

    /**
     * Encodes and writes raster data as a 1-bit bitmap.
     *
     * @param raster the source raster data
     * @param out    the output to which the bitmap will be written
     * @throws java.io.IOException if an error occurs
     */
    public static void write1(Raster raster, LittleEndianOutputStream out)
            throws IOException {
        int bytesPerLine = getBytesPerLine1(raster.getWidth());

        byte[] line = new byte[bytesPerLine];

        for (int y = raster.getHeight() - 1; y >= 0; y--) {
            for (int i = 0; i < bytesPerLine; i++) {
                line[i] = 0;
            }

            for (int x = 0; x < raster.getWidth(); x++) {
                int bi = x / 8;
                int i = x % 8;
                int index = raster.getSample(x, y, 0);
                line[bi] = setBit(line[bi], i, index);
            }

            out.write(line);
        }
    }

    /**
     * Encodes and writes raster data as a 4-bit bitmap.
     *
     * @param raster the source raster data
     * @param out    the output to which the bitmap will be written
     * @throws java.io.IOException if an error occurs
     */
    public static void write4(Raster raster, LittleEndianOutputStream out)
            throws IOException {

        // The approach taken here is to use a buffer to hold encoded raster
        // data
        // one line at a time.
        // Perhaps we could just write directly to output instead
        // and avoid using a buffer altogether. Hypothetically speaking,
        // a very wide image would dependency a large line buffer here, but then
        // again,
        // large 4 bit bitmaps are pretty uncommon, so using the line buffer
        // approach
        // should be okay.

        int width = raster.getWidth();
        int height = raster.getHeight();

        // calculate bytes per line
        int bytesPerLine = getBytesPerLine4(width);

        // line buffer
        byte[] line = new byte[bytesPerLine];

        // encode and write lines
        for (int y = height - 1; y >= 0; y--) {

            // clear line buffer
            for (int i = 0; i < bytesPerLine; i++) {
                line[i] = 0;
            }

            // encode raster data for line
            for (int x = 0; x < width; x++) {

                // calculate buffer index
                int bi = x / 2;

                // calculate nibble index (high order or low order)
                int i = x % 2;

                // get color index
                int index = raster.getSample(x, y, 0);
                // set color index in buffer
                line[bi] = setNibble(line[bi], i, index);
            }

            // write line data (padding bytes included)
            out.write(line);
        }
    }

    /**
     * Encodes and writes raster data as an 8-bit bitmap.
     *
     * @param raster the source raster data
     * @param out    the output to which the bitmap will be written
     * @throws java.io.IOException if an error occurs
     */
    public static void write8(Raster raster, LittleEndianOutputStream out)
            throws IOException {

        int width = raster.getWidth();
        int height = raster.getHeight();

        // calculate bytes per line
        int bytesPerLine = getBytesPerLine8(width);

        // write lines
        for (int y = height - 1; y >= 0; y--) {

            // write raster data for each line
            for (int x = 0; x < width; x++) {

                // get color index for pixel
                int index = raster.getSample(x, y, 0);

                // write color index
                out.writeByte(index);
            }

            // write padding bytes at end of line
            for (int i = width; i < bytesPerLine; i++) {
                out.writeByte(0);
            }

        }
    }

    /**
     * Encodes and writes raster data as a 24-bit bitmap.
     *
     * @param raster the source raster data
     * @param out    the output to which the bitmap will be written
     * @throws java.io.IOException if an error occurs
     */
    public static void write24(Raster raster, LittleEndianOutputStream out)
            throws IOException {

        int width = raster.getWidth();
        int height = raster.getHeight();

        // calculate bytes per line
        int bytesPerLine = getBytesPerLine24(width);

        // write lines
        for (int y = height - 1; y >= 0; y--) {

            // write pixel data for each line
            for (int x = 0; x < width; x++) {

                // get RGB values for pixel
                int r = raster.getSample(x, y, 0);
                int g = raster.getSample(x, y, 1);
                int b = raster.getSample(x, y, 2);

                // write RGB values
                out.writeByte(b);
                out.writeByte(g);
                out.writeByte(r);
            }

            // write padding bytes at end of line
            for (int i = width * 3; i < bytesPerLine; i++) {
                out.writeByte(0);
            }
        }
    }

    /**
     * Encodes and writes raster data, together with alpha (transparency) data,
     * as a 32-bit bitmap.
     *
     * @param raster the source raster data
     * @param alpha  the source alpha data
     * @param out    the output to which the bitmap will be written
     * @throws java.io.IOException if an error occurs
     */
    public static void write32(Raster raster, Raster alpha,
                               LittleEndianOutputStream out) throws IOException {

        int width = raster.getWidth();
        int height = raster.getHeight();

        // write lines
        for (int y = height - 1; y >= 0; y--) {

            // write pixel data for each line
            for (int x = 0; x < width; x++) {

                // get RGBA values
                int r = raster.getSample(x, y, 0);
                int g = raster.getSample(x, y, 1);
                int b = raster.getSample(x, y, 2);
                int a = alpha.getSample(x, y, 0);

                // write RGBA values
                out.writeByte(b);
                out.writeByte(g);
                out.writeByte(r);
                out.writeByte(a);
            }
        }
    }

    /**
     * Sets a particular bit in a byte.
     *
     * @param bits  the source byte
     * @param index the index of the bit to set
     * @param bit   the value for the bit, which should be either <tt>0</tt> or
     *              <tt>1</tt>.
     * @return the   resultant byte
     */
    private static byte setBit(byte bits, int index, int bit) {
        if (bit == 0) {
            bits &= ~(1 << (7 - index));
        } else {
            bits |= 1 << (7 - index);
        }
        return bits;
    }

    /**
     * Sets a particular nibble (4 bits) in a byte.
     *
     * @param nibbles the source byte
     * @param index   the index of the nibble to set
     * @return the     value for the nibble, which should be in the range
     *                <tt>0x0..0xF</tt>.
     */
    private static byte setNibble(byte nibbles, int index, int nibble) {
        nibbles |= (nibble << ((1 - index) * 4));

        return nibbles;
    }

    /**
     * Calculates the size in bytes for a colour map with the specified bit
     * count.
     *
     * @param sBitCount the bit count, which represents the colour depth
     * @return the size of the colour map, in bytes if <tt>sBitCount</tt> is
     * less than or equal to 8, otherwise <tt>0</tt> as colour maps are
     * only used for bitmaps with a colour depth of 8 bits or less.
     */
    public static int getColorMapSize(short sBitCount) {
        int ret = 0;
        if (sBitCount <= 8) {
            ret = (1 << sBitCount) * 4;
        }
        return ret;
    }

    public static class InfoHeader {

        /**
         * The size of this <tt>InfoHeader</tt> structure in bytes.
         */
        public int iSize;
        /**
         * The width in pixels of the bitmap represented by this
         * <tt>InfoHeader</tt>.
         */
        public int iWidth;
        /**
         * The height in pixels of the bitmap represented by this
         * <tt>InfoHeader</tt>.
         */
        public int iHeight;
        /**
         * The number of planes, which should always be <tt>1</tt>.
         */
        public short sPlanes;
        /**
         * The bit count, which represents the colour depth (bits per pixel).
         * This should be either <tt>1</tt>, <tt>4</tt>, <tt>8</tt>, <tt>24</tt>
         * or <tt>32</tt>.
         */
        public short sBitCount;
        /**
         * The compression type, which should be one of the following:
         * <ul>
         * <li>{@link com.sun.imageio.plugins.bmp.BMPConstants#BI_RGB BI_RGB} - no compression</li>
         * <li>{@link com.sun.imageio.plugins.bmp.BMPConstants#BI_RLE8 BI_RLE8} - 8-bit RLE compression</li>
         * <li>{@link com.sun.imageio.plugins.bmp.BMPConstants#BI_RLE4 BI_RLE4} - 4-bit RLE compression</li>
         * </ul>
         */
        public int iCompression;
        /**
         * The compressed size of the image in bytes, or <tt>0</tt> if
         * <tt>iCompression</tt> is <tt>0</tt>.
         */
        public int iImageSize;
        /**
         * Horizontal resolution in pixels/m.
         */
        public int iXpixelsPerM;
        /**
         * Vertical resolution in pixels/m.
         */
        public int iYpixelsPerM;
        /**
         * Number of colours actually used in the bitmap.
         */
        public int iColorsUsed;
        /**
         * Number of important colours (<tt>0</tt> = all).
         */
        public int iColorsImportant;

        /**
         * Calculated number of colours, based on the colour depth specified by
         * {@link #sBitCount sBitCount}.
         */
        public int iNumColors;

        /**
         * Creates an <tt>InfoHeader</tt> with default values.
         */
        public InfoHeader() {
            // Size of InfoHeader structure = 40
            iSize = 40;
            // Width
            iWidth = 0;
            // Height
            iHeight = 0;
            // Planes (=1)
            sPlanes = 1;
            // Bit count
            sBitCount = 0;

            // caculate NumColors
            iNumColors = 0;

            // Compression
            iCompression = 0;
            // Image size - compressed size of image or 0 if Compression = 0
            iImageSize = 0;
            // horizontal resolution pixels/meter
            iXpixelsPerM = 0;
            // vertical resolution pixels/meter
            iYpixelsPerM = 0;
            // Colors used - number of colors actually used
            iColorsUsed = 0;
            // Colors important - number of important colors 0 = all
            iColorsImportant = 0;
        }

        /**
         * Creates a copy of the source <tt>InfoHeader</tt>.
         *
         * @param source the source to copy
         */
        public InfoHeader(InfoHeader source) {
            iColorsImportant = source.iColorsImportant;
            iColorsUsed = source.iColorsUsed;
            iCompression = source.iCompression;
            iHeight = source.iHeight;
            iWidth = source.iWidth;
            iImageSize = source.iImageSize;
            iNumColors = source.iNumColors;
            iSize = source.iSize;
            iXpixelsPerM = source.iXpixelsPerM;
            iYpixelsPerM = source.iYpixelsPerM;
            sBitCount = source.sBitCount;
            sPlanes = source.sPlanes;

        }

        /**
         * Writes the <tt>InfoHeader</tt> structure to output
         *
         * @param out the output to which the structure will be written
         * @throws java.io.IOException if an error occurs
         */
        public void write(LittleEndianOutputStream out) throws IOException {
            // Size of InfoHeader structure = 40
            out.writeIntLE(iSize);
            // Width
            out.writeIntLE(iWidth);
            // Height
            out.writeIntLE(iHeight);
            // Planes (=1)
            out.writeShortLE(sPlanes);
            // Bit count
            out.writeShortLE(sBitCount);

            // caculate NumColors
            // iNumColors = (int) Math.pow(2, sBitCount);

            // Compression
            out.writeIntLE(iCompression);
            // Image size - compressed size of image or 0 if Compression = 0
            out.writeIntLE(iImageSize);
            // horizontal resolution pixels/meter
            out.writeIntLE(iXpixelsPerM);
            // vertical resolution pixels/meter
            out.writeIntLE(iYpixelsPerM);
            // Colors used - number of colors actually used
            out.writeIntLE(iColorsUsed);
            // Colors important - number of important colors 0 = all
            out.writeIntLE(iColorsImportant);
        }
    }

    public static class LittleEndianOutputStream extends DataOutputStream {

        /**
         * Creates a new instance of <tt>LittleEndianOutputStream</tt>, which
         * will write to the specified target.
         *
         * @param out the target <tt>OutputStream</tt>
         */
        public LittleEndianOutputStream(java.io.OutputStream out) {
            super(out);
        }

        /**
         * Reverses the byte order of the source <tt>short</tt> value
         *
         * @param value the source value
         * @return the converted value
         */
        public static short swapShort(short value) {
            return (short) (((value & 0xFF00) >> 8) | ((value & 0x00FF) << 8));
        }

        /**
         * Reverses the byte order of the source <tt>int</tt> value
         *
         * @param value the source value
         * @return the converted value
         */
        public static int swapInteger(int value) {
            return ((value & 0xFF000000) >> 24) | ((value & 0x00FF0000) >> 8)
                    | ((value & 0x0000FF00) << 8)
                    | ((value & 0x000000FF) << 24);
        }

        /**
         * Reverses the byte order of the source <tt>long</tt> value
         *
         * @param value the source value
         * @return the converted value
         */
        public static long swapLong(long value) {
            return ((value & 0xFF00000000000000L) >> 56)
                    | ((value & 0x00FF000000000000L) >> 40)
                    | ((value & 0x0000FF0000000000L) >> 24)
                    | ((value & 0x000000FF00000000L) >> 8)
                    | ((value & 0x00000000FF000000L) << 8)
                    | ((value & 0x0000000000FF0000L) << 24)
                    | ((value & 0x000000000000FF00L) << 40)
                    | ((value & 0x00000000000000FFL) << 56);
        }

        /**
         * Reverses the byte order of the source <tt>float</tt> value
         *
         * @param value the source value
         * @return the converted value
         */
        public static float swapFloat(float value) {
            int i = Float.floatToIntBits(value);
            i = swapInteger(i);
            return Float.intBitsToFloat(i);
        }

        /**
         * Reverses the byte order of the source <tt>double</tt> value
         *
         * @param value the source value
         * @return the converted value
         */
        public static double swapDouble(double value) {
            long l = Double.doubleToLongBits(value);
            l = swapLong(l);
            return Double.longBitsToDouble(l);
        }

        public static String toHexString(int i, boolean littleEndian, int bytes) {
            if (littleEndian) {
                i = swapInteger(i);
            }
            StringBuilder sb = new StringBuilder();
            sb.append(Integer.toHexString(i));
            if (sb.length() % 2 != 0) {
                sb.insert(0, '0');
            }
            while (sb.length() < bytes * 2) {
                sb.insert(0, "00");
            }
            return sb.toString();
        }

        public static StringBuilder toCharString(StringBuilder sb, int i,
                                                 int bytes, char def) {
            int shift = 24;
            for (int j = 0; j < bytes; j++) {
                int b = (i >> shift) & 0xFF;
                char c = b < 32 ? def : (char) b;
                sb.append(c);
                shift -= 8;
            }
            return sb;
        }

        public static String toInfoString(int info) {
            StringBuilder sb = new StringBuilder();
            sb.append("Decimal: ").append(info);
            sb.append(", Hex BE: ").append(toHexString(info, false, 4));
            sb.append(", Hex LE: ").append(toHexString(info, true, 4));
            sb.append(", String BE: [");
            sb = toCharString(sb, info, 4, '.');
            sb.append(']');
            sb.append(", String LE: [");
            sb = toCharString(sb, swapInteger(info), 4, '.');
            sb.append(']');
            return sb.toString();
        }

        /**
         * Writes a little-endian <tt>short</tt> value
         *
         * @param value the source value to convert
         * @throws java.io.IOException if an error occurs
         */
        public void writeShortLE(short value) throws IOException {
            value = swapShort(value);
            super.writeShort(value);
        }

        /**
         * Writes a little-endian <tt>int</tt> value
         *
         * @param value the source value to convert
         * @throws java.io.IOException if an error occurs
         */
        public void writeIntLE(int value) throws IOException {
            value = swapInteger(value);
            super.writeInt(value);
        }

        /**
         * Writes a little-endian <tt>float</tt> value
         *
         * @param value the source value to convert
         * @throws java.io.IOException if an error occurs
         */
        public void writeFloatLE(float value) throws IOException {
            value = swapFloat(value);
            super.writeFloat(value);
        }

        /**
         * Writes a little-endian <tt>long</tt> value
         *
         * @param value the source value to convert
         * @throws java.io.IOException if an error occurs
         */
        public void writeLongLE(long value) throws IOException {
            value = swapLong(value);
            super.writeLong(value);
        }

        /**
         * Writes a little-endian <tt>double</tt> value
         *
         * @param value the source value to convert
         * @throws java.io.IOException if an error occurs
         */
        public void writeDoubleLE(double value) throws IOException {
            value = swapDouble(value);
            super.writeDouble(value);
        }

        /**
         * @since 0.6
         */
        public void writeUnsignedInt(long value) throws IOException {
            int i1 = (int) (value >> 24);
            int i2 = (int) ((value >> 16) & 0xFF);
            int i3 = (int) ((value >> 8) & 0xFF);
            int i4 = (int) (value & 0xFF);

            write(i1);
            write(i2);
            write(i3);
            write(i4);
        }

        /**
         * @since 0.6
         */
        public void writeUnsignedIntLE(long value) throws IOException {
            int i1 = (int) (value >> 24);
            int i2 = (int) ((value >> 16) & 0xFF);
            int i3 = (int) ((value >> 8) & 0xFF);
            int i4 = (int) (value & 0xFF);

            write(i4);
            write(i3);
            write(i2);
            write(i1);
        }
    }
}

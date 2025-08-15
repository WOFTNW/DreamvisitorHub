package org.woftnw.mc_renderer;

import org.lwjgl.BufferUtils;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;

/**
 * Utility class for loading textures from various sources including URLs
 */

/**
 * The TextureLoader class provides utility methods for loading textures from
 * various sources
 * and creating procedural textures. It supports loading textures from URLs,
 * input streams,
 * and generating fallback textures with a grid pattern.
 *
 * <p>
 * Key functionalities include:
 * <ul>
 * <li>Loading textures from a URL with proper handling of HTTP headers.</li>
 * <li>Loading textures from an input stream and converting them to a
 * ByteBuffer.</li>
 * <li>Creating a procedural texture with a customizable grid pattern.</li>
 * <li>Extracting and scaling the head portion from a Minecraft skin
 * texture.</li>
 * </ul>
 *
 * <p>
 * All textures are returned as ByteBuffer objects, which are suitable for use
 * with OpenGL
 * or other graphics APIs.
 *
 * <p>
 * Example usage:
 *
 * <pre>
 * {@code
 * ByteBuffer texture = TextureLoader.loadTextureFromUrl("http://example.com/texture.png");
 * ByteBuffer fallbackTexture = TextureLoader.createProceduralTexture(256, 256);
 * ByteBuffer minecraftHead = TextureLoader.extractAndScaleMinecraftHead(texture, 64);
 * }
 * </pre>
 *
 *
 */
public class TextureLoader {

    /**
     * Loads a texture from a URL
     *
     * @param url The URL to load the texture from
     * @return ByteBuffer containing the texture data
     */
    public static ByteBuffer loadTextureFromUrl(String url) throws IOException {
        // Open a connection to the URL
        URL textureUrl = new URL(url);
        URLConnection connection = textureUrl.openConnection();

        // Set user agent to avoid being blocked by some servers
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");

        // Download the image data
        try (InputStream inputStream = connection.getInputStream()) {
            return loadTextureFromStream(inputStream);
        }
    }

    /**
     * Loads a texture from an input stream
     *
     * @param inputStream The input stream to load the texture from
     * @return ByteBuffer containing the texture data
     */
    public static ByteBuffer loadTextureFromStream(InputStream inputStream) throws IOException {
        // Read the image using ImageIO
        BufferedImage image = ImageIO.read(inputStream);
        if (image == null) {
            throw new IOException("Failed to load image data");
        }

        // Convert the image to a ByteBuffer
        return imageToByteBuffer(image);
    }

    /**
     * Converts a BufferedImage to a ByteBuffer for OpenGL
     *
     * @param image The BufferedImage to convert
     * @return ByteBuffer containing the image data
     */
    private static ByteBuffer imageToByteBuffer(BufferedImage image) throws IOException {
        // Get image dimensions
        int width = image.getWidth();
        int height = image.getHeight();

        // Convert image to PNG format in memory
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        byte[] imageData = baos.toByteArray();

        // Create and return ByteBuffer
        ByteBuffer buffer = BufferUtils.createByteBuffer(imageData.length);
        buffer.put(imageData);
        buffer.flip();

        return buffer;
    }

    /**
     * Extracts and scales the head portion from a Minecraft skin texture
     *
     * @param skinTexture ByteBuffer containing the skin texture data
     * @param size        The size to scale the head to (both width and height)
     * @return byte array containing the scaled head image data in PNG format
     */
    public static byte[] extractAndScaleMinecraftHeadBytes(ByteBuffer skinTexture, int size) throws IOException {
        // Convert ByteBuffer to BufferedImage
        byte[] bytes = new byte[skinTexture.remaining()];
        skinTexture.get(bytes);

        BufferedImage skinImage = ImageIO.read(new ByteArrayInputStream(bytes));

        // Crop the head part (8x8 pixels starting at coordinates 8,8)
        BufferedImage headImage = skinImage.getSubimage(8, 8, 8, 8);

        // Scale the head to requested size
        BufferedImage scaledHeadImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = scaledHeadImage.createGraphics();
        g2d.drawImage(headImage, 0, 0, size, size, null);
        g2d.dispose();

        // Convert to byte array
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(scaledHeadImage, "png", outputStream);
        return outputStream.toByteArray();
    }

    /**
     * Extracts and scales the head portion from a Minecraft skin texture
     *
     * @param skinTexture ByteBuffer containing the skin texture data
     * @param size        The size to scale the head to (both width and height)
     * @return ByteBuffer containing the scaled head image data
     */
    public static ByteBuffer extractAndScaleMinecraftHead(ByteBuffer skinTexture, int size) throws IOException {
        byte[] imageBytes = extractAndScaleMinecraftHeadBytes(skinTexture, size);
        ByteBuffer buffer = BufferUtils.createByteBuffer(imageBytes.length);
        buffer.put(imageBytes);
        buffer.flip();
        return buffer;
    }
}

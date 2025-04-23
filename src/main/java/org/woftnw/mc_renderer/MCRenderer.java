package org.woftnw.mc_renderer;

import org.lwjgl.stb.STBImageWrite;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Main facade class for the Minecraft Renderer library.
 * Provides simplified access to the rendering functionality.
 */
public class MCRenderer {

  /**
   * Render a model with the given texture and save it to a file
   *
   * @param texturePath Path to texture resource
   * @param outputPath  Path to save the rendered image
   * @return true if rendering was successful
   */
  public static boolean renderModelToFile(String texturePath, String outputPath) {
    try {
      ByteBuffer textureData = loadResourceAsBuffer(texturePath);
      ByteBuffer renderedImage = ModelRenderer.renderModelWithTexture(textureData);
      return saveImage(renderedImage, 300, 600, outputPath);
    } catch (Exception e) {
      System.err.println("Error rendering model: " + e.getMessage());
      e.printStackTrace();
      return false;
    }
  }

  /**
   * Render a model with the given texture, configuration, and save it to a file
   *
   * @param texturePath Path to texture resource
   * @param outputPath  Path to save the rendered image
   * @param config      Renderer configuration
   * @return true if rendering was successful
   */
  public static boolean renderModelToFile(String texturePath, String outputPath, RendererConfig config) {
    try {
      ByteBuffer textureData = loadResourceAsBuffer(texturePath);

      ModelRenderer renderer = new ModelRenderer(config.getModelPath(), config.getWidth(), config.getHeight());
      renderer.setBackgroundColor(
          config.getBackgroundColor().x,
          config.getBackgroundColor().y,
          config.getBackgroundColor().z)
          .setLightPosition(
              config.getLightPosition().x,
              config.getLightPosition().y,
              config.getLightPosition().z)
          .setModelScale(config.getModelScale())
          .setModelRotationY(config.getModelRotationY())
          .setModelTranslation(
              config.getModelTranslation().x,
              config.getModelTranslation().y,
              config.getModelTranslation().z);

      renderer.initialize();
      renderer.loadModel();
      renderer.loadTextureFromBuffer(textureData);
      ByteBuffer renderedImage = renderer.renderToBuffer();
      renderer.cleanUp();

      return saveImage(renderedImage, config.getWidth(), config.getHeight(), outputPath);
    } catch (Exception e) {
      System.err.println("Error rendering model: " + e.getMessage());
      e.printStackTrace();
      return false;
    }
  }

  /**
   * Render a model with the given texture URL and save it to a file
   *
   * @param textureUrl URL to the texture
   * @param outputPath Path to save the rendered image
   * @return true if rendering was successful
   */
  public static boolean renderModelFromUrl(String textureUrl, String outputPath) {
    try {
      ByteBuffer textureData = TextureLoader.loadTextureFromUrl(textureUrl);
      ByteBuffer renderedImage = ModelRenderer.renderModelWithTexture(textureData);
      return saveImage(renderedImage, 300, 600, outputPath);
    } catch (Exception e) {
      System.err.println("Error rendering model from URL: " + e.getMessage());
      e.printStackTrace();
      return false;
    }
  }

  public static ByteBuffer renderModelToBufferFromUrl(String textureUrl, RendererConfig config) throws IOException {
    ByteBuffer textureData = TextureLoader.loadTextureFromUrl(textureUrl);
    ByteBuffer renderedImage = ModelRenderer.renderModelWithTexture(config.getModelPath(), textureData,
        config.getWidth(), config.getHeight());
    return renderedImage;
  }

  public static ByteBuffer renderModelToBufferFromUrl(String textureUrl) {
    try {
      ByteBuffer textureData = TextureLoader.loadTextureFromUrl(textureUrl);
      ByteBuffer renderedImage = ModelRenderer.renderModelWithTexture(textureData);
      return renderedImage;
    } catch (Exception e) {
      System.err.println("Error rendering model from URL: " + e.getMessage());
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Get raw rendered image data without saving to file
   *
   * @param texturePath Path to texture resource
   * @param config      Renderer configuration
   * @return ByteBuffer containing the rendered image data
   */
  public static ByteBuffer renderModelToBuffer(String texturePath, RendererConfig config) throws IOException {
    ByteBuffer textureData = loadResourceAsBuffer(texturePath);
    return ModelRenderer.renderModelWithTexture(
        config.getModelPath(),
        textureData,
        config.getWidth(),
        config.getHeight());
  }

  // Utility methods
  private static ByteBuffer loadResourceAsBuffer(String resourcePath) throws IOException {
    try (InputStream inputStream = MCRenderer.class.getResourceAsStream(resourcePath)) {
      if (inputStream == null) {
        throw new IOException("Resource not found: " + resourcePath);
      }

      byte[] bytes;
      try (java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream()) {
        int nRead;
        byte[] data = new byte[1024];
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
          buffer.write(data, 0, nRead);
        }
        buffer.flush();
        bytes = buffer.toByteArray();
      }
      ByteBuffer buffer = org.lwjgl.BufferUtils.createByteBuffer(bytes.length);
      buffer.put(bytes);
      buffer.flip();
      return buffer;
    }
  }

  /**
   * Save image data to a file
   *
   * @param imageData  ByteBuffer containing the image data
   * @param width      Width of the image in pixels
   * @param height     Height of the image in pixels
   * @param outputPath Path to save the image to
   * @return true if saving was successful
   */
  public static boolean saveImage(ByteBuffer imageData, int width, int height, String outputPath) {
    STBImageWrite.stbi_flip_vertically_on_write(true);
    return STBImageWrite.stbi_write_png(outputPath, width, height, 3, imageData, width * 3);
  }
}

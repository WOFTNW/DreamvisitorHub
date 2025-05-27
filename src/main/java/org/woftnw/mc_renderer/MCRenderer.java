// package org.woftnw.mc_renderer;

// import org.lwjgl.stb.STBImage;
// import org.lwjgl.stb.STBImageWrite;
// import java.awt.image.BufferedImage;
// import java.awt.image.DataBufferByte;
// import java.io.IOException;
// import java.io.InputStream;
// import java.nio.ByteBuffer;

// /**
//  * Main facade class for the Minecraft Renderer library.
//  * Provides simplified access to the rendering functionality.
//  */
// public class MCRenderer {

//   /**
//    * Render a model with the given texture and save it to a file
//    *
//    * @param texturePath Path to texture resource
//    * @param outputPath  Path to save the rendered image
//    * @return true if rendering was successful
//    */
//   public static boolean renderModelToFile(String texturePath, String outputPath) {
//     try {
//       ByteBuffer textureData = loadResourceAsBuffer(texturePath);
//       ByteBuffer renderedBuffer = ModelRenderer.renderModelWithTexture(textureData);
//       BufferedImage renderedImage = byteBufferToBufferedImage(renderedBuffer, 300, 600);
//       return saveImage(renderedImage, outputPath);
//     } catch (Exception e) {
//       System.err.println("Error rendering model: " + e.getMessage());
//       e.printStackTrace();
//       return false;
//     }
//   }

//   /**
//    * Render a model with the given texture, configuration, and save it to a file
//    *
//    * @param texturePath Path to texture resource
//    * @param outputPath  Path to save the rendered image
//    * @param config      Renderer configuration
//    * @return true if rendering was successful
//    */
//   public static boolean renderModelToFile(String texturePath, String outputPath, RendererConfig config) {
//     try {
//       ByteBuffer textureData = loadResourceAsBuffer(texturePath);

//       ModelRenderer renderer = new ModelRenderer(config.getModelPath(), config.getWidth(), config.getHeight());
//       renderer.setBackgroundColor(
//           config.getBackgroundColor().x,
//           config.getBackgroundColor().y,
//           config.getBackgroundColor().z)
//           .setLightPosition(
//               config.getLightPosition().x,
//               config.getLightPosition().y,
//               config.getLightPosition().z)
//           .setModelScale(config.getModelScale())
//           .setModelRotationY(config.getModelRotationY())
//           .setModelTranslation(
//               config.getModelTranslation().x,
//               config.getModelTranslation().y,
//               config.getModelTranslation().z);

//       renderer.initialize();
//       renderer.loadModel();
//       renderer.loadTextureFromBuffer(textureData);
//       ByteBuffer renderedBuffer = renderer.renderToBuffer();
//       BufferedImage renderedImage = byteBufferToBufferedImage(renderedBuffer, config.getWidth(), config.getHeight());
//       renderer.cleanUp();

//       return saveImage(renderedImage, outputPath);
//     } catch (Exception e) {
//       System.err.println("Error rendering model: " + e.getMessage());
//       e.printStackTrace();
//       return false;
//     }
//   }

//   /**
//    * Render a model with the given texture URL and save it to a file
//    *
//    * @param textureUrl URL to the texture
//    * @param outputPath Path to save the rendered image
//    * @return true if rendering was successful
//    */
//   public static boolean renderModelFromUrl(String textureUrl, String outputPath) {
//     try {
//       ByteBuffer textureData = TextureLoader.loadTextureFromUrl(textureUrl);
//       ByteBuffer renderedBuffer = ModelRenderer.renderModelWithTexture(textureData);
//       BufferedImage renderedImage = byteBufferToBufferedImage(renderedBuffer, 300, 600);
//       return saveImage(renderedImage, outputPath);
//     } catch (Exception e) {
//       System.err.println("Error rendering model from URL: " + e.getMessage());
//       e.printStackTrace();
//       return false;
//     }
//   }

//   public static BufferedImage renderModelToBufferFromUrl(String textureUrl, RendererConfig config) throws IOException {
//     ByteBuffer textureData = TextureLoader.loadTextureFromUrl(textureUrl);
//     ByteBuffer renderedBuffer = ModelRenderer.renderModelWithTexture(config.getModelPath(), textureData,
//         config.getWidth(), config.getHeight());
//     return byteBufferToBufferedImage(renderedBuffer, config.getWidth(), config.getHeight());
//   }

//   public static BufferedImage renderModelToBufferFromUrl(String textureUrl) {
//     try {
//       ByteBuffer textureData = TextureLoader.loadTextureFromUrl(textureUrl);
//       ByteBuffer renderedBuffer = ModelRenderer.renderModelWithTexture(textureData);
//       return byteBufferToBufferedImage(renderedBuffer, 300, 600);
//     } catch (Exception e) {
//       System.err.println("Error rendering model from URL: " + e.getMessage());
//       e.printStackTrace();
//       return null;
//     }
//   }

//   /**
//    * Get raw rendered image data without saving to file
//    *
//    * @param texturePath Path to texture resource
//    * @param config      Renderer configuration
//    * @return BufferedImage containing the rendered image data
//    */
//   public static BufferedImage renderModelToBuffer(String texturePath, RendererConfig config) throws IOException {
//     ByteBuffer textureData = loadResourceAsBuffer(texturePath);
//     ByteBuffer renderedBuffer = ModelRenderer.renderModelWithTexture(
//         config.getModelPath(),
//         textureData,
//         config.getWidth(),
//         config.getHeight());
//     return byteBufferToBufferedImage(renderedBuffer, config.getWidth(), config.getHeight());
//   }

//   // Utility methods
//   private static ByteBuffer loadResourceAsBuffer(String resourcePath) throws IOException {
//     try (InputStream inputStream = MCRenderer.class.getResourceAsStream(resourcePath)) {
//       if (inputStream == null) {
//         throw new IOException("Resource not found: " + resourcePath);
//       }

//       byte[] bytes;
//       try (java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream()) {
//         int nRead;
//         byte[] data = new byte[1024];
//         while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
//           buffer.write(data, 0, nRead);
//         }
//         buffer.flush();
//         bytes = buffer.toByteArray();
//       }
//       ByteBuffer buffer = org.lwjgl.BufferUtils.createByteBuffer(bytes.length);
//       buffer.put(bytes);
//       buffer.flip();
//       return buffer;
//     }
//   }

//   /**
//    * Convert ByteBuffer to BufferedImage
//    *
//    * @param buffer ByteBuffer containing RGB data
//    * @param width Width of the image
//    * @param height Height of the image
//    * @return BufferedImage created from buffer data
//    */
//   private static BufferedImage byteBufferToBufferedImage(ByteBuffer buffer, int width, int height) {
//     BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
//     for (int y = 0; y < height; y++) {
//       for (int x = 0; x < width; x++) {
//         int r = buffer.get() & 0xFF;
//         int g = buffer.get() & 0xFF;
//         int b = buffer.get() & 0xFF;
//         int rgb = (r << 16) | (g << 8) | b;
//         image.setRGB(x, height - y - 1, rgb); // Flip vertically to match OpenGL coordinate system
//       }
//     }
//     buffer.rewind(); // Reset buffer position for potential reuse
//     return image;
//   }

//   /**
//    * Save BufferedImage to a file
//    *
//    * @param image BufferedImage to save
//    * @param outputPath Path to save the image to
//    * @return true if saving was successful
//    */
//   public static boolean saveImage(BufferedImage image, String outputPath) {
//     try {
//       return javax.imageio.ImageIO.write(image, "PNG", new java.io.File(outputPath));
//     } catch (IOException e) {
//       System.err.println("Error saving image: " + e.getMessage());
//       return false;
//     }
//   }

//   /**
//    * Legacy save image method - converts BufferedImage back to ByteBuffer for STB
//    */
//   public static boolean saveImage(ByteBuffer imageData, int width, int height, String outputPath) {
//     BufferedImage image = byteBufferToBufferedImage(imageData, width, height);
//     return saveImage(image, outputPath);
//   }
// }

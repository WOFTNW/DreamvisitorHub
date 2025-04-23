package org.woftnw.mc_renderer;

import org.joml.Vector3f;

/**
 * Configuration class for the ModelRenderer.
 * Provides various settings to customize the rendering process.
 */
public class RendererConfig {
  // Default values
  private int width = 300;
  private int height = 600;
  private String modelPath = "minecraft-steve/source/steve.glb";
  private Vector3f backgroundColor = new Vector3f(0.1f, 0.1f, 0.1f);
  private Vector3f lightPosition = new Vector3f(10.0f, 10.0f, 50.0f);
  private Vector3f viewPosition = new Vector3f(0.0f, 20.0f, 30.0f);
  private Vector3f lightColor = new Vector3f(1.0f, 1.0f, 1.0f);
  private float modelScale = 1.0f;
  private float modelRotationY = 35.0f;
  private Vector3f modelTranslation = new Vector3f(0.0f, -5.0f, 0.0f);
  private boolean debugMode = false;

  /**
   * Create a default configuration
   */
  public RendererConfig() {
    // Use defaults
  }

  /**
   * Set output image dimensions
   *
   * @param width  Width in pixels
   * @param height Height in pixels
   * @return this instance for method chaining
   */
  public RendererConfig setDimensions(int width, int height) {
    this.width = width;
    this.height = height;
    return this;
  }

  /**
   * Set the model path to load
   *
   * @param modelPath Resource path to the model
   * @return this instance for method chaining
   */
  public RendererConfig setModelPath(String modelPath) {
    this.modelPath = modelPath;
    return this;
  }

  /**
   * Set background color for rendering
   *
   * @param r Red component (0.0-1.0)
   * @param g Green component (0.0-1.0)
   * @param b Blue component (0.0-1.0)
   * @return this instance for method chaining
   */
  public RendererConfig setBackgroundColor(float r, float g, float b) {
    this.backgroundColor = new Vector3f(r, g, b);
    return this;
  }

  /**
   * Set light position
   *
   * @param x X coordinate
   * @param y Y coordinate
   * @param z Z coordinate
   * @return this instance for method chaining
   */
  public RendererConfig setLightPosition(float x, float y, float z) {
    this.lightPosition = new Vector3f(x, y, z);
    return this;
  }

  /**
   * Set model scale
   *
   * @param scale Scale factor
   * @return this instance for method chaining
   */
  public RendererConfig setModelScale(float scale) {
    this.modelScale = scale;
    return this;
  }

  /**
   * Set model rotation around Y axis
   *
   * @param degrees Rotation in degrees
   * @return this instance for method chaining
   */
  public RendererConfig setModelRotationY(float degrees) {
    this.modelRotationY = degrees;
    return this;
  }

  /**
   * Set model translation
   *
   * @param x X coordinate
   * @param y Y coordinate
   * @param z Z coordinate
   * @return this instance for method chaining
   */
  public RendererConfig setModelTranslation(float x, float y, float z) {
    this.modelTranslation = new Vector3f(x, y, z);
    return this;
  }

  /**
   * Enable debug mode
   *
   * @param debug Whether to enable debug mode
   * @return this instance for method chaining
   */
  public RendererConfig setDebugMode(boolean debug) {
    this.debugMode = debug;
    return this;
  }

  // Getters
  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public String getModelPath() {
    return modelPath;
  }

  public Vector3f getBackgroundColor() {
    return backgroundColor;
  }

  public Vector3f getLightPosition() {
    return lightPosition;
  }

  public Vector3f getViewPosition() {
    return viewPosition;
  }

  public Vector3f getLightColor() {
    return lightColor;
  }

  public float getModelScale() {
    return modelScale;
  }

  public float getModelRotationY() {
    return modelRotationY;
  }

  public Vector3f getModelTranslation() {
    return modelTranslation;
  }

  public boolean isDebugMode() {
    return debugMode;
  }
}

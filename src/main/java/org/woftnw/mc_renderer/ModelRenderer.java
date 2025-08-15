package org.woftnw.mc_renderer;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.assimp.*;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.*;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * The `ModelRenderer` class provides functionality for rendering 3D models
 * with textures in a headless OpenGL context. It supports loading models
 * from various sources, applying textures, and rendering the output to an
 * offscreen framebuffer. The rendered image can be retrieved as a `ByteBuffer`.
 *
 * <p>
 * Key features of the `ModelRenderer` class include:
 * </p>
 * <ul>
 * <li>Loading 3D models using the Assimp library.</li>
 * <li>Applying textures from a `ByteBuffer`.</li>
 * <li>Rendering models with configurable dimensions.</li>
 * <li>Using OpenGL shaders for rendering.</li>
 * <li>Outputting rendered images as raw pixel data.</li>
 * <li>Support for Minecraft skin outer layers (hat, jacket, etc).</li>
 * </ul>
 *
 * <p>
 * Usage example:
 * </p>
 *
 * <pre>
 * ByteBuffer textureData = ...; // Load texture data
 * ByteBuffer renderedImage = ModelRenderer.renderModelWithTexture("/path/to/model.obj", textureData, 800, 600);
 * </pre>
 *
 * <p>
 * Note: This class requires an OpenGL-compatible environment and the
 * LWJGL library for OpenGL bindings.
 * </p>
 *
 * <p>
 * Dependencies:
 * </p>
 * <ul>
 * <li>LWJGL (Lightweight Java Game Library)</li>
 * <li>Assimp (Open Asset Import Library)</li>
 * <li>STB (Simple TrueType and Bitmap library)</li>
 * </ul>
 *
 */
public class ModelRenderer {
    // Default paths
    private static final String DEFAULT_MODEL_PATH = "minecraft-steve/source/steve.glb";
    private static final String VERTEX_SHADER_PATH = "shaders/vertex.glsl";
    private static final String FRAGMENT_SHADER_PATH = "shaders/fragment.glsl";

    // Default rendering size
    private static final int DEFAULT_WIDTH = 300;
    private static final int DEFAULT_HEIGHT = 600;

    // Minecraft skin texture dimensions (standard 64x64 skin)
    private static final int MC_SKIN_WIDTH = 64;
    private static final int MC_SKIN_HEIGHT = 64;

    // Layer inflation constants for outer layer meshes
    private static final float HEAD_INFLATION = 0.0f; // Increased from 1.2f for even more visible effect
    private static final float BODY_INFLATION = 0.0f; // Increased from 0.25f
    private static final float LIMB_INFLATION = 0.0f; // Increased from 0.25f

    // Instance variables
    private final int width;
    private final int height;
    private String modelPath;
    private long window;
    private int shaderProgram;
    private int fbo;
    private int rbo;
    private int textureColorbuffer;
    private int modelTexture;
    private List<Mesh> meshes = new ArrayList<>();
    private List<Mesh> overlayMeshes = new ArrayList<>(); // New list for overlay meshes
    private boolean isMinecraftSkin = false; // Flag to track if we're rendering a Minecraft skin
    private int textureWidth = 0;
    private int textureHeight = 0;

    // Render configuration
    private Vector3f backgroundColor = new Vector3f(0.1f, 0.1f, 0.1f);
    private Vector3f lightPosition = new Vector3f(10.0f, 10.0f, 50.0f);
    private Vector3f viewPosition = new Vector3f(0.0f, 20.0f, 30.0f);
    private Vector3f lightColor = new Vector3f(1.0f, 1.0f, 1.0f);
    private float modelScale = 1.0f;
    private float modelRotationY = 35.0f;
    private Vector3f modelTranslation = new Vector3f(0.0f, -5.0f, 0.0f);

    /**
     * Static method to render a model with the provided texture image
     *
     * @param textureImage ByteBuffer containing the texture image data
     * @return ByteBuffer containing the rendered image data
     */
    public static ByteBuffer renderModelWithTexture(ByteBuffer textureImage) {
        return renderModelWithTexture(DEFAULT_MODEL_PATH, textureImage, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    /**
     * Static method to render a specific model with the provided texture image
     *
     * @param modelPath    Path to the 3D model (resource path)
     * @param textureImage ByteBuffer containing the texture image data
     * @return ByteBuffer containing the rendered image data
     */
    public static ByteBuffer renderModelWithTexture(String modelPath, ByteBuffer textureImage) {
        return renderModelWithTexture(modelPath, textureImage, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    /**
     * Static method to render a model with the provided texture image and
     * dimensions
     *
     * @param textureImage ByteBuffer containing the texture image data
     * @param width        Width of the resulting image
     * @param height       Height of the resulting image
     * @return ByteBuffer containing the rendered image data
     */
    public static ByteBuffer renderModelWithTexture(ByteBuffer textureImage, int width, int height) {
        return renderModelWithTexture(DEFAULT_MODEL_PATH, textureImage, width, height);
    }

    /**
     * Static method to render a specific model with the provided texture image and
     * dimensions
     *
     * @param modelPath    Path to the 3D model (resource path)
     * @param textureImage ByteBuffer containing the texture image data
     * @param width        Width of the resulting image
     * @param height       Height of the resulting image
     * @return ByteBuffer containing the rendered image data
     */
    public static ByteBuffer renderModelWithTexture(String modelPath, ByteBuffer textureImage, int width, int height) {
        ModelRenderer renderer = new ModelRenderer(modelPath, width, height);
        try {
            // Initialize the renderer
            renderer.initialize();

            // Load the model
            renderer.loadModel();

            // Load the provided texture
            renderer.loadTextureFromBuffer(textureImage);

            // Render the model and get the image data
            ByteBuffer imageData = renderer.renderToBuffer();

            return imageData;
        } finally {
            // Clean up resources
            renderer.cleanUp();
        }
    }

    /**
     * Constructor with custom dimensions
     */
    public ModelRenderer(int width, int height) {
        this(DEFAULT_MODEL_PATH, width, height);
    }

    /**
     * Constructor with custom model path and dimensions
     */
    public ModelRenderer(String modelPath, int width, int height) {
        this.modelPath = modelPath;
        this.width = width;
        this.height = height;
    }

    /**
     * Default constructor
     */
    public ModelRenderer() {
        this(DEFAULT_MODEL_PATH, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    /**
     * Set background color for rendering
     *
     * @param r Red component (0.0-1.0)
     * @param g Green component (0.0-1.0)
     * @param b Blue component (0.0-1.0)
     * @return this instance for method chaining
     */
    public ModelRenderer setBackgroundColor(float r, float g, float b) {
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
    public ModelRenderer setLightPosition(float x, float y, float z) {
        this.lightPosition = new Vector3f(x, y, z);
        return this;
    }

    /**
     * Set model scale
     *
     * @param scale Scale factor
     * @return this instance for method chaining
     */
    public ModelRenderer setModelScale(float scale) {
        this.modelScale = scale;
        return this;
    }

    /**
     * Set model rotation around Y axis
     *
     * @param degrees Rotation in degrees
     * @return this instance for method chaining
     */
    public ModelRenderer setModelRotationY(float degrees) {
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
    public ModelRenderer setModelTranslation(float x, float y, float z) {
        this.modelTranslation = new Vector3f(x, y, z);
        return this;
    }

    /**
     * Initialize GLFW and OpenGL
     */
    public void initialize() {
        // Setup error callback
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW
        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        // Configure GLFW for headless rendering
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);

        // Create the window (hidden)
        window = GLFW.glfwCreateWindow(width, height, "Headless Renderer", MemoryUtil.NULL, MemoryUtil.NULL);
        if (window == MemoryUtil.NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }

        // Make the OpenGL context current
        GLFW.glfwMakeContextCurrent(window);
        GL.createCapabilities();

        // Create shaders
        shaderProgram = createShaderProgram();

        // Create framebuffer for offscreen rendering
        createFramebuffer();
    }

    /**
     * Load a texture from a ByteBuffer containing image data
     */
    public void loadTextureFromBuffer(ByteBuffer textureData) {
        modelTexture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, modelTexture);

        // Set texture wrapping and filtering options
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

        // Load texture image
        IntBuffer width = BufferUtils.createIntBuffer(1);
        IntBuffer height = BufferUtils.createIntBuffer(1);
        IntBuffer channels = BufferUtils.createIntBuffer(1);

        // Use STBImage to decode texture
        STBImage.stbi_set_flip_vertically_on_load(true);
        ByteBuffer imageData = STBImage.stbi_load_from_memory(
                textureData, width, height, channels, 4);

        if (imageData == null) {
            throw new RuntimeException("Failed to load texture: " + STBImage.stbi_failure_reason());
        }

        // Store texture dimensions
        textureWidth = width.get(0);
        textureHeight = height.get(0);

        // Check if this is likely a Minecraft skin (64x64 or 64x32)
        isMinecraftSkin = (textureWidth == MC_SKIN_WIDTH &&
                (textureHeight == MC_SKIN_HEIGHT || textureHeight == MC_SKIN_HEIGHT / 2));

        // If this is a Minecraft skin and we've already loaded the model, create
        // overlay meshes
        if (isMinecraftSkin && !meshes.isEmpty()) {
            createOverlayMeshes();
        }

        // Upload texture to GPU
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width.get(0), height.get(0),
                0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, imageData);
        GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);

        // Free the image memory as it's been uploaded to GPU
        STBImage.stbi_image_free(imageData);

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    /**
     * Load the model from resources
     */
    public void loadModel() {
        // Load model using Assimp
        AIScene scene = null;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer modelData = loadResourceAsBuffer(modelPath);

            // Try to determine the model format from the path
            String formatHint = determineModelFormat(modelPath);

            // Update flags for model processing
            int flags = Assimp.aiProcess_Triangulate |
                    Assimp.aiProcess_GenSmoothNormals |
                    Assimp.aiProcess_CalcTangentSpace |
                    Assimp.aiProcess_JoinIdenticalVertices |
                    Assimp.aiProcess_PreTransformVertices;

            // Import the model
            scene = Assimp.aiImportFileFromMemory(modelData, flags, formatHint);

            // If first attempt fails, try some common formats
            if (scene == null) {
                for (String format : new String[]{"obj", "gltf", "fbx", "dae", "blend"}) {
                    if (!format.equals(formatHint)) {
                        scene = Assimp.aiImportFileFromMemory(modelData, flags, format);
                        if (scene != null) {
                            System.out.println("Successfully loaded model as " + format);
                            break;
                        }
                    }
                }
            }

            if (scene == null) {
                throw new RuntimeException("Failed to load model " + modelPath + ": " + Assimp.aiGetErrorString());
            }

            // Process meshes
            processNode(scene.mRootNode(), scene);
        }

        // If this is a Minecraft skin (determined by texture dimensions) and texture is
        // loaded,
        // create overlay meshes
        if (isMinecraftSkin && textureWidth > 0 && textureHeight > 0) {
            createOverlayMeshes();
        }
    }

    /**
     * Determine the model format from its path
     */
    private String determineModelFormat(String path) {
        int dotIndex = path.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < path.length() - 1) {
            return path.substring(dotIndex + 1).toLowerCase();
        }
        return "glb"; // Default format
    }

    /**
     * Load model from a ByteBuffer directly
     *
     * @param modelData  The model data
     * @param formatHint Hint about the format (e.g., "obj", "glb", "fbx")
     */
    public void loadModelFromBuffer(ByteBuffer modelData, String formatHint) {
        AIScene scene = null;
        int flags = Assimp.aiProcess_Triangulate |
                Assimp.aiProcess_GenSmoothNormals |
                Assimp.aiProcess_CalcTangentSpace |
                Assimp.aiProcess_JoinIdenticalVertices |
                Assimp.aiProcess_PreTransformVertices;

        scene = Assimp.aiImportFileFromMemory(modelData, flags, formatHint);

        if (scene == null) {
            throw new RuntimeException("Failed to load model from buffer: " + Assimp.aiGetErrorString());
        }

        // Process meshes
        processNode(scene.mRootNode(), scene);
    }

    /**
     * Process a node in the model hierarchy
     */
    private void processNode(AINode node, AIScene scene) {
        // Process all meshes in the current node
        for (int i = 0; i < node.mNumMeshes(); i++) {
            AIMesh aiMesh = AIMesh.create(scene.mMeshes().get(node.mMeshes().get(i)));
            meshes.add(processMesh(aiMesh, scene));
        }

        // Process child nodes
        for (int i = 0; i < node.mNumChildren(); i++) {
            processNode(AINode.create(node.mChildren().get(i)), scene);
        }
    }

    /**
     * Process a mesh in the model
     */
    private Mesh processMesh(AIMesh mesh, AIScene scene) {
        List<Float> vertices = new ArrayList<>();
        List<Float> normals = new ArrayList<>();
        List<Float> texCoords = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();

        // Process vertices
        for (int i = 0; i < mesh.mNumVertices(); i++) {
            AIVector3D vertex = mesh.mVertices().get(i);
            vertices.add(vertex.x());
            vertices.add(vertex.y());
            vertices.add(vertex.z());

            // Process normals if available
            if (mesh.mNormals() != null) {
                AIVector3D normal = mesh.mNormals().get(i);
                normals.add(normal.x());
                normals.add(normal.y());
                normals.add(normal.z());
            }

            // Process texture coordinates
            if (mesh.mTextureCoords(0) != null) {
                AIVector3D texCoord = mesh.mTextureCoords(0).get(i);
                texCoords.add(texCoord.x());
                texCoords.add(texCoord.y());
            } else {
                // Generate texture coordinates based on position if not available
                float u, v;
                // Generate texture coordinates based on position and normal
                AIVector3D normal = mesh.mNormals() != null ? mesh.mNormals().get(i) : new AIVector3D(null);
                if (mesh.mNormals() == null) {
                    normal.x(0f);
                    normal.y(1f);
                    normal.z(0f);
                }

                // Determine dominant face direction
                float absX = Math.abs(normal.x());
                float absY = Math.abs(normal.y());
                float absZ = Math.abs(normal.z());

                if (absY > absX && absY > absZ) {
                    // Top or bottom face
                    u = (vertex.x() + 8.0f) / 16.0f;
                    v = (vertex.z() + 8.0f) / 16.0f;
                } else if (absX > absZ) {
                    // Left or right face
                    u = (vertex.z() + 8.0f) / 16.0f;
                    v = (vertex.y() + 8.0f) / 16.0f;
                } else {
                    // Front or back face
                    u = (vertex.x() + 8.0f) / 16.0f;
                    v = (vertex.y() + 8.0f) / 16.0f;
                }
                texCoords.add(u);
                texCoords.add(v);
            }
        }

        // Process faces (indices)
        for (int i = 0; i < mesh.mNumFaces(); i++) {
            AIFace face = mesh.mFaces().get(i);
            for (int j = 0; j < face.mNumIndices(); j++) {
                indices.add(face.mIndices().get(j));
            }
        }

        // Create mesh VAO/VBO
        int vao = createMeshVAO(vertices, normals, texCoords, indices);
        return new Mesh(vao, indices.size(), vertices, normals, texCoords, indices);
    }

    /**
     * Create inflated overlay meshes for Minecraft skin outer layer
     */
    private void createOverlayMeshes() {
        overlayMeshes.clear();

        for (Mesh baseMesh : meshes) {
            // Create an inflated version of each mesh for the overlay
            Mesh overlayMesh = createInflatedMesh(baseMesh);
            if (overlayMesh != null) {
                overlayMeshes.add(overlayMesh);
            }
        }
    }

    /**
     * Create an inflated version of a mesh for the outer layer
     */
    private Mesh createInflatedMesh(Mesh baseMesh) {
        List<Float> vertices = new ArrayList<>(baseMesh.vertices);
        List<Float> normals = new ArrayList<>(baseMesh.normals);
        List<Float> texCoords = new ArrayList<>();
        List<Integer> indices = new ArrayList<>(baseMesh.indices);

        // Determine inflation amount based on mesh size
        float inflation = determineMeshInflation(baseMesh);

        // Inflate vertices outward along normals
        List<Float> inflatedVertices = new ArrayList<>();
        for (int i = 0; i < vertices.size(); i += 3) {
            float x = vertices.get(i);
            float y = vertices.get(i + 1);
            float z = vertices.get(i + 2);

            float nx = normals.get(i);
            float ny = normals.get(i + 1);
            float nz = normals.get(i + 2);

            // Inflate vertex position along normal direction
            inflatedVertices.add(x + nx * inflation);
            inflatedVertices.add(y + ny * inflation);
            inflatedVertices.add(z + nz * inflation);
        }

        // Adjust texture coordinates for outer layer (shift to overlay section)
        for (int i = 0; i < baseMesh.texCoords.size(); i += 2) {
            float u = baseMesh.texCoords.get(i);
            float v = baseMesh.texCoords.get(i + 1);

            // Map to overlay section (typically at offset 32,0 for head, etc.)
            // For simplicity, this implementation uses base texcoords with offset
            // A more complete solution would map correctly for each body part
            if (u >= 0 && u <= 0.5f) { // Left half of texture
                u += 0.5f; // Move to right half for overlay
            }

            texCoords.add(u);
            texCoords.add(v);
        }

        // Create VAO for overlay mesh
        int vao = createMeshVAO(inflatedVertices, normals, texCoords, indices);
        return new Mesh(vao, indices.size(), inflatedVertices, normals, texCoords, indices);
    }

    /**
     * Determine the inflation amount for a mesh based on its dimensions
     */
    private float determineMeshInflation(Mesh mesh) {
        // Calculate mesh bounding box
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;

        for (int i = 0; i < mesh.vertices.size(); i += 3) {
            float x = mesh.vertices.get(i);
            float y = mesh.vertices.get(i + 1);
            float z = mesh.vertices.get(i + 2);

            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            maxZ = Math.max(maxZ, z);
        }

        // Calculate dimensions
        float width = maxX - minX;
        float height = maxY - minY;
        float depth = maxZ - minZ;

        // Determine mesh type based on dimensions (approximate)
        if (Math.abs(width - 8) < 1 && Math.abs(height - 8) < 1 && Math.abs(depth - 8) < 1) {
            // Head (8x8x8)
            return HEAD_INFLATION;
        } else if (Math.abs(width - 8) < 1 && Math.abs(height - 12) < 1 && Math.abs(depth - 4) < 1) {
            // Torso (8x12x4)
            return BODY_INFLATION;
        } else {
            // Arms/Legs (4x12x4) or other
            return LIMB_INFLATION;
        }
    }

    /**
     * Create a VAO for a mesh
     */
    private int createMeshVAO(List<Float> vertices, List<Float> normals,
                              List<Float> texCoords, List<Integer> indices) {
        // Create and bind VAO
        int vao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vao);

        // Create and bind VBO for vertices
        int vbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);

        // Convert List<Float> to FloatBuffer
        FloatBuffer verticesBuffer = BufferUtils.createFloatBuffer(vertices.size());
        for (float value : vertices) {
            verticesBuffer.put(value);
        }
        verticesBuffer.flip();

        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, verticesBuffer, GL15.GL_STATIC_DRAW);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 0, 0);
        GL20.glEnableVertexAttribArray(0);

        // Create and bind VBO for normals if available
        if (!normals.isEmpty()) {
            int normalVBO = GL15.glGenBuffers();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, normalVBO);

            // Convert List<Float> to FloatBuffer
            FloatBuffer normalsBuffer = BufferUtils.createFloatBuffer(normals.size());
            for (float value : normals) {
                normalsBuffer.put(value);
            }
            normalsBuffer.flip();

            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, normalsBuffer, GL15.GL_STATIC_DRAW);
            GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, 0, 0);
            GL20.glEnableVertexAttribArray(1);
        }

        // Create and bind VBO for texture coordinates
        if (!texCoords.isEmpty()) {
            int texCoordVBO = GL15.glGenBuffers();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, texCoordVBO);

            // Convert List<Float> to FloatBuffer
            FloatBuffer texCoordsBuffer = BufferUtils.createFloatBuffer(texCoords.size());
            for (float value : texCoords) {
                texCoordsBuffer.put(value);
            }
            texCoordsBuffer.flip();

            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, texCoordsBuffer, GL15.GL_STATIC_DRAW);
            GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, 0, 0);
            GL20.glEnableVertexAttribArray(2);
        }

        // Create and bind element array buffer for indices
        int ebo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ebo);

        // Convert List<Integer> to IntBuffer
        IntBuffer indicesBuffer = BufferUtils.createIntBuffer(indices.size());
        for (int value : indices) {
            indicesBuffer.put(value);
        }
        indicesBuffer.flip();

        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL15.GL_STATIC_DRAW);

        // Unbind VAO
        GL30.glBindVertexArray(0);

        return vao;
    }

    /**
     * Create a shader program
     */
    private int createShaderProgram() {
        // Load and compile vertex shader
        String vertexShaderSource = loadResourceAsString(VERTEX_SHADER_PATH);
        int vertexShader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        GL20.glShaderSource(vertexShader, vertexShaderSource);
        GL20.glCompileShader(vertexShader);
        checkShaderCompileStatus(vertexShader);

        // Load and compile fragment shader
        String fragmentShaderSource = loadResourceAsString(FRAGMENT_SHADER_PATH);
        int fragmentShader = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        GL20.glShaderSource(fragmentShader, fragmentShaderSource);
        GL20.glCompileShader(fragmentShader);
        checkShaderCompileStatus(fragmentShader);

        // Create shader program
        int program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vertexShader);
        GL20.glAttachShader(program, fragmentShader);
        GL20.glLinkProgram(program);
        checkProgramLinkStatus(program);

        // Cleanup shaders as they're linked into the program now
        GL20.glDeleteShader(vertexShader);
        GL20.glDeleteShader(fragmentShader);

        return program;
    }

    /**
     * Create a framebuffer for offscreen rendering
     */
    private void createFramebuffer() {
        // Generate and bind framebuffer
        fbo = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);

        // Create a color attachment texture
        textureColorbuffer = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureColorbuffer);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB, width, height, 0, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE,
                (ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL32.glFramebufferTexture(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, textureColorbuffer, 0);

        // Create a renderbuffer object for depth and stencil attachment
        rbo = GL30.glGenRenderbuffers();
        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, rbo);
        GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL30.GL_DEPTH24_STENCIL8, width, height);
        GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_STENCIL_ATTACHMENT, GL30.GL_RENDERBUFFER, rbo);

        // Check if framebuffer is complete
        if (GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("Framebuffer is not complete!");
        }

        // Unbind framebuffer
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }

    /**
     * Render the model to a buffer and return the image data
     */
    public ByteBuffer renderToBuffer() {
        // Bind framebuffer
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);

        // Clear the framebuffer with the configured background color
        GL11.glClearColor(backgroundColor.x, backgroundColor.y, backgroundColor.z, 1.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glEnable(GL11.GL_DEPTH_TEST);

        // Set viewport
        GL11.glViewport(0, 0, width, height);

        // Use shader program
        GL20.glUseProgram(shaderProgram);

        // Create projection matrix
        Matrix4f projection = new Matrix4f().perspective((float) Math.toRadians(45.0f),
                (float) width / (float) height, 0.1f, 100.0f);

        // Create view matrix
        Matrix4f view = new Matrix4f().lookAt(
                new Vector3f(0.0f, 0.0f, 50.0f), // Camera position
                new Vector3f(0.0f, 0.0f, 0.0f), // Look at target
                new Vector3f(0.0f, 1.0f, 0.0f) // Up vector
        );

        // Set up model matrix with configured parameters
        Matrix4f model = new Matrix4f().identity()
                .translate(modelTranslation)
                .rotate((float) Math.toRadians(modelRotationY), new Vector3f(0, 1, 0))
                .scale(modelScale);

        // Upload matrices to shader
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int modelLoc = GL20.glGetUniformLocation(shaderProgram, "model");
            FloatBuffer modelBuffer = stack.mallocFloat(16);
            model.get(modelBuffer);
            GL20.glUniformMatrix4fv(modelLoc, false, modelBuffer);

            int viewLoc = GL20.glGetUniformLocation(shaderProgram, "view");
            FloatBuffer viewBuffer = stack.mallocFloat(16);
            view.get(viewBuffer);
            GL20.glUniformMatrix4fv(viewLoc, false, viewBuffer);

            int projLoc = GL20.glGetUniformLocation(shaderProgram, "projection");
            FloatBuffer projBuffer = stack.mallocFloat(16);
            projection.get(projBuffer);
            GL20.glUniformMatrix4fv(projLoc, false, projBuffer);

            // Set lighting uniforms with configured values
            int lightPosLoc = GL20.glGetUniformLocation(shaderProgram, "lightPos");
            GL20.glUniform3f(lightPosLoc, lightPosition.x, lightPosition.y, lightPosition.z);

            int viewPosLoc = GL20.glGetUniformLocation(shaderProgram, "viewPos");
            GL20.glUniform3f(viewPosLoc, viewPosition.x, viewPosition.y, viewPosition.z);

            int lightColorLoc = GL20.glGetUniformLocation(shaderProgram, "lightColor");
            GL20.glUniform3f(lightColorLoc, lightColor.x, lightColor.y, lightColor.z);

            // Set texture uniform
            int textureLoc = GL20.glGetUniformLocation(shaderProgram, "textureSampler");
            GL20.glUniform1i(textureLoc, 0); // 0 corresponds to GL_TEXTURE0
        }

        // Activate texture unit and bind texture
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, modelTexture);

        // Enable alpha blending for transparent textures
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        // Regular (two-pass) render for Minecraft skins with overlays
        if (isMinecraftSkin && !overlayMeshes.isEmpty()) {
            // First pass: render base mesh
            for (Mesh mesh : meshes) {
                GL30.glBindVertexArray(mesh.getVao());
                GL11.glDrawElements(GL11.GL_TRIANGLES, mesh.getIndicesCount(), GL11.GL_UNSIGNED_INT, 0);
                GL30.glBindVertexArray(0);
            }

            // Second pass: render overlay mesh with alpha blending
            for (Mesh mesh : overlayMeshes) {
                GL30.glBindVertexArray(mesh.getVao());
                GL11.glDrawElements(GL11.GL_TRIANGLES, mesh.getIndicesCount(), GL11.GL_UNSIGNED_INT, 0);
                GL30.glBindVertexArray(0);
            }
        } else {
            // Standard render for non-skin models
            for (Mesh mesh : meshes) {
                GL30.glBindVertexArray(mesh.getVao());
                GL11.glDrawElements(GL11.GL_TRIANGLES, mesh.getIndicesCount(), GL11.GL_UNSIGNED_INT, 0);
                GL30.glBindVertexArray(0);
            }
        }

        // Disable blending
        GL11.glDisable(GL11.GL_BLEND);

        // Unbind texture
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

        // Read the rendered image from the framebuffer
        ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 3);
        GL11.glReadPixels(0, 0, width, height, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, buffer);

        // Unbind framebuffer
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);

        return buffer;
    }

    /**
     * Clean up OpenGL resources
     */
    public void cleanUp() {
        // Delete framebuffer resources
        GL30.glDeleteFramebuffers(fbo);
        GL30.glDeleteRenderbuffers(rbo);
        GL11.glDeleteTextures(textureColorbuffer);

        // Delete texture
        GL11.glDeleteTextures(modelTexture);

        // Delete shader program
        GL20.glDeleteProgram(shaderProgram);

        // Delete mesh resources
        for (Mesh mesh : meshes) {
            GL30.glDeleteVertexArrays(mesh.getVao());
        }

        // Delete overlay mesh resources
        for (Mesh mesh : overlayMeshes) {
            GL30.glDeleteVertexArrays(mesh.getVao());
        }

        // Terminate GLFW
        if (window != 0) {
            GLFW.glfwDestroyWindow(window);
        }

        GLFW.glfwTerminate();
    }

    // Utility methods
    private void checkShaderCompileStatus(int shader) {
        int success = GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS);
        if (success == GL11.GL_FALSE) {
            String log = GL20.glGetShaderInfoLog(shader);
            throw new RuntimeException("Shader compilation failed: " + log);
        }
    }

    private void checkProgramLinkStatus(int program) {
        int success = GL20.glGetProgrami(program, GL20.GL_LINK_STATUS);
        if (success == GL11.GL_FALSE) {
            String log = GL20.glGetProgramInfoLog(program);
            throw new RuntimeException("Program linking failed: " + log);
        }
    }

    private static String loadResourceAsString(String resourcePath) {
        try (InputStream inputStream = new FileInputStream(new File(resourcePath))) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] temp = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(temp)) != -1) {
                buffer.write(temp, 0, bytesRead);
            }
            byte[] bytes = buffer.toByteArray();
            return new String(bytes);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load resource: " + resourcePath, e);
        }
    }

    private static ByteBuffer loadResourceAsBuffer(String resourcePath) {
        try (InputStream inputStream = new FileInputStream(new File(resourcePath))) {
            if (inputStream == null) {
                throw new RuntimeException("Resource not found: " + resourcePath);
            }

            ByteArrayOutputStream tempBuffer = new ByteArrayOutputStream();
            byte[] temp = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(temp)) != -1) {
                tempBuffer.write(temp, 0, bytesRead);
            }
            byte[] bytes = tempBuffer.toByteArray();
            ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length);
            buffer.put(bytes);
            buffer.flip();
            return buffer;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load resource: " + resourcePath, e);
        }
    }

    // Utility class to represent a mesh
    private static class Mesh {
        private final int vao;
        private final int indicesCount;
        private final List<Float> vertices;
        private final List<Float> normals;
        private final List<Float> texCoords;
        private final List<Integer> indices;

        public Mesh(int vao, int indicesCount, List<Float> vertices, List<Float> normals,
                    List<Float> texCoords, List<Integer> indices) {
            this.vao = vao;
            this.indicesCount = indicesCount;
            this.vertices = vertices;
            this.normals = normals;
            this.texCoords = texCoords;
            this.indices = indices;
        }

        public int getVao() {
            return vao;
        }

        public int getIndicesCount() {
            return indicesCount;
        }
    }
}

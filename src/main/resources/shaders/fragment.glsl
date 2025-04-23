#version 330 core
out vec4 FragColor;

in vec3 FragPos;
in vec3 Normal;
in vec2 TexCoord;

uniform vec3 lightPos;
uniform vec3 viewPos;
uniform vec3 lightColor;
uniform sampler2D textureSampler;
uniform int debugMode;

// Helper function to create outline effect
float calculateOutline(vec3 normal, vec3 viewDir) {
    float NdotV = dot(normalize(normal), normalize(viewDir));
    float outline = 1.0 - smoothstep(0.0, 0.6, NdotV);
    return pow(outline, 2.0) * 0.5;
}

void main()
{
    // Debug mode to visualize texture coordinates
    if (debugMode == 1) {
        FragColor = vec4(TexCoord.x, TexCoord.y, 0.0, 1.0);
        return;
    }

    // Get texture color with improved texture mapping
    vec2 adjustedTexCoord = TexCoord;

    // Make sure texture coordinates are in valid range
    adjustedTexCoord = fract(adjustedTexCoord); // Keep only fractional part (0-1)

    vec4 texColor = texture(textureSampler, adjustedTexCoord);

    // Flag to track if this is an overlay layer
    bool isOverlay = adjustedTexCoord.x > 0.5;

    // View direction for lighting calculations
    vec3 viewDir = normalize(viewPos - FragPos);

    // For Minecraft skins - handle transparency properly for overlay layers
    if (isOverlay) {
        // This is an overlay (second layer) texture
        if (texColor.a < 0.1) {
            // Discard transparent pixels in the overlay to show the base layer beneath
            discard;
        }

        // Calculate outline effect for all overlay parts
        float outlineStrength = calculateOutline(Normal, viewDir);

        // Enhance the head part specifically (head is usually in the top portion of the texture)
        if (adjustedTexCoord.y < 0.25) {
            // Apply enhanced highlight to make hat/head overlay more visible
            float rimFactor = 1.0 - max(0.0, dot(normalize(Normal), normalize(viewPos - FragPos)));
            rimFactor = pow(rimFactor, 2.5) * 0.7; // Stronger rim effect (was 3.0, 0.5)

            // Brighten overlay texture more noticeably
            texColor.rgb *= 1.25; // Was 1.15

            // Add stronger rim highlighting
            texColor.rgb += vec3(rimFactor);

            // Add a subtle color tint to the hat to make it stand out
            // Slight blue tint for visual distinction
            texColor.rgb = mix(texColor.rgb, texColor.rgb * vec3(0.9, 0.95, 1.1), 0.15);

            // Enhanced outline for hat edges
            outlineStrength *= 1.5;
        }
        else {
            // For body parts overlay (jacket, pants, etc.)
            // Apply a subtle tint to differentiate from base layer
            texColor.rgb = mix(texColor.rgb, texColor.rgb * vec3(1.05, 1.05, 1.08), 0.1);
            texColor.rgb *= 1.1; // Slightly brighten
        }

        // Apply outline effect to all overlay parts
        vec3 outlineColor = texColor.rgb * 0.8; // Slightly darker outline
        texColor.rgb = mix(texColor.rgb, outlineColor, outlineStrength);
    }
    // For base layer or non-skin models, show a pattern for missing textures
    else if (texColor.a < 0.1) {
        // Use a procedural pattern based on position only for non-overlay parts
        float checker = mod(floor(FragPos.x * 2) + floor(FragPos.y * 2) + floor(FragPos.z * 2), 2.0);
        texColor = vec4(checker * 0.8, 0.2, 1.0 - checker * 0.8, 1.0);
    }

    // Ambient
    float ambientStrength = isOverlay ? 0.35 : 0.3; // Slightly higher ambient for overlay
    vec3 ambient = ambientStrength * lightColor;

    // Diffuse
    vec3 norm = normalize(Normal);
    vec3 lightDir = normalize(lightPos - FragPos);
    float diff = max(dot(norm, lightDir), 0.0);

    // Enhanced diffuse for overlay parts (more pronounced lighting)
    if (isOverlay) {
        diff = pow(diff, 0.9) * 1.1; // Boost diffuse lighting on overlay
    }

    vec3 diffuse = diff * lightColor;

    // Specular
    float specularStrength = isOverlay ? 0.7 : 0.5; // Higher specular for overlay
    vec3 reflectDir = reflect(-lightDir, norm);

    // Tighter, more intense specular highlight for overlay parts
    float specPower = isOverlay ? 64.0 : 32.0;
    float spec = pow(max(dot(viewDir, reflectDir), 0.0), specPower);

    vec3 specular = specularStrength * spec * lightColor;

    // Result - use texture color with enhanced lighting
    vec3 result = (ambient + diffuse + specular) * texColor.rgb;

    // Final contrast adjustment for overlay parts to make them pop
    if (isOverlay) {
        result = pow(result, vec3(0.95)); // Slightly increase contrast
    }

    FragColor = vec4(result, texColor.a);
}

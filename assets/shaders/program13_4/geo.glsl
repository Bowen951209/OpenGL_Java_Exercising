#version 430

// Inputs from vertex shader
layout (triangles) in;
in vec3 varyingNormal[];

// Outputs through rasterizer to fragment shader.
layout (line_strip, max_vertices = 2) out;
out vec3 varyingNormalG;
out vec3 varyingLightDirG;
out vec3 varyingHalfVectorG;
out vec3 varyingVertPosrG;

struct PositionalLight
{ vec4 ambient;
    vec4 diffuse;
    vec4 specular;
    vec3 position;
};

struct Material
{ vec4 ambient;
    vec4 diffuse;
    vec4 specular;
    float shininess;
};

uniform vec4 globalAmbient;
uniform PositionalLight light;
uniform Material material;
uniform mat4 mv_matrix;
uniform mat4 proj_matrix;
uniform mat4 norm_matrix;

float lineHeight = 0.5f;

void main(void) {
    // A triangle's normal = average of 3 vertices' normals.
    vec4 triangleNormal = vec4((varyingNormal[0] + varyingNormal[1] + varyingNormal[2]) / 3.0, 1.0);

    // oringinal point
    vec3 oringinalPoint0 = gl_in[0].gl_Position.xyz;
    vec3 oringinalPoint1 = gl_in[1].gl_Position.xyz;
    vec3 oringinalPoint2 = gl_in[2].gl_Position.xyz;

    vec3 movedPoint0 = gl_in[0].gl_Position.xyz + normalize(varyingNormal[0]) * lineHeight;
    vec3 movedPoint1 = gl_in[1].gl_Position.xyz + normalize(varyingNormal[1]) * lineHeight;
    vec3 movedPoint2 = gl_in[2].gl_Position.xyz + normalize(varyingNormal[2]) * lineHeight;

    vec3 startPoint = (oringinalPoint0 + oringinalPoint1 + oringinalPoint2) / 3.0;
    vec3 endPoint = (movedPoint0 + movedPoint1 + movedPoint2) / 3.0;

    // first vertex of the line primitive
    gl_Position = proj_matrix * mv_matrix * vec4(startPoint, 1.0);
    varyingVertPosrG = startPoint;
    varyingLightDirG = light.position - startPoint;
    varyingNormalG = varyingNormal[0];
    EmitVertex();

    // second vertex of the line primitive
    gl_Position = proj_matrix * mv_matrix * vec4(endPoint, 1.0);
    varyingVertPosrG = endPoint;
    varyingLightDirG = light.position - endPoint;
    varyingNormalG = varyingNormal[1];
    EmitVertex();

    EndPrimitive();
}
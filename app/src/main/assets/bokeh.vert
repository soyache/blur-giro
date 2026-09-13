attribute vec2 aPosition;
uniform vec2 uScreenHalfSize;
uniform mat4 uModelMatrix;
varying vec3 vWorldPos;
void main() {
    gl_Position=vec4(aPosition,0.0,1.0);
    vWorldPos=(uModelMatrix*vec4(aPosition*uScreenHalfSize,0.0,1.0)).xyz;
}

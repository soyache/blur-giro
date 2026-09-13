#extension GL_OES_EGL_image_external : require
precision highp float;
uniform samplerExternalOES uScreen;
uniform mat4 uTextureMatrix;
varying vec2 vUv;
void main() {
    vec2 st=(uTextureMatrix*vec4(vUv,0.0,1.0)).xy;
    gl_FragColor=texture2D(uScreen,st);
}

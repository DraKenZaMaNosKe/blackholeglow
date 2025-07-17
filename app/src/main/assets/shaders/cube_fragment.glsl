#version 100
precision mediump float;

uniform vec4 u_Color;
uniform float u_Time;

void main() {

    float t = (sin(u_Time) + 1.0) * 0.5; // rango 0 a 1
    if(t>0.6){
        gl_FragColor = vec4(vec3(0.0, 0.3 * t , t * 0.4), 1.0);
    }else{
        t+= 0.3;
        gl_FragColor = vec4(vec3(0.0, 0.3 * t , t * 0.4), 1.0);
    }
    //gl_FragColor = vec4(vec3(0.0, 0.3 * t , t * 0.4), 1.0);
}
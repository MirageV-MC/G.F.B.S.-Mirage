package org.mirage.fams.central;

/**
 * G.F.B.S. Mirage (mirage_gfbs) - A Minecraft Mod
 * Copyright (C) 2025-2029 Mirage-MC
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

final class SpaceMath {
    private SpaceMath(){}

    static void zero(double[] a){ for(int i=0;i<a.length;i++) a[i]=0; }
    static void copy(double[] src,double[] dst){ int n=Math.min(src.length,dst.length); for(int i=0;i<n;i++) dst[i]=src[i]; }
    static double dot(double[] a,double[] b){ double s=0; int n=Math.min(a.length,b.length); for(int i=0;i<n;i++) s+=a[i]*b[i]; return s; }
    static double norm2(double[] a){ return Math.sqrt(dot(a,a)); }

    static void clamp(double[] v,double[] lo,double[] hi){
        for(int i=0;i<v.length;i++){
            double x=v[i], l=lo[i], h=hi[i];
            if(x<l) v[i]=l; else if(x>h) v[i]=h;
        }
    }

    // tanh activation
    static void tanh(double[] x){
        for(int i=0;i<x.length;i++){
            double v=x[i];
            x[i]=Math.tanh(v);
        }
    }

    // y = A*x （A: r*c）
    static void mul(double[][] A,double[] x,double[] y){
        int r=A.length, c=(r==0?0:A[0].length);
        for(int i=0;i<r;i++){
            double s=0;
            double[] Ai=A[i];
            for(int k=0;k<c;k++) s+=Ai[k]*x[k];
            y[i]=s;
        }
    }

    // A += lr * (grad)  (同shape)
    static void addScaled(double[][] A,double[][] G,double lr){
        for(int i=0;i<A.length;i++){
            double[] Ai=A[i], Gi=G[i];
            for(int j=0;j<Ai.length;j++) Ai[j]+=lr*Gi[j];
        }
    }

    // 外积：G = a (len r) ⊗ b (len c) => r*c
    static void outer(double[] a,double[] b,double[][] G){
        for(int i=0;i<a.length;i++){
            double ai=a[i];
            double[] Gi=G[i];
            for(int j=0;j<b.length;j++) Gi[j]=ai*b[j];
        }
    }

    // 轻量非线性“流形投影”：x' = tanh(x * s)  (s为尺度)
    static void manifoldProject(double[] x,double s){
        for(int i=0;i<x.length;i++){
            double v=x[i]*s;
            x[i]=Math.tanh(v);
        }
    }

    // 观测融合：x = x * wObs
    static void applyObsWeight(double[] x,double[] w){
        for(int i=0;i<x.length;i++) x[i]*=w[i];
    }

    // 误差 e = (target - x) * weight
    static void error(double[] x,double[] target,double[] w,double[] e){
        for(int i=0;i<x.length;i++) e[i]=(target[i]-x[i])*w[i];
    }
}

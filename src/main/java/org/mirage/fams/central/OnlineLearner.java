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

import org.mirage.fams.central.FamsTypes.*;
import org.mirage.fams.central.MetaSpace.Shell;

final class OnlineLearner {
    private final int n,m;
    private final double[] e;        // N
    private final double[] dU;       // M
    private final double[][] gW;     // MxN
    private double lr;              // 学习率
    private double l2;              // L2正则

    OnlineLearner(int n,int m){
        this.n=n; this.m=m;
        e=new double[n];
        dU=new double[m];
        gW=new double[m][n];
        lr=0.01;
        l2=1e-4;
    }

    void setLR(double lr){ this.lr=lr; }
    void setL2(double l2){ this.l2=l2; }

    // 目标:让动作 u 促使状态逼近 target
    // dU = J * e  近似：用一组固定投影把N维误差映射到M维动作梯度
    private void errorToDU(double[] e, double[] dU){
        for(int k=0;k<m;k++){
            double s=0;
            for(int i=0;i<n;i++){
                double p=((k*73+i*19)%29-14)*0.03;
                s += p*e[i];
            }
            dU[k]=Math.tanh(s);
        }
    }

    // 更新某个壳：W,b 朝着能减少 loss 的方向走; 同时受风险门控
    void trainShell(Shell sh, State s, Goal g, Safety safe, double loss){
        SpaceMath.error(s.x, g.targetX, g.weight, e);
        errorToDU(e, dU);

        double gate = 1.0 - safe.risk;
        if(gate<0.05) gate=0.05;

        // 近似梯度：希望 (W*x+b) 更接近 dU
        // gradW = (dU - uPred) ⊗ x
        double[] uPred = new double[m];
        SpaceMath.mul(sh.W, s.x, uPred);
        for(int k=0;k<m;k++){
            double err = (dU[k] - (uPred[k] + sh.b[k]));
            // gW[k][i] = err * x[i]
            double[] gWi=gW[k];
            for(int i=0;i<n;i++) gWi[i]=err*s.x[i] - l2*sh.W[k][i];
            // b 梯度
            sh.b[k] += lr*gate*err;
        }
        SpaceMath.addScaled(sh.W, gW, lr*gate);

        if(loss>1.0) sh.temp*=1.001;
        else sh.temp*=0.999;
        if(sh.temp>2.0) sh.temp=2.0;
        if(sh.temp<0.2) sh.temp=0.2;
    }
}

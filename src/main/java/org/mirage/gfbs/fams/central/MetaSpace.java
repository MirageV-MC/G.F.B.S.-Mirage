package org.mirage.gfbs.fams.central;

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

import org.mirage.gfbs.fams.central.FamsTypes.*;

public final class MetaSpace {
    public static final class Shell {
        public final double[][] W;  // m x n
        public final double[] b;    // m
        public double temp;         // softmax temperature
        public double score;        // fitness score
        Shell(int n,int m){
            W=new double[m][n];
            b=new double[m];
            temp=1.0;
            score=0;
        }
    }

    public final int n,m;
    private final Shell[] shells;
    private int count;

    public MetaSpace(int n,int m,int max){
        this.n=n; this.m=m;
        shells=new Shell[max];
        count=0;
    }

    public int count(){ return count; }
    public Shell get(int i){ return shells[i]; }

    public void addShell(){
        if(count>=shells.length) return;
        shells[count++]=new Shell(n,m);
    }

    // 生成动作提案
    public void propose(State s, Goal g, Safety safe, Action out){
        int best=0;
        double bs=shells[0].score;
        for(int i=1;i<count;i++){
            double sc=shells[i].score;
            if(sc>bs){ bs=sc; best=i; }
        }
        Shell sh=shells[best];

        double[] x=new double[n];
        for(int i=0;i<n;i++) x[i]=s.x[i];
        SpaceMath.applyObsWeight(x, s.obsWeight);
        SpaceMath.tanh(x);

        // u = W*x + b
        for(int i=0;i<m;i++){
            double v=sh.b[i];
            for(int j=0;j<n;j++) v += sh.W[i][j]*x[j];
            out.u[i]=v;
        }
        SpaceMath.clamp(out.u, safe.uMin, safe.uMax);
    }

    // 反馈: 根据损失调整 score, 并对联锁给予惩罚
    public void feedback(double loss, Safety safe){
        double penalty = safe.risk >= safe.riskHardLimit ? 0.5 : 0.0;
        double fit = 1.0/(1.0+loss+penalty);
        for(int i=0;i<count;i++){
            shells[i].score = shells[i].score*0.97 + fit*0.03;
        }
    }
}

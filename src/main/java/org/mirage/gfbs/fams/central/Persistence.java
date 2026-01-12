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

import java.io.*;
import org.mirage.gfbs.fams.central.MetaSpace.Shell;

final class Persistence {
    private Persistence(){}

    static void save(File f, MetaSpace ms) throws IOException{
        DataOutputStream out=new DataOutputStream(new BufferedOutputStream(new FileOutputStream(f)));
        try{
            out.writeInt(ms.n); out.writeInt(ms.m); out.writeInt(ms.count());
            for(int si=0;si<ms.count();si++){
                Shell sh=ms.get(si);
                out.writeDouble(sh.score);
                out.writeDouble(sh.temp);
                for(int i=0;i<ms.m;i++) out.writeDouble(sh.b[i]);
                for(int i=0;i<ms.m;i++) for(int j=0;j<ms.n;j++) out.writeDouble(sh.W[i][j]);
            }
        } finally { out.close(); }
    }

    static void load(File f, MetaSpace ms) throws IOException{
        DataInputStream in=new DataInputStream(new BufferedInputStream(new FileInputStream(f)));
        try{
            int n=in.readInt(), m=in.readInt(), c=in.readInt();
            if(n!=ms.n||m!=ms.m) throw new IOException("shape mismatch");
            for(int i=0;i<c;i++) ms.addShell();
            for(int si=0;si<ms.count();si++){
                Shell sh=ms.get(si);
                sh.score=in.readDouble();
                sh.temp=in.readDouble();
                for(int i=0;i<ms.m;i++) sh.b[i]=in.readDouble();
                for(int i=0;i<ms.m;i++) for(int j=0;j<ms.n;j++) sh.W[i][j]=in.readDouble();
            }
        } finally { in.close(); }
    }
}

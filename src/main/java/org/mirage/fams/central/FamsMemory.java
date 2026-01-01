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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.mirage.fams.central.FamsTypes.SystemMode;

/**
 * FAMS 记忆系统：
 *  - 实时短期记忆（ShortTerm）：环形缓存 + TTL 过期清理，用于实时决策/趋势检测
 *  - 历史长期记忆（LongTerm）：分段 WAL 日志 + 索引 + 保留策略，用于长期回放/统计
 *
 * 设计目标：
 *  - 线程安全：控制线程与写盘线程并发
 *  - 可恢复：异常退出后仍可从日志重放
 *  - 可控增长：分段、大小上限、按天保留清理
 */
public final class FamsMemory implements Closeable {

    public static final class MemoryConfig {
        /** 短期记忆容量（条目数） */
        public int shortTermCapacity = 4096;
        /** 短期记忆 TTL（毫秒），<=0 表示不过期 */
        public long shortTermTtlMs = 10L * 60L * 1000L;

        /** 长期记忆是否启用 */
        public boolean longTermEnabled = true;
        /** 写盘队列容量（条目数） */
        public int longTermQueueCapacity = 2048;
        /** 每次 flush 的最大间隔（毫秒） */
        public long longTermFlushIntervalMs = 1500L;
        /** 单个日志分段文件最大大小（字节） */
        public long longTermSegmentMaxBytes = 64L * 1024L * 1024L; // 64MB
        /** 索引步长：每写入 N 条记录写一条 index（越小越快检索但更占空间） */
        public int longTermIndexStride = 128;
        /** 日志保留天数（<=0 表示不自动清理） */
        public int longTermRetentionDays = 30;

        /** 当写盘队列满时是否允许同步回退写入（避免丢数据但会阻塞控制线程） */
        public boolean allowSyncFallbackWhenQueueFull = true;

        public MemoryConfig copy(){
            MemoryConfig c=new MemoryConfig();
            c.shortTermCapacity=this.shortTermCapacity;
            c.shortTermTtlMs=this.shortTermTtlMs;
            c.longTermEnabled=this.longTermEnabled;
            c.longTermQueueCapacity=this.longTermQueueCapacity;
            c.longTermFlushIntervalMs=this.longTermFlushIntervalMs;
            c.longTermSegmentMaxBytes=this.longTermSegmentMaxBytes;
            c.longTermIndexStride=this.longTermIndexStride;
            c.longTermRetentionDays=this.longTermRetentionDays;
            c.allowSyncFallbackWhenQueueFull=this.allowSyncFallbackWhenQueueFull;
            return c;
        }
    }

    /** 单条记忆记录（不可变） */
    public static final class Record {
        public final long timeMs;
        public final long step;
        public final SystemMode mode;
        public final int flags;

        public final double risk;
        public final double loss;

        public final double[] x; // 状态快照
        public final double[] u; // 动作快照
        public final String note;

        public Record(long timeMs, long step, SystemMode mode, int flags,
                      double risk, double loss,
                      double[] x, double[] u, String note){
            this.timeMs=timeMs;
            this.step=step;
            this.mode=mode;
            this.flags=flags;
            this.risk=risk;
            this.loss=loss;
            this.x=x;
            this.u=u;
            this.note=note==null? "" : note;
        }
    }

    /** 记录 flag：用于快速标注关键事件（便于检索/统计） */
    public static final class Flags {
        private Flags(){}
        public static final int NONE             = 0;
        public static final int MODE_SWITCH      = 1<<0;
        public static final int INCIDENT         = 1<<1;
        public static final int SAFETY_INTERLOCK = 1<<2;
        public static final int QUEUE_FALLBACK   = 1<<3;
        public static final int DROPPED          = 1<<4;
    }

    /** 短期记忆统计信息（便于诊断） */
    public static final class ShortTermStats {
        public long totalAdded;
        public long totalExpired;
        public long totalOverwritten;

        public ShortTermStats copy(){
            ShortTermStats s=new ShortTermStats();
            s.totalAdded=this.totalAdded;
            s.totalExpired=this.totalExpired;
            s.totalOverwritten=this.totalOverwritten;
            return s;
        }
    }

    /** 长期记忆统计信息（便于诊断） */
    public static final class LongTermStats {
        public long totalEnqueued;
        public long totalWritten;
        public long totalSyncFallback;
        public long totalQueueFull;
        public long totalDropped;
        public long totalRotations;
        public long totalFlushes;

        public LongTermStats copy(){
            LongTermStats s=new LongTermStats();
            s.totalEnqueued=this.totalEnqueued;
            s.totalWritten=this.totalWritten;
            s.totalSyncFallback=this.totalSyncFallback;
            s.totalQueueFull=this.totalQueueFull;
            s.totalDropped=this.totalDropped;
            s.totalRotations=this.totalRotations;
            s.totalFlushes=this.totalFlushes;
            return s;
        }
    }

    /** 实时短期记忆：环形缓存 + TTL */
    public static final class ShortTerm {
        private final int n,m;
        private volatile int cap;
        private volatile long ttlMs;

        private Record[] ring;
        private int head;
        private int size;

        private final ShortTermStats stats = new ShortTermStats();

        public ShortTerm(int n,int m,int capacity,long ttlMs){
            this.n=n; this.m=m;
            setCapacityInternal(capacity);
            this.ttlMs=ttlMs;
        }

        public synchronized void setCapacity(int capacity){
            if(capacity<16) capacity=16;
            if(capacity==this.cap) return;

            Record[] old=ring;
            int oldSize=size;
            List<Record> keep = new ArrayList<Record>(oldSize);
            for(int i=0;i<oldSize;i++){
                int idx = (head - oldSize + i);
                idx %= cap; if(idx<0) idx+=cap;
                Record r = old[idx];
                if(r!=null) keep.add(r);
            }

            setCapacityInternal(capacity);

            head=0; size=0;
            for(int i=0;i<keep.size();i++){
                add(keep.get(i));
            }
        }

        public synchronized void setTtlMs(long ttlMs){
            this.ttlMs=ttlMs;
            purgeExpired(System.currentTimeMillis());
        }

        public int capacity(){ return cap; }
        public long ttlMs(){ return ttlMs; }

        private void setCapacityInternal(int capacity){
            if(capacity<16) capacity=16;
            this.cap=capacity;
            this.ring=new Record[capacity];
            this.head=0;
            this.size=0;
        }

        public synchronized ShortTermStats stats(){ return stats.copy(); }

        public synchronized int size(){ return size; }

        public synchronized void add(Record r){
            if(r==null) return;
            stats.totalAdded++;

            if(size==cap){
                stats.totalOverwritten++;
            } else {
                size++;
            }

            ring[head]=r;
            head++;
            if(head>=cap) head=0;

            purgeExpired(r.timeMs);
        }

        /** 获取最近 k 条（按时间从新到旧） */
        public synchronized List<Record> recent(int k){
            if(k<=0) return Collections.emptyList();
            if(k>size) k=size;
            List<Record> out=new ArrayList<Record>(k);
            for(int i=0;i<k;i++){
                int idx = head - 1 - i;
                idx %= cap; if(idx<0) idx+=cap;
                Record r = ring[idx];
                if(r!=null) out.add(r);
            }
            return out;
        }

        /** 获取时间窗口内的记录（从旧到新） */
        public synchronized List<Record> window(long backMs){
            long now=System.currentTimeMillis();
            long start=now - Math.max(0L, backMs);
            List<Record> out=new ArrayList<Record>(size);
            for(int i=0;i<size;i++){
                int idx = (head - size + i);
                idx %= cap; if(idx<0) idx+=cap;
                Record r=ring[idx];
                if(r!=null && r.timeMs>=start) out.add(r);
            }
            return out;
        }

        /** 风险趋势斜率（近 backMs 线性回归斜率：risk/second） */
        public synchronized double riskSlope(long backMs){
            List<Record> w=window(backMs);
            int n=w.size();
            if(n<2) return 0.0;

            double t0=w.get(0).timeMs;
            double sumT=0,sumR=0,sumTT=0,sumTR=0;
            for(int i=0;i<n;i++){
                double t=(w.get(i).timeMs - t0)/1000.0;
                double r=w.get(i).risk;
                sumT += t;
                sumR += r;
                sumTT += t*t;
                sumTR += t*r;
            }
            double denom = n*sumTT - sumT*sumT;
            if(Math.abs(denom)<1e-9) return 0.0;
            return (n*sumTR - sumT*sumR)/denom;
        }

        /** 近 backMs 的平均损失 */
        public synchronized double avgLoss(long backMs){
            List<Record> w=window(backMs);
            int n=w.size();
            if(n==0) return 0.0;
            double s=0;
            for(int i=0;i<n;i++) s += w.get(i).loss;
            return s/n;
        }

        /** 近 backMs 的平均风险 */
        public synchronized double avgRisk(long backMs){
            List<Record> w=window(backMs);
            int n=w.size();
            if(n==0) return 0.0;
            double s=0;
            for(int i=0;i<n;i++) s += w.get(i).risk;
            return s/n;
        }

        private void purgeExpired(long nowMs){
            long ttl=this.ttlMs;
            if(ttl<=0) return;

            long cutoff = nowMs - ttl;
            while(size>0){
                int oldest = head - size;
                oldest %= cap; if(oldest<0) oldest+=cap;
                Record r = ring[oldest];
                if(r==null){
                    ring[oldest]=null;
                    size--;
                    continue;
                }
                if(r.timeMs >= cutoff) break;
                ring[oldest]=null;
                size--;
                stats.totalExpired++;
            }
        }
    }

    /** 长期记忆：分段日志 + 索引 + 清理 */
    public static final class LongTerm implements Closeable {
        private static final int MAGIC = 0x46414D53; // 'FAMS'
        private static final int VERSION = 1;

        private final int n,m;
        private final File dir;

        private volatile MemoryConfig cfg;
        private ArrayBlockingQueue<Record> q;

        private volatile boolean run;
        private Thread writerThread;

        private DataOutputStream out;
        private RandomAccessFile raf;
        private File segFile;
        private File idxFile;
        private long bytesWritten;
        private long recCountInSeg;

        private final LongTermStats stats = new LongTermStats();

        public LongTerm(int n,int m,File dir,MemoryConfig cfg){
            this.n=n; this.m=m;
            this.dir=dir;
            this.cfg=cfg.copy();
            ensureDir(dir);

            if(this.cfg.longTermQueueCapacity<64) this.cfg.longTermQueueCapacity=64;
            this.q=new ArrayBlockingQueue<Record>(this.cfg.longTermQueueCapacity);

            openOrRotate(true);
            cleanupOldSegments();
            startWriter();
        }

        public synchronized MemoryConfig config(){ return cfg.copy(); }

        public synchronized void reconfigure(MemoryConfig newCfg){
            if(newCfg==null) return;
            MemoryConfig c=newCfg.copy();
            if(c.longTermQueueCapacity<64) c.longTermQueueCapacity=64;

            this.cfg=c;

            ArrayBlockingQueue<Record> oldQ=this.q;
            ArrayBlockingQueue<Record> newQ=new ArrayBlockingQueue<Record>(c.longTermQueueCapacity);
            oldQ.drainTo(newQ);
            this.q=newQ;

            if(segFile!=null && bytesWritten>c.longTermSegmentMaxBytes){
                openOrRotate(false);
            }
            cleanupOldSegments();
        }

        public synchronized LongTermStats stats(){ return stats.copy(); }

        public boolean offer(Record r){
            if(r==null) return true;
            stats.totalEnqueued++;
            boolean ok=q.offer(r);
            if(!ok){
                stats.totalQueueFull++;
            }
            return ok;
        }

        public synchronized void writeSync(Record r) throws IOException {
            if(r==null) return;
            writeOne(r);
            stats.totalSyncFallback++;
        }

        private void startWriter(){
            run=true;
            writerThread=new Thread(new Runnable(){
                @Override public void run(){
                    writerLoop();
                }
            }, "FAMS-MemoryWriter");
            writerThread.setDaemon(true);
            writerThread.start();
        }

        private void writerLoop(){
            long lastFlush=System.currentTimeMillis();
            int batch=0;
            while(run){
                try{
                    Record r=q.poll(200, TimeUnit.MILLISECONDS);
                    if(r!=null){
                        synchronized(LongTerm.this){
                            writeOne(r);
                        }
                        stats.totalWritten++;
                        batch++;
                    }

                    long now=System.currentTimeMillis();
                    long flushInterval=cfg.longTermFlushIntervalMs;
                    if(flushInterval<100) flushInterval=100;

                    if(batch>0 && (now-lastFlush)>=flushInterval){
                        synchronized(LongTerm.this){
                            flushInternal();
                        }
                        stats.totalFlushes++;
                        lastFlush=now;
                        batch=0;
                    }

                }catch(InterruptedException e){
                }catch(IOException ioe){
                    try{
                        synchronized(LongTerm.this){
                            openOrRotate(false);
                        }
                    }catch(Throwable ignore){}
                }catch(Throwable t){
                }
            }

            try{
                synchronized(LongTerm.this){
                    flushInternal();
                }
            }catch(Throwable ignore){}
            try{
                synchronized(LongTerm.this){
                    closeInternal();
                }
            }catch(Throwable ignore){}
        }

        private void writeOne(Record r) throws IOException {
            if(!cfg.longTermEnabled) return;

            if(segFile==null || out==null || raf==null){
                openOrRotate(true);
            }

            // 旋转：按大小
            long max=cfg.longTermSegmentMaxBytes;
            if(max<1024*1024) max=1024*1024;
            if(bytesWritten>max){
                openOrRotate(false);
            }

            long offsetBefore = bytesWritten;

            byte modeOrd = (byte)(r.mode==null? -1 : r.mode.ordinal());
            byte[] noteBytes = r.note==null? new byte[0] : r.note.getBytes("UTF-8");

            int recordLen =
                    8 + // time
                    8 + // step
                    1 + // mode
                    4 + // flags
                    8 + // risk
                    8 + // loss
                    (8*n) + // x
                    (8*m) + // u
                    4 + // noteLen
                    noteBytes.length;

            out.writeInt(recordLen);
            out.writeLong(r.timeMs);
            out.writeLong(r.step);
            out.writeByte(modeOrd);
            out.writeInt(r.flags);
            out.writeDouble(r.risk);
            out.writeDouble(r.loss);

            // x
            for(int i=0;i<n;i++) out.writeDouble(r.x[i]);
            // u
            for(int i=0;i<m;i++) out.writeDouble(r.u[i]);

            out.writeInt(noteBytes.length);
            out.write(noteBytes);

            bytesWritten += 4L + recordLen;
            recCountInSeg++;

            // 索引
            int stride=cfg.longTermIndexStride;
            if(stride<1) stride=1;
            if(recCountInSeg%stride==0){
                appendIndex(r.timeMs, r.step, offsetBefore);
            }
        }

        private void appendIndex(long timeMs,long step,long offset) throws IOException{
            DataOutputStream idxOut=new DataOutputStream(new BufferedOutputStream(new FileOutputStream(idxFile,true)));
            try{
                idxOut.writeInt(MAGIC);
                idxOut.writeInt(VERSION);
                idxOut.writeLong(timeMs);
                idxOut.writeLong(step);
                idxOut.writeLong(offset);
            } finally {
                idxOut.close();
            }
        }

        private static void ensureDir(File dir){
            if(dir==null) return;
            if(dir.exists()){
                if(dir.isDirectory()) return;
                dir.delete();
            }
            dir.mkdirs();
        }

        private static String tsName(long timeMs){
            // YYYYMMDD_HHMMSS
            java.util.Calendar c=java.util.Calendar.getInstance(Locale.ROOT);
            c.setTimeInMillis(timeMs);
            int y=c.get(java.util.Calendar.YEAR);
            int mo=c.get(java.util.Calendar.MONTH)+1;
            int d=c.get(java.util.Calendar.DAY_OF_MONTH);
            int h=c.get(java.util.Calendar.HOUR_OF_DAY);
            int mi=c.get(java.util.Calendar.MINUTE);
            int s=c.get(java.util.Calendar.SECOND);
            return String.format(Locale.ROOT, "%04d%02d%02d_%02d%02d%02d", y,mo,d,h,mi,s);
        }

        private void openOrRotate(boolean first){
            try{
                closeInternal();
            }catch(Throwable ignore){}

            ensureDir(dir);

            long now=System.currentTimeMillis();
            String name="fams_mem_"+tsName(now)+".log";
            segFile=new File(dir, name);
            idxFile=new File(dir, name+".idx");

            try{
                raf=new RandomAccessFile(segFile, "rw");
                raf.seek(0);
                out=new DataOutputStream(new BufferedOutputStream(new FileOutputStream(raf.getFD()), 64*1024));

                // header
                out.writeInt(MAGIC);
                out.writeInt(VERSION);
                out.writeInt(n);
                out.writeInt(m);
                out.writeLong(now);

                bytesWritten = 4+4+4+4+8;
                recCountInSeg=0;
                stats.totalRotations++;

                flushInternal();

            }catch(IOException e){
                try{ closeInternal(); }catch(Throwable ignore){}
            }
        }

        private void flushInternal() throws IOException{
            if(out!=null) out.flush();
            if(raf!=null) raf.getFD().sync();
        }

        private void closeInternal() throws IOException{
            if(out!=null){
                try{ out.flush(); }catch(Throwable ignore){}
                try{ out.close(); }catch(Throwable ignore){}
                out=null;
            }
            if(raf!=null){
                try{ raf.close(); }catch(Throwable ignore){}
                raf=null;
            }
            segFile=null;
            idxFile=null;
            bytesWritten=0;
            recCountInSeg=0;
        }

        /** 清理过期分段文件 */
        public synchronized void cleanupOldSegments(){
            int days=cfg.longTermRetentionDays;
            if(days<=0) return;

            File[] segs = dir.listFiles(new FilenameFilter(){
                @Override public boolean accept(File d,String name){
                    return name!=null && name.startsWith("fams_mem_") && name.endsWith(".log");
                }
            });
            if(segs==null || segs.length==0) return;

            long now=System.currentTimeMillis();
            long cutoff = now - days*24L*60L*60L*1000L;

            for(int i=0;i<segs.length;i++){
                File f=segs[i];
                long lm=f.lastModified();
                if(lm<=0) lm=extractTimeFromNameSafe(f.getName(), f.lastModified());
                if(lm>0 && lm<cutoff){
                    try{
                        f.delete();
                        File idx=new File(dir, f.getName()+".idx");
                        if(idx.exists()) idx.delete();
                    }catch(Throwable ignore){}
                }
            }
        }

        private static long extractTimeFromNameSafe(String name,long fallback){
            try{
                // fams_mem_YYYYMMDD_HHMMSS.log
                int p=name.indexOf("fams_mem_");
                if(p<0) return fallback;
                String t=name.substring(p+"fams_mem_".length(), p+"fams_mem_".length()+15);
                String y=t.substring(0,4);
                String mo=t.substring(4,6);
                String d=t.substring(6,8);
                String h=t.substring(9,11);
                String mi=t.substring(11,13);
                String s=t.substring(13,15);
                java.util.Calendar c=java.util.Calendar.getInstance(Locale.ROOT);
                c.clear();
                c.set(Integer.parseInt(y), Integer.parseInt(mo)-1, Integer.parseInt(d),
                        Integer.parseInt(h), Integer.parseInt(mi), Integer.parseInt(s));
                return c.getTimeInMillis();
            }catch(Throwable e){
                return fallback;
            }
        }

        /** 读取最近 max 条（从旧到新） */
        public List<Record> readLast(int max) throws IOException{
            if(max<=0) return Collections.emptyList();
            List<File> segs=listSegmentsNewestFirst();
            if(segs.isEmpty()) return Collections.emptyList();

            Deque<Record> buf=new ArrayDeque<Record>(max);
            int remaining=max;

            for(int si=0;si<segs.size() && remaining>0;si++){
                File seg=segs.get(si);
                List<Record> got = readSegmentTail(seg, remaining);
                // got 是从旧到新
                for(int i=0;i<got.size();i++){
                    if(buf.size()==max) buf.removeFirst();
                    buf.addLast(got.get(i));
                }
                remaining = max - buf.size();
            }

            return new ArrayList<Record>(buf);
        }

        /** 按时间读取（从旧到新） */
        public List<Record> readFromTime(long startTimeMs, int max) throws IOException{
            if(max<=0) return Collections.emptyList();
            List<File> segs=listSegmentsOldestFirst();
            if(segs.isEmpty()) return Collections.emptyList();

            List<Record> out=new ArrayList<Record>(Math.min(max, 1024));
            for(int si=0;si<segs.size() && out.size()<max;si++){
                File seg=segs.get(si);
                readSegmentFromTime(seg, startTimeMs, max, out);
            }
            return out;
        }

        private List<File> listSegmentsOldestFirst(){
            File[] segs = dir.listFiles(new FilenameFilter(){
                @Override public boolean accept(File d,String name){
                    return name!=null && name.startsWith("fams_mem_") && name.endsWith(".log");
                }
            });
            if(segs==null || segs.length==0) return Collections.emptyList();
            List<File> list=new ArrayList<File>(segs.length);
            for(File f:segs) list.add(f);
            Collections.sort(list, new Comparator<File>(){
                @Override public int compare(File a, File b){
                    return a.getName().compareTo(b.getName());
                }
            });
            return list;
        }

        private List<File> listSegmentsNewestFirst(){
            List<File> list=listSegmentsOldestFirst();
            if(list.isEmpty()) return list;
            Collections.reverse(list);
            return list;
        }

        private List<Record> readSegmentTail(File seg, int need) throws IOException{
            // 尝试使用 idx 快速定位接近尾部
            File idx=new File(dir, seg.getName()+".idx");
            long startOffset=0;

            if(idx.exists() && idx.length()>= (4+4+8+8+8)){
                // 从 idx 末尾读取若干条 offset，向前尝试以覆盖 need
                long entrySize = 4+4+8+8+8;
                RandomAccessFile ir=new RandomAccessFile(idx, "r");
                try{
                    long entries = idx.length()/entrySize;
                    long take = Math.min(entries, Math.max(1, need/16)); // 粗略：每个索引点后大概覆盖 16 条
                    long pos = (entries - take) * entrySize;
                    if(pos<0) pos=0;
                    ir.seek(pos);

                    long lastGood=0;
                    while(ir.getFilePointer()<idx.length()){
                        int mg=ir.readInt();
                        int ver=ir.readInt();
                        if(mg!=MAGIC || ver!=VERSION){
                            // 如果 idx 损坏，放弃索引
                            lastGood=0;
                            break;
                        }
                        ir.readLong(); // time
                        ir.readLong(); // step
                        long off=ir.readLong();
                        if(off>=0) lastGood=off;
                    }
                    startOffset=lastGood;
                } finally { ir.close(); }
            }

            List<Record> out=new ArrayList<Record>(need);
            // 全段扫描（从 startOffset 开始）
            RandomAccessFile r=new RandomAccessFile(seg, "r");
            try{
                if(startOffset>0 && startOffset<r.length()) r.seek(startOffset);

                DataInputStream in=new DataInputStream(new BufferedInputStream(new FileInputStream(r.getFD()), 64*1024));
                // 若从中间开始，DataInputStream 仍可读；但 header 只在文件起始
                if(startOffset==0){
                    readHeader(in);
                } else {
                    // 从中间开始时，需要对齐到 recordLen；索引提供的是 record 起点（包含 recordLen）
                }

                Deque<Record> buf=new ArrayDeque<Record>(need);
                while(true){
                    Record rec = readOne(in);
                    if(rec==null) break;
                    if(buf.size()==need) buf.removeFirst();
                    buf.addLast(rec);
                }
                out.addAll(buf);
            } catch(EOFException eof){
                // ignore
            } finally {
                try{ r.close(); }catch(Throwable ignore){}
            }

            return out;
        }

        private void readSegmentFromTime(File seg,long startTimeMs,int max,List<Record> out) throws IOException{
            RandomAccessFile r=new RandomAccessFile(seg, "r");
            try{
                DataInputStream in=new DataInputStream(new BufferedInputStream(new FileInputStream(r.getFD()), 64*1024));
                readHeader(in);
                while(out.size()<max){
                    Record rec=readOne(in);
                    if(rec==null) break;
                    if(rec.timeMs>=startTimeMs) out.add(rec);
                }
            } catch(EOFException eof){
                // ignore
            } finally {
                try{ r.close(); }catch(Throwable ignore){}
            }
        }

        private void readHeader(DataInputStream in) throws IOException{
            int mg=in.readInt();
            int ver=in.readInt();
            if(mg!=MAGIC) throw new IOException("bad magic");
            if(ver!=VERSION) throw new IOException("bad version");
            int rn=in.readInt();
            int rm=in.readInt();
            in.readLong(); // startTime
            if(rn!=n || rm!=m) throw new IOException("shape mismatch");
        }

        private Record readOne(DataInputStream in) throws IOException{
            int len;
            try{
                len=in.readInt();
            }catch(EOFException eof){
                return null;
            }

            long timeMs=in.readLong();
            long step=in.readLong();
            int modeOrd=in.readByte();
            int flags=in.readInt();
            double risk=in.readDouble();
            double loss=in.readDouble();

            double[] x=new double[n];
            double[] u=new double[m];

            for(int i=0;i<n;i++) x[i]=in.readDouble();
            for(int i=0;i<m;i++) u[i]=in.readDouble();

            int noteLen=in.readInt();
            if(noteLen<0 || noteLen>1024*1024) throw new IOException("note too large: "+noteLen);
            byte[] b=new byte[noteLen];
            if(noteLen>0) in.readFully(b);

            SystemMode mode=null;
            if(modeOrd>=0){
                SystemMode[] ms=SystemMode.values();
                if(modeOrd<ms.length) mode=ms[modeOrd];
            }
            String note = noteLen==0 ? "" : new String(b, "UTF-8");

            // 如果 len 与实际不一致，尽量跳过差异
            int expected =
                    8+8+1+4+8+8 + (8*n) + (8*m) + 4 + noteLen;
            int diff = len - expected;
            if(diff>0){
                // 读多余字节（兼容未来扩展）
                in.skipBytes(diff);
            } else if(diff<0){
                // len 小于期望，数据损坏：抛出 EOF 终止本段
                throw new EOFException("corrupt record");
            }

            return new Record(timeMs, step, mode, flags, risk, loss, x, u, note);
        }

        @Override public void close(){
            run=false;
            if(writerThread!=null){
                try{ writerThread.interrupt(); }catch(Throwable ignore){}
                try{ writerThread.join(1500); }catch(Throwable ignore){}
            }
            try{
                synchronized(this){
                    flushInternal();
                    closeInternal();
                }
            }catch(Throwable ignore){}
        }
    }

    private final int n,m;
    private final ShortTerm shortTerm;
    private final LongTerm longTerm;
    private volatile MemoryConfig cfg;
    private final File dir;

    public FamsMemory(int n,int m,File dir){
        this(n,m,dir,new MemoryConfig());
    }

    public FamsMemory(int n,int m,File dir,MemoryConfig cfg){
        this.n=n; this.m=m;
        this.dir=dir;
        this.cfg=cfg==null? new MemoryConfig() : cfg.copy();

        shortTerm = new ShortTerm(n,m, this.cfg.shortTermCapacity, this.cfg.shortTermTtlMs);
        longTerm = new LongTerm(n,m, dir, this.cfg);
    }

    public synchronized MemoryConfig config(){ return cfg.copy(); }

    public synchronized void reconfigure(MemoryConfig cfg){
        if(cfg==null) return;
        this.cfg=cfg.copy();
        shortTerm.setCapacity(this.cfg.shortTermCapacity);
        shortTerm.setTtlMs(this.cfg.shortTermTtlMs);
        longTerm.reconfigure(this.cfg);
    }

    public ShortTerm shortTerm(){ return shortTerm; }
    public LongTerm longTerm(){ return longTerm; }
    public File dir(){ return dir; }

    /**
     * 记录一条记忆：
     *  - 始终进入短期记忆（实时）
     *  - 长期记忆：优先异步入队；队列满时视配置选择同步回退或丢弃
     */
    public void record(long timeMs, long step, SystemMode mode, int flags,
                       double risk, double loss,
                       double[] x, double[] u, String note){

        double[] xx = new double[n];
        double[] uu = new double[m];
        for(int i=0;i<n;i++) xx[i]=x[i];
        for(int i=0;i<m;i++) uu[i]=u[i];

        Record r=new Record(timeMs, step, mode, flags, risk, loss, xx, uu, note);
        shortTerm.add(r);

        if(!cfg.longTermEnabled) return;

        boolean ok = longTerm.offer(r);
        if(!ok){
            if(cfg.allowSyncFallbackWhenQueueFull){
                try{
                    longTerm.writeSync(r);
                }catch(IOException e){
                    longTerm.stats.totalDropped++;
                    shortTerm.add(new Record(timeMs, step, mode, flags|Flags.DROPPED,
                            risk, loss, xx, uu, "LONG_TERM_WRITE_FAILED:"+safeMsg(e)));
                }
            } else {
                longTerm.stats.totalDropped++;
            }
        }
    }

    private static String safeMsg(Throwable t){
        if(t==null) return "";
        String s=t.getClass().getSimpleName();
        String m=t.getMessage();
        if(m==null) m="";
        if(m.length()>200) m=m.substring(0,200);
        return s+":"+m;
    }

    @Override public void close(){
        try{ longTerm.close(); }catch(Throwable ignore){}
    }
}

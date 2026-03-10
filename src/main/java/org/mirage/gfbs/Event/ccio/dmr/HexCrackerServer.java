package org.mirage.gfbs.Event.ccio.dmr;

import net.minecraft.server.MinecraftServer;
import org.mirage.gfbs.Phenomenon.network.HexCrackerNetwork;

import java.util.Random;

public final class HexCrackerServer {
    private HexCrackerServer() {}

    private static Thread crackerThread = null;
    private static volatile boolean running = false;
    private static final Random random = new Random();
    private static int currentIntervalMs = 5000;
    private static int generatedCount = 0;

    public static void start(MinecraftServer server) {
        if (running) return;
        running = true;
        currentIntervalMs = 5000;
        generatedCount = 0;

        crackerThread = new Thread(() -> {
            while (running && DmrShutdownCodeManager.hasActiveCode()) {
                try {
                    tryCrackRandomDigits(server);
                    generatedCount++;

                    if (generatedCount % 5 == 0 && currentIntervalMs > 500) {
                        currentIntervalMs = (int)(currentIntervalMs * 0.9);
                    }

                    Thread.sleep(currentIntervalMs);
                } catch (InterruptedException e) {
                    break;
                }
            }
            running = false;
        }, "dmr-hex-cracker-server");

        crackerThread.setDaemon(true);
        crackerThread.start();
    }

    public static void stop() {
        running = false;
        if (crackerThread != null) {
            crackerThread.interrupt();
            crackerThread = null;
        }
    }

    private static void tryCrackRandomDigits(MinecraftServer server) {
        String code = DmrShutdownCodeManager.getCurrentCode();
        if (code == null) return;

        int[] cracked = DmrShutdownCodeManager.getCrackedDigits();
        boolean anyNew = false;

        int uncrackedCount = 0;
        for (int pos = 0; pos < 6; pos++) {
            if (cracked[pos] < 0) uncrackedCount++;
        }
        if (uncrackedCount == 0) return;

        int targetPos = -1;
        int attempts = 0;
        while (targetPos < 0 && attempts < 10) {
            int randomPos = random.nextInt(6);
            if (cracked[randomPos] < 0) {
                targetPos = randomPos;
            }
            attempts++;
        }
        if (targetPos < 0) return;

        int tryDigit = random.nextInt(10);

        if (random.nextDouble() < 0.85) {
            return;
        }

        int result = DmrShutdownCodeManager.tryCrackDigit(targetPos, tryDigit);
        if (result == 1) {
            anyNew = true;
        }

        if (anyNew) {
            int[] newCracked = DmrShutdownCodeManager.getCrackedDigits();
            HexCrackerNetwork.sendCrackedDigitsUpdate(server, newCracked);

            if (DmrShutdownCodeManager.isFullyCracked()) {
                stop();
            }
        }
    }

    public static boolean isRunning() {
        return running;
    }
}

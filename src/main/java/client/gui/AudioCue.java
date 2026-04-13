package client.gui;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public final class AudioCue {
    private static final float SAMPLE_RATE = 44100f;

    private AudioCue() {
    }

    public static void success() {
        play(880, 120);
    }

    public static void error() {
        play(220, 180);
    }

    public static void market() {
        play(660, 90);
    }

    private static void play(int frequency, int durationMillis) {
        Thread thread = new Thread(() -> playTone(frequency, durationMillis), "tradehouse-audio-cue");
        thread.setDaemon(true);
        thread.start();
    }

    private static void playTone(int frequency, int durationMillis) {
        AudioFormat format = new AudioFormat(SAMPLE_RATE, 8, 1, true, false);
        SourceDataLine line = null;
        try {
            line = AudioSystem.getSourceDataLine(format);
            byte[] tone = createTone(frequency, durationMillis);
            line.open(format);
            line.start();
            line.write(tone, 0, tone.length);
            line.drain();
        } catch (LineUnavailableException | IllegalArgumentException ignored) {
            // Audio is optional; the app remains usable on muted or unsupported systems.
        } finally {
            if (line != null) {
                line.close();
            }
        }
    }

    private static byte[] createTone(int frequency, int durationMillis) {
        int samples = (int) (SAMPLE_RATE * durationMillis / 1000);
        byte[] tone = new byte[samples];
        for (int i = 0; i < samples; i++) {
            double angle = 2 * Math.PI * i * frequency / SAMPLE_RATE;
            tone[i] = (byte) (Math.sin(angle) * 64);
        }
        return tone;
    }
}

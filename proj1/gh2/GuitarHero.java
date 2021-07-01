package gh2;

import edu.princeton.cs.algs4.StdAudio;
import edu.princeton.cs.algs4.StdDraw;

public class GuitarHero {
    public static final String KEYBOARD = "q2we4r5ty7u8i9op-[=zxdcfvgbnjmk,.;/' ";
    public static final int KEYS_AMOUNT = KEYBOARD.length();

    public static void main(String[] args) {
        GuitarString[] guitarStrings = new GuitarString[KEYS_AMOUNT];

        for (int i = 0; i < KEYS_AMOUNT; i++) {
            double frequency = 440 * Math.pow(2, (i - 24) / 12.0);
            guitarStrings[i] = new GuitarString(frequency);
        }

        while (true) {
            if (StdDraw.hasNextKeyTyped()) {
                char key = StdDraw.nextKeyTyped();
                int keyIndex = KEYBOARD.indexOf(key);

                if (keyIndex > 0) {
                    guitarStrings[keyIndex].pluck();
                }
            }

            double sample = 0.0;
            for (GuitarString s : guitarStrings) {
                sample += s.sample();
            }

            StdAudio.play(sample);

            for (GuitarString s : guitarStrings) {
                s.tic();
            }
        }
    }
}

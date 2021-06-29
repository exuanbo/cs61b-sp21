package gh2;

import edu.princeton.cs.algs4.StdAudio;
import edu.princeton.cs.algs4.StdDraw;

public class GuitarHero {
    private static final String keyboard = "q2we4r5ty7u8i9op-[=zxdcfvgbnjmk,.;/' ";

    public static void main(String[] args) {
        int keyAmount = keyboard.length();
        GuitarString[] guitarStrings = new GuitarString[keyAmount];

        for (int i = 0; i < keyAmount; i++) {
            double frequency = 440 * Math.pow(2, (i - 24) / 12.0);
            guitarStrings[i] = new GuitarString(frequency);
        }

        while (true) {
            if (StdDraw.hasNextKeyTyped()) {
                char key = StdDraw.nextKeyTyped();
                int keyIndex = keyboard.indexOf(key);

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

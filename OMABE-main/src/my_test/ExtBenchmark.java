package my_test;

import it.unisa.dia.gas.jpbc.Element;

import java.util.Arrays;
import java.util.Locale;

public class ExtBenchmark {

    
    private static volatile long sink;

    private static final int INPUT_COUNT = 256;
    private static final int WARMUP_CALLS = 20_000;
    private static final int BATCHES = 20;
    private static final int CALLS_PER_BATCH = 5_000;

    public static void main(String[] args) {
        int rBits = 256;
        int qBits = 1536;

        

        System.out.println("Java: " + System.getProperty("java.version"));
        System.out.println("VM: " + System.getProperty("java.vm.name"));
        System.out.println("OS: " + System.getProperty("os.name")
                + " / " + System.getProperty("os.arch"));
        System.out.println("rBits=" + rBits + ", qBits=" + qBits);

        
        EpPP pp = new EpPP(rBits, qBits);

        
        
        Element[] inputs = new Element[INPUT_COUNT];
        for (int i = 0; i < INPUT_COUNT; i++) {
            inputs[i] = pp.GT.newRandomElement().getImmutable();
        }

        System.out.println("GT encoding bytes: " + inputs[0].toBytes().length);
        System.out.println("Salt bytes: " + pp.sd.length);
        System.out.println("Warmup calls per output length: " + WARMUP_CALLS);
        System.out.println("Measured calls per output length: "
                + ((long) BATCHES * CALLS_PER_BATCH));

        
        byte[] shortOutput = pp.ext(inputs[0], 16);
        byte[] longOutput = pp.ext(inputs[0], 32);
        if (!Arrays.equals(shortOutput, Arrays.copyOf(longOutput, 16))) {
            throw new AssertionError("HKDF output-prefix check failed");
        }

        for (int len : new int[]{16, 32}) {
            benchmark(pp, inputs, len);
        }

        System.out.println("Result checksum: " + sink);
    }

    private static void benchmark(EpPP pp, Element[] inputs, int len) {
        
        runCalls(pp, inputs, len, WARMUP_CALLS, 0);

        double[] batchNsPerCall = new double[BATCHES];
        long totalNs = 0;

        for (int b = 0; b < BATCHES; b++) {
            long start = System.nanoTime();

            runCalls(pp, inputs, len, CALLS_PER_BATCH, b);

            long elapsed = System.nanoTime() - start;
            totalNs += elapsed;
            batchNsPerCall[b] = elapsed / (double) CALLS_PER_BATCH;
        }

        double meanNs = totalNs
                / (double) ((long) BATCHES * CALLS_PER_BATCH);

        double variance = 0;
        for (double value : batchNsPerCall) {
            double difference = value - meanNs;
            variance += difference * difference;
        }
        double batchSdNs = Math.sqrt(variance / (BATCHES - 1));

        double[] sorted = batchNsPerCall.clone();
        Arrays.sort(sorted);
        double medianNs = (sorted[BATCHES / 2 - 1]
                + sorted[BATCHES / 2]) / 2.0;

        System.out.printf(Locale.ROOT,
                "%nOutput length: %d bytes%n"
                + "Mean: %.3f us/call (%.6f ms/call)%n"
                + "Median batch mean: %.3f us/call%n"
                + "SD of batch means: %.3f us/call%n"
                + "Min/max batch mean: %.3f / %.3f us/call%n",
                len,
                meanNs / 1_000.0,
                meanNs / 1_000_000.0,
                medianNs / 1_000.0,
                batchSdNs / 1_000.0,
                sorted[0] / 1_000.0,
                sorted[BATCHES - 1] / 1_000.0);
    }

    private static void runCalls(
            EpPP pp, Element[] inputs, int len, int count, int offset) {

        long checksum = 0;
        int index = offset % inputs.length;

        for (int j = 0; j < count; j++) {
            byte[] output = pp.ext(inputs[index], len);

            
            for (byte value : output) {
                checksum += value & 0xff;
            }

            if (++index == inputs.length) {
                index = 0;
            }
        }

        sink = checksum;
    }
}
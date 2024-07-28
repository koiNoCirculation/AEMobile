package dev.youtiao.aemobile.web.service;

import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class MSPTDummyImpl implements MSPTService{
    AtomicInteger s1 = new AtomicInteger(0);
    AtomicInteger s2 = new AtomicInteger(0);
    AtomicInteger s3 =  new AtomicInteger(0);


    ArrayDeque<Integer> q1 = new ArrayDeque<>();
    int q1l = 0;
    ArrayDeque<Integer> q2 = new ArrayDeque<>();
    int q2l = 0;
    ArrayDeque<Integer> q3 = new ArrayDeque<>();
    int q3l = 0;

    Random random = new Random();
    public MSPTDummyImpl() {
        new Thread() {
            @Override
            public void run() {
                //5s, 30s, 300s
                try {
                    while(true) {
                        long ms = (long) (random.nextFloat() * 60);
                        Thread.sleep(ms);
                        if (q1.size() < 100) {
                            q1.add((int) ms);
                            q1l++;
                            s1.accumulateAndGet((int) ms, Integer::sum);
                        } else {
                            s1.accumulateAndGet((int) (-q1.pollFirst() + ms), Integer::sum);
                        }
                        if (q2.size() < 600) {
                            q2.add((int) ms);
                            q2l++;
                            s2.accumulateAndGet((int) ms, Integer::sum);
                        } else {
                            s2.accumulateAndGet((int) (-q2.pollFirst() + ms), Integer::sum);
                        }
                        if (q3.size() < 6000) {
                            q3.add((int) ms);
                            q3l++;
                            s3.accumulateAndGet((int) ms, Integer::sum);
                        } else {
                            s3.accumulateAndGet((int) (-q3.pollFirst() + ms), Integer::sum);
                        }
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }.start();
    }
    @Override
    public float[] getServerMSPT() {
        return new float[] {((float)s1.get()) / q1l, ((float)s2.get()) / q2l,((float)s3.get()) / q3l};
    }
}
